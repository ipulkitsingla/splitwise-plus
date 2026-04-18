package com.splitwiseplusplus.util;

import com.splitwiseplusplus.dto.BalanceDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Debt Simplification Algorithm
 *
 * Uses a greedy approach to minimize the number of transactions needed
 * to settle all debts within a group.
 *
 * Algorithm:
 * 1. Compute net balance for each person (positive = owed money, negative = owes money)
 * 2. Repeatedly match the highest creditor with the highest debtor
 * 3. Settle the minimum of the two amounts, carry over the remainder
 *
 * Complexity: O(n log n) where n = number of members
 * Reduces transactions from O(n²) to at most O(n-1)
 */
@Component
@Slf4j
public class DebtSimplificationAlgorithm {

    private static final BigDecimal EPSILON = new BigDecimal("0.01");

    /**
     * Simplify debts given a map of net balances per user.
     *
     * @param netBalances Map of userId -> netAmount
     *                    Positive: user is owed money (creditor)
     *                    Negative: user owes money (debtor)
     * @param userNames   Map of userId -> userName for DTO population
     * @param currency    Currency for the transactions
     * @return List of simplified payment transactions
     */
    public List<BalanceDTO> simplify(
            Map<Long, BigDecimal> netBalances,
            Map<Long, String> userNames,
            String currency
    ) {
        List<BalanceDTO> transactions = new ArrayList<>();

        // Separate creditors (net > 0) and debtors (net < 0)
        // Use priority queues for O(log n) max extraction
        PriorityQueue<long[]> creditors = new PriorityQueue<>(
                (a, b) -> Double.compare(b[1], a[1]) // max-heap by amount
        );
        PriorityQueue<long[]> debtors = new PriorityQueue<>(
                (a, b) -> Double.compare(a[1], b[1]) // min-heap (most negative first)
        );

        for (Map.Entry<Long, BigDecimal> entry : netBalances.entrySet()) {
            long userId = entry.getKey();
            long amountCents = entry.getValue()
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValue();

            if (amountCents > 0) {
                creditors.offer(new long[]{userId, amountCents});
            } else if (amountCents < 0) {
                debtors.offer(new long[]{userId, amountCents});
            }
        }

        // Greedy matching
        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            long[] creditor = creditors.poll();
            long[] debtor   = debtors.poll();

            long creditorId = creditor[0];
            long debtorId   = debtor[0];
            long credit     =  creditor[1];  // positive
            long debt       = -debtor[1];    // positive magnitude

            long settleAmount = Math.min(credit, debt);

            if (settleAmount > 0) {
                BigDecimal amount = BigDecimal.valueOf(settleAmount)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                transactions.add(BalanceDTO.builder()
                        .fromUserId(debtorId)
                        .fromUserName(userNames.getOrDefault(debtorId, "User " + debtorId))
                        .toUserId(creditorId)
                        .toUserName(userNames.getOrDefault(creditorId, "User " + creditorId))
                        .amount(amount)
                        .currency(currency)
                        .build());
            }

            // Carry over remainder
            long remainingCredit = credit - settleAmount;
            long remainingDebt   = debt  - settleAmount;

            if (remainingCredit > 1) { // > 1 cent to avoid float noise
                creditors.offer(new long[]{creditorId, remainingCredit});
            }
            if (remainingDebt > 1) {
                debtors.offer(new long[]{debtorId, -remainingDebt});
            }
        }

        log.debug("Debt simplification: reduced to {} transactions", transactions.size());
        return transactions;
    }

    /**
     * Compute net balances for all members of a group.
     *
     * @param memberIds      All member IDs in the group
     * @param expensesPaid   Map of userId -> total amount paid
     * @param expensesOwed   Map of userId -> total amount owed
     * @return Net balance per user
     */
    public Map<Long, BigDecimal> computeNetBalances(
            Set<Long> memberIds,
            Map<Long, BigDecimal> expensesPaid,
            Map<Long, BigDecimal> expensesOwed
    ) {
        Map<Long, BigDecimal> netBalances = new HashMap<>();

        for (Long userId : memberIds) {
            BigDecimal paid = expensesPaid.getOrDefault(userId, BigDecimal.ZERO);
            BigDecimal owed = expensesOwed.getOrDefault(userId, BigDecimal.ZERO);
            // Net = paid - owed: positive means others owe them, negative means they owe others
            netBalances.put(userId, paid.subtract(owed).setScale(2, RoundingMode.HALF_UP));
        }

        return netBalances;
    }
}
