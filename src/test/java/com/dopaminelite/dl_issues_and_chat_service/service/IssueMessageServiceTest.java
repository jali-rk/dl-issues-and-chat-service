package com.dopaminelite.dl_issues_and_chat_service.service;

import com.dopaminelite.dl_issues_and_chat_service.constants.Role;
import com.dopaminelite.dl_issues_and_chat_service.dto.IssueMessageListResponse;
import com.dopaminelite.dl_issues_and_chat_service.dto.UploadedFileRef;
import com.dopaminelite.dl_issues_and_chat_service.entity.IssueMessage;
import com.dopaminelite.dl_issues_and_chat_service.repository.IssueMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssueMessageServiceTest {

    @Mock
    private IssueMessageRepository issueMessageRepository;

    @InjectMocks
    private IssueMessageService service;

    @Captor
    private ArgumentCaptor<IssueMessage> msgCaptor;

    @Captor
    private ArgumentCaptor<Pageable> pageCaptor;

    private UUID issueId;

    @BeforeEach
    void setUp() {
        issueId = UUID.randomUUID();
    }

    @Test
    void createMessage_withAttachments_usesFirstAttachment() {
        UploadedFileRef a1 = new UploadedFileRef();
        a1.setFileId("f1");
        UploadedFileRef a2 = new UploadedFileRef();
        a2.setFileId("f2");

        IssueMessage saved = IssueMessage.builder()
                .id(UUID.randomUUID())
                .issueId(issueId)
                .content("hello")
                .attachment(a1)
                .senderId(UUID.randomUUID())
                .senderRole(Role.STUDENT)
                .createdAt(Instant.now())
                .build();

        when(issueMessageRepository.save(any(IssueMessage.class))).thenReturn(saved);

        IssueMessage result = service.createMessage(issueId, "hello", List.of(a1, a2));

        assertNotNull(result);
        assertEquals(saved.getId(), result.getId());

        verify(issueMessageRepository).save(msgCaptor.capture());
        IssueMessage captured = msgCaptor.getValue();
        assertEquals(issueId, captured.getIssueId());
        assertEquals("hello", captured.getContent());
        assertNotNull(captured.getAttachment());
        assertEquals("f1", captured.getAttachment().getFileId());
        assertNotNull(captured.getSenderId());
        assertEquals(Role.STUDENT, captured.getSenderRole());
    }

    @Test
    void createMessage_withEmptyAttachments_setsNullAttachment() {
        IssueMessage saved = IssueMessage.builder()
                .id(UUID.randomUUID())
                .issueId(issueId)
                .content("noattach")
                .attachment(null)
                .senderId(UUID.randomUUID())
                .senderRole(Role.STUDENT)
                .createdAt(Instant.now())
                .build();

        when(issueMessageRepository.save(any(IssueMessage.class))).thenReturn(saved);

        IssueMessage result = service.createMessage(issueId, "noattach", List.of());

        assertNotNull(result);
        verify(issueMessageRepository).save(msgCaptor.capture());
        IssueMessage captured = msgCaptor.getValue();
        assertNull(captured.getAttachment());
        assertNotNull(captured.getSenderId());
        assertEquals(Role.STUDENT, captured.getSenderRole());
    }

    @Test
    void createMessage_withNullAttachments_setsNullAttachment() {
        IssueMessage saved = IssueMessage.builder()
                .id(UUID.randomUUID())
                .issueId(issueId)
                .content("nullattach")
                .attachment(null)
                .senderId(UUID.randomUUID())
                .senderRole(Role.STUDENT)
                .createdAt(Instant.now())
                .build();

        when(issueMessageRepository.save(any(IssueMessage.class))).thenReturn(saved);

        IssueMessage result = service.createMessage(issueId, "nullattach", null);

        assertNotNull(result);
        verify(issueMessageRepository).save(msgCaptor.capture());
        IssueMessage captured = msgCaptor.getValue();
        assertNull(captured.getAttachment());
        assertNotNull(captured.getSenderId());
        assertEquals(Role.STUDENT, captured.getSenderRole());
    }

    @Test
    void createMessage_withSenderIdAndRole_usesThem() {
        UUID senderId = UUID.randomUUID();
        IssueMessage saved = IssueMessage.builder()
                .id(UUID.randomUUID())
                .issueId(issueId)
                .content("admin message")
                .senderId(senderId)
                .senderRole(Role.ADMIN)
                .createdAt(Instant.now())
                .build();

        when(issueMessageRepository.save(any(IssueMessage.class))).thenReturn(saved);

        IssueMessage result = service.createMessage(issueId, "admin message", null, senderId, "ADMIN");

        assertNotNull(result);
        verify(issueMessageRepository).save(msgCaptor.capture());
        IssueMessage captured = msgCaptor.getValue();
        assertEquals(senderId, captured.getSenderId());
        assertEquals(Role.ADMIN, captured.getSenderRole());
    }

    @Test
    void createMessage_withInvalidRole_defaultsToStudent() {
        UUID senderId = UUID.randomUUID();
        IssueMessage saved = IssueMessage.builder()
                .id(UUID.randomUUID())
                .issueId(issueId)
                .content("message")
                .senderId(senderId)
                .senderRole(Role.STUDENT)
                .createdAt(Instant.now())
                .build();

        when(issueMessageRepository.save(any(IssueMessage.class))).thenReturn(saved);

        IssueMessage result = service.createMessage(issueId, "message", null, senderId, "INVALID_ROLE");

        assertNotNull(result);
        verify(issueMessageRepository).save(msgCaptor.capture());
        IssueMessage captured = msgCaptor.getValue();
        assertEquals(Role.STUDENT, captured.getSenderRole());
    }

    @Test
    void createMessage_withNullIssueId_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            service.createMessage(null, "content", null);
        });

        verify(issueMessageRepository, never()).save(any());
    }

    @Test
    void listMessages_callsRepositoryWithCalculatedPageAndReturnsResponse() {
        IssueMessage m1 = IssueMessage.builder()
                .id(UUID.randomUUID())
                .issueId(issueId)
                .content("m1")
                .createdAt(Instant.now())
                .build();
        IssueMessage m2 = IssueMessage.builder()
                .id(UUID.randomUUID())
                .issueId(issueId)
                .content("m2")
                .createdAt(Instant.now())
                .build();

        when(issueMessageRepository.findByIssueIdOrderByCreatedAtAsc(eq(issueId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(m1, m2)));

        int offset = 0;
        int limit = 10;
        IssueMessageListResponse resp = service.listMessages(issueId, offset, limit);

        assertNotNull(resp);
        assertEquals(2, resp.getTotal());
        assertEquals(2, resp.getItems().size());

        verify(issueMessageRepository).findByIssueIdOrderByCreatedAtAsc(eq(issueId), pageCaptor.capture());
        Pageable captured = pageCaptor.getValue();
        assertEquals(offset, captured.getPageNumber());
        assertEquals(limit, captured.getPageSize());
    }

    @Test
    void listMessages_offsetZero_usesPageZero() {
        IssueMessage m = IssueMessage.builder()
                .id(UUID.randomUUID())
                .issueId(issueId)
                .content("m")
                .createdAt(Instant.now())
                .build();
        when(issueMessageRepository.findByIssueIdOrderByCreatedAtAsc(eq(issueId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(m)));

        IssueMessageListResponse resp = service.listMessages(issueId, 0, 10);
        assertEquals(1, resp.getTotal());

        verify(issueMessageRepository).findByIssueIdOrderByCreatedAtAsc(eq(issueId), pageCaptor.capture());
        Pageable p = pageCaptor.getValue();
        assertEquals(0, p.getPageNumber());
        assertEquals(10, p.getPageSize());
    }

    @Test
    void listMessages_withNullIssueId_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            service.listMessages(null, 0, 10);
        });

        verify(issueMessageRepository, never()).findByIssueIdOrderByCreatedAtAsc(any(), any());
    }

    @Test
    void listMessages_withNegativeOffset_usesZero() {
        IssueMessage m = IssueMessage.builder()
                .id(UUID.randomUUID())
                .issueId(issueId)
                .content("m")
                .createdAt(Instant.now())
                .build();
        when(issueMessageRepository.findByIssueIdOrderByCreatedAtAsc(eq(issueId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(m)));

        service.listMessages(issueId, -5, 10);

        verify(issueMessageRepository).findByIssueIdOrderByCreatedAtAsc(eq(issueId), pageCaptor.capture());
        Pageable p = pageCaptor.getValue();
        assertEquals(0, p.getPageNumber());
    }

    @Test
    void listMessages_withNegativeLimit_usesOne() {
        IssueMessage m = IssueMessage.builder()
                .id(UUID.randomUUID())
                .issueId(issueId)
                .content("m")
                .createdAt(Instant.now())
                .build();
        when(issueMessageRepository.findByIssueIdOrderByCreatedAtAsc(eq(issueId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(m)));

        service.listMessages(issueId, 0, -10);

        verify(issueMessageRepository).findByIssueIdOrderByCreatedAtAsc(eq(issueId), pageCaptor.capture());
        Pageable p = pageCaptor.getValue();
        assertEquals(1, p.getPageSize());
    }
}