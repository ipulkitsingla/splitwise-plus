package com.splitwiseplusplus.dto;

// Re-export all DTOs with public visibility for use across the application.
// This file contains all concrete DTO classes used by controllers and services.

import com.fasterxml.jackson.annotation.JsonInclude;
import com.splitwiseplusplus.model.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// ── Auth ──────────────────────────────────────────────────────

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RegisterRequest {
    @NotBlank @Size(min = 2, max = 100) private String name;
    @NotBlank @Email                    private String email;
    @NotBlank @Size(min = 8, max = 100) private String password;
    private String phone;
    @Size(max = 3) private String preferredCurrency;
}

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LoginRequest {
    @NotBlank @Email    private String email;
    @NotBlank           private String password;
}

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    @Builder.Default private String tokenType = "Bearer";
    private long expiresIn;
    private UserDTO user;
}

// ── User ──────────────────────────────────────────────────────

@Data @Builder @NoArgsConstructor @AllArgsConstructor
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

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UpdateProfileRequest {
    @Size(min = 2, max = 100) private String name;
    private String phone;
    @Size(max = 3) private String preferredCurrency;
    private boolean pushNotificationsEnabled;
    private boolean emailNotificationsEnabled;
}

// ── Group ─────────────────────────────────────────────────────

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateGroupRequest {
    @NotBlank @Size(min = 2, max = 100) private String name;
    @Size(max = 500) private String description;
    private Group.GroupType type;
    @Size(max = 3) private String currency;
    private List<Long> memberIds;
}

@Data @Builder @NoArgsConstructor @AllArgsConstructor
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

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GroupMemberDTO {
    private Long userId;
    private String name;
    private String email;
    private String profileImageUrl;
    private GroupMember.MemberRole role;
    private GroupMember.MemberStatus status;
    private LocalDateTime joinedAt;
}

// ── Expense ───────────────────────────────────────────────────

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateExpenseRequest {
    @NotBlank @Size(max = 200) private String description;
    @NotNull @DecimalMin("0.01") private BigDecimal amount;
    @Size(max = 3) private String currency;
    @NotNull private Long groupId;
    @NotNull private Long paidById;
    @NotNull private Expense.SplitType splitType;
    private Expense.Category category;
    @NotNull private LocalDate expenseDate;
    private String merchantName;
    private Map<Long, BigDecimal> splitData;
    private List<Long> participantIds;
    private boolean recurring;
    private Expense.RecurrenceType recurrenceType;
    private LocalDate recurrenceEndDate;
}

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExpenseDTO {
    private Long id;
    private String description;
    private BigDecimal amount;
    private String currency;
    private BigDecimal amountInBaseCurrency;
    private Expense.SplitType splitType;
    private Expense.Category category;
    private LocalDate expenseDate;
    private String receiptImageUrl;
    private String merchantName;
    private boolean settled;
    private boolean recurring;
    private Expense.RecurrenceType recurrenceType;
    private UserDTO paidBy;
    private Long groupId;
    private String groupName;
    private List<SplitDTO> splits;
    private LocalDateTime createdAt;
}

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SplitDTO {
    private Long userId;
    private String userName;
    private BigDecimal owedAmount;
    private BigDecimal percentage;
    private boolean settled;
}

// ── Settlement ───────────────────────────────────────────────

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateSettlementRequest {
    @NotNull private Long groupId;
    @NotNull private Long receiverId;
    @NotNull @DecimalMin("0.01") private BigDecimal amount;
    @Size(max = 3) private String currency;
    private Settlement.PaymentMethod paymentMethod;
    @Size(max = 200) private String note;
}

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SettlementDTO {
    private Long id;
    private Long groupId;
    private UserDTO payer;
    private UserDTO receiver;
    private BigDecimal amount;
    private String currency;
    private Settlement.PaymentMethod paymentMethod;
    private String note;
    private LocalDateTime settledAt;
}

// ── Balance ──────────────────────────────────────────────────

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BalanceDTO {
    private Long fromUserId;
    private String fromUserName;
    private Long toUserId;
    private String toUserName;
    private BigDecimal amount;
    private String currency;
}

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GroupBalanceSummary {
    private Long groupId;
    private String groupName;
    private String currency;
    private List<BalanceDTO> simplifiedTransactions;
    private Map<Long, BigDecimal> netBalances;
    private Map<Long, String> userNames;
}

// ── Notification ─────────────────────────────────────────────

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationDTO {
    private Long id;
    private String title;
    private String message;
    private Notification.NotificationType type;
    private Long referenceId;
    private String referenceType;
    private boolean read;
    private LocalDateTime createdAt;
}

// ── Analytics ─────────────────────────────────────────────────

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AnalyticsSummary {
    private BigDecimal totalExpenses;
    private BigDecimal totalOwed;
    private BigDecimal totalOwe;
    private int expenseCount;
    private Map<String, BigDecimal> categoryBreakdown;
    private List<MonthlyTrend> monthlyTrends;
    private List<UserSpending> topSpenders;
}

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MonthlyTrend {
    private int year;
    private int month;
    private String monthName;
    private BigDecimal total;
}

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserSpending {
    private Long userId;
    private String userName;
    private BigDecimal totalPaid;
    private BigDecimal percentage;
}

// ── OCR ──────────────────────────────────────────────────────

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OcrReceiptResult {
    private BigDecimal extractedAmount;
    private String merchantName;
    private LocalDate date;
    private String rawText;
    private double confidence;
}

// ── Generic API Wrapper ──────────────────────────────────────

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private int statusCode;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder().success(true).data(data).statusCode(200).build();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder().success(true).message(message).data(data).statusCode(200).build();
    }

    public static <T> ApiResponse<T> error(String message, int statusCode) {
        return ApiResponse.<T>builder().success(false).message(message).statusCode(statusCode).build();
    }
}

// ── Page Wrapper ─────────────────────────────────────────────

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PagedResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;
}

// ── Device Token ─────────────────────────────────────────────

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RegisterDeviceTokenRequest {
    @NotBlank private String token;
    private String deviceType;
}

// ── Smart Split Suggestion ────────────────────────────────────

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SplitSuggestion {
    private Long userId;
    private String userName;
    private BigDecimal suggestedAmount;
    private BigDecimal percentage;
    private String reason;
}
