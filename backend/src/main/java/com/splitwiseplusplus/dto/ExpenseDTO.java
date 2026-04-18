package com.splitwiseplusplus.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.splitwiseplusplus.model.Expense;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExpenseDTO {
    private Long id;
    private String description;
    private BigDecimal amount;
    private String currency;
    private BigDecimal amountInBaseCurrency;
    private Expense.SplitType splitType;
    private Expense.Category category;
    private LocalDate expenseDate;
    private String receiptImageUrl;
    private String merchantName;
    private boolean settled;
    private boolean recurring;
    private Expense.RecurrenceType recurrenceType;
    private UserDTO paidBy;
    private Long groupId;
    private String groupName;
    private List<SplitDTO> splits;
    private LocalDateTime createdAt;
}

