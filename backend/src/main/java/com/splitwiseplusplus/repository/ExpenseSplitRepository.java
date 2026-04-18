package com.splitwiseplusplus.repository;

import com.splitwiseplusplus.model.ExpenseSplit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ExpenseSplitRepository extends JpaRepository<ExpenseSplit, Long> {

    List<ExpenseSplit> findByExpenseId(Long expenseId);

    List<ExpenseSplit> findByUserId(Long userId);

    /** Calculate net balance between two users in a group */
    @Query("SELECT COALESCE(SUM(es.owedAmount), 0) FROM ExpenseSplit es " +
           "WHERE es.expense.group.id = :groupId " +
           "AND es.user.id = :userId " +
           "AND es.expense.paidBy.id = :creditorId " +
           "AND es.settled = false " +
           "AND es.user.id != es.expense.paidBy.id")
    BigDecimal getUnsettledAmountOwedTo(
            @Param("groupId") Long groupId,
            @Param("userId") Long userId,
            @Param("creditorId") Long creditorId
    );

    /** All unsettled splits for a user in a group */
    @Query("SELECT es FROM ExpenseSplit es " +
           "WHERE es.expense.group.id = :groupId " +
           "AND es.user.id = :userId " +
           "AND es.settled = false")
    List<ExpenseSplit> findUnsettledByGroupAndUser(
            @Param("groupId") Long groupId,
            @Param("userId") Long userId
    );

    void deleteByExpenseId(Long expenseId);
}
