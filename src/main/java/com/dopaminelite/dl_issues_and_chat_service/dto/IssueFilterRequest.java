package com.dopaminelite.dl_issues_and_chat_service.dto;

import com.dopaminelite.dl_issues_and_chat_service.constants.IssueAssignmentStatus;
import com.dopaminelite.dl_issues_and_chat_service.constants.IssueStatus;
import lombok.Data;

import java.util.UUID;

@Data
public class IssueFilterRequest {
    private UUID studentId;
    private IssueStatus status;
    private IssueAssignmentStatus assignmentStatus;
    private UUID assignedAdminId;
}
