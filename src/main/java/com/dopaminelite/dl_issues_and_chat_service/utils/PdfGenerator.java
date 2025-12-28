package com.dopaminelite.dl_issues_and_chat_service.utils;

import com.dopaminelite.dl_issues_and_chat_service.entity.Issue;
import com.dopaminelite.dl_issues_and_chat_service.entity.IssueMessage;
import com.dopaminelite.dl_issues_and_chat_service.constants.Role;
import com.dopaminelite.dl_issues_and_chat_service.dto.UserInfo;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PdfGenerator {

    private static final float MARGIN = 50f;
    private static final float LEADING = 14f;
    private static final float FONT_SIZE = 12f;
    private static final float TITLE_FONT_SIZE = 14f;
    private static final float DETAILS_FONT_SIZE = 11f;
    private static final float MESSAGE_META_FONT_SIZE = 9f;
    private static final float MESSAGE_BODY_FONT_SIZE = 12f;
    private static final float HEADER_FOOTER_FONT_SIZE = 10f;
    private static final float MESSAGE_GAP = 8f;
    private static final float DATE_HEADER_FONT_SIZE = 11f;
    private static final float DATE_HEADER_GAP = 12f;
    private static final DateTimeFormatter READABLE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter MESSAGE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DATE_HEADER_FORMAT =
            DateTimeFormatter.ofPattern("dd MMMM yyyy").withZone(ZoneId.systemDefault());

    public static byte[] generateIssueReport(Issue issue, List<IssueMessage> messages, Map<UUID, UserInfo> userMap) {

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
            String titleText = String.format("Issue Report - Issue No. : %d", issue.getIssueNumber());
            curY = writeLine(cs, sanitize(titleText), curY, headerFont, TITLE_FONT_SIZE, maxTextWidth, headerFont);

            // Issue details
            curY = writeLine(cs, sanitize("Issue ID: " + issue.getId()), curY, regularFont, DETAILS_FONT_SIZE, maxTextWidth, regularFont);
            curY = writeLine(cs, sanitize("Title: " + issue.getTitle()), curY, regularFont, DETAILS_FONT_SIZE, maxTextWidth, regularFont);
            curY = writeLine(cs, sanitize("Description: " + issue.getDescription()), curY, regularFont, DETAILS_FONT_SIZE, maxTextWidth, regularFont);
            curY = writeLine(cs, "Status: " + issue.getStatus(), curY, regularFont, DETAILS_FONT_SIZE, maxTextWidth, regularFont);

            // Display assigned admin's name
            String assignedAdminName = "Not Assigned";
            if (issue.getAssignedAdminId() != null) {
                UserInfo adminInfo = userMap.get(issue.getAssignedAdminId());
                if (adminInfo != null && adminInfo.getFullName() != null && !adminInfo.getFullName().isEmpty()) {
                    assignedAdminName = adminInfo.getFullName();
                } else {
                    assignedAdminName = issue.getAssignedAdminId().toString();
                }
            }
            curY = writeLine(cs, "Assigned Admin: " + assignedAdminName, curY, regularFont, DETAILS_FONT_SIZE, maxTextWidth, regularFont);

            curY = writeLine(cs, "Created At: " + READABLE_DATE_FORMAT.format(issue.getCreatedAt()), curY, regularFont, DETAILS_FONT_SIZE, maxTextWidth, regularFont);
            curY = writeLine(cs, "Solved At: " + (issue.getSolvedAt() != null ? READABLE_DATE_FORMAT.format(issue.getSolvedAt()) : "N/A"), curY, regularFont, DETAILS_FONT_SIZE, maxTextWidth, regularFont);

            curY -= LEADING; // blank line
            curY = writeLine(cs, sanitize("Conversation:"), curY, headerFont, FONT_SIZE, maxTextWidth, headerFont);
            curY -= LEADING / 2;

            // Group messages by date
            Map<LocalDate, List<IssueMessage>> messagesByDate = new LinkedHashMap<>();
            for (IssueMessage m : messages) {
                if (m.getCreatedAt() != null) {
                    LocalDate date = m.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate();
                    messagesByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(m);
                }
            }

            // Messages
            int pageNumber = 1;
            for (Map.Entry<LocalDate, List<IssueMessage>> entry : messagesByDate.entrySet()) {
                LocalDate date = entry.getKey();
                List<IssueMessage> dayMessages = entry.getValue();

                // Check if we need a new page for date header
                if (curY <= MARGIN + 50) {
                    drawFooter(cs, pageWidth, regularFont, pageNumber);
                    cs.close();
                    pageNumber++;
                    page = new PDPage(PDRectangle.LETTER);
                    doc.addPage(page);
                    cs = new PDPageContentStream(doc, page);
                    drawHeader(cs, pageWidth, pageHeight, headerFont);
                    curY = pageHeight - MARGIN - 30;
                }

                // Draw centered date header
                String dateStr = DATE_HEADER_FORMAT.format(date);
                float dateWidth = regularFont.getStringWidth(dateStr) / 1000f * DATE_HEADER_FONT_SIZE;
                float dateX = (pageWidth - dateWidth) / 2;

                cs.beginText();
                cs.setFont(regularFont, DATE_HEADER_FONT_SIZE);
                cs.newLineAtOffset(dateX, curY);
                cs.showText(dateStr);
                cs.endText();
                curY -= DATE_HEADER_GAP;

                // Render messages for this date
                for (IssueMessage m : dayMessages) {
                String roleLabel = formatRoleLabel(m.getSenderRole());
                String userName = getUserName(m.getSenderId(), userMap);

                boolean isAdminSide = m.getSenderRole() == Role.ADMIN || m.getSenderRole() == Role.MAIN_ADMIN;

                // Meta line: Name | Time | Role
                String meta = String.format("%s | %s | [%s]",
                         userName,
                         m.getCreatedAt() != null ? MESSAGE_TIME_FORMAT.format(m.getCreatedAt()) : "",
                         roleLabel
                         );

                float bubbleWidth = maxTextWidth * 0.85f;
                float rightX = pageWidth - MARGIN - bubbleWidth;
                float baseX = isAdminSide ? rightX : MARGIN;

                // Page break check (rough, based on a minimum space need)
                if (curY <= MARGIN + 40) {
                    drawFooter(cs, pageWidth, regularFont, pageNumber);
                    cs.close();
                    pageNumber++;
                    page = new PDPage(PDRectangle.LETTER);
                    doc.addPage(page);
                    cs = new PDPageContentStream(doc, page);
                    drawHeader(cs, pageWidth, pageHeight, headerFont);
                    curY = pageHeight - MARGIN - 30;
                }

                // Meta line (smaller, italic-ish)
                curY = writeAlignedBlock(cs, sanitize(meta), baseX, curY, regularFont, MESSAGE_META_FONT_SIZE, bubbleWidth, isAdminSide, pageWidth);

                // Body lines (normal)
                String body = m.getContent() != null ? m.getContent() : "";
                curY = writeAlignedBlock(cs, sanitize(body), baseX, curY, regularFont, MESSAGE_BODY_FONT_SIZE, bubbleWidth, isAdminSide, pageWidth);

                curY -= MESSAGE_GAP;
                }
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

    /**
     * PDFBox built-in Type1 fonts only support WinAnsiEncoding.
     * Strip control chars and replace unsupported whitespace/separators with safe ASCII.
     */
    private static String sanitize(String s) {
        if (s == null) return "";

        // 1) Remove NUL and other control chars except \n and \t (we later convert newlines to spaces anyway)
        String out = s
                .replace('\u0000', ' ')
                .replace('\r', '\n');

        StringBuilder sb = new StringBuilder(out.length());
        for (int i = 0; i < out.length(); i++) {
            char c = out.charAt(i);
            if (c == '\n' || c == '\t' || c >= 0x20) {
                sb.append(c);
            }
        }

        // 2) PDF text operators don't accept raw newlines; turn them into spaces
        return sb.toString().replace('\n', ' ');
    }

    private static float writeAlignedBlock(PDPageContentStream cs,
                                          String text,
                                          float x,
                                          float y,
                                          PDType1Font font,
                                          float fontSize,
                                          float maxWidth,
                                          boolean isRightAligned,
                                          float pageWidth) throws IOException {
        List<String> lines = wrapText(sanitize(text), maxWidth, font, fontSize);
        // keep consistent inter-line spacing
        for (String line : lines) {
            float lineX = x;
            if (isRightAligned) {
                // Calculate actual text width and position from right
                float textWidth = font.getStringWidth(sanitize(line)) / 1000f * fontSize;
                lineX = pageWidth - MARGIN - textWidth;
            }

            cs.beginText();
            cs.setFont(font, fontSize);
            cs.newLineAtOffset(lineX, y);
            cs.showText(sanitize(line));
            cs.endText();
            y -= LEADING;
        }
        return y;
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

    private static float writeLine(PDPageContentStream cs, String text, float y,
                                   PDType1Font font, float fontSize, float maxWidth,
                                   PDType1Font wrapFont) throws IOException {
        List<String> lines = wrapText(sanitize(text), maxWidth, wrapFont, fontSize);
        for (String line : lines) {
            cs.beginText();
            cs.setFont(font, fontSize);
            cs.newLineAtOffset(PdfGenerator.MARGIN, y);
            cs.showText(sanitize(line));
            cs.endText();
            y -= LEADING;
        }
        return y;
    }

    private static List<String> wrapText(String text, float maxWidth, PDType1Font font, float fontSize) throws IOException {
        List<String> lines = new ArrayList<>();
        text = sanitize(text);
        if (text.isEmpty()) {
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

    private static String getUserName(UUID userId, Map<UUID, UserInfo> userMap) {
        if (userId == null) {
            return "Unknown User";
        }

        UserInfo user = userMap.get(userId);
        if (user != null && user.getFullName() != null && !user.getFullName().isEmpty()) {
            return user.getFullName();
        }

        return "Unknown User";
    }

    private static String formatRoleLabel(Role role) {
        if (role == null) return "Unknown";

        return switch (role) {
            case ADMIN -> "Admin";
            case MAIN_ADMIN -> "Main Admin";
            case STUDENT -> "Student";
        };
    }
}
