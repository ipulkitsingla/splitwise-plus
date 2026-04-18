package com.splitwiseplusplus.repository;

import com.splitwiseplusplus.model.Group;
import com.splitwiseplusplus.model.GroupMember;
import com.splitwiseplusplus.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    @Query("SELECT g FROM Group g JOIN g.members m WHERE m.user.id = :userId AND m.status = 'ACTIVE'")
    List<Group> findGroupsByUserId(@Param("userId") Long userId);

    Optional<Group> findByInviteCode(String inviteCode);

    boolean existsByNameAndCreatedById(String name, Long createdById);
}
