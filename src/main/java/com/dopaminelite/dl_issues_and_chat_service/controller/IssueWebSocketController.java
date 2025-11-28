package com.dopaminelite.dl_issues_and_chat_service.controller;

import com.dopaminelite.dl_issues_and_chat_service.dto.websocket.WebSocketMessageEnvelope;
import com.dopaminelite.dl_issues_and_chat_service.dto.websocket.WebSocketMessageEventPayload;
import com.dopaminelite.dl_issues_and_chat_service.dto.websocket.WebSocketSendMessagePayload;
import com.dopaminelite.dl_issues_and_chat_service.entity.IssueMessage;
import com.dopaminelite.dl_issues_and_chat_service.service.IssueMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Optional;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class IssueWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final IssueMessageService messageService;

    public void sendMessage(UUID issueId, WebSocketSendMessagePayload payload) {
        log.debug("Processing internal sendMessage for issueId: {}", issueId);

        try {
            IssueMessage msg = messageService.createMessage(issueId, payload.getContent(), payload.getAttachments());

            WebSocketMessageEventPayload eventPayload = new WebSocketMessageEventPayload(msg);

            WebSocketMessageEnvelope envelope = WebSocketMessageEnvelope.builder()
                    .type("MESSAGE")
                    .issueId(issueId)
                    .payload(eventPayload)
                    .build();

            messagingTemplate.convertAndSend("/topic/issues/" + issueId, envelope);
            log.debug("WebSocket message sent to /topic/issues/{}", issueId);

        } catch (Exception e) {
            log.error("Failed to process internal sendMessage for issueId: {}", issueId, e);
        }
    }

    @MessageMapping("/issues/{issueId}/send")
    public void sendMessage(
            @DestinationVariable String issueId,
            @Payload WebSocketSendMessagePayload payload,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        log.debug("WebSocket message received for issueId: {}, payload: {}", issueId, payload);

        UUID parsedIssueId;
        try {
            parsedIssueId = UUID.fromString(issueId);
        } catch (IllegalArgumentException ex) {
            log.error("Invalid issueId provided in WebSocket path: {}", issueId, ex);
            return;
        }

        try {
            Optional<String> possibleSender = Optional.ofNullable(headerAccessor.getFirstNativeHeader("x-sender-id"));
            Optional<String> possibleRole = Optional.ofNullable(headerAccessor.getFirstNativeHeader("x-sender-role"));

            UUID senderId = possibleSender.map(UUID::fromString).orElse(null);
            String senderRole = possibleRole.orElse(null);

            log.debug("Sender info extracted: senderId: {}, senderRole: {}", senderId, senderRole);

            IssueMessage msg = messageService.createMessage(
                    parsedIssueId,
                    payload.getContent(),
                    payload.getAttachments(),
                    senderId,
                    senderRole
            );

            WebSocketMessageEventPayload eventPayload = new WebSocketMessageEventPayload(msg);

            WebSocketMessageEnvelope envelope = WebSocketMessageEnvelope.builder()
                    .type("MESSAGE")
                    .issueId(parsedIssueId)
                    .payload(eventPayload)
                    .build();

            messagingTemplate.convertAndSend("/topic/issues/" + parsedIssueId, envelope);
            log.debug("Broadcasted message to /topic/issues/{}", parsedIssueId);

        } catch (Exception e) {
            log.error("Failed to process WebSocket message for issueId: {}", issueId, e);
        }
    }
}
