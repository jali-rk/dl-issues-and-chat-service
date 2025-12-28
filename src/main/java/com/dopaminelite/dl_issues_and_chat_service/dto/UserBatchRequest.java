package com.dopaminelite.dl_issues_and_chat_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserBatchRequest {
    private List<UUID> userIds;
}

