package com.splitwiseplusplus.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.splitwiseplusplus.model.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// ============================================================
//  AUTH DTOs
// ============================================================

class AuthDTOs {

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RegisterRequest {
        @NotBlank @Size(min = 2, max = 100)
        private String name;
        @NotBlank @Email
        private String email;
        @NotBlank @Size(min = 8, max = 100)
        private String password;
        private String phone;
        @Size(max = 3)
        private String preferredCurrency;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank @Email
        private String email;
        @NotBlank
        private String password;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AuthResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType = "Bearer";
        private long expiresIn;
        private UserDTO user;
    }
}

// ============================================================
//  USER DTOs
// ============================================================
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
class UserDTO {
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
class UpdateProfileRequest {
    @Size(min = 2, max = 100)
    private String name;
    private String phone;
    @Size(max = 3)
    private String preferredCurrency;
    private boolean pushNotificationsEnabled;
    private boolean emailNotificationsEnabled;
}

// ============================================================
//  GROUP DTOs
// ============================================================
@Data @Builder @NoArgsConstructor @AllArgsConstructor
class CreateGroupRequest {
    @NotBlank @Size(min = 2, max = 100)
    private String name;
    @Size(max = 500)
    private String description;
    private Group.GroupType type;
    @Size(max = 3)
    private String currency;
    private List<Long> memberIds;
}

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
class GroupDTO {
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
class GroupMemberDTO {
    private Long userId;
    private String name;
    private String email;
    private String profileImageUrl;
    private GroupMember.MemberRole role;
    private GroupMember.MemberStatus status;
    private LocalDateTime joinedAt;
}

// ============================================================
//  EXPENSE DTOs
// ============================================================
@Data @Builder @NoArgsConstructor @AllArgsConstructor
class CreateExpenseRequest {
    @NotBlank @Size(max = 200)
    private String description;
    @NotNull @DecimalMin("0.01")
    private BigDecimal amount;
    @Size(max = 3)
    private String currency;
    @NotNull
    private Long groupId;
    @NotNull
    private Long paidById;
    @NotNull
    private Expense.SplitType splitType;
    private Expense.Category category;
    @NotNull
    private LocalDate expenseDate;
    private String merchantName;
    /** Map of userId -> owedAmount (for UNEQUAL) or userId -> percentage (for PERCENTAGE) */
    private Map<Long, BigDecimal> splitData;
    /** Participant IDs for EQUAL split */
    private List<Long> participantIds;
    private boolean recurring;
    private Expense.RecurrenceType recurrenceType;
    private LocalDate recurrenceEndDate;
}

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
class ExpenseDTO {
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
class SplitDTO {
    private Long userId;
    private String userName;
    private BigDecimal owedAmount;
    private BigDecimal percentage;
    private boolean settled;
}

// ============================================================
//  SETTLEMENT DTOs
// ============================================================
@Data @Builder @NoArgsConstructor @AllArgsConstructor
class CreateSettlementRequest {
    @NotNull
    private Long groupId;
    @NotNull
    private Long receiverId;
    @NotNull @DecimalMin("0.01")
    private BigDecimal amount;
    @Size(max = 3)
    private String currency;
    private Settlement.PaymentMethod paymentMethod;
    @Size(max = 200)
    private String note;
}

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
class SettlementDTO {
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

// ============================================================
//  BALANCE DTOs
// ============================================================
@Data @Builder @NoArgsConstructor @AllArgsConstructor
class BalanceDTO {
    /** userId who owes money */
    private Long fromUserId;
    private String fromUserName;
    /** userId who is owed money */
    private Long toUserId;
    private String toUserName;
    private BigDecimal amount;
    private String currency;
}

@Data @Builder @NoArgsConstructor @AllArgsConstructor
class GroupBalanceSummary {
    private Long groupId;
    private String groupName;
    private String currency;
    /** Simplified transactions after debt minimization */
    private List<BalanceDTO> simplifiedTransactions;
    /** Raw net balance per user: userId -> netAmount (positive = owed, negative = owes) */
    private Map<Long, BigDecimal> netBalances;
    private Map<Long, String> userNames;
}

// ============================================================
//  NOTIFICATION DTOs
// ============================================================
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
class NotificationDTO {
    private Long id;
    private String title;
    private String message;
    private Notification.NotificationType type;
    private Long referenceId;
    private String referenceType;
    private boolean read;
    private LocalDateTime createdAt;
}

// ============================================================
//  ANALYTICS DTOs
// ============================================================
@Data @Builder @NoArgsConstructor @AllArgsConstructor
class AnalyticsSummary {
    private BigDecimal totalExpenses;
    private BigDecimal totalOwed;
    private BigDecimal totalOwe;
    private int expenseCount;
    private Map<String, BigDecimal> categoryBreakdown;
    private List<MonthlyTrend> monthlyTrends;
    private List<UserSpending> topSpenders;
}

@Data @Builder @NoArgsConstructor @AllArgsConstructor
class MonthlyTrend {
    private int year;
    private int month;
    private String monthName;
    private BigDecimal total;
}

@Data @Builder @NoArgsConstructor @AllArgsConstructor
class UserSpending {
    private Long userId;
    private String userName;
    private BigDecimal totalPaid;
    private BigDecimal percentage;
}

// ============================================================
//  OCR DTOs
// ============================================================
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
class OcrReceiptResult {
    private BigDecimal extractedAmount;
    private String merchantName;
    private LocalDate date;
    private String rawText;
    private double confidence;
}

// ============================================================
//  GENERIC RESPONSE WRAPPER
// ============================================================
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
class ApiResponse<T> {
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
