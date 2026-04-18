package com.splitwiseplusplus.repository;

import com.splitwiseplusplus.model.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    Page<Expense> findByGroupIdOrderByExpenseDateDesc(Long groupId, Pageable pageable);

    List<Expense> findByGroupIdAndExpenseDateBetween(Long groupId, LocalDate start, LocalDate end);

    @Query("SELECT e FROM Expense e WHERE e.group.id = :groupId " +
           "AND (:category IS NULL OR e.category = :category) " +
           "AND (:paidById IS NULL OR e.paidBy.id = :paidById) " +
           "AND (:startDate IS NULL OR e.expenseDate >= :startDate) " +
           "AND (:endDate IS NULL OR e.expenseDate <= :endDate) " +
           "AND (:search IS NULL OR LOWER(e.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY e.expenseDate DESC")
    Page<Expense> findWithFilters(
            @Param("groupId") Long groupId,
            @Param("category") Expense.Category category,
            @Param("paidById") Long paidById,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("search") String search,
            Pageable pageable
    );

    /** Monthly spending per category for analytics */
    @Query("SELECT e.category, SUM(e.amountInBaseCurrency) FROM Expense e " +
           "WHERE e.group.id = :groupId " +
           "AND YEAR(e.expenseDate) = :year AND MONTH(e.expenseDate) = :month " +
           "GROUP BY e.category")
    List<Object[]> getCategorySpendingByMonth(
            @Param("groupId") Long groupId,
            @Param("year") int year,
            @Param("month") int month
    );

    /** Per-user spending totals for analytics */
    @Query("SELECT e.paidBy.id, e.paidBy.name, SUM(e.amountInBaseCurrency) FROM Expense e " +
           "WHERE e.group.id = :groupId " +
           "AND e.expenseDate BETWEEN :start AND :end " +
           "GROUP BY e.paidBy.id, e.paidBy.name ORDER BY SUM(e.amountInBaseCurrency) DESC")
    List<Object[]> getSpendingByUser(
            @Param("groupId") Long groupId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    /** Monthly totals for trend chart */
    @Query(value = "SELECT YEAR(expense_date) as yr, MONTH(expense_date) as mo, " +
                   "SUM(amount_in_base_currency) as total " +
                   "FROM expenses WHERE group_id = :groupId " +
                   "GROUP BY YEAR(expense_date), MONTH(expense_date) " +
                   "ORDER BY yr DESC, mo DESC LIMIT 12", nativeQuery = true)
    List<Object[]> getMonthlyTrends(@Param("groupId") Long groupId);

    /** Find recurring expenses due today or earlier */
    @Query("SELECT e FROM Expense e WHERE e.recurring = true " +
           "AND e.nextOccurrenceDate <= :today " +
           "AND (e.recurrenceEndDate IS NULL OR e.recurrenceEndDate >= :today)")
    List<Expense> findDueRecurringExpenses(@Param("today") LocalDate today);

    /** Past spending patterns for a user in a group (for smart split suggestions) */
    @Query("SELECT SUM(e.amountInBaseCurrency) FROM Expense e " +
           "WHERE e.group.id = :groupId AND e.paidBy.id = :userId " +
           "AND e.expenseDate >= :since")
    BigDecimal getTotalPaidByUserInGroup(
            @Param("groupId") Long groupId,
            @Param("userId") Long userId,
            @Param("since") LocalDate since
    );
}
