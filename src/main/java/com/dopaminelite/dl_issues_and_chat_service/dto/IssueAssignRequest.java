package com.dopaminelite.dl_issues_and_chat_service.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class IssueAssignRequest {
    private UUID adminId;
}

