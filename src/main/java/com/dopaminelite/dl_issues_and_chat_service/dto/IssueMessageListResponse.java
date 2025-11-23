package com.dopaminelite.dl_issues_and_chat_service.dto;

import com.dopaminelite.dl_issues_and_chat_service.entity.IssueMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueMessageListResponse {
    private List<IssueMessage> items;
    private int total;
}