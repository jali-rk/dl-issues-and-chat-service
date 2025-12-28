package com.dopaminelite.dl_issues_and_chat_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserBatchResponse {
    private boolean success;
    private List<UserInfo> data;
}
