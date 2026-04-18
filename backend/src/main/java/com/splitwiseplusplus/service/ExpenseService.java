package com.splitwiseplusplus.service;

import com.splitwiseplusplus.dto.*;
import com.splitwiseplusplus.exception.BadRequestException;
import com.splitwiseplusplus.exception.ForbiddenException;
import com.splitwiseplusplus.exception.ResourceNotFoundException;
import com.splitwiseplusplus.model.*;
import com.splitwiseplusplus.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Expense Service — creates, updates, deletes expenses and handles split logic.
 *
 * Split strategies:
 * - EQUAL: divide evenly among all participants
 * - UNEQUAL: explicit amounts per user (must sum to total)
 * - PERCENTAGE: percentage per user (must sum to 100%)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitRepository splitRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final CurrencyService currencyService;
    private final FileStorageService fileStorageService;

    @Transactional
    public ExpenseDTO createExpense(CreateExpenseRequest request, Long creatorId) {
        // Validate group exists and user is a member
        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + request.getGroupId()));

        validateMembership(group.getId(), creatorId);

        User paidBy = userRepository.findById(request.getPaidById())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getPaidById()));

        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Currency conversion
        String groupCurrency = group.getCurrency() != null ? group.getCurrency() : "USD";
        String expenseCurrency = request.getCurrency() != null ? request.getCurrency() : groupCurrency;
        BigDecimal exchangeRate = currencyService.getExchangeRate(expenseCurrency, groupCurrency);
        BigDecimal amountInBase = request.getAmount().multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);

        // Build expense
        Expense expense = Expense.builder()
                .description(request.getDescription())
                .amount(request.getAmount())
                .currency(expenseCurrency)
                .amountInBaseCurrency(amountInBase)
                .exchangeRate(exchangeRate)
                .splitType(request.getSplitType())
                .category(request.getCategory() != null ? request.getCategory() : Expense.Category.OTHER)
                .expenseDate(request.getExpenseDate())
                .merchantName(request.getMerchantName())
                .group(group)
                .paidBy(paidBy)
                .createdBy(creator)
                .recurring(request.isRecurring())
                .recurrenceType(request.getRecurrenceType())
                .recurrenceEndDate(request.getRecurrenceEndDate())
                .nextOccurrenceDate(computeNextOccurrence(request))
                .build();

        expense = expenseRepository.save(expense);

        // Create splits
        List<ExpenseSplit> splits = createSplits(expense, request, group);
        splitRepository.saveAll(splits);
        expense.setSplits(splits);

        log.info("Expense created: {} in group {} by user {}", expense.getId(), group.getId(), creatorId);

        // Notify group members
        notificationService.notifyExpenseAdded(expense, creator);

        return mapToExpenseDTO(expense);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ExpenseDTO> getGroupExpenses(
            Long groupId, Long userId, int page, int size,
            Expense.Category category, Long paidById,
            LocalDate startDate, LocalDate endDate, String search
    ) {
        validateMembership(groupId, userId);

        PageRequest pageable = PageRequest.of(page, size, Sort.by("expenseDate").descending());
        Page<Expense> expensePage = expenseRepository.findWithFilters(
                groupId, category, paidById, startDate, endDate, search, pageable
        );

        List<ExpenseDTO> dtos = expensePage.getContent().stream()
                .map(this::mapToExpenseDTO)
                .collect(Collectors.toList());

        return PagedResponse.<ExpenseDTO>builder()
                .content(dtos)
                .page(page)
                .size(size)
                .totalElements(expensePage.getTotalElements())
                .totalPages(expensePage.getTotalPages())
                .last(expensePage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public ExpenseDTO getExpenseById(Long expenseId, Long userId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found: " + expenseId));
        validateMembership(expense.getGroup().getId(), userId);
        return mapToExpenseDTO(expense);
    }

    @Transactional
    public ExpenseDTO updateExpense(Long expenseId, CreateExpenseRequest request, Long userId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found: " + expenseId));

        if (!expense.getCreatedBy().getId().equals(userId)) {
            throw new ForbiddenException("Only the expense creator can update it");
        }

        expense.setDescription(request.getDescription());
        expense.setAmount(request.getAmount());
        expense.setCategory(request.getCategory());
        expense.setExpenseDate(request.getExpenseDate());
        expense.setMerchantName(request.getMerchantName());

        // Rebuild splits
        splitRepository.deleteByExpenseId(expenseId);
        List<ExpenseSplit> newSplits = createSplits(expense, request, expense.getGroup());
        splitRepository.saveAll(newSplits);
        expense.setSplits(newSplits);

        return mapToExpenseDTO(expenseRepository.save(expense));
    }

    @Transactional
    public void deleteExpense(Long expenseId, Long userId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found: " + expenseId));

        if (!expense.getCreatedBy().getId().equals(userId)) {
            throw new ForbiddenException("Only the expense creator can delete it");
        }

        splitRepository.deleteByExpenseId(expenseId);
        expenseRepository.delete(expense);
        log.info("Expense {} deleted by user {}", expenseId, userId);
    }

    @Transactional
    public ExpenseDTO attachReceipt(Long expenseId, MultipartFile file, Long userId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        validateMembership(expense.getGroup().getId(), userId);

        String fileUrl = fileStorageService.storeFile(file, "receipts");
        expense.setReceiptImageUrl(fileUrl);
        return mapToExpenseDTO(expenseRepository.save(expense));
    }

    // ── Smart Split Suggestions ───────────────────────────────

    /**
     * AI-like smart split suggestion based on past spending patterns.
     * Members who historically pay more get slightly lower suggested shares.
     */
    @Transactional(readOnly = true)
    public List<SplitSuggestion> getSplitSuggestions(Long groupId, BigDecimal totalAmount, Long userId) {
        validateMembership(groupId, userId);

        List<GroupMember> activeMembers = memberRepository.findByGroupIdAndStatus(
                groupId, GroupMember.MemberStatus.ACTIVE
        );

        LocalDate since = LocalDate.now().minusMonths(3);
        Map<Long, BigDecimal> pastContributions = new HashMap<>();
        BigDecimal totalContributions = BigDecimal.ZERO;

        for (GroupMember member : activeMembers) {
            BigDecimal paid = expenseRepository.getTotalPaidByUserInGroup(
                    groupId, member.getUser().getId(), since
            );
            if (paid == null) paid = BigDecimal.ZERO;
            pastContributions.put(member.getUser().getId(), paid);
            totalContributions = totalContributions.add(paid);
        }

        int memberCount = activeMembers.size();
        BigDecimal equalShare = totalAmount.divide(BigDecimal.valueOf(memberCount), 2, RoundingMode.HALF_UP);

        List<SplitSuggestion> suggestions = new ArrayList<>();
        for (GroupMember member : activeMembers) {
            Long uid = member.getUser().getId();
            BigDecimal contribution = pastContributions.get(uid);

            BigDecimal suggested;
            String reason;

            if (totalContributions.compareTo(BigDecimal.ZERO) == 0) {
                suggested = equalShare;
                reason = "Equal split (no history)";
            } else {
                // Inversely weight by past contributions: heavy payers get small shares
                BigDecimal weight = BigDecimal.ONE.subtract(
                        contribution.divide(totalContributions.multiply(BigDecimal.valueOf(2)), 4, RoundingMode.HALF_UP)
                );
                BigDecimal rawSuggested = totalAmount.multiply(weight)
                        .divide(BigDecimal.valueOf(memberCount - 0.5), 2, RoundingMode.HALF_UP);
                suggested = rawSuggested.max(equalShare.multiply(new BigDecimal("0.5")));
                reason = "Based on past contributions";
            }

            suggestions.add(SplitSuggestion.builder()
                    .userId(uid)
                    .userName(member.getUser().getName())
                    .suggestedAmount(suggested)
                    .percentage(suggested.multiply(BigDecimal.valueOf(100))
                            .divide(totalAmount, 1, RoundingMode.HALF_UP))
                    .reason(reason)
                    .build());
        }

        // Normalize to ensure total matches
        BigDecimal suggestedTotal = suggestions.stream()
                .map(SplitSuggestion::getSuggestedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal diff = totalAmount.subtract(suggestedTotal);
        if (!diff.equals(BigDecimal.ZERO) && !suggestions.isEmpty()) {
            SplitSuggestion last = suggestions.get(suggestions.size() - 1);
            last.setSuggestedAmount(last.getSuggestedAmount().add(diff));
        }

        return suggestions;
    }

    // ── Split Creation ────────────────────────────────────────

    private List<ExpenseSplit> createSplits(Expense expense, CreateExpenseRequest request, Group group) {
        return switch (expense.getSplitType()) {
            case EQUAL  -> createEqualSplits(expense, request, group);
            case UNEQUAL -> createUnequalSplits(expense, request);
            case PERCENTAGE -> createPercentageSplits(expense, request);
        };
    }

    private List<ExpenseSplit> createEqualSplits(Expense expense, CreateExpenseRequest request, Group group) {
        List<Long> participantIds = request.getParticipantIds();

        if (participantIds == null || participantIds.isEmpty()) {
            // Default to all active group members
            participantIds = memberRepository
                    .findByGroupIdAndStatus(group.getId(), GroupMember.MemberStatus.ACTIVE)
                    .stream()
                    .map(m -> m.getUser().getId())
                    .collect(Collectors.toList());
        }

        int count = participantIds.size();
        BigDecimal share = expense.getAmountInBaseCurrency()
                .divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
        BigDecimal remainder = expense.getAmountInBaseCurrency()
                .subtract(share.multiply(BigDecimal.valueOf(count)));

        List<ExpenseSplit> splits = new ArrayList<>();
        for (int i = 0; i < participantIds.size(); i++) {
            Long uid = participantIds.get(i);
            User user = userRepository.findById(uid)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + uid));
            BigDecimal amount = (i == participantIds.size() - 1) ? share.add(remainder) : share;

            splits.add(ExpenseSplit.builder()
                    .expense(expense)
                    .user(user)
                    .owedAmount(amount)
                    .percentage(new BigDecimal("100").divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP))
                    .build());
        }
        return splits;
    }

    private List<ExpenseSplit> createUnequalSplits(Expense expense, CreateExpenseRequest request) {
        if (request.getSplitData() == null || request.getSplitData().isEmpty()) {
            throw new BadRequestException("Split data required for UNEQUAL split");
        }

        BigDecimal total = request.getSplitData().values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (total.compareTo(expense.getAmount()) != 0) {
            throw new BadRequestException(
                    String.format("Split amounts (%.2f) must sum to total (%.2f)", total, expense.getAmount()));
        }

        BigDecimal exchangeRate = expense.getExchangeRate();
        List<ExpenseSplit> splits = new ArrayList<>();

        for (Map.Entry<Long, BigDecimal> entry : request.getSplitData().entrySet()) {
            User user = userRepository.findById(entry.getKey())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + entry.getKey()));

            BigDecimal owedInBase = entry.getValue().multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);
            splits.add(ExpenseSplit.builder()
                    .expense(expense)
                    .user(user)
                    .owedAmount(owedInBase)
                    .build());
        }
        return splits;
    }

    private List<ExpenseSplit> createPercentageSplits(Expense expense, CreateExpenseRequest request) {
        if (request.getSplitData() == null || request.getSplitData().isEmpty()) {
            throw new BadRequestException("Split data (percentages) required for PERCENTAGE split");
        }

        BigDecimal totalPct = request.getSplitData().values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPct.compareTo(new BigDecimal("100")) != 0) {
            throw new BadRequestException(
                    String.format("Percentages must sum to 100 (got %.2f)", totalPct));
        }

        List<ExpenseSplit> splits = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : request.getSplitData().entrySet()) {
            User user = userRepository.findById(entry.getKey())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + entry.getKey()));

            BigDecimal owedAmount = expense.getAmountInBaseCurrency()
                    .multiply(entry.getValue())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            splits.add(ExpenseSplit.builder()
                    .expense(expense)
                    .user(user)
                    .owedAmount(owedAmount)
                    .percentage(entry.getValue())
                    .build());
        }
        return splits;
    }

    // ── Mapping ───────────────────────────────────────────────

    public ExpenseDTO mapToExpenseDTO(Expense expense) {
        List<SplitDTO> splitDTOs = expense.getSplits().stream()
                .map(s -> SplitDTO.builder()
                        .userId(s.getUser().getId())
                        .userName(s.getUser().getName())
                        .owedAmount(s.getOwedAmount())
                        .percentage(s.getPercentage())
                        .settled(s.isSettled())
                        .build())
                .collect(Collectors.toList());

        return ExpenseDTO.builder()
                .id(expense.getId())
                .description(expense.getDescription())
                .amount(expense.getAmount())
                .currency(expense.getCurrency())
                .amountInBaseCurrency(expense.getAmountInBaseCurrency())
                .splitType(expense.getSplitType())
                .category(expense.getCategory())
                .expenseDate(expense.getExpenseDate())
                .receiptImageUrl(expense.getReceiptImageUrl())
                .merchantName(expense.getMerchantName())
                .settled(expense.isSettled())
                .recurring(expense.isRecurring())
                .recurrenceType(expense.getRecurrenceType())
                .paidBy(AuthService.mapToUserDTO(expense.getPaidBy()))
                .groupId(expense.getGroup().getId())
                .groupName(expense.getGroup().getName())
                .splits(splitDTOs)
                .createdAt(expense.getCreatedAt())
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────

    private void validateMembership(Long groupId, Long userId) {
        if (!memberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new ForbiddenException("User is not a member of this group");
        }
    }

    private LocalDate computeNextOccurrence(CreateExpenseRequest request) {
        if (!request.isRecurring() || request.getRecurrenceType() == null) return null;
        return switch (request.getRecurrenceType()) {
            case DAILY   -> request.getExpenseDate().plusDays(1);
            case WEEKLY  -> request.getExpenseDate().plusWeeks(1);
            case MONTHLY -> request.getExpenseDate().plusMonths(1);
            case YEARLY  -> request.getExpenseDate().plusYears(1);
        };
    }
}
