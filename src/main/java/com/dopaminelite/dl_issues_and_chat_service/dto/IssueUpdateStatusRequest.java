package com.dopaminelite.dl_issues_and_chat_service.dto;

import com.dopaminelite.dl_issues_and_chat_service.constants.IssueStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class IssueUpdateStatusRequest {
    private IssueStatus status;
}
