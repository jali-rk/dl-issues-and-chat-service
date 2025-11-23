package com.dopaminelite.dl_issues_and_chat_service.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebSocketMessageEnvelope {
    private String type;
    private UUID issueId;
    private String clientMessageId;
    private Map<String, Object> payload;
    private Instant timestamp;
}
