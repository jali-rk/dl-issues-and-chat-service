package com.dopaminelite.dl_issues_and_chat_service.dto.websocket;

import com.dopaminelite.dl_issues_and_chat_service.constants.IssueStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebSocketIssueStatusUpdatePayload {
    private IssueStatus oldStatus;
    private IssueStatus newStatus;
    private boolean isChatReadOnly;
    private Instant solvedAt;
}
