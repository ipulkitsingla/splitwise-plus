package com.splitwiseplusplus.dto;

import com.splitwiseplusplus.model.Settlement;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSettlementRequest {
    @NotNull
    private Long groupId;
    @NotNull
    private Long receiverId;
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;
    @Size(max = 3)
    private String currency;
    private Settlement.PaymentMethod paymentMethod;
    @Size(max = 200)
    private String note;
}

