package com.splitwiseplusplus;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Smart Expense Splitter (Splitwise++) - Main Application Entry Point
 *
 * Features:
 * - JWT Authentication & Role-based Authorization
 * - Intelligent Debt Simplification Algorithm
 * - OCR Receipt Scanning (Tesseract)
 * - Real-time WebSocket Notifications
 * - Multi-currency Support
 * - Analytics Dashboard
 * - Recurring Expenses & Scheduled Tasks
 * - Firebase Push Notifications
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableCaching
@Slf4j
public class SplitwisePlusApplication {

    public static void main(String[] args) {
        try {
            SpringApplication.run(SplitwisePlusApplication.class, args);
        } catch (Throwable t) {
            Throwable root = t;
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }
            log.error("Fatal startup error: {}", root.getMessage(), t);
            System.err.println("Fatal startup error: " + root.getClass().getName() + " - " + root.getMessage());
            t.printStackTrace(System.err);
            throw t;
        }
    }
}
