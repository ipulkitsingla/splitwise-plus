package com.splitwiseplusplus.service;

import com.splitwiseplusplus.dto.*;
import com.splitwiseplusplus.exception.*;
import com.splitwiseplusplus.model.*;
import com.splitwiseplusplus.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Transactional
    public GroupDTO createGroup(CreateGroupRequest request, Long creatorId) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String inviteCode = generateInviteCode();

        Group group = Group.builder()
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType() != null ? request.getType() : Group.GroupType.OTHER)
                .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                .inviteCode(inviteCode)
                .createdBy(creator)
                .build();

        group = groupRepository.save(group);

        // Add creator as admin
        GroupMember adminMember = GroupMember.builder()
                .group(group)
                .user(creator)
                .role(GroupMember.MemberRole.ADMIN)
                .status(GroupMember.MemberStatus.ACTIVE)
                .build();
        memberRepository.save(adminMember);

        // Add additional members
        if (request.getMemberIds() != null) {
            for (Long memberId : request.getMemberIds()) {
                if (!memberId.equals(creatorId)) {
                    addMemberToGroup(group, memberId, GroupMember.MemberRole.MEMBER);
                }
            }
        }

        log.info("Group '{}' created by user {}", group.getName(), creatorId);
        return mapToGroupDTO(groupRepository.findById(group.getId()).orElseThrow());
    }

    @Transactional(readOnly = true)
    public GroupDTO getGroup(Long groupId, Long userId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        if (!memberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new ForbiddenException("Not a group member");
        }
        return mapToGroupDTO(group);
    }

    @Transactional(readOnly = true)
    public List<GroupDTO> getUserGroups(Long userId) {
        return groupRepository.findGroupsByUserId(userId).stream()
                .map(this::mapToGroupDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public GroupDTO addMember(Long groupId, Long memberId, Long requesterId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        validateAdminAccess(groupId, requesterId);

        if (memberRepository.existsByGroupIdAndUserId(groupId, memberId)) {
            throw new BadRequestException("User is already a member");
        }

        addMemberToGroup(group, memberId, GroupMember.MemberRole.MEMBER);
        return mapToGroupDTO(groupRepository.findById(groupId).orElseThrow());
    }

    @Transactional
    public void removeMember(Long groupId, Long memberId, Long requesterId) {
        validateAdminAccess(groupId, requesterId);
        memberRepository.deleteByGroupIdAndUserId(groupId, memberId);
    }

    @Transactional
    public GroupDTO joinByInviteCode(String inviteCode, Long userId) {
        Group group = groupRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid invite code"));

        if (memberRepository.existsByGroupIdAndUserId(group.getId(), userId)) {
            throw new BadRequestException("Already a member of this group");
        }

        addMemberToGroup(group, userId, GroupMember.MemberRole.MEMBER);
        return mapToGroupDTO(group);
    }

    @Transactional
    public void inviteByEmail(Long groupId, String email, Long requesterId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        validateAdminAccess(groupId, requesterId);
        User requester = userRepository.findById(requesterId).orElseThrow();
        emailService.sendGroupInvite(email, requester.getName(), group.getName(), group.getInviteCode());
    }

    // ── Helpers ───────────────────────────────────────────────

    private void addMemberToGroup(Group group, Long userId, GroupMember.MemberRole role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        GroupMember member = GroupMember.builder()
                .group(group).user(user).role(role)
                .status(GroupMember.MemberStatus.ACTIVE).build();
        memberRepository.save(member);
    }

    private void validateAdminAccess(Long groupId, Long userId) {
        GroupMember member = memberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new ForbiddenException("Not a group member"));
        if (member.getRole() != GroupMember.MemberRole.ADMIN) {
            throw new ForbiddenException("Admin access required");
        }
    }

    private String generateInviteCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    public GroupDTO mapToGroupDTO(Group group) {
        List<GroupMemberDTO> memberDTOs = group.getMembers().stream()
                .map(m -> GroupMemberDTO.builder()
                        .userId(m.getUser().getId())
                        .name(m.getUser().getName())
                        .email(m.getUser().getEmail())
                        .profileImageUrl(m.getUser().getProfileImageUrl())
                        .role(m.getRole())
                        .status(m.getStatus())
                        .joinedAt(m.getJoinedAt())
                        .build())
                .collect(Collectors.toList());

        return GroupDTO.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .type(group.getType())
                .imageUrl(group.getImageUrl())
                .currency(group.getCurrency())
                .inviteCode(group.getInviteCode())
                .createdBy(AuthService.mapToUserDTO(group.getCreatedBy()))
                .members(memberDTOs)
                .memberCount(memberDTOs.size())
                .createdAt(group.getCreatedAt())
                .build();
    }
}
