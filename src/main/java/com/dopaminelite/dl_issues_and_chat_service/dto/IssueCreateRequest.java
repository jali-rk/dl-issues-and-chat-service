package com.dopaminelite.dl_issues_and_chat_service.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class IssueCreateRequest {
    private UUID studentId;
    private String title;
    private String description;
    private List<UploadedFileRef> attachments;
}
