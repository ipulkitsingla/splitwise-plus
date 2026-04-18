package com.splitwiseplusplus;

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
public class SplitwisePlusApplication {

    public static void main(String[] args) {
        SpringApplication.run(SplitwisePlusApplication.class, args);
    }
}
