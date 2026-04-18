package com.splitwiseplusplus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Email Service — sends HTML emails for notifications, reminders, and monthly reports.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    @Async
    public void sendPaymentReminder(String toEmail, String debtorName,
                                    String creditorName, double amount,
                                    String currency, String groupName) {
        String subject = String.format("💰 Payment reminder: You owe %s in %s", creditorName, groupName);
        String html = buildPaymentReminderEmail(debtorName, creditorName, amount, currency, groupName);
        sendHtmlEmail(toEmail, subject, html);
    }

    @Async
    public void sendMonthlyReport(String toEmail, String userName,
                                  String groupName, Map<String, BigDecimal> categorySpending,
                                  BigDecimal totalSpent, BigDecimal totalOwed) {
        String subject = String.format("📊 Monthly expense report — %s", groupName);
        String html = buildMonthlyReportEmail(userName, groupName, categorySpending, totalSpent, totalOwed);
        sendHtmlEmail(toEmail, subject, html);
    }

    @Async
    public void sendGroupInvite(String toEmail, String invitedByName, String groupName, String inviteCode) {
        String subject = String.format("🎉 %s invited you to join '%s' on Splitwise++", invitedByName, groupName);
        String html = buildGroupInviteEmail(invitedByName, groupName, inviteCode);
        sendHtmlEmail(toEmail, subject, html);
    }

    @Async
    public void sendWelcomeEmail(String toEmail, String name) {
        String subject = "Welcome to Splitwise++ 🎉";
        String html = buildWelcomeEmail(name);
        sendHtmlEmail(toEmail, subject, html);
    }

    // ── HTML Templates ────────────────────────────────────────

    private String buildPaymentReminderEmail(String debtor, String creditor,
                                              double amount, String currency, String group) {
        return """
                <!DOCTYPE html><html><head>
                <meta charset="UTF-8">
                <style>
                  body { font-family: 'Segoe UI', sans-serif; background: #f5f5f5; padding: 20px; }
                  .card { background: #fff; border-radius: 12px; padding: 32px; max-width: 520px; margin: 0 auto; box-shadow: 0 4px 20px rgba(0,0,0,.08); }
                  .amount { font-size: 2.5em; font-weight: 700; color: #e64b3b; text-align: center; margin: 24px 0; }
                  .btn { display: inline-block; background: #4f46e5; color: #fff; padding: 12px 32px; border-radius: 8px; text-decoration: none; font-weight: 600; }
                  .footer { color: #888; font-size: 12px; text-align: center; margin-top: 32px; }
                </style>
                </head><body>
                <div class="card">
                  <h2 style="color:#1a1a2e">💸 Payment Reminder</h2>
                  <p>Hi <strong>%s</strong>,</p>
                  <p>You have an outstanding payment to <strong>%s</strong> in the group <strong>%s</strong>:</p>
                  <div class="amount">%s %.2f</div>
                  <p style="text-align:center"><a href="#" class="btn">Open Splitwise++</a></p>
                </div>
                <div class="footer">Splitwise++ · Smart expense splitting</div>
                </body></html>
                """.formatted(debtor, creditor, group, currency, amount);
    }

    private String buildMonthlyReportEmail(String userName, String groupName,
                                            Map<String, BigDecimal> categorySpending,
                                            BigDecimal totalSpent, BigDecimal totalOwed) {
        StringBuilder rows = new StringBuilder();
        categorySpending.forEach((cat, amt) ->
                rows.append("<tr><td>").append(cat).append("</td><td style='text-align:right;font-weight:600'>")
                        .append(amt.toPlainString()).append("</td></tr>"));

        return """
                <!DOCTYPE html><html><head>
                <style>
                  body { font-family: 'Segoe UI', sans-serif; background: #f5f5f5; padding: 20px; }
                  .card { background: #fff; border-radius: 12px; padding: 32px; max-width: 560px; margin: 0 auto; }
                  table { width: 100%%; border-collapse: collapse; margin: 16px 0; }
                  th, td { padding: 10px 12px; border-bottom: 1px solid #eee; }
                  th { background: #4f46e5; color: #fff; text-align: left; }
                  .summary { display: flex; gap: 16px; margin: 24px 0; }
                  .stat { flex: 1; background: #f0f0ff; border-radius: 10px; padding: 16px; text-align: center; }
                  .stat .value { font-size: 1.6em; font-weight: 700; color: #4f46e5; }
                </style>
                </head><body>
                <div class="card">
                  <h2>📊 Monthly Report — %s</h2>
                  <p>Hi <strong>%s</strong>, here's your expense summary for this month:</p>
                  <div class="summary">
                    <div class="stat"><div class="value">%s</div><div>Total Spent</div></div>
                    <div class="stat"><div class="value">%s</div><div>You Owe</div></div>
                  </div>
                  <h3>Spending by Category</h3>
                  <table><tr><th>Category</th><th style="text-align:right">Amount</th></tr>%s</table>
                </div>
                </body></html>
                """.formatted(groupName, userName, totalSpent, totalOwed, rows);
    }

    private String buildGroupInviteEmail(String invitedBy, String groupName, String inviteCode) {
        return """
                <!DOCTYPE html><html><head>
                <style>
                  body { font-family: 'Segoe UI', sans-serif; background: #f5f5f5; padding: 20px; }
                  .card { background: #fff; border-radius: 12px; padding: 32px; max-width: 520px; margin: 0 auto; }
                  .code { font-size: 2em; font-weight: 800; letter-spacing: 6px; color: #4f46e5; text-align: center; background: #f0f0ff; border-radius: 8px; padding: 16px; margin: 24px 0; }
                  .btn { display: inline-block; background: #4f46e5; color: #fff; padding: 12px 32px; border-radius: 8px; text-decoration: none; }
                </style>
                </head><body>
                <div class="card">
                  <h2>🎉 You've been invited!</h2>
                  <p><strong>%s</strong> invited you to join the group <strong>%s</strong> on Splitwise++.</p>
                  <p>Your invite code:</p>
                  <div class="code">%s</div>
                  <p style="text-align:center"><a href="#" class="btn">Join Group</a></p>
                </div>
                </body></html>
                """.formatted(invitedBy, groupName, inviteCode);
    }

    private String buildWelcomeEmail(String name) {
        return """
                <!DOCTYPE html><html><body style="font-family:sans-serif;padding:32px">
                <h1>Welcome, %s! 👋</h1>
                <p>You've joined <strong>Splitwise++</strong> — the smarter way to split expenses.</p>
                <p>Get started by creating a group and adding your first expense.</p>
                </body></html>
                """.formatted(name);
    }

    // ── Send Helper ───────────────────────────────────────────

    private void sendHtmlEmail(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.debug("Email sent to {}: {}", to, subject);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
