package com.splitwiseplusplus.service;

import com.splitwiseplusplus.dto.*;
import com.splitwiseplusplus.exception.ForbiddenException;
import com.splitwiseplusplus.model.Expense;
import com.splitwiseplusplus.repository.ExpenseRepository;
import com.splitwiseplusplus.repository.ExpenseSplitRepository;
import com.splitwiseplusplus.repository.GroupMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.util.*;

/**
 * Analytics Service — provides rich spending insights for the dashboard.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitRepository splitRepository;
    private final GroupMemberRepository memberRepository;

    @Transactional(readOnly = true)
    public AnalyticsSummary getGroupAnalytics(Long groupId, Long userId,
                                               LocalDate startDate, LocalDate endDate) {
        if (!memberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new ForbiddenException("Not a group member");
        }

        if (startDate == null) startDate = LocalDate.now().minusMonths(6);
        if (endDate == null)   endDate   = LocalDate.now();

        List<Expense> expenses = expenseRepository.findByGroupIdAndExpenseDateBetween(
                groupId, startDate, endDate
        );

        // Total expenses
        BigDecimal totalExpenses = expenses.stream()
                .map(Expense::getAmountInBaseCurrency)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Category breakdown
        Map<String, BigDecimal> categoryBreakdown = new LinkedHashMap<>();
        for (Expense e : expenses) {
            categoryBreakdown.merge(
                    e.getCategory().name(),
                    e.getAmountInBaseCurrency(),
                    BigDecimal::add
            );
        }
        categoryBreakdown.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .forEachOrdered(entry -> categoryBreakdown.put(entry.getKey(), entry.getValue()));

        // What user owes vs is owed
        BigDecimal totalOwed = splitRepository.findUnsettledByGroupAndUser(groupId, userId)
                .stream().map(s -> s.getOwedAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalOwe = expenses.stream()
                .filter(e -> e.getPaidBy().getId().equals(userId))
                .flatMap(e -> e.getSplits().stream())
                .filter(s -> !s.isSettled() && !s.getUser().getId().equals(userId))
                .map(s -> s.getOwedAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Monthly trends
        List<Object[]> trendData = expenseRepository.getMonthlyTrends(groupId);
        List<MonthlyTrend> trends = new ArrayList<>();
        for (Object[] row : trendData) {
            trends.add(MonthlyTrend.builder()
                    .year(((Number) row[0]).intValue())
                    .month(((Number) row[1]).intValue())
                    .monthName(Month.of(((Number) row[1]).intValue()).name())
                    .total(new BigDecimal(row[2].toString()).setScale(2, RoundingMode.HALF_UP))
                    .build());
        }

        // Top spenders
        List<Object[]> spenderData = expenseRepository.getSpendingByUser(groupId, startDate, endDate);
        List<UserSpending> topSpenders = new ArrayList<>();
        for (Object[] row : spenderData) {
            BigDecimal paid = new BigDecimal(row[2].toString()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal pct = totalExpenses.compareTo(BigDecimal.ZERO) > 0
                    ? paid.multiply(BigDecimal.valueOf(100)).divide(totalExpenses, 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            topSpenders.add(UserSpending.builder()
                    .userId(((Number) row[0]).longValue())
                    .userName((String) row[1])
                    .totalPaid(paid)
                    .percentage(pct)
                    .build());
        }

        return AnalyticsSummary.builder()
                .totalExpenses(totalExpenses)
                .totalOwed(totalOwed)
                .totalOwe(totalOwe)
                .expenseCount(expenses.size())
                .categoryBreakdown(categoryBreakdown)
                .monthlyTrends(trends)
                .topSpenders(topSpenders)
                .build();
    }
}
