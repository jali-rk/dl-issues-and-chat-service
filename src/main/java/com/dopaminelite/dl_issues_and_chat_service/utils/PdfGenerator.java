package com.dopaminelite.dl_issues_and_chat_service.utils;

import com.dopaminelite.dl_issues_and_chat_service.constants.Role;
import com.dopaminelite.dl_issues_and_chat_service.dto.UserInfo;
import com.dopaminelite.dl_issues_and_chat_service.entity.Issue;
import com.dopaminelite.dl_issues_and_chat_service.entity.IssueMessage;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
public class PdfGenerator {

    private static final float MARGIN = 50f;
    private static final float TITLE_FONT_SIZE = 14f;
    private static final float DETAILS_FONT_SIZE = 11f;
    private static final float MESSAGE_META_FONT_SIZE = 9f;
    private static final float MESSAGE_BODY_FONT_SIZE = 12f;
    private static final float HEADER_FOOTER_FONT_SIZE = 10f;
    private static final float DATE_HEADER_FONT_SIZE = 11f;

    private static final DateTimeFormatter READABLE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss")
                    .withZone(ZoneId.systemDefault());

    private static final DateTimeFormatter MESSAGE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm")
                    .withZone(ZoneId.systemDefault());

    private static final DateTimeFormatter DATE_HEADER_FORMAT =
            DateTimeFormatter.ofPattern("dd MMMM yyyy")
                    .withZone(ZoneId.systemDefault());

    public static byte[] generateIssueReport(
            Issue issue,
            List<IssueMessage> messages,
            Map<UUID, UserInfo> userMap
    ) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document doc = new Document(pdf, PageSize.A4);
            doc.setMargins(MARGIN, MARGIN, MARGIN, MARGIN);

            // Load fonts (EMBEDDED, UNICODE)
            PdfFont latinRegular = loadFont("fonts/NotoSans-Regular.ttf");
            PdfFont latinBold = loadFont("fonts/NotoSans-Bold.ttf");
            PdfFont sinhalaRegular = loadFont("fonts/NotoSansSinhala-Regular.ttf");
            PdfFont sinhalaBold = loadFont("fonts/NotoSansSinhala-Bold.ttf");

            // Register footer event handler
            pdf.addEventHandler(PdfDocumentEvent.END_PAGE, new FooterHandler(latinRegular));

            // Header
            doc.add(new Paragraph("DopamineLite")
                    .setFont(latinBold)
                    .setFontSize(HEADER_FOOTER_FONT_SIZE)
                    .setTextAlignment(TextAlignment.CENTER));

            // Title
            doc.add(new Paragraph("Issue Report - Issue No. : " + issue.getIssueNumber())
                    .setFont(latinBold)
                    .setFontSize(TITLE_FONT_SIZE)
                    .setMultipliedLeading(0.5f));  // reduced line spacing

// Details
            doc.add(new Paragraph("Issue ID: " + issue.getId())
                    .setFont(latinRegular)
                    .setFontSize(DETAILS_FONT_SIZE)
                    .setMultipliedLeading(0.5f));

            doc.add(mixed(
                    "Title: " + safe(issue.getTitle()),
                    latinRegular, sinhalaRegular,
                    DETAILS_FONT_SIZE, TextAlignment.LEFT
            ).setMultipliedLeading(0.5f));

            doc.add(mixed(
                    "Description: " + safe(issue.getDescription()),
                    latinRegular, sinhalaRegular,
                    DETAILS_FONT_SIZE, TextAlignment.LEFT
            ).setMultipliedLeading(0.5f));

            doc.add(new Paragraph("Status: " + issue.getStatus())
                    .setFont(latinRegular)
                    .setFontSize(DETAILS_FONT_SIZE)
                    .setMultipliedLeading(0.5f));

// Assigned admin
            String assignedAdminName = "Not Assigned";
            if (issue.getAssignedAdminId() != null) {
                UserInfo adminInfo = userMap.get(issue.getAssignedAdminId());
                if (adminInfo != null && adminInfo.getFullName() != null) {
                    assignedAdminName = adminInfo.getFullName();
                }
            }

            doc.add(mixed(
                    "Assigned Admin: " + assignedAdminName,
                    latinRegular, sinhalaRegular,
                    DETAILS_FONT_SIZE, TextAlignment.LEFT
            ).setMultipliedLeading(0.5f));

            doc.add(new Paragraph("Created At: " +
                    READABLE_DATE_FORMAT.format(issue.getCreatedAt()))
                    .setFont(latinRegular)
                    .setFontSize(DETAILS_FONT_SIZE)
                    .setMultipliedLeading(0.5f));

            doc.add(new Paragraph("Solved At: " +
                    (issue.getSolvedAt() != null
                            ? READABLE_DATE_FORMAT.format(issue.getSolvedAt())
                            : "N/A"))
                    .setFont(latinRegular)
                    .setFontSize(DETAILS_FONT_SIZE)
                    .setMultipliedLeading(0.5f));

            // Conversation
            doc.add(new Paragraph("Conversation:")
                    .setFont(latinBold)
                    .setFontSize(DETAILS_FONT_SIZE));

            // Group messages by date
            Map<LocalDate, List<IssueMessage>> grouped = new LinkedHashMap<>();
            for (IssueMessage m : messages) {
                LocalDate d = m.getCreatedAt()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
                grouped.computeIfAbsent(d, k -> new ArrayList<>()).add(m);
            }

            for (var entry : grouped.entrySet()) {
                doc.add(new Paragraph(DATE_HEADER_FORMAT.format(entry.getKey()))
                        .setFont(latinRegular)
                        .setFontSize(DATE_HEADER_FONT_SIZE)
                        .setTextAlignment(TextAlignment.CENTER));

                for (IssueMessage m : entry.getValue()) {
                    String meta = String.format(
                            "%s | %s | [%s]",
                            getUserName(m.getSenderId(), userMap),
                            MESSAGE_TIME_FORMAT.format(m.getCreatedAt()),
                            formatRoleLabel(m.getSenderRole())
                    );

                    // Determine alignment based on role
                    boolean isAdminSide = (m.getSenderRole() == Role.ADMIN || m.getSenderRole() == Role.MAIN_ADMIN);
                    TextAlignment alignment = isAdminSide ? TextAlignment.RIGHT : TextAlignment.LEFT;
                    HorizontalAlignment hAlign = isAdminSide ? HorizontalAlignment.RIGHT : HorizontalAlignment.LEFT;

                    // Meta line with constrained width
                    Paragraph metaPara = mixed(
                            meta,
                            latinRegular, sinhalaRegular,
                            MESSAGE_META_FONT_SIZE, alignment
                    );
                    metaPara.setWidth(UnitValue.createPercentValue(85));
                    metaPara.setHorizontalAlignment(hAlign);
                    // Disable line splitting/wrapping (best-effort)
                    metaPara.setKeepTogether(true);
                    doc.add(metaPara);

                    // Body with constrained width
                    String body = safe(m.getContent());
                    Paragraph bodyPara = mixed(
                            body,
                            latinRegular, sinhalaRegular,
                            MESSAGE_BODY_FONT_SIZE, alignment
                    );
                    bodyPara.setWidth(UnitValue.createPercentValue(85));
                    bodyPara.setHorizontalAlignment(hAlign);
                    // Disable line splitting/wrapping (best-effort)
                    bodyPara.setKeepTogether(true);
                    doc.add(bodyPara);
                }
            }

            // Remove manual footer paragraph
            // doc.add(new Paragraph("Page " + pdf.getNumberOfPages())
            //         .setFont(latinRegular)
            //         .setFontSize(HEADER_FOOTER_FONT_SIZE)
            //         .setTextAlignment(TextAlignment.CENTER));

            doc.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    // ===== FOOTER HANDLER =====
    private static class FooterHandler implements IEventHandler {
        private final PdfFont font;
        public FooterHandler(PdfFont font) {
            this.font = font;
        }
        @Override
        public void handleEvent(Event event) {
            PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
            PdfDocument pdfDoc = docEvent.getDocument();
            int pageNumber = pdfDoc.getPageNumber(docEvent.getPage());
            PdfCanvas canvas = new PdfCanvas(docEvent.getPage());
            String footer = "Page " + pageNumber;
            float x = pdfDoc.getDefaultPageSize().getWidth() / 2;
            float y = MARGIN / 2; // Place at bottom margin
            canvas.beginText()
                    .setFontAndSize(font, HEADER_FOOTER_FONT_SIZE)
                    .moveText(x, y)
                    .showText(footer)
                    .endText();
            canvas.release();
        }
    }

    // ===== FONT LOADER =====
    private static PdfFont loadFont(String resourcePath) throws IOException {
        try (var is = PdfGenerator.class
                .getClassLoader()
                .getResourceAsStream(resourcePath)) {

            if (is == null) {
                throw new IOException("Font resource not found: " + resourcePath);
            }

            byte[] fontBytes = is.readAllBytes();

            return PdfFontFactory.createFont(
                    fontBytes,
                    PdfEncodings.IDENTITY_H
            );
        }
    }


    // ===== MIXED TEXT (THE IMPORTANT PART) =====
    private static Paragraph mixed(
            String text,
            PdfFont latin,
            PdfFont sinhala,
            float size,
            TextAlignment alignment
    ) {
        Paragraph p = new Paragraph()
                .setFontSize(size)
                .setTextAlignment(alignment)
                .setMultipliedLeading(0.75f);;

        if (text == null || text.isEmpty()) return p;

        StringBuilder buf = new StringBuilder();
        boolean sinhalaRun = isSinhala(text.charAt(0));

        for (char c : text.toCharArray()) {
            boolean isSinhala = isSinhala(c);
            if (isSinhala != sinhalaRun) {
                p.add(new Text(buf.toString())
                        .setFont(sinhalaRun ? sinhala : latin));
                buf.setLength(0);
                sinhalaRun = isSinhala;
            }
            buf.append(c);
        }

        if (!buf.isEmpty()) {
            p.add(new Text(buf.toString())
                    .setFont(sinhalaRun ? sinhala : latin));
        }

        return p;
    }

    private static boolean isSinhala(char c) {
        return c >= '\u0D80' && c <= '\u0DFF';
    }

    // ===== UTILS =====
    private static String getUserName(UUID id, Map<UUID, UserInfo> map) {
        if (id == null) return "Unknown User";
        UserInfo u = map.get(id);
        return u != null && u.getFullName() != null
                ? u.getFullName()
                : "Unknown User";
    }

    private static String formatRoleLabel(Role role) {
        if (role == null) return "Unknown";
        return switch (role) {
            case ADMIN -> "Admin";
            case MAIN_ADMIN -> "Main Admin";
            case STUDENT -> "Student";
        };
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
