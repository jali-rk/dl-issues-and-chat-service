package com.dopaminelite.dl_issues_and_chat_service.dto.websocket;

import com.dopaminelite.dl_issues_and_chat_service.dto.UploadedFileRef;
import com.dopaminelite.dl_issues_and_chat_service.entity.IssueMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebSocketMessageEventPayload {
    private UUID id;
    private UUID issueId;
    private UUID senderId;
    private String senderRole;
    private String content;
    private UploadedFileRef attachment;
    private Instant createdAt;

    public WebSocketMessageEventPayload(IssueMessage message) {
        this.id = message.getId();
        this.issueId = message.getIssueId();
        this.senderId = message.getSenderId();
        this.senderRole = message.getSenderRole().name();
        this.content = message.getContent();
        this.attachment = message.getAttachment();
        this.createdAt = message.getCreatedAt();
    }
}
