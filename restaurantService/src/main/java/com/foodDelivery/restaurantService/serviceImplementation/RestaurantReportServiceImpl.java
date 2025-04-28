package com.foodDelivery.restaurantService.serviceImplementation;

import com.foodDelivery.restaurantService.client.UserServiceClient;
import com.foodDelivery.restaurantService.dto.MenuItemResponse;
import com.foodDelivery.restaurantService.dto.RestaurantReportRequest;
import com.foodDelivery.restaurantService.dto.user.UserProfileResponse;
import com.foodDelivery.restaurantService.model.CuisineType;
import com.foodDelivery.restaurantService.model.MenuCategory;
import com.foodDelivery.restaurantService.model.Restaurant;
import com.foodDelivery.restaurantService.serviceInterfaces.*;
import com.itextpdf.text.*;
import com.itextpdf.text.Font;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RestaurantReportServiceImpl implements RestaurantReportService {

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, new BaseColor(41, 128, 185));
    private static final Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new BaseColor(44, 62, 80));
    private static final Font SECTION_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new BaseColor(52, 73, 94));
    private static final Font TABLE_HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.WHITE);
    private static final Font CONTENT_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.BLACK);

    private static final BaseColor PRIMARY_COLOR = new BaseColor(41, 128, 185);
    private static final BaseColor SECONDARY_COLOR = new BaseColor(52, 152, 219);
    private static final BaseColor LIGHT_GRAY = new BaseColor(236, 240, 241);
    private static final BaseColor DARK_GRAY = new BaseColor(52, 73, 94);

    private final RestaurantService restaurantService;
    private final CuisineTypeService cuisineTypeService;
    private final MenuItemService menuItemService;
    private final UserServiceClient userServiceClient;
    private final MenuCategoryService menuCategoryService;

    @Override
    public byte[] generateRestaurantReport(String restaurantId, String token) {
        Restaurant restaurant = restaurantService.getRestaurantById(restaurantId);
        return generatePdfForRestaurant(restaurant, token);
    }

    @Override
    public byte[] generateCustomReport(RestaurantReportRequest request) {
        return new byte[0];
    }

    private void addMetaData(Document document, String title) {
        document.addTitle(title);
        document.addCreator("Restaurant Management System");
        document.addCreationDate();
    }

    private byte[] generatePdfForRestaurant(Restaurant restaurant, String token) {
        try {
            Document document = new Document(PageSize.A4, 36, 36, 60, 36);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = PdfWriter.getInstance(document, baos);

            // Add header and footer
            HeaderFooter event = new HeaderFooter();
            writer.setPageEvent(event);

            document.open();
            addMetaData(document, "FlavourFleet - Restaurant Report: " + restaurant.getName());

            addMainHeader(document, restaurant);
            addSeparator(document);
            addBasicInformationTable(document, restaurant);
            addSeparator(document);
            addLocationInformation(document, restaurant);
            addSeparator(document);
            addOperatingHours(document, restaurant);
            addSeparator(document);
            addCuisineTypes(document, restaurant);
            addSeparator(document);
            addAdminInformation(document, restaurant, token);
            addSeparator(document);
            addMenuItems(document, restaurant);
            addSeparator(document);
            addPerformanceMetrics(document, restaurant);
            addFooter(document);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generating PDF for restaurant {}: {}", restaurant.getName(), e.getMessage());
            throw new RuntimeException("Failed to generate restaurant report", e);
        }
    }

    private void addMainHeader(Document document, Restaurant restaurant) throws DocumentException {
        Paragraph brand = new Paragraph("FlavourFleet", TITLE_FONT);
        brand.setAlignment(Element.ALIGN_CENTER);
        brand.setSpacingAfter(10);
        document.add(brand);

        Paragraph title = new Paragraph("Restaurant Profile Report", SUBTITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        Paragraph restaurantName = new Paragraph(restaurant.getName(),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, DARK_GRAY));
        restaurantName.setAlignment(Element.ALIGN_CENTER);
        restaurantName.setSpacingAfter(20);
        document.add(restaurantName);

        if (restaurant.getRestaurantImageUrl() != null || restaurant.getBannerImageUrl() != null) {
            PdfPTable imageTable = new PdfPTable(2);
            imageTable.setWidthPercentage(100);
            imageTable.setSpacingBefore(10);
            imageTable.setSpacingAfter(20);

            if (restaurant.getRestaurantImageUrl() != null) {
                addImageCell(imageTable, "Restaurant Image", restaurant.getRestaurantImageUrl());
            }
            if (restaurant.getBannerImageUrl() != null) {
                addImageCell(imageTable, "Banner Image", restaurant.getBannerImageUrl());
            }
            document.add(imageTable);
        }
    }

    private void addSeparator(Document document) throws DocumentException {
        LineSeparator line = new LineSeparator();
        line.setLineColor(LIGHT_GRAY);
        line.setOffset(5);
        document.add(new Chunk(line));
        document.add(new Paragraph(" "));
    }

    private void addImageCell(PdfPTable table, String title, String url) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(10);
        cell.setBorderColor(LIGHT_GRAY);

        Paragraph titlePara = new Paragraph(title + ":",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, DARK_GRAY));
        cell.addElement(titlePara);

        Paragraph urlPara = new Paragraph(url, CONTENT_FONT);
        cell.addElement(urlPara);

        table.addCell(cell);
    }

    private class HeaderFooter extends PdfPageEventHelper {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA, 8, BaseColor.GRAY);
        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 8, BaseColor.GRAY);

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();

            // Header
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                    new Phrase("FlavourFleet Restaurant Report", headerFont),
                    document.right(), document.top() + 10, 0);

            // Footer
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    new Phrase(String.format("Page %d", writer.getPageNumber()), footerFont),
                    (document.right() - document.left()) / 2 + document.left(),
                    document.bottom() - 10, 0);

            // Add timestamp to footer
            String timestamp = "Generated: " +
                    java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase(timestamp, footerFont),
                    document.left(), document.bottom() - 10, 0);
        }
    }

    private void addBasicInformationTable(Document document, Restaurant restaurant) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);

        addTableRow(table, "Restaurant ID", restaurant.getId());
        addTableRow(table, "Name", restaurant.getName());
        addTableRow(table, "Description", restaurant.getDescription());
        addTableRow(table, "Email", restaurant.getEmail());
        addTableRow(table, "Phone", restaurant.getPhoneNumber());
        addTableRow(table, "Status", restaurant.isEnabled() ? "Active" : "Inactive");
        addTableRow(table, "Rating", String.format("%.1f (%d reviews)",
                restaurant.getAvgRating(), restaurant.getTotalRatings()));

        document.add(table);
        document.add(Chunk.NEWLINE);
    }

    private void addLocationInformation(Document document, Restaurant restaurant) throws DocumentException {
        Paragraph locationTitle = new Paragraph("Location Information",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14));
        document.add(locationTitle);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);

        addTableRow(table, "Address", restaurant.getFormattedAddress());
        addTableRow(table, "Coordinates", String.format("Lat: %f, Long: %f",
                restaurant.getLatitude(), restaurant.getLongitude()));

        document.add(table);
        document.add(Chunk.NEWLINE);
    }

    private void addOperatingHours(Document document, Restaurant restaurant) throws DocumentException {
        Paragraph hoursTitle = new Paragraph("Operating Hours",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14));
        document.add(hoursTitle);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);

        addTableHeader(table, "Day", "Open Time", "Close Time", "Status");

        for (Restaurant.OpeningHourInfo hour : restaurant.getOpeningHours()) {
            String day = getDayOfWeek(hour.getDayOfWeek());
            String status = hour.isClosed() ? "Closed" : "Open";
            table.addCell(day);
            table.addCell(hour.getOpenTime());
            table.addCell(hour.getCloseTime());
            table.addCell(status);
        }

        document.add(table);
        document.add(Chunk.NEWLINE);
    }

    private void addCuisineTypes(Document document, Restaurant restaurant) throws DocumentException {
        Paragraph cuisineTitle = new Paragraph("Cuisine Types",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14));
        document.add(cuisineTitle);

        List<CuisineType> cuisineTypes = cuisineTypeService.getCuisineTypesByIds(
                restaurant.getCuisineTypeIds());

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);

        for (CuisineType cuisine : cuisineTypes) {
            table.addCell(cuisine.getName());
            table.addCell(cuisine.getDescription());
        }

        document.add(table);
        document.add(Chunk.NEWLINE);
    }

    private void addAdminInformation(Document document, Restaurant restaurant, String token) throws DocumentException {
        Paragraph adminsTitle = new Paragraph("Restaurant Administrators",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14));
        document.add(adminsTitle);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        addTableHeader(table, "Admin ID", "Name", "Email", "Phone");

        boolean serviceAvailable = true;
        StringBuilder errorMessage = new StringBuilder();

        for (String adminId : restaurant.getAdminIds()) {
            try {
                UserProfileResponse adminInfo = userServiceClient.getAdminInfo(adminId, token);
                if (adminInfo != null) {
                    table.addCell(adminId);
                    table.addCell(adminInfo.getUsername());
                    table.addCell(adminInfo.getEmail());
                    table.addCell(adminInfo.getPhoneNumber());
                } else {
                    addUnavailableRow(table, adminId);
                }
            } catch (Exception e) {
                log.error("Failed to fetch admin info for ID {}: {}", adminId, e.getMessage());
                serviceAvailable = false;
                errorMessage.append("User service is currently unavailable. Some administrator information may be incomplete.");
                addUnavailableRow(table, adminId);
            }
        }

        document.add(table);
        document.add(Chunk.NEWLINE);

        if (!serviceAvailable) {
            Paragraph errorNote = new Paragraph(errorMessage.toString(),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.RED));
            errorNote.setSpacingBefore(5);
            document.add(errorNote);
        }
    }

    private void addUnavailableRow(PdfPTable table, String adminId) {
        table.addCell(adminId);
        table.addCell("Data Unavailable");
        table.addCell("Data Unavailable");
        table.addCell("Data Unavailable");
    }

    private void addMenuItems(Document document, Restaurant restaurant) throws DocumentException {
        Paragraph menuTitle = new Paragraph("Menu Items",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14));
        document.add(menuTitle);

        List<MenuItemResponse> menuItems = menuItemService.getAvailableMenuItemsByRestaurantId(
                restaurant.getId());

        if (menuItems.isEmpty()) {
            document.add(new Paragraph("No menu items available"));
            return;
        }

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);

        addTableHeader(table, "Item Name", "Category", "Price", "Status");

        for (MenuItemResponse item : menuItems) {
            MenuCategory category = menuCategoryService.getCategoryById(item.getCategoryId());
            String categoryName = category != null ? category.getName() : "N/A";

            table.addCell(item.getName());
            table.addCell(categoryName);
            table.addCell(String.format("$%.2f", item.getPrice()));
            table.addCell(item.isAvailable() ? "Available" : "Unavailable");
        }

        document.add(table);
        document.add(Chunk.NEWLINE);
    }

    private void addPerformanceMetrics(Document document, Restaurant restaurant) throws DocumentException {
        Paragraph metricsTitle = new Paragraph("Performance Metrics",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14));
        document.add(metricsTitle);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);

        addTableRow(table, "Average Rating", String.format("%.1f", restaurant.getAvgRating()));
        addTableRow(table, "Total Reviews", String.valueOf(restaurant.getTotalRatings()));

        document.add(table);
        document.add(Chunk.NEWLINE);
    }

    private void addFooter(Document document) throws DocumentException {
        document.add(Chunk.NEWLINE);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        Paragraph footer = new Paragraph("Report generated on: " +
                java.time.LocalDateTime.now().format(formatter),
                FontFactory.getFont(FontFactory.HELVETICA, 10));
        document.add(footer);
    }

    private void addTableHeader(PdfPTable table, String... headers) {
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header,
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            cell.setPadding(5);
            table.addCell(cell);
        }
    }

    private void addTableRow(PdfPTable table, String key, String value) {
        PdfPCell keyCell = new PdfPCell(new Phrase(key,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
        keyCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        keyCell.setPadding(5);

        PdfPCell valueCell = new PdfPCell(new Phrase(value));
        valueCell.setPadding(5);

        table.addCell(keyCell);
        table.addCell(valueCell);
    }

    private String getDayOfWeek(int day) {
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday",
                "Friday", "Saturday", "Sunday"};
        return days[day - 1];
    }
}