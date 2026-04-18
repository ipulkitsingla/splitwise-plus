package com.splitwiseplusplus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceDTO {
    private Long fromUserId;
    private String fromUserName;
    private Long toUserId;
    private String toUserName;
    private BigDecimal amount;
    private String currency;
}

