package com.splitwiseplusplus.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {
    @Size(min = 2, max = 100)
    private String name;
    private String phone;
    @Size(max = 3)
    private String preferredCurrency;
    private boolean pushNotificationsEnabled;
    private boolean emailNotificationsEnabled;
}

