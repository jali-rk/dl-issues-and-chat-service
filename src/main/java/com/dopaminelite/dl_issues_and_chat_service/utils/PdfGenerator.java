package com.dopaminelite.dl_issues_and_chat_service.utils;

import com.dopaminelite.dl_issues_and_chat_service.entity.Issue;
import com.dopaminelite.dl_issues_and_chat_service.entity.IssueMessage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PdfGenerator {

    public static byte[] generateIssueReport(Issue issue, List<IssueMessage> messages) {
        DateTimeFormatter dtf = DateTimeFormatter.ISO_INSTANT;

        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float margin = 50;
                float curY = page.getMediaBox().getHeight() - margin;
                float leading = 14f;
                float fontSize = 12f;

                PDType1Font regularFont = new PDType1Font(Standard14Fonts.FontName.TIMES_ROMAN);
                PDType1Font headerFont = new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD);

                // Helper to write one line and update curY
                class LineWriter {
                    float y = curY;

                    void writeLine(String text, boolean isHeader) throws IOException {
                        cs.beginText();
                        cs.setFont(isHeader ? headerFont : regularFont, fontSize);
                        cs.newLineAtOffset(margin, y);
                        cs.showText(text != null ? text : "");
                        cs.endText();
                        y -= leading;
                    }
                }

                LineWriter writer = new LineWriter();

                // Issue title
                writer.writeLine("Issue Report", true);

                // Issue details
                writer.writeLine("Issue ID: " + issue.getId(), false);
                writer.writeLine("Title: " + issue.getTitle(), false);
                writer.writeLine("Description: " + issue.getDescription(), false);
                writer.writeLine("Status: " + issue.getStatus(), false);
                writer.writeLine("Assigned Admin ID: " + issue.getAssignedAdminId(), false);
                writer.writeLine("Created At: " + dtf.format(issue.getCreatedAt()), false);
                writer.writeLine("Solved At: " + (issue.getSolvedAt() != null ? dtf.format(issue.getSolvedAt()) : "N/A"), false);

                writer.writeLine("", false); // blank line
                writer.writeLine("Conversation:", true);

                // Messages
                for (IssueMessage m : messages) {
                    String header = String.format("[%s] %s (%s):",
                            m.getCreatedAt() != null ? dtf.format(m.getCreatedAt()) : "",
                            m.getSenderRole(),
                            m.getSenderId());
                    writer.writeLine(header, true);
                    writer.writeLine(m.getContent(), false);
                }
            }

            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }
}