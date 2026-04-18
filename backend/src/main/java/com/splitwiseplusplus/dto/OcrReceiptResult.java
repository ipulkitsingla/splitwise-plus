package com.splitwiseplusplus.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OcrReceiptResult {
    private BigDecimal extractedAmount;
    private String merchantName;
    private LocalDate date;
    private String rawText;
    private double confidence;
}

