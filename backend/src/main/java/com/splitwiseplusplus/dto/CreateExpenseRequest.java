package com.splitwiseplusplus.dto;

import com.splitwiseplusplus.model.Expense;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateExpenseRequest {
    @NotBlank
    @Size(max = 200)
    private String description;
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;
    @Size(max = 3)
    private String currency;
    @NotNull
    private Long groupId;
    @NotNull
    private Long paidById;
    @NotNull
    private Expense.SplitType splitType;
    private Expense.Category category;
    @NotNull
    private LocalDate expenseDate;
    private String merchantName;
    private Map<Long, BigDecimal> splitData;
    private List<Long> participantIds;
    private boolean recurring;
    private Expense.RecurrenceType recurrenceType;
    private LocalDate recurrenceEndDate;
}

