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
public class GroupBalanceSummary {
    private Long groupId;
    private String groupName;
    private String currency;
    private List<BalanceDTO> simplifiedTransactions;
    private Map<Long, BigDecimal> netBalances;
    private Map<Long, String> userNames;
}

