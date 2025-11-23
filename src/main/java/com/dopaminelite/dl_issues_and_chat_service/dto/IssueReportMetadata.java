package com.dopaminelite.dl_issues_and_chat_service.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueReportMetadata {
    private UUID issueId;
    private Instant generatedAt;
    private String reportType;
}
