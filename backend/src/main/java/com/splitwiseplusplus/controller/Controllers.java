package com.splitwiseplusplus.controller;

import com.splitwiseplusplus.dto.*;
import com.splitwiseplusplus.model.Expense;
import com.splitwiseplusplus.repository.UserRepository;
import com.splitwiseplusplus.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

// ============================================================
//  GROUP CONTROLLER
// ============================================================
@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Groups", description = "Group management and membership")
class GroupController {

    private final GroupService groupService;
    private final UserRepository userRepository;

    private Long getCurrentUserId(UserDetails ud) {
        return userRepository.findByEmail(ud.getUsername()).orElseThrow().getId();
    }

    @PostMapping
    @Operation(summary = "Create a new group")
    public ResponseEntity<ApiResponse<GroupDTO>> create(
            @Valid @RequestBody CreateGroupRequest request,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.status(201).body(
                ApiResponse.success("Group created", groupService.createGroup(request, getCurrentUserId(ud))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get group by ID")
    public ResponseEntity<ApiResponse<GroupDTO>> get(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success(groupService.getGroup(id, getCurrentUserId(ud))));
    }

    @GetMapping("/my")
    @Operation(summary = "Get all groups for current user")
    public ResponseEntity<ApiResponse<List<GroupDTO>>> myGroups(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success(groupService.getUserGroups(getCurrentUserId(ud))));
    }

    @PostMapping("/{id}/members")
    @Operation(summary = "Add a member to group (admin only)")
    public ResponseEntity<ApiResponse<GroupDTO>> addMember(
            @PathVariable Long id,
            @RequestParam Long userId,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success(
                groupService.addMember(id, userId, getCurrentUserId(ud))));
    }

    @DeleteMapping("/{id}/members/{memberId}")
    @Operation(summary = "Remove a member from group (admin only)")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable Long id,
            @PathVariable Long memberId,
            @AuthenticationPrincipal UserDetails ud) {
        groupService.removeMember(id, memberId, getCurrentUserId(ud));
        return ResponseEntity.ok(ApiResponse.success("Member removed", null));
    }

    @PostMapping("/join")
    @Operation(summary = "Join group via invite code")
    public ResponseEntity<ApiResponse<GroupDTO>> join(
            @RequestParam String inviteCode,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success(
                groupService.joinByInviteCode(inviteCode, getCurrentUserId(ud))));
    }

    @PostMapping("/{id}/invite")
    @Operation(summary = "Invite user by email")
    public ResponseEntity<ApiResponse<Void>> inviteByEmail(
            @PathVariable Long id,
            @RequestParam String email,
            @AuthenticationPrincipal UserDetails ud) {
        groupService.inviteByEmail(id, email, getCurrentUserId(ud));
        return ResponseEntity.ok(ApiResponse.success("Invitation sent", null));
    }
}

// ============================================================
//  EXPENSE CONTROLLER
// ============================================================
@RestController
@RequestMapping("/expenses")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Expenses", description = "Expense management and splitting")
class ExpenseController {

    private final ExpenseService expenseService;
    private final OcrService ocrService;
    private final UserRepository userRepository;

    private Long getCurrentUserId(UserDetails ud) {
        return userRepository.findByEmail(ud.getUsername()).orElseThrow().getId();
    }

    @PostMapping
    @Operation(summary = "Create a new expense")
    public ResponseEntity<ApiResponse<ExpenseDTO>> create(
            @Valid @RequestBody CreateExpenseRequest request,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.status(201).body(ApiResponse.success("Expense created",
                expenseService.createExpense(request, getCurrentUserId(ud))));
    }

    @GetMapping("/group/{groupId}")
    @Operation(summary = "Get paginated expenses for a group")
    public ResponseEntity<ApiResponse<PagedResponse<ExpenseDTO>>> getGroupExpenses(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Expense.Category category,
            @RequestParam(required = false) Long paidById,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success(
                expenseService.getGroupExpenses(groupId, getCurrentUserId(ud),
                        page, size, category, paidById, startDate, endDate, search)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get expense by ID")
    public ResponseEntity<ApiResponse<ExpenseDTO>> getById(
            @PathVariable Long id, @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success(
                expenseService.getExpenseById(id, getCurrentUserId(ud))));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an expense")
    public ResponseEntity<ApiResponse<ExpenseDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateExpenseRequest request,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success("Expense updated",
                expenseService.updateExpense(id, request, getCurrentUserId(ud))));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an expense")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id, @AuthenticationPrincipal UserDetails ud) {
        expenseService.deleteExpense(id, getCurrentUserId(ud));
        return ResponseEntity.ok(ApiResponse.success("Expense deleted", null));
    }

    @PostMapping(value = "/{id}/receipt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Attach a receipt image to an expense")
    public ResponseEntity<ApiResponse<ExpenseDTO>> attachReceipt(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success("Receipt attached",
                expenseService.attachReceipt(id, file, getCurrentUserId(ud))));
    }

    @PostMapping(value = "/scan-receipt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Scan a receipt with OCR to auto-fill expense data")
    public ResponseEntity<ApiResponse<OcrReceiptResult>> scanReceipt(
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success(ocrService.scanReceipt(file)));
    }

    @GetMapping("/group/{groupId}/suggestions")
    @Operation(summary = "Get smart split suggestions based on spending history")
    public ResponseEntity<ApiResponse<List<SplitSuggestion>>> getSplitSuggestions(
            @PathVariable Long groupId,
            @RequestParam java.math.BigDecimal amount,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success(
                expenseService.getSplitSuggestions(groupId, amount, getCurrentUserId(ud))));
    }
}

// ============================================================
//  SETTLEMENT CONTROLLER
// ============================================================
@RestController
@RequestMapping("/settlements")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Settlements", description = "Debt settlement and balance calculation")
class SettlementController {

    private final SettlementService settlementService;
    private final UserRepository userRepository;

    private Long getCurrentUserId(UserDetails ud) {
        return userRepository.findByEmail(ud.getUsername()).orElseThrow().getId();
    }

    @PostMapping
    @Operation(summary = "Record a settlement payment")
    public ResponseEntity<ApiResponse<SettlementDTO>> settle(
            @Valid @RequestBody CreateSettlementRequest request,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.status(201).body(ApiResponse.success("Settlement recorded",
                settlementService.settle(request, getCurrentUserId(ud))));
    }

    @GetMapping("/group/{groupId}/balances")
    @Operation(summary = "Get simplified group balances (debt minimization algorithm)")
    public ResponseEntity<ApiResponse<GroupBalanceSummary>> getBalances(
            @PathVariable Long groupId,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success(
                settlementService.getGroupBalances(groupId, getCurrentUserId(ud))));
    }

    @GetMapping("/group/{groupId}")
    @Operation(summary = "Get settlement history for a group")
    public ResponseEntity<ApiResponse<List<SettlementDTO>>> getGroupSettlements(
            @PathVariable Long groupId,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success(
                settlementService.getGroupSettlements(groupId, getCurrentUserId(ud))));
    }
}

// ============================================================
//  NOTIFICATION CONTROLLER
// ============================================================
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Notifications", description = "In-app notification management")
class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    private Long getCurrentUserId(UserDetails ud) {
        return userRepository.findByEmail(ud.getUsername()).orElseThrow().getId();
    }

    @GetMapping
    @Operation(summary = "Get paginated notifications for current user")
    public ResponseEntity<ApiResponse<PagedResponse<NotificationDTO>>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.getUserNotifications(getCurrentUserId(ud), page, size)));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread notification count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.getUnreadCount(getCurrentUserId(ud))));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<ApiResponse<NotificationDTO>> markRead(
            @PathVariable Long id, @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.markAsRead(id, getCurrentUserId(ud))));
    }

    @PutMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<ApiResponse<Integer>> markAllRead(@AuthenticationPrincipal UserDetails ud) {
        int count = notificationService.markAllAsRead(getCurrentUserId(ud));
        return ResponseEntity.ok(ApiResponse.success(count + " notifications marked as read", count));
    }
}

// ============================================================
//  ANALYTICS CONTROLLER
// ============================================================
@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Analytics", description = "Spending analytics and reports")
class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final UserRepository userRepository;

    private Long getCurrentUserId(UserDetails ud) {
        return userRepository.findByEmail(ud.getUsername()).orElseThrow().getId();
    }

    @GetMapping("/group/{groupId}")
    @Operation(summary = "Get analytics summary for a group")
    public ResponseEntity<ApiResponse<AnalyticsSummary>> getGroupAnalytics(
            @PathVariable Long groupId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success(
                analyticsService.getGroupAnalytics(groupId, getCurrentUserId(ud), startDate, endDate)));
    }
}
