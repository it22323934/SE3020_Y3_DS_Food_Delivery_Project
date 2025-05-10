package com.foodDelivery.paymentService.serviceImpl;

import com.foodDelivery.paymentService.model.Payment;
import com.foodDelivery.paymentService.repository.PaymentRepository;
import com.foodDelivery.paymentService.serviceInterfaces.PaymentReportService;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentReportServiceImpl implements PaymentReportService {

    private final PaymentRepository paymentRepository;
    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
    private static final Font HEADER_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
    private static final Font NORMAL_FONT = new Font(Font.FontFamily.HELVETICA, 10);
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public byte[] generatePaymentReportByOrder(String orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found for order: " + orderId));
        return generateSinglePaymentReport(payment);
    }

    @Override
    public byte[] generatePaymentReportByUser(String email) {
        List<Payment> payments = paymentRepository.findByCustomerEmailOrderByPaymentDateDesc(email);
        if (payments.isEmpty()) {
            throw new IllegalArgumentException("No payments found for email: " + email);
        }
        return generatePaymentListReport(payments, "Payments for user: " + email);
    }

    @Override
    public byte[] generatePaymentReportByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        List<Payment> payments = paymentRepository.findByPaymentDateBetweenOrderByPaymentDateDesc(startDate, endDate);
        if (payments.isEmpty()) {
            throw new IllegalArgumentException(
                    "No payments found between " + formatDate(startDate) + " and " + formatDate(endDate));
        }
        return generatePaymentListReport(payments,
                "Payments from " + formatDate(startDate) + " to " + formatDate(endDate));
    }

    @Override
    public byte[] generatePaymentReportByStatus(String status) {
        List<Payment> payments = paymentRepository.findByPaymentStatusIgnoreCaseOrderByPaymentDateDesc(status);
        if (payments.isEmpty()) {
            throw new IllegalArgumentException("No payments found with status: " + status);
        }
        return generatePaymentListReport(payments, "Payments with status: " + status.toUpperCase());
    }

    @Override
    public List<String> getAvailableReportTypes() {
        return Arrays.asList("ORDER", "USER", "DATE_RANGE", "STATUS");
    }

    private byte[] generateSinglePaymentReport(Payment payment) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, outputStream);

            document.open();
            addDocumentMetadata(document, "Payment Receipt: " + payment.getOrderId());
            addTitle(document, "PAYMENT RECEIPT");

            PdfPTable table = createDetailsTable();
            addTableRow(table, "Order ID:", payment.getOrderId());
            addTableRow(table, "Customer Email:", payment.getCustomerEmail());
            addTableRow(table, "Amount:", String.format("$%.2f", payment.getAmount()));
            addTableRow(table, "Status:", payment.getPaymentStatus().equalsIgnoreCase("succeeded") ? "SUCCEEDED" : "FAILED");
            addTableRow(table, "Transaction ID:", payment.getStripePaymentId() != null ? payment.getStripePaymentId() : "N/A");
            addTableRow(table, "Payment Date:", formatDate(payment.getPaymentDate()));

            document.add(table);
            addFooter(document);
            document.close();

            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate payment PDF", e);
        }
    }

    private byte[] generatePaymentListReport(List<Payment> payments, String title) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, outputStream);

            document.open();
            addDocumentMetadata(document, title);
            addTitle(document, title);
            addGeneratedDate(document);

            document.add(new Paragraph("Total payments: " + payments.size(), NORMAL_FONT));
            document.add(Chunk.NEWLINE);

            PdfPTable table = createListTable();
            addListTableHeaders(table);
            payments.forEach(payment -> addPaymentToTable(table, payment));

            document.add(table);
            addSummaryStats(document, payments);
            document.close();

            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate payments list PDF", e);
        }
    }

    private PdfPTable createDetailsTable() {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(80);
        table.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(10f);
        return table;
    }

    private PdfPTable createListTable() {
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(10f);
        return table;
    }

    private void addListTableHeaders(PdfPTable table) {
        String[] headers = {"Order ID", "Customer Email", "Amount", "Status", "Payment Date", "Transaction ID"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, HEADER_FONT));
            cell.setBackgroundColor(new BaseColor(220, 220, 220));
            cell.setPadding(5);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }
    }

    private void addPaymentToTable(PdfPTable table, Payment payment) {
        table.addCell(createTableCell(payment.getOrderId()));
        table.addCell(createTableCell(payment.getCustomerEmail()));
        table.addCell(createTableCell(String.format("$%.2f", payment.getAmount())));

        PdfPCell statusCell = createTableCell(
                payment.getPaymentStatus().equalsIgnoreCase("succeeded") ? "SUCCEEDED" : "FAILED"
        );
        statusCell.setBackgroundColor(
                payment.getPaymentStatus().equalsIgnoreCase("succeeded") ?
                        new BaseColor(200, 230, 200) : new BaseColor(230, 200, 200)
        );
        table.addCell(statusCell);

        table.addCell(createTableCell(formatDate(payment.getPaymentDate())));
        table.addCell(createTableCell(
                payment.getStripePaymentId() != null ? payment.getStripePaymentId() : "N/A"
        ));
    }

    private PdfPCell createTableCell(String content) {
        PdfPCell cell = new PdfPCell(new Phrase(content, NORMAL_FONT));
        cell.setPadding(5);
        return cell;
    }

    private void addTableRow(PdfPTable table, String key, String value) {
        table.addCell(new Phrase(key, HEADER_FONT));
        table.addCell(new Phrase(value, NORMAL_FONT));
    }

    private void addTitle(Document document, String title) throws DocumentException {
        Paragraph paragraph = new Paragraph(title, TITLE_FONT);
        paragraph.setAlignment(Element.ALIGN_CENTER);
        document.add(paragraph);
        document.add(Chunk.NEWLINE);
    }

    private void addGeneratedDate(Document document) throws DocumentException {
        Paragraph paragraph = new Paragraph(
                "Generated on: " + formatDate(LocalDateTime.now()),
                new Font(Font.FontFamily.HELVETICA, 10, Font.ITALIC)
        );
        paragraph.setAlignment(Element.ALIGN_RIGHT);
        document.add(paragraph);
        document.add(Chunk.NEWLINE);
    }

    private void addFooter(Document document) throws DocumentException {
        Paragraph footer = new Paragraph(
                "Thank you for your payment!\n\n" +
                        "For any questions, please contact support@fooddelivery.com",
                new Font(Font.FontFamily.HELVETICA, 10)
        );
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);
    }

    private void addSummaryStats(Document document, List<Payment> payments) throws DocumentException {
        double totalAmount = payments.stream().mapToDouble(Payment::getAmount).sum();
        long successfulPayments = payments.stream()
                .filter(p -> p.getPaymentStatus().equalsIgnoreCase("succeeded"))
                .count();

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(50);
        table.setHorizontalAlignment(Element.ALIGN_RIGHT);

        addSummaryRow(table, "Total Amount:", String.format("$%.2f", totalAmount));
        addSummaryRow(table, "Successful Payments:", String.valueOf(successfulPayments));
        addSummaryRow(table, "Failed Payments:", String.valueOf(payments.size() - successfulPayments));
        addSummaryRow(table, "Average Payment:", String.format("$%.2f", totalAmount / payments.size()));

        document.add(table);
    }

    private void addSummaryRow(PdfPTable table, String label, String value) {
        table.addCell(new Phrase(label, HEADER_FONT));
        table.addCell(new Phrase(value, NORMAL_FONT));
    }

    private void addDocumentMetadata(Document document, String title) {
        document.addTitle(title);
        document.addSubject("Payment Information");
        document.addKeywords("Payment, Receipt, Report");
        document.addAuthor("Food Delivery System");
        document.addCreator("Food Delivery System");
    }

    private String formatDate(LocalDateTime date) {
        return date.format(DATE_FORMATTER);
    }
}