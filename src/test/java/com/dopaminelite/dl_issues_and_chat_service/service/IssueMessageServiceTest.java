// ...existing code...
package com.dopaminelite.dl_issues_and_chat_service.service;

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
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssueMessageServiceTest {

    @Mock
    private IssueMessageRepository repository;

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
                .createdAt(Instant.now())
                .build();

        when(repository.save(any(IssueMessage.class))).thenReturn(saved);

        IssueMessage result = service.createMessage(issueId, "hello", List.of(a1, a2));

        assertNotNull(result);
        assertEquals(saved.getId(), result.getId());

        verify(repository).save(msgCaptor.capture());
        IssueMessage captured = msgCaptor.getValue();
        assertEquals(issueId, captured.getIssueId());
        assertEquals("hello", captured.getContent());
        assertNotNull(captured.getAttachment());
        assertEquals("f1", captured.getAttachment().getFileId());
    }

    @Test
    void createMessage_withEmptyAttachments_setsNullAttachment() {
        IssueMessage saved = IssueMessage.builder()
                .id(UUID.randomUUID())
                .issueId(issueId)
                .content("noattach")
                .attachment(null)
                .createdAt(Instant.now())
                .build();

        when(repository.save(any(IssueMessage.class))).thenReturn(saved);

        IssueMessage result = service.createMessage(issueId, "noattach", List.of());

        assertNotNull(result);
        verify(repository).save(msgCaptor.capture());
        IssueMessage captured = msgCaptor.getValue();
        assertNull(captured.getAttachment());
    }

    @Test
    void createMessage_withNullAttachments_setsNullAttachment() {
        IssueMessage saved = IssueMessage.builder()
                .id(UUID.randomUUID())
                .issueId(issueId)
                .content("nullattach")
                .attachment(null)
                .createdAt(Instant.now())
                .build();

        when(repository.save(any(IssueMessage.class))).thenReturn(saved);

        IssueMessage result = service.createMessage(issueId, "nullattach", null);

        assertNotNull(result);
        verify(repository).save(msgCaptor.capture());
        IssueMessage captured = msgCaptor.getValue();
        assertNull(captured.getAttachment());
    }

    @Test
    void listMessages_callsRepositoryWithCalculatedPageAndReturnsResponse() {
        IssueMessage m1 = IssueMessage.builder().id(UUID.randomUUID()).issueId(issueId).content("m1").createdAt(Instant.now()).build();
        IssueMessage m2 = IssueMessage.builder().id(UUID.randomUUID()).issueId(issueId).content("m2").createdAt(Instant.now()).build();

        when(repository.findByIssueIdOrderByCreatedAtAsc(eq(issueId), any(Pageable.class)))
                .thenReturn(List.of(m1, m2));

        int offset = 15;
        int limit = 10;
        IssueMessageListResponse resp = service.listMessages(issueId, offset, limit);

        assertNotNull(resp);
        assertEquals(2, resp.getTotal());
        assertEquals(2, resp.getItems().size());

        verify(repository).findByIssueIdOrderByCreatedAtAsc(eq(issueId), pageCaptor.capture());
        Pageable captured = pageCaptor.getValue();
        // page is offset / limit integer division
        assertEquals(offset / limit, captured.getPageNumber());
        assertEquals(limit, captured.getPageSize());
    }

    @Test
    void listMessages_offsetZero_usesPageZero() {
        IssueMessage m = IssueMessage.builder().id(UUID.randomUUID()).issueId(issueId).content("m").createdAt(Instant.now()).build();
        when(repository.findByIssueIdOrderByCreatedAtAsc(eq(issueId), any(Pageable.class))).thenReturn(List.of(m));

        IssueMessageListResponse resp = service.listMessages(issueId, 0, 10);
        assertEquals(1, resp.getTotal());

        verify(repository).findByIssueIdOrderByCreatedAtAsc(eq(issueId), pageCaptor.capture());
        Pageable p = pageCaptor.getValue();
        assertEquals(0, p.getPageNumber());
        assertEquals(10, p.getPageSize());
    }
}

