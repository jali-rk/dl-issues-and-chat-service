package com.dopaminelite.dl_issues_and_chat_service.utils;

import com.dopaminelite.dl_issues_and_chat_service.constants.IssueAssignmentStatus;
import com.dopaminelite.dl_issues_and_chat_service.constants.IssueStatus;
import com.dopaminelite.dl_issues_and_chat_service.constants.Role;
import com.dopaminelite.dl_issues_and_chat_service.dto.UserInfo;
import com.dopaminelite.dl_issues_and_chat_service.entity.Issue;
import com.dopaminelite.dl_issues_and_chat_service.entity.IssueMessage;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class PdfGeneratorTest {

    @Test
    public void testGenerateReportWithLongWords() {
        // Create an issue with very long words that should be wrapped
        UUID issueId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        Issue issue = Issue.builder()
                .id(issueId)
                .issueNumber(12345L)
                .title("Test Issue with VeryLongWordThatShouldBeWrappedProperlyInThePDFDocument")
                .description("ThisIsAVeryLongWordWithoutSpacesThatShouldBeWrappedCharacterByCharacterToFitWithinThePageMargins and normal text")
                .studentId(studentId)
                .assignedAdminId(adminId)
                .status(IssueStatus.OPEN)
                .assignmentStatus(IssueAssignmentStatus.ASSIGNED)
                .isChatReadOnly(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        // Create messages with long words
        IssueMessage message1 = IssueMessage.builder()
                .id(UUID.randomUUID())
                .issueId(issueId)
                .senderId(studentId)
                .senderRole(Role.STUDENT)
                .content("HelloThisIsAReallyLongWordThatNeedsToBeWrappedIntoMultipleLinesInThePDFBecauseItExceedsTheMaximumWidth")
                .createdAt(Instant.now())
                .build();

        IssueMessage message2 = IssueMessage.builder()
                .id(UUID.randomUUID())
                .issueId(issueId)
                .senderId(adminId)
                .senderRole(Role.ADMIN)
                .content("Normal message with regular words and AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA long sequence")
                .createdAt(Instant.now())
                .build();

        List<IssueMessage> messages = Arrays.asList(message1, message2);

        // Create user map
        Map<UUID, UserInfo> userMap = new HashMap<>();
        UserInfo student = new UserInfo();
        student.setId(studentId);
        student.setFullName("John Doe");
        userMap.put(studentId, student);

        UserInfo admin = new UserInfo();
        admin.setId(adminId);
        admin.setFullName("Admin User");
        userMap.put(adminId, admin);

        // Generate PDF - should not throw exception
        byte[] pdfBytes = PdfGenerator.generateIssueReport(issue, messages, userMap);

        // Verify PDF was generated
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);

        // PDF should start with PDF magic bytes
        assertEquals('%', (char) pdfBytes[0]);
        assertEquals('P', (char) pdfBytes[1]);
        assertEquals('D', (char) pdfBytes[2]);
        assertEquals('F', (char) pdfBytes[3]);
    }

    @Test
    public void testGenerateReportWithSinhalaCharacters() {
        // Test with Sinhala characters (should be replaced with ?)
        UUID issueId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        Issue issue = Issue.builder()
                .id(issueId)
                .issueNumber(123L)
                .title("Issue with Sinhala: මගේ ගැටළුව")
                .description("Description: මෙය පරීක්ෂණයකි")
                .studentId(studentId)
                .status(IssueStatus.OPEN)
                .assignmentStatus(IssueAssignmentStatus.UNASSIGNED)
                .isChatReadOnly(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        IssueMessage message = IssueMessage.builder()
                .id(UUID.randomUUID())
                .issueId(issueId)
                .senderId(studentId)
                .senderRole(Role.STUDENT)
                .content("Message with Sinhala: ස්තූතියි")
                .createdAt(Instant.now())
                .build();

        Map<UUID, UserInfo> userMap = new HashMap<>();
        UserInfo student = new UserInfo();
        student.setId(studentId);
        student.setFullName("Student");
        userMap.put(studentId, student);

        // Should not throw exception
        byte[] pdfBytes = PdfGenerator.generateIssueReport(issue, Collections.singletonList(message), userMap);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    public void testGenerateReportWithEmptyMessages() {
        UUID issueId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        Issue issue = Issue.builder()
                .id(issueId)
                .issueNumber(1L)
                .title("Empty Messages Issue")
                .description("Test")
                .studentId(studentId)
                .status(IssueStatus.OPEN)
                .assignmentStatus(IssueAssignmentStatus.UNASSIGNED)
                .isChatReadOnly(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Map<UUID, UserInfo> userMap = new HashMap<>();

        byte[] pdfBytes = PdfGenerator.generateIssueReport(issue, Collections.emptyList(), userMap);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }
}

