package com.splitwiseplusplus.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * ExpenseSplit — records how much each participant owes for a given expense.
 */
@Entity
@Table(name = "expense_splits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseSplit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Owed share of the expense */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal owedAmount;

    /** For percentage splits */
    @Column(precision = 5, scale = 2)
    private BigDecimal percentage;

    @Column(name = "is_settled")
    @Builder.Default
    private boolean settled = false;
}
