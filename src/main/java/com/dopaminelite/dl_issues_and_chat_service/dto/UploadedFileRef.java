package com.dopaminelite.dl_issues_and_chat_service.dto;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Value object for attachment reference
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadedFileRef {
    @Column(nullable = false)
    private String fileId;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String fileType;
}

