package com.foodDelivery.paymentService.controller;

import com.foodDelivery.paymentService.serviceInterfaces.PaymentReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/payments/reports")
@RequiredArgsConstructor
public class PaymentReportController {

    private final PaymentReportService paymentReportService;

    @GetMapping("/types")
    public ResponseEntity<List<String>> getAvailableReportTypes() {
        return ResponseEntity.ok(paymentReportService.getAvailableReportTypes());
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<byte[]> generatePaymentReportByOrder(@PathVariable String orderId) {
        try {
            byte[] report = paymentReportService.generatePaymentReportByOrder(orderId);
            return buildPdfResponse(report, "payment-report-" + orderId + ".pdf");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage().getBytes());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage().getBytes());
        }
    }

    @GetMapping("/user/{email}")
    public ResponseEntity<byte[]> generatePaymentReportByUser(@PathVariable String email) {
        try {
            byte[] report = paymentReportService.generatePaymentReportByUser(email);
            return buildPdfResponse(report, "user-payments-" + email + ".pdf");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage().getBytes());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage().getBytes());
        }
    }

    @GetMapping("/date-range")
    public ResponseEntity<byte[]> generatePaymentReportByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        try {
            if (startDate.isAfter(endDate)) {
                throw new IllegalArgumentException("Start date must be before end date");
            }
            byte[] report = paymentReportService.generatePaymentReportByDateRange(startDate, endDate);
            return buildPdfResponse(report,
                    "payments-" + startDate + "-to-" + endDate + ".pdf");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage().getBytes());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage().getBytes());
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<byte[]> generatePaymentReportByStatus(@PathVariable String status) {
        try {
            if (!List.of("succeeded", "failed").contains(status.toLowerCase())) {
                throw new IllegalArgumentException("Invalid status. Must be 'succeeded' or 'failed'");
            }
            byte[] report = paymentReportService.generatePaymentReportByStatus(status);
            return buildPdfResponse(report, "payments-by-status-" + status + ".pdf");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage().getBytes());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage().getBytes());
        }
    }

    private ResponseEntity<byte[]> buildPdfResponse(byte[] content, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("filename", filename);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        return ResponseEntity.ok().headers(headers).body(content);
    }
}