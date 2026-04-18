package com.splitwiseplusplus.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.splitwiseplusplus.model.Group;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GroupDTO {
    private Long id;
    private String name;
    private String description;
    private Group.GroupType type;
    private String imageUrl;
    private String currency;
    private String inviteCode;
    private UserDTO createdBy;
    private List<GroupMemberDTO> members;
    private int memberCount;
    private BigDecimal totalExpenses;
    private LocalDateTime createdAt;
}

