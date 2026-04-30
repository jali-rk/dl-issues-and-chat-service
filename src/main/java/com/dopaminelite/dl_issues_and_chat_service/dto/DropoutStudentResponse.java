package com.dopaminelite.dl_issues_and_chat_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DropoutStudentResponse {
    private UUID studentId;
    private String fullName;
    private String whatsappNumber;
    private String email;
    private String codeNumber;
}
