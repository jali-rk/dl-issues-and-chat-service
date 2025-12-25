package com.dopaminelite.dl_issues_and_chat_service.dto;

import com.dopaminelite.dl_issues_and_chat_service.constants.IssueAssignmentStatus;
import com.dopaminelite.dl_issues_and_chat_service.constants.IssueStatus;
import com.dopaminelite.dl_issues_and_chat_service.entity.Issue;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class IssueResponse {

    private UUID id;
    private Long issueNumber;
    private String title;
    private String description;
    private UUID studentId;
    private UUID assignedAdminId;

    private IssueStatus status;
    private IssueAssignmentStatus assignmentStatus;

    private boolean isChatReadOnly;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant solvedAt;

    public static IssueResponse fromDomain(Issue issue) {
        IssueResponse r = new IssueResponse();
        r.id = issue.getId();
        r.issueNumber = issue.getIssueNumber();
        r.title = issue.getTitle();
        r.description = issue.getDescription();
        r.studentId = issue.getStudentId();
        r.assignedAdminId = issue.getAssignedAdminId();
        r.status = issue.getStatus();
        r.assignmentStatus = issue.getAssignmentStatus();
        r.isChatReadOnly = issue.isChatReadOnly();
        r.createdAt = issue.getCreatedAt();
        r.updatedAt = issue.getUpdatedAt();
        r.solvedAt = issue.getSolvedAt();
        return r;
    }
}
