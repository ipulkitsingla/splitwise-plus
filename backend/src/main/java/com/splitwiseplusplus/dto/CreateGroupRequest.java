package com.splitwiseplusplus.dto;

import com.splitwiseplusplus.model.Group;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGroupRequest {
    @NotBlank
    @Size(min = 2, max = 100)
    private String name;
    @Size(max = 500)
    private String description;
    private Group.GroupType type;
    @Size(max = 3)
    private String currency;
    private List<Long> memberIds;
}

