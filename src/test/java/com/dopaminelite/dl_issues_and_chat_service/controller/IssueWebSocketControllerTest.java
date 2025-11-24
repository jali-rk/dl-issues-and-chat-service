package com.dopaminelite.dl_issues_and_chat_service.controller;

import com.dopaminelite.dl_issues_and_chat_service.constants.Role;
import com.dopaminelite.dl_issues_and_chat_service.dto.UploadedFileRef;
import com.dopaminelite.dl_issues_and_chat_service.dto.websocket.WebSocketMessageEnvelope;
import com.dopaminelite.dl_issues_and_chat_service.dto.websocket.WebSocketMessageEventPayload;
import com.dopaminelite.dl_issues_and_chat_service.dto.websocket.WebSocketSendMessagePayload;
import com.dopaminelite.dl_issues_and_chat_service.entity.IssueMessage;
import com.dopaminelite.dl_issues_and_chat_service.service.IssueMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssueWebSocketControllerTest {

    private SimpMessagingTemplate messagingTemplate;
    private IssueMessageService messageService;
    private IssueWebSocketController controller;

    private ArgumentCaptor<Object> payloadCaptor;

    @BeforeEach
    void setUp() {
        messagingTemplate = Mockito.mock(SimpMessagingTemplate.class);
        messageService = Mockito.mock(IssueMessageService.class);
        controller = new IssueWebSocketController(messagingTemplate, messageService);
        payloadCaptor = ArgumentCaptor.forClass(Object.class);
    }

    @Test
    void sendMessage_withAttachments_sendsEnvelope() {
        UUID issueId = UUID.randomUUID();
        WebSocketSendMessagePayload payload = new WebSocketSendMessagePayload();
        payload.setContent("Hello");
        UploadedFileRef ref = new UploadedFileRef();
        ref.setFileId("file-1");
        payload.setAttachments(List.of(ref));

        IssueMessage created = IssueMessage.builder()
                .id(UUID.randomUUID())
                .issueId(issueId)
                .senderId(UUID.randomUUID())
                .senderRole(Role.STUDENT)
                .content("Hello")
                .attachment(ref)
                .createdAt(Instant.now())
                .build();

        when(messageService.createMessage(eq(issueId), eq("Hello"), eq(payload.getAttachments()))).thenReturn(created);

        controller.sendMessage(issueId, payload);

        verify(messageService, times(1)).createMessage(eq(issueId), eq("Hello"), eq(payload.getAttachments()));

        String expectedDestination = "/topic/issues/" + issueId;
        verify(messagingTemplate, times(1)).convertAndSend(eq(expectedDestination), payloadCaptor.capture());

        Object sent = payloadCaptor.getValue();
        assertNotNull(sent);
        assertInstanceOf(WebSocketMessageEnvelope.class, sent);
        WebSocketMessageEnvelope env = (WebSocketMessageEnvelope) sent;
        assertEquals("MESSAGE", env.getType());
        assertEquals(issueId, env.getIssueId());
        assertNotNull(env.getPayload());
        assertInstanceOf(WebSocketMessageEventPayload.class, env.getPayload());
        WebSocketMessageEventPayload event = (WebSocketMessageEventPayload) env.getPayload();
        assertEquals(created.getId(), event.getId());
        assertEquals(created.getIssueId(), event.getIssueId());
        assertEquals(created.getContent(), event.getContent());
    }

    @Test
    void sendMessage_withoutAttachments_sendsEnvelope() {
        UUID issueId = UUID.randomUUID();
        WebSocketSendMessagePayload payload = new WebSocketSendMessagePayload();
        payload.setContent("No attach");
        payload.setAttachments(null);

        IssueMessage created = IssueMessage.builder()
                .id(UUID.randomUUID())
                .issueId(issueId)
                .senderId(UUID.randomUUID())
                .senderRole(Role.STUDENT)
                .content("No attach")
                .attachment(null)
                .createdAt(Instant.now())
                .build();

        when(messageService.createMessage(eq(issueId), eq("No attach"), eq(null))).thenReturn(created);

        controller.sendMessage(issueId, payload);

        verify(messageService, times(1)).createMessage(eq(issueId), eq("No attach"), eq(null));

        String expectedDestination = "/topic/issues/" + issueId;
        verify(messagingTemplate, times(1)).convertAndSend(eq(expectedDestination), payloadCaptor.capture());

        Object sent = payloadCaptor.getValue();
        assertNotNull(sent);
        assertInstanceOf(WebSocketMessageEnvelope.class, sent);
        WebSocketMessageEnvelope env = (WebSocketMessageEnvelope) sent;
        assertEquals("MESSAGE", env.getType());
        assertEquals(issueId, env.getIssueId());
        assertNotNull(env.getPayload());
    }
}
