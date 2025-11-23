package com.dopaminelite.dl_issues_and_chat_service.dto.websocket;

import com.dopaminelite.dl_issues_and_chat_service.dto.UploadedFileRef;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebSocketSendMessagePayload {
    private String content;
    private List<UploadedFileRef> attachments;
}
