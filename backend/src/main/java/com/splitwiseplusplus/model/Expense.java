package com.splitwiseplusplus.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Expense entity — the core domain object.
 * Supports equal, unequal, and percentage-based splits.
 * Supports multi-currency, receipt images, and recurring logic.
 */
@Entity
@Table(name = "expenses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(length = 3, nullable = false)
    @Builder.Default
    private String currency = "USD";

    /** Converted amount stored in group's base currency for balance calculations */
    @Column(name = "amount_in_base_currency", precision = 15, scale = 2)
    private BigDecimal amountInBaseCurrency;

    @Column(name = "exchange_rate", precision = 10, scale = 6)
    @Builder.Default
    private BigDecimal exchangeRate = BigDecimal.ONE;

    @Enumerated(EnumType.STRING)
    @Column(name = "split_type", nullable = false)
    @Builder.Default
    private SplitType splitType = SplitType.EQUAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Category category = Category.OTHER;

    // Explicit getters used by analytics/service layer in build environments
    // where Lombok annotation processing may be inconsistent.
    public BigDecimal getAmountInBaseCurrency() {
        return amountInBaseCurrency;
    }

    public Category getCategory() {
        return category;
    }

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(name = "receipt_image_url")
    private String receiptImageUrl;

    @Column(name = "merchant_name", length = 100)
    private String merchantName;

    @Column(name = "is_settled")
    @Builder.Default
    private boolean settled = false;

    // ── Recurring Expense Fields ───────────────────────────────
    @Column(name = "is_recurring")
    @Builder.Default
    private boolean recurring = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_type")
    private RecurrenceType recurrenceType;

    @Column(name = "recurrence_end_date")
    private LocalDate recurrenceEndDate;

    @Column(name = "next_occurrence_date")
    private LocalDate nextOccurrenceDate;

    // ── Relationships ──────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paid_by", nullable = false)
    private User paidBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<ExpenseSplit> splits = new ArrayList<>();

    // ── Audit ──────────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Enums ──────────────────────────────────────────────────
    public enum SplitType {
        EQUAL, UNEQUAL, PERCENTAGE
    }

    public enum Category {
        FOOD, TRANSPORT, ACCOMMODATION, ENTERTAINMENT, UTILITIES,
        SHOPPING, HEALTHCARE, EDUCATION, SPORTS, OTHER
    }

    public enum RecurrenceType {
        DAILY, WEEKLY, MONTHLY, YEARLY
    }
}
