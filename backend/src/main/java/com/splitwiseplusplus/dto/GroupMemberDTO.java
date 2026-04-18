package com.splitwiseplusplus.dto;

import com.splitwiseplusplus.model.GroupMember;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberDTO {
    private Long userId;
    private String name;
    private String email;
    private String profileImageUrl;
    private GroupMember.MemberRole role;
    private GroupMember.MemberStatus status;
    private LocalDateTime joinedAt;
}

