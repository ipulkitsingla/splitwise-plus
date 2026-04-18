package com.splitwiseplusplus.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.splitwiseplusplus.model.Settlement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SettlementDTO {
    private Long id;
    private Long groupId;
    private UserDTO payer;
    private UserDTO receiver;
    private BigDecimal amount;
    private String currency;
    private Settlement.PaymentMethod paymentMethod;
    private String note;
    private LocalDateTime settledAt;
}

