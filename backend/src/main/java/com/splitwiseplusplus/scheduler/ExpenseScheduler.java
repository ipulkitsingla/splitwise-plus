package com.splitwiseplusplus.scheduler;

import com.splitwiseplusplus.model.*;
import com.splitwiseplusplus.repository.*;
import com.splitwiseplusplus.service.*;
import com.splitwiseplusplus.util.DebtSimplificationAlgorithm;
import com.splitwiseplusplus.dto.BalanceDTO;
import com.splitwiseplusplus.dto.GroupBalanceSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Scheduled Tasks:
 * - Process recurring expenses daily at midnight
 * - Send payment reminders every Monday at 9am
 * - Send monthly reports on the 1st of each month at 8am
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExpenseScheduler {

    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitRepository splitRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AnalyticsService analyticsService;
    private final EmailService emailService;
    private final SettlementService settlementService;

    /**
     * Process all recurring expenses due today.
     * Runs every day at 00:05 AM.
     */
    @Scheduled(cron = "0 5 0 * * *")
    @Transactional
    public void processRecurringExpenses() {
        LocalDate today = LocalDate.now();
        List<Expense> dueExpenses = expenseRepository.findDueRecurringExpenses(today);

        log.info("Processing {} recurring expenses due on {}", dueExpenses.size(), today);

        for (Expense template : dueExpenses) {
            try {
                // Clone the expense for this period
                Expense newExpense = Expense.builder()
                        .description(template.getDescription())
                        .amount(template.getAmount())
                        .currency(template.getCurrency())
                        .amountInBaseCurrency(template.getAmountInBaseCurrency())
                        .exchangeRate(template.getExchangeRate())
                        .splitType(template.getSplitType())
                        .category(template.getCategory())
                        .expenseDate(today)
                        .group(template.getGroup())
                        .paidBy(template.getPaidBy())
                        .createdBy(template.getPaidBy())
                        .recurring(false) // cloned instance is not recurring
                        .merchantName(template.getMerchantName())
                        .build();

                expenseRepository.save(newExpense);

                // Clone splits
                List<ExpenseSplit> clonedSplits = template.getSplits().stream()
                        .map(s -> ExpenseSplit.builder()
                                .expense(newExpense)
                                .user(s.getUser())
                                .owedAmount(s.getOwedAmount())
                                .percentage(s.getPercentage())
                                .build())
                        .collect(Collectors.toList());
                splitRepository.saveAll(clonedSplits);

                // Advance next occurrence date on template
                LocalDate next = advanceDate(today, template.getRecurrenceType());
                template.setNextOccurrenceDate(next);
                if (template.getRecurrenceEndDate() != null && next.isAfter(template.getRecurrenceEndDate())) {
                    template.setRecurring(false);
                }
                expenseRepository.save(template);

                log.info("Recurring expense {} cloned for group {}", template.getId(), template.getGroup().getId());
                notificationService.notifyExpenseAdded(newExpense, template.getPaidBy());

            } catch (Exception e) {
                log.error("Failed to process recurring expense {}: {}", template.getId(), e.getMessage());
            }
        }
    }

    /**
     * Send payment reminders to users with outstanding debts.
     * Runs every Monday at 09:00 AM.
     */
    @Scheduled(cron = "0 0 9 * * MON")
    public void sendPaymentReminders() {
        log.info("Sending weekly payment reminders");

        List<Group> allGroups = groupRepository.findAll();

        for (Group group : allGroups) {
            try {
                GroupBalanceSummary summary = settlementService.getGroupBalances(
                        group.getId(),
                        group.getCreatedBy().getId()
                );

                for (BalanceDTO balance : summary.getSimplifiedTransactions()) {
                    User debtor = userRepository.findById(balance.getFromUserId()).orElse(null);
                    User creditor = userRepository.findById(balance.getToUserId()).orElse(null);

                    if (debtor != null && creditor != null &&
                            balance.getAmount().compareTo(new BigDecimal("0.50")) > 0) {
                        notificationService.sendPaymentReminder(
                                debtor, creditor,
                                balance.getAmount().doubleValue(),
                                balance.getCurrency(),
                                group.getName()
                        );
                    }
                }
            } catch (Exception e) {
                log.warn("Payment reminder failed for group {}: {}", group.getId(), e.getMessage());
            }
        }
    }

    /**
     * Send monthly expense reports to all group members.
     * Runs on the 1st of every month at 08:00 AM.
     */
    @Scheduled(cron = "0 0 8 1 * *")
    public void sendMonthlyReports() {
        log.info("Sending monthly expense reports");

        LocalDate lastMonth = LocalDate.now().minusMonths(1);
        LocalDate start = lastMonth.withDayOfMonth(1);
        LocalDate end   = lastMonth.withDayOfMonth(lastMonth.lengthOfMonth());

        List<Group> allGroups = groupRepository.findAll();

        for (Group group : allGroups) {
            List<GroupMember> members = memberRepository.findByGroupIdAndStatus(
                    group.getId(), GroupMember.MemberStatus.ACTIVE);

            for (GroupMember member : members) {
                User user = member.getUser();
                if (!user.isEmailNotificationsEnabled()) continue;

                try {
                    var analytics = analyticsService.getGroupAnalytics(
                            group.getId(), user.getId(), start, end);

                    if (analytics.getExpenseCount() == 0) continue;

                    Map<String, BigDecimal> catBreakdown = analytics.getCategoryBreakdown();
                    emailService.sendMonthlyReport(
                            user.getEmail(),
                            user.getName(),
                            group.getName(),
                            catBreakdown,
                            analytics.getTotalExpenses(),
                            analytics.getTotalOwed()
                    );
                } catch (Exception e) {
                    log.warn("Monthly report failed for user {} group {}: {}",
                            user.getId(), group.getId(), e.getMessage());
                }
            }
        }

        log.info("Monthly reports dispatched for {} groups", allGroups.size());
    }

    // ── Helpers ───────────────────────────────────────────────

    private LocalDate advanceDate(LocalDate from, Expense.RecurrenceType type) {
        if (type == null) return from.plusMonths(1);
        return switch (type) {
            case DAILY   -> from.plusDays(1);
            case WEEKLY  -> from.plusWeeks(1);
            case MONTHLY -> from.plusMonths(1);
            case YEARLY  -> from.plusYears(1);
        };
    }
}
