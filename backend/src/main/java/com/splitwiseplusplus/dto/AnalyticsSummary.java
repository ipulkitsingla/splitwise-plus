package com.splitwiseplusplus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsSummary {
    private BigDecimal totalExpenses;
    private BigDecimal totalOwed;
    private BigDecimal totalOwe;
    private int expenseCount;
    private Map<String, BigDecimal> categoryBreakdown;
    private List<MonthlyTrend> monthlyTrends;
    private List<UserSpending> topSpenders;
}

