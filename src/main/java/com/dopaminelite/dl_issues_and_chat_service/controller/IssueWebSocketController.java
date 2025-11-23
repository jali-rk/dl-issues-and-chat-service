package com.dopaminelite.dl_issues_and_chat_service.websocket;

import com.dopaminelite.dl_issues_and_chat_service.dto.websocket.WebSocketMessageEnvelope;
import com.dopaminelite.dl_issues_and_chat_service.dto.websocket.WebSocketMessageEventPayload;
import com.dopaminelite.dl_issues_and_chat_service.dto.websocket.WebSocketSendMessagePayload;
import com.dopaminelite.dl_issues_and_chat_service.entity.IssueMessage;
import com.dopaminelite.dl_issues_and_chat_service.service.IssueMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class IssueWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final IssueMessageService messageService;

    @MessageMapping("/issues/{issueId}/send")
    public void sendMessage(
            @DestinationVariable("issueId") UUID issueId,
            WebSocketSendMessagePayload payload
    ) {
        IssueMessage msg = messageService.createMessage(issueId, payload.getContent(), payload.getAttachments());
        WebSocketMessageEventPayload eventPayload = new WebSocketMessageEventPayload(msg);

        messagingTemplate.convertAndSend(
                "/topic/issues/" + issueId,
                WebSocketMessageEnvelope.builder()
                        .type("MESSAGE")
                        .issueId(issueId)
                        .payload(eventPayload)
                        .build()
        );
    }
}
