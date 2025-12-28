package com.dopaminelite.dl_issues_and_chat_service.utils;

import com.dopaminelite.dl_issues_and_chat_service.entity.Issue;
import com.dopaminelite.dl_issues_and_chat_service.entity.IssueMessage;
import com.dopaminelite.dl_issues_and_chat_service.constants.Role;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PdfGenerator {

    private static final float MARGIN = 50f;
    private static final float LEADING = 14f;
    private static final float FONT_SIZE = 12f;
    private static final float HEADER_FOOTER_FONT_SIZE = 10f;
    private static final DateTimeFormatter READABLE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter MESSAGE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM HH:mm").withZone(ZoneId.systemDefault());

    public static byte[] generateIssueReport(Issue issue, List<IssueMessage> messages) {

        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PDType1Font regularFont = new PDType1Font(Standard14Fonts.FontName.TIMES_ROMAN);
            PDType1Font headerFont = new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD);

            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);

            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();
            float maxTextWidth = pageWidth - 2 * MARGIN;

            PDPageContentStream cs = new PDPageContentStream(doc, page);

            // Draw header
            drawHeader(cs, pageWidth, pageHeight, headerFont);

            float curY = pageHeight - MARGIN - 30; // Start below header

            // Issue title
            curY = writeLine(cs, "Issue Report", MARGIN, curY, headerFont, FONT_SIZE, maxTextWidth, regularFont);

            // Issue details
            curY = writeLine(cs, "Issue ID: " + issue.getId(), MARGIN, curY, regularFont, FONT_SIZE, maxTextWidth, regularFont);
            curY = writeLine(cs, "Title: " + issue.getTitle(), MARGIN, curY, regularFont, FONT_SIZE, maxTextWidth, regularFont);
            curY = writeLine(cs, "Description: " + issue.getDescription(), MARGIN, curY, regularFont, FONT_SIZE, maxTextWidth, regularFont);
            curY = writeLine(cs, "Status: " + issue.getStatus(), MARGIN, curY, regularFont, FONT_SIZE, maxTextWidth, regularFont);
            curY = writeLine(cs, "Assigned Admin ID: " + issue.getAssignedAdminId(), MARGIN, curY, regularFont, FONT_SIZE, maxTextWidth, regularFont);
            curY = writeLine(cs, "Created At: " + READABLE_DATE_FORMAT.format(issue.getCreatedAt()), MARGIN, curY, regularFont, FONT_SIZE, maxTextWidth, regularFont);
            curY = writeLine(cs, "Solved At: " + (issue.getSolvedAt() != null ? READABLE_DATE_FORMAT.format(issue.getSolvedAt()) : "N/A"), MARGIN, curY, regularFont, FONT_SIZE, maxTextWidth, regularFont);

            curY -= LEADING; // blank line
            curY = writeLine(cs, "Conversation:", MARGIN, curY, headerFont, FONT_SIZE, maxTextWidth, regularFont);

            // Messages
            int pageNumber = 1;
            for (IssueMessage m : messages) {
                // Format role nicely
                String roleLabel = formatRoleLabel(m.getSenderRole());

                String header = String.format("[%s] %s:",
                        m.getCreatedAt() != null ? MESSAGE_TIME_FORMAT.format(m.getCreatedAt()) : "",
                        roleLabel);

                // Check if we need a new page
                if (curY <= MARGIN + 30) { // Leave space for footer
                    // Draw footer on current page
                    drawFooter(cs, pageWidth, regularFont, pageNumber);
                    cs.close();

                    // Create new page
                    pageNumber++;
                    page = new PDPage(PDRectangle.LETTER);
                    doc.addPage(page);
                    cs = new PDPageContentStream(doc, page);

                    // Draw header on new page
                    drawHeader(cs, pageWidth, pageHeight, headerFont);
                    curY = pageHeight - MARGIN - 30;
                }

                // Write message header
                List<String> headerLines = wrapText(header, maxTextWidth, headerFont, FONT_SIZE);
                for (String hl : headerLines) {
                    if (curY <= MARGIN + 30) {
                        drawFooter(cs, pageWidth, regularFont, pageNumber);
                        cs.close();
                        pageNumber++;
                        page = new PDPage(PDRectangle.LETTER);
                        doc.addPage(page);
                        cs = new PDPageContentStream(doc, page);
                        drawHeader(cs, pageWidth, pageHeight, headerFont);
                        curY = pageHeight - MARGIN - 30;
                    }

                    cs.beginText();
                    cs.setFont(headerFont, FONT_SIZE);
                    cs.newLineAtOffset(MARGIN, curY);
                    cs.showText(hl);
                    cs.endText();
                    curY -= LEADING;
                }

                // Write message content
                List<String> contentLines = wrapText(m.getContent(), maxTextWidth, regularFont, FONT_SIZE);
                for (String cl : contentLines) {
                    if (curY <= MARGIN + 30) {
                        drawFooter(cs, pageWidth, regularFont, pageNumber);
                        cs.close();
                        pageNumber++;
                        page = new PDPage(PDRectangle.LETTER);
                        doc.addPage(page);
                        cs = new PDPageContentStream(doc, page);
                        drawHeader(cs, pageWidth, pageHeight, headerFont);
                        curY = pageHeight - MARGIN - 30;
                    }

                    cs.beginText();
                    cs.setFont(regularFont, FONT_SIZE);
                    cs.newLineAtOffset(MARGIN, curY);
                    cs.showText(cl);
                    cs.endText();
                    curY -= LEADING;
                }

                curY -= (LEADING / 2); // blank line after each message
            }

            // Draw footer on last page
            drawFooter(cs, pageWidth, regularFont, pageNumber);
            cs.close();

            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    private static void drawHeader(PDPageContentStream cs, float pageWidth, float pageHeight, PDType1Font font) throws IOException {
        String headerText = "DopamineLite";
        float textWidth = font.getStringWidth(headerText) / 1000f * HEADER_FOOTER_FONT_SIZE;
        float x = (pageWidth - textWidth) / 2; // Center
        float y = pageHeight - 30;

        cs.beginText();
        cs.setFont(font, HEADER_FOOTER_FONT_SIZE);
        cs.newLineAtOffset(x, y);
        cs.showText(headerText);
        cs.endText();
    }

    private static void drawFooter(PDPageContentStream cs, float pageWidth, PDType1Font font, int pageNumber) throws IOException {
        String footerText = "Page " + pageNumber;
        float textWidth = font.getStringWidth(footerText) / 1000f * HEADER_FOOTER_FONT_SIZE;
        float x = (pageWidth - textWidth) / 2; // Center
        float y = 30;

        cs.beginText();
        cs.setFont(font, HEADER_FOOTER_FONT_SIZE);
        cs.newLineAtOffset(x, y);
        cs.showText(footerText);
        cs.endText();
    }

    private static float writeLine(PDPageContentStream cs, String text, float x, float y,
                                   PDType1Font font, float fontSize, float maxWidth,
                                   PDType1Font wrapFont) throws IOException {
        List<String> lines = wrapText(text, maxWidth, wrapFont, fontSize);
        for (String line : lines) {
            cs.beginText();
            cs.setFont(font, fontSize);
            cs.newLineAtOffset(x, y);
            cs.showText(line != null ? line : "");
            cs.endText();
            y -= LEADING;
        }
        return y;
    }

    private static List<String> wrapText(String text, float maxWidth, PDType1Font font, float fontSize) throws IOException {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            lines.add("");
            return lines;
        }

        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            float width = font.getStringWidth(candidate) / 1000f * fontSize;

            if (width <= maxWidth) {
                if (!line.isEmpty()) line.append(' ');
                line.append(word);
            } else {
                if (!line.isEmpty()) {
                    lines.add(line.toString());
                    line = new StringBuilder(word);
                } else {
                    // Single word too long, break it
                    lines.add(word);
                }
            }
        }

        if (!line.isEmpty()) {
            lines.add(line.toString());
        }

        if (lines.isEmpty()) {
            lines.add("");
        }

        return lines;
    }

    private static String formatRoleLabel(Role role) {
        if (role == null) return "Unknown";

        return switch (role) {
            case ADMIN -> "Admin";
            case MAIN_ADMIN -> "Main Admin";
            case STUDENT -> "Student";
            default -> "Unknown";
        };
    }
}
