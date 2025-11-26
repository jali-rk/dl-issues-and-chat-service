package com.dopaminelite.dl_issues_and_chat_service.service;

import com.dopaminelite.dl_issues_and_chat_service.constants.Role;
import com.dopaminelite.dl_issues_and_chat_service.dto.IssueMessageListResponse;
import com.dopaminelite.dl_issues_and_chat_service.dto.UploadedFileRef;
import com.dopaminelite.dl_issues_and_chat_service.entity.IssueMessage;
import com.dopaminelite.dl_issues_and_chat_service.repository.IssueMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class IssueMessageService {

    private final IssueMessageRepository issueMessageRepository;

    public IssueMessage createMessage(UUID issueId,
                                      String content,
                                      List<UploadedFileRef> attachments,
                                      UUID senderId,
                                      String senderRole) {
        if (issueId == null) {
            throw new IllegalArgumentException("issueId is required");
        }

        // Fallback sender id & role if authentication is not present
        UUID resolvedSenderId = senderId != null ? senderId : UUID.randomUUID();
        Role resolvedRole;
        try {
            resolvedRole = senderRole != null ? Role.valueOf(senderRole) : Role.STUDENT;
        } catch (Exception ex) {
            resolvedRole = Role.STUDENT;
        }

        // If your IssueMessage entity stores e.g. a single attachment, pick the first.
        UploadedFileRef attachment = (attachments != null && !attachments.isEmpty()) ? attachments.get(0) : null;

        IssueMessage msg = IssueMessage.builder()
                .issueId(issueId)
                .content(content)
                .attachment(attachment)
                .senderId(resolvedSenderId)
                .senderRole(resolvedRole)
                .createdAt(Instant.now())
                .build();

        IssueMessage saved = issueMessageRepository.save(msg);
        log.debug("Saved IssueMessage id={} for issueId={}", saved.getId(), issueId);
        return saved;
    }

    public IssueMessage createMessage(UUID issueId,
                                      String content,
                                      List<UploadedFileRef> attachments) {
        return createMessage(issueId, content, attachments, null, null);
    }

    public IssueMessageListResponse listMessages(UUID issueId, int offset, int limit) {
        if (issueId == null) {
            throw new IllegalArgumentException("issueId is required");
        }
        PageRequest pageRequest = PageRequest.of(Math.max(0, offset), Math.max(1, limit));
        var page = issueMessageRepository.findByIssueIdOrderByCreatedAtAsc(issueId, pageRequest);

        return IssueMessageListResponse.builder()
                .items(page.getContent())
                .total((int) page.getTotalElements())
                .build();
    }
}
