package com.splitwiseplusplus.service;

import com.splitwiseplusplus.dto.*;
import com.splitwiseplusplus.exception.ForbiddenException;
import com.splitwiseplusplus.exception.ResourceNotFoundException;
import com.splitwiseplusplus.model.*;
import com.splitwiseplusplus.repository.*;
import com.splitwiseplusplus.util.DebtSimplificationAlgorithm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Settlement Service — records payments and computes group balances.
 * Integrates DebtSimplificationAlgorithm to minimize transactions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final ExpenseSplitRepository splitRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final DebtSimplificationAlgorithm debtAlgorithm;
    private final CurrencyService currencyService;

    /**
     * Record a settlement payment.
     */
    @Transactional
    public SettlementDTO settle(CreateSettlementRequest request, Long payerId) {
        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        if (!memberRepository.existsByGroupIdAndUserId(group.getId(), payerId)) {
            throw new ForbiddenException("You are not a member of this group");
        }

        User payer    = userRepository.findById(payerId)
                .orElseThrow(() -> new ResourceNotFoundException("Payer not found"));
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found"));

        String groupCurrency = group.getCurrency() != null ? group.getCurrency() : "USD";
        String reqCurrency   = request.getCurrency() != null ? request.getCurrency() : groupCurrency;
        BigDecimal amount    = request.getAmount();

        if (!reqCurrency.equals(groupCurrency)) {
            BigDecimal rate = currencyService.getExchangeRate(reqCurrency, groupCurrency);
            amount = amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        }

        Settlement settlement = Settlement.builder()
                .group(group)
                .payer(payer)
                .receiver(receiver)
                .amount(amount)
                .currency(groupCurrency)
                .paymentMethod(request.getPaymentMethod())
                .note(request.getNote())
                .build();

        settlementRepository.save(settlement);
        log.info("Settlement recorded: {} -> {} for {} {}", payerId, receiver.getId(), amount, groupCurrency);

        // Mark related expense splits as settled
        markSplitsAsSettled(group.getId(), payerId, receiver.getId(), amount);

        // Notify receiver
        notificationService.notifyPaymentMade(settlement);

        return mapToSettlementDTO(settlement);
    }

    /**
     * Get simplified group balances using the debt minimization algorithm.
     */
    @Transactional(readOnly = true)
    public GroupBalanceSummary getGroupBalances(Long groupId, Long userId) {
        if (!memberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new ForbiddenException("You are not a member of this group");
        }

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        List<GroupMember> members = memberRepository.findByGroupIdAndStatus(
                groupId, GroupMember.MemberStatus.ACTIVE
        );

        Set<Long> memberIds = members.stream()
                .map(m -> m.getUser().getId())
                .collect(Collectors.toSet());

        Map<Long, String> userNames = members.stream()
                .collect(Collectors.toMap(
                        m -> m.getUser().getId(),
                        m -> m.getUser().getName()
                ));

        // Compute how much each person paid
        Map<Long, BigDecimal> totalPaid = new HashMap<>();
        // Compute how much each person owes
        Map<Long, BigDecimal> totalOwed = new HashMap<>();

        for (Long memberId : memberIds) {
            totalPaid.put(memberId, BigDecimal.ZERO);
            totalOwed.put(memberId, BigDecimal.ZERO);
        }

        // Sum paid amounts from expenses
        group.getExpenses().forEach(expense -> {
            Long payerId2 = expense.getPaidBy().getId();
            totalPaid.merge(payerId2, expense.getAmountInBaseCurrency(), BigDecimal::add);

            expense.getSplits().forEach(split -> {
                if (!split.isSettled()) {
                    totalOwed.merge(split.getUser().getId(), split.getOwedAmount(), BigDecimal::add);
                }
            });
        });

        // Subtract settlements
        settlementRepository.findByGroupIdOrderBySettledAtDesc(groupId).forEach(s -> {
            totalPaid.merge(s.getPayer().getId(), s.getAmount(), BigDecimal::add);
            totalOwed.merge(s.getReceiver().getId(), s.getAmount(), BigDecimal::add);
        });

        Map<Long, BigDecimal> netBalances = debtAlgorithm.computeNetBalances(
                memberIds, totalPaid, totalOwed
        );

        String currency = group.getCurrency() != null ? group.getCurrency() : "USD";
        List<BalanceDTO> simplified = debtAlgorithm.simplify(netBalances, userNames, currency);

        return GroupBalanceSummary.builder()
                .groupId(groupId)
                .groupName(group.getName())
                .currency(currency)
                .simplifiedTransactions(simplified)
                .netBalances(netBalances)
                .userNames(userNames)
                .build();
    }

    @Transactional(readOnly = true)
    public List<SettlementDTO> getGroupSettlements(Long groupId, Long userId) {
        if (!memberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new ForbiddenException("You are not a member of this group");
        }
        return settlementRepository.findByGroupIdOrderBySettledAtDesc(groupId)
                .stream().map(this::mapToSettlementDTO).collect(Collectors.toList());
    }

    // ── Helpers ───────────────────────────────────────────────

    private void markSplitsAsSettled(Long groupId, Long payerId, Long receiverId, BigDecimal amount) {
        List<ExpenseSplit> unsettled = splitRepository.findUnsettledByGroupAndUser(groupId, payerId);
        BigDecimal remaining = amount;

        for (ExpenseSplit split : unsettled) {
            if (split.getExpense().getPaidBy().getId().equals(receiverId)) {
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
                if (remaining.compareTo(split.getOwedAmount()) >= 0) {
                    split.setSettled(true);
                    remaining = remaining.subtract(split.getOwedAmount());
                }
            }
        }
        splitRepository.saveAll(unsettled);
    }

    private SettlementDTO mapToSettlementDTO(Settlement s) {
        return SettlementDTO.builder()
                .id(s.getId())
                .groupId(s.getGroup().getId())
                .payer(AuthService.mapToUserDTO(s.getPayer()))
                .receiver(AuthService.mapToUserDTO(s.getReceiver()))
                .amount(s.getAmount())
                .currency(s.getCurrency())
                .paymentMethod(s.getPaymentMethod())
                .note(s.getNote())
                .settledAt(s.getSettledAt())
                .build();
    }
}
