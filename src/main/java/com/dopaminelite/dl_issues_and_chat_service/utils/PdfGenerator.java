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
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
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

            // Load Noto Sans font which supports Unicode (including Sinhala)
            PDFont regularFont;
            PDFont headerFont;
            boolean unicodeFontsLoaded = false;

            try {
                // Use Noto Sans which has full Unicode support including Sinhala
                InputStream fontStream = PdfGenerator.class.getResourceAsStream("/fonts/NotoSans-Regular.ttf");
                if (fontStream != null) {
                    regularFont = PDType0Font.load(doc, fontStream);
                    log.info("Successfully loaded NotoSans-Regular font");
                } else {
                    log.warn("NotoSans-Regular.ttf not found in resources");
                    regularFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                }

                InputStream boldFontStream = PdfGenerator.class.getResourceAsStream("/fonts/NotoSans-Bold.ttf");
                if (boldFontStream != null) {
                    headerFont = PDType0Font.load(doc, boldFontStream);
                    log.info("Successfully loaded NotoSans-Bold font");
                } else {
                    log.warn("NotoSans-Bold.ttf not found in resources");
                    headerFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                }

                // Check if both fonts are Unicode fonts
                unicodeFontsLoaded = (regularFont instanceof PDType0Font) && (headerFont instanceof PDType0Font);
                log.info("Unicode fonts loaded: {}", unicodeFontsLoaded);
            } catch (Exception e) {
                log.error("Failed to load Sinhala fonts, falling back to Helvetica: {}", e.getMessage());
                // Fallback to basic fonts if Unicode fonts aren't available
                regularFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                headerFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                unicodeFontsLoaded = false;
            }

            final boolean useUnicode = unicodeFontsLoaded;

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
            curY = writeLine(cs, sanitize(titleText, useUnicode), curY, headerFont, TITLE_FONT_SIZE, maxTextWidth, headerFont);

            // Issue details
            curY = writeLine(cs, sanitize("Issue ID: " + issue.getId(), useUnicode), curY, regularFont, DETAILS_FONT_SIZE, maxTextWidth, regularFont);
            curY = writeLine(cs, sanitize("Title: " + issue.getTitle(), useUnicode), curY, regularFont, DETAILS_FONT_SIZE, maxTextWidth, regularFont);
            curY = writeLine(cs, sanitize("Description: " + issue.getDescription(), useUnicode), curY, regularFont, DETAILS_FONT_SIZE, maxTextWidth, regularFont);
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
            curY = writeLine(cs, sanitize("Assigned Admin: " + assignedAdminName, useUnicode), curY, regularFont, DETAILS_FONT_SIZE, maxTextWidth, regularFont);

            curY = writeLine(cs, "Created At: " + READABLE_DATE_FORMAT.format(issue.getCreatedAt()), curY, regularFont, DETAILS_FONT_SIZE, maxTextWidth, regularFont);
            curY = writeLine(cs, "Solved At: " + (issue.getSolvedAt() != null ? READABLE_DATE_FORMAT.format(issue.getSolvedAt()) : "N/A"), curY, regularFont, DETAILS_FONT_SIZE, maxTextWidth, regularFont);

            curY -= LEADING; // blank line
            curY = writeLine(cs, sanitize("Conversation:", useUnicode), curY, headerFont, FONT_SIZE, maxTextWidth, headerFont);
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
                dateStr = sanitizeForFont(dateStr, regularFont);
                float dateWidth = safeGetStringWidth(dateStr, regularFont, DATE_HEADER_FONT_SIZE);
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
                curY = writeAlignedBlock(cs, sanitize(meta, useUnicode), baseX, curY, regularFont, MESSAGE_META_FONT_SIZE, bubbleWidth, isAdminSide, pageWidth, useUnicode);

                // Body lines (normal)
                String body = m.getContent() != null ? m.getContent() : "";
                curY = writeAlignedBlock(cs, sanitize(body, useUnicode), baseX, curY, regularFont, MESSAGE_BODY_FONT_SIZE, bubbleWidth, isAdminSide, pageWidth, useUnicode);

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
     * Clean control characters from text but preserve Unicode characters
     * Also removes characters that the font cannot encode
     */
    private static String sanitize(String s, boolean useUnicode) {
        if (s == null) return "";

        StringBuilder sb = new StringBuilder(s.length());

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            // Handle common control characters
            if (c == '\r' || c == '\n' || c == '\t') {
                sb.append(' ');
                continue;
            }

            // Skip null and other control characters
            if (c < 0x20) {
                continue;
            }

            // Keep all Unicode characters (including Sinhala, Arabic, Chinese, etc.) if Unicode fonts are loaded
            if (useUnicode) {
                sb.append(c);
            } else {
                // Otherwise, only keep ASCII characters
                if (c < 128) {
                    sb.append(c);
                }
            }
        }

        return sb.toString();
    }

    /**
     * Sanitize text specifically for a font, removing characters that cannot be encoded
     */
    private static String sanitizeForFont(String text, PDFont font) {
        if (text == null || text.isEmpty()) return "";

        StringBuilder result = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            // Handle control characters
            if (c == '\r' || c == '\n' || c == '\t') {
                result.append(' ');
                continue;
            }

            if (c < 0x20) {
                continue;
            }

            // Check if the font can encode this character
            try {
                font.encode(String.valueOf(c));
                result.append(c);
            } catch (Exception e) {
                // Character not supported by font, replace with a space or question mark
                if (c > 127) {
                    result.append('?'); // Replace unsupported Unicode with ?
                }
            }
        }
        return result.toString();
    }

    /**
     * Safely get string width, handling encoding errors
     */
    private static float safeGetStringWidth(String text, PDFont font, float fontSize) throws IOException {
        try {
            return font.getStringWidth(text) / 1000f * fontSize;
        } catch (IllegalArgumentException e) {
            // Font doesn't support some characters, sanitize and try again
            String sanitized = sanitizeForFont(text, font);
            return font.getStringWidth(sanitized) / 1000f * fontSize;
        }
    }

    private static float writeAlignedBlock(PDPageContentStream cs,
                                          String text,
                                          float x,
                                          float y,
                                          PDFont font,
                                          float fontSize,
                                          float maxWidth,
                                          boolean isRightAligned,
                                          float pageWidth,
                                          boolean useUnicode) throws IOException {
        // Sanitize text for the specific font being used
        text = sanitizeForFont(text, font);
        List<String> lines = wrapText(text, maxWidth, font, fontSize);
        // keep consistent inter-line spacing
        for (String line : lines) {
            float lineX = x;
            if (isRightAligned) {
                // Calculate actual text width and position from right
                float textWidth = safeGetStringWidth(line, font, fontSize);
                lineX = pageWidth - MARGIN - textWidth;
            }

            cs.beginText();
            cs.setFont(font, fontSize);
            cs.newLineAtOffset(lineX, y);
            cs.showText(line);
            cs.endText();
            y -= LEADING;
        }
        return y;
    }

    private static void drawHeader(PDPageContentStream cs, float pageWidth, float pageHeight, PDFont font) throws IOException {
        String headerText = "DopamineLite";
        float textWidth = safeGetStringWidth(headerText, font, HEADER_FOOTER_FONT_SIZE);
        float x = (pageWidth - textWidth) / 2; // Center
        float y = pageHeight - 30;

        cs.beginText();
        cs.setFont(font, HEADER_FOOTER_FONT_SIZE);
        cs.newLineAtOffset(x, y);
        cs.showText(headerText);
        cs.endText();
    }

    private static void drawFooter(PDPageContentStream cs, float pageWidth, PDFont font, int pageNumber) throws IOException {
        String footerText = "Page " + pageNumber;
        float textWidth = safeGetStringWidth(footerText, font, HEADER_FOOTER_FONT_SIZE);
        float x = (pageWidth - textWidth) / 2; // Center
        float y = 30;

        cs.beginText();
        cs.setFont(font, HEADER_FOOTER_FONT_SIZE);
        cs.newLineAtOffset(x, y);
        cs.showText(footerText);
        cs.endText();
    }

    private static float writeLine(PDPageContentStream cs, String text, float y,
                                   PDFont font, float fontSize, float maxWidth,
                                   PDFont wrapFont) throws IOException {
        // Sanitize text for the specific font being used
        text = sanitizeForFont(text, wrapFont);
        List<String> lines = wrapText(text, maxWidth, wrapFont, fontSize);
        for (String line : lines) {
            cs.beginText();
            cs.setFont(font, fontSize);
            cs.newLineAtOffset(PdfGenerator.MARGIN, y);
            cs.showText(line);
            cs.endText();
            y -= LEADING;
        }
        return y;
    }

    private static List<String> wrapText(String text, float maxWidth, PDFont font, float fontSize) throws IOException {
        List<String> lines = new ArrayList<>();
        // Sanitize for font first
        text = sanitizeForFont(text, font);
        if (text.isEmpty()) {
            lines.add("");
            return lines;
        }

        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            float width = safeGetStringWidth(candidate, font, fontSize);

            if (width <= maxWidth) {
                if (!line.isEmpty()) line.append(' ');
                line.append(word);
            } else {
                // Add current line if not empty
                if (!line.isEmpty()) {
                    lines.add(line.toString());
                    line = new StringBuilder();
                }

                // Check if the word itself is too long
                float wordWidth = safeGetStringWidth(word, font, fontSize);
                if (wordWidth <= maxWidth) {
                    // Word fits on its own line
                    line.append(word);
                } else {
                    // Word is too long, need to break it character by character
                    List<String> brokenWord = breakLongWord(word, maxWidth, font, fontSize);
                    lines.addAll(brokenWord.subList(0, brokenWord.size() - 1));
                    // Last fragment goes into current line
                    if (!brokenWord.isEmpty()) {
                        line.append(brokenWord.get(brokenWord.size() - 1));
                    }
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

    /**
     * Break a long word into multiple lines by character
     */
    private static List<String> breakLongWord(String word, float maxWidth, PDFont font, float fontSize) throws IOException {
        List<String> fragments = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            String candidate = current.toString() + c;
            float width = safeGetStringWidth(candidate, font, fontSize);

            if (width <= maxWidth) {
                current.append(c);
            } else {
                // Current fragment is full
                if (!current.isEmpty()) {
                    fragments.add(current.toString());
                    current = new StringBuilder();
                }
                // Add the character to new fragment
                current.append(c);
            }
        }

        // Add remaining fragment
        if (!current.isEmpty()) {
            fragments.add(current.toString());
        }

        // Ensure we return at least one fragment
        if (fragments.isEmpty()) {
            fragments.add("");
        }

        return fragments;
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
