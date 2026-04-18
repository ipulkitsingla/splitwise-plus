package com.splitwiseplusplus.repository;

import com.splitwiseplusplus.model.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    List<Settlement> findByGroupIdOrderBySettledAtDesc(Long groupId);

    List<Settlement> findByPayerIdOrReceiverIdOrderBySettledAtDesc(Long payerId, Long receiverId);

    @Query("SELECT COALESCE(SUM(s.amount), 0) FROM Settlement s " +
           "WHERE s.group.id = :groupId " +
           "AND s.payer.id = :payerId AND s.receiver.id = :receiverId")
    BigDecimal getTotalSettled(
            @Param("groupId") Long groupId,
            @Param("payerId") Long payerId,
            @Param("receiverId") Long receiverId
    );
}
