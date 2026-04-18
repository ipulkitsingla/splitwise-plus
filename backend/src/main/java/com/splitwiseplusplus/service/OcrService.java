package com.splitwiseplusplus.service;

import com.splitwiseplusplus.dto.OcrReceiptResult;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OCR Service — extracts total amount, merchant name, and date from receipt images.
 * Uses Tesseract4J engine. Falls back gracefully if Tesseract is unavailable.
 */
@Service
@Slf4j
public class OcrService {

    @Value("${app.ocr.tessdata-path}")
    private String tessdataPath;

    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
            "(?:total|amount|grand total|subtotal|sum)[:\\s]*[\\$₹£€]?\\s*([0-9,]+\\.?[0-9]{0,2})",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern FALLBACK_AMOUNT_PATTERN = Pattern.compile(
            "[\\$₹£€]\\s*([0-9,]+\\.?[0-9]{0,2})"
    );
    private static final Pattern DATE_PATTERN = Pattern.compile(
            "\\b(\\d{1,2}[/\\-\\.](\\d{1,2})[/\\-\\.](\\d{2,4}))\\b"
    );

    public OcrReceiptResult scanReceipt(MultipartFile file) {
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                throw new IllegalArgumentException("Cannot read image file");
            }

            String rawText = extractText(image);
            log.debug("OCR extracted text (first 500 chars): {}",
                    rawText.substring(0, Math.min(rawText.length(), 500)));

            return OcrReceiptResult.builder()
                    .extractedAmount(parseAmount(rawText))
                    .merchantName(parseMerchantName(rawText))
                    .date(parseDate(rawText))
                    .rawText(rawText)
                    .confidence(estimateConfidence(rawText))
                    .build();

        } catch (IOException e) {
            log.error("Failed to read receipt image: {}", e.getMessage());
            throw new RuntimeException("Failed to process receipt image", e);
        }
    }

    private String extractText(BufferedImage image) {
        try {
            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath(tessdataPath);
            tesseract.setLanguage("eng");
            tesseract.setPageSegMode(3); // Fully automatic page segmentation
            return tesseract.doOCR(image);
        } catch (TesseractException | UnsatisfiedLinkError e) {
            log.warn("Tesseract not available, returning placeholder. Error: {}", e.getMessage());
            return "TOTAL: $0.00"; // Graceful fallback
        }
    }

    private BigDecimal parseAmount(String text) {
        Matcher matcher = AMOUNT_PATTERN.matcher(text);
        if (matcher.find()) {
            return new BigDecimal(matcher.group(1).replace(",", ""));
        }
        // Fallback: find largest currency amount in receipt
        Matcher fallback = FALLBACK_AMOUNT_PATTERN.matcher(text);
        BigDecimal largest = BigDecimal.ZERO;
        while (fallback.find()) {
            try {
                BigDecimal val = new BigDecimal(fallback.group(1).replace(",", ""));
                if (val.compareTo(largest) > 0) largest = val;
            } catch (NumberFormatException ignored) {}
        }
        return largest.compareTo(BigDecimal.ZERO) > 0 ? largest : null;
    }

    private String parseMerchantName(String text) {
        String[] lines = text.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.length() > 3 && line.length() < 50 &&
                !line.matches(".*\\d{2}/\\d{2}/\\d{2,4}.*") &&
                !line.matches(".*[0-9]+\\.[0-9]{2}.*")) {
                return line;
            }
        }
        return null;
    }

    private LocalDate parseDate(String text) {
        Matcher matcher = DATE_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                String dateStr = matcher.group(1);
                for (DateTimeFormatter fmt : new DateTimeFormatter[]{
                        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                        DateTimeFormatter.ofPattern("MM-dd-yyyy"),
                        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                }) {
                    try { return LocalDate.parse(dateStr, fmt); } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
        }
        return LocalDate.now();
    }

    private double estimateConfidence(String text) {
        if (text == null || text.isBlank()) return 0.0;
        long wordCount = text.split("\\s+").length;
        return Math.min(1.0, wordCount / 50.0);
    }
}
