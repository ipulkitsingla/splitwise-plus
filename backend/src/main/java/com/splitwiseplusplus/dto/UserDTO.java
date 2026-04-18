package com.splitwiseplusplus.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.splitwiseplusplus.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDTO {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String profileImageUrl;
    private String preferredCurrency;
    private User.Role role;
    private LocalDateTime createdAt;
}

