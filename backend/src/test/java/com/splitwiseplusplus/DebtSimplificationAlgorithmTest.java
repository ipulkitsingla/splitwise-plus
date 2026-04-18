package com.splitwiseplusplus;

import com.splitwiseplusplus.dto.BalanceDTO;
import com.splitwiseplusplus.util.DebtSimplificationAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the core Debt Simplification Algorithm.
 */
class DebtSimplificationAlgorithmTest {

    private DebtSimplificationAlgorithm algorithm;

    @BeforeEach
    void setUp() {
        algorithm = new DebtSimplificationAlgorithm();
    }

    @Test
    @DisplayName("Equal contributors have zero net balance")
    void testEqualContributions() {
        Map<Long, BigDecimal> paid = Map.of(1L, bd("50"), 2L, bd("50"));
        Map<Long, BigDecimal> owed = Map.of(1L, bd("50"), 2L, bd("50"));

        Map<Long, BigDecimal> net = algorithm.computeNetBalances(paid.keySet(), paid, owed);
        List<BalanceDTO> simplified = algorithm.simplify(net, Map.of(1L, "Alice", 2L, "Bob"), "USD");

        assertThat(simplified).isEmpty();
    }

    @Test
    @DisplayName("Simple two-person debt")
    void testSimpleTwoPerson() {
        // Alice paid 100, Bob paid 0; both owe 50 each
        Map<Long, BigDecimal> paid = Map.of(1L, bd("100"), 2L, bd("0"));
        Map<Long, BigDecimal> owed = Map.of(1L, bd("50"),  2L, bd("50"));

        Map<Long, BigDecimal> net = algorithm.computeNetBalances(paid.keySet(), paid, owed);
        List<BalanceDTO> simplified = algorithm.simplify(net, Map.of(1L, "Alice", 2L, "Bob"), "USD");

        assertThat(simplified).hasSize(1);
        BalanceDTO txn = simplified.get(0);
        assertThat(txn.getFromUserId()).isEqualTo(2L);  // Bob pays
        assertThat(txn.getToUserId()).isEqualTo(1L);    // Alice receives
        assertThat(txn.getAmount()).isEqualByComparingTo(bd("50"));
    }

    @Test
    @DisplayName("Three-person group reduces to 2 transactions")
    void testThreePersonGroup() {
        // Alice: paid 120, owed 40
        // Bob:   paid 0,   owed 40
        // Carol: paid 0,   owed 40
        Map<Long, BigDecimal> paid = new HashMap<>();
        paid.put(1L, bd("120")); paid.put(2L, bd("0")); paid.put(3L, bd("0"));

        Map<Long, BigDecimal> owed = new HashMap<>();
        owed.put(1L, bd("40")); owed.put(2L, bd("40")); owed.put(3L, bd("40"));

        Map<Long, String> names = Map.of(1L, "Alice", 2L, "Bob", 3L, "Carol");
        Map<Long, BigDecimal> net = algorithm.computeNetBalances(paid.keySet(), paid, owed);
        List<BalanceDTO> simplified = algorithm.simplify(net, names, "USD");

        // Should reduce to: Bob -> Alice (40), Carol -> Alice (40) = 2 transactions
        assertThat(simplified).hasSize(2);

        BigDecimal totalFlow = simplified.stream()
                .map(BalanceDTO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalFlow).isEqualByComparingTo(bd("80"));
    }

    @Test
    @DisplayName("Complex group minimizes transactions")
    void testComplexGroupMinimization() {
        // 4 people: A paid 300, B paid 100, C paid 50, D paid 50 (total 500)
        // Each owes 125
        Map<Long, BigDecimal> paid = new HashMap<>();
        paid.put(1L, bd("300")); paid.put(2L, bd("100"));
        paid.put(3L, bd("50"));  paid.put(4L, bd("50"));

        Map<Long, BigDecimal> owed = new HashMap<>();
        owed.put(1L, bd("125")); owed.put(2L, bd("125"));
        owed.put(3L, bd("125")); owed.put(4L, bd("125"));

        Map<Long, String> names = Map.of(1L, "A", 2L, "B", 3L, "C", 4L, "D");
        Map<Long, BigDecimal> net = algorithm.computeNetBalances(paid.keySet(), paid, owed);
        List<BalanceDTO> simplified = algorithm.simplify(net, names, "USD");

        // Net: A=+175, B=-25, C=-75, D=-75
        // Minimum transactions: 3 (at most n-1 for n people)
        assertThat(simplified.size()).isLessThanOrEqualTo(3);

        // Verify total flow equals total debt
        BigDecimal totalFlow = simplified.stream()
                .map(BalanceDTO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalFlow).isEqualByComparingTo(bd("175")); // total owed to A
    }

    @Test
    @DisplayName("Empty group produces no transactions")
    void testEmptyGroup() {
        Map<Long, BigDecimal> net = new HashMap<>();
        List<BalanceDTO> simplified = algorithm.simplify(net, new HashMap<>(), "USD");
        assertThat(simplified).isEmpty();
    }

    private BigDecimal bd(String val) {
        return new BigDecimal(val);
    }
}
