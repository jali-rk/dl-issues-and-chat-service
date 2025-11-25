package com.dopaminelite.dl_issues_and_chat_service.service;

import com.dopaminelite.dl_issues_and_chat_service.constants.Role;
import com.dopaminelite.dl_issues_and_chat_service.dto.IssueMessageListResponse;
import com.dopaminelite.dl_issues_and_chat_service.dto.UploadedFileRef;
import com.dopaminelite.dl_issues_and_chat_service.entity.IssueMessage;
import com.dopaminelite.dl_issues_and_chat_service.repository.IssueMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IssueMessageService {

    private final IssueMessageRepository issueMessageRepository;

    public IssueMessage createMessage(UUID issueId, String content, List<UploadedFileRef> attachments) {
        // Note: senderId and senderRole are non-nullable in IssueMessage entity.
        // At the moment there is no authentication context in this module, so we use a generated senderId
        // and a sensible default role. If authentication is added later, this should be replaced with
        // the authenticated principal's id and role.
        UUID senderId = UUID.randomUUID();
        Role senderRole = Role.STUDENT;

        IssueMessage msg = IssueMessage.builder()
                .issueId(issueId)
                .content(content)
                .attachment(attachments != null && !attachments.isEmpty() ? attachments.get(0) : null)
                .senderId(senderId)
                .senderRole(senderRole)
                .build();
        return issueMessageRepository.save(msg);
    }

    public IssueMessageListResponse listMessages(UUID issueId, int offset, int limit) {
        List<IssueMessage> messages = issueMessageRepository.findByIssueIdOrderByCreatedAtAsc(
                issueId, PageRequest.of(offset / limit, limit)
        );
        return IssueMessageListResponse.builder()
                .items(messages)
                .total(messages.size())
                .build();
    }
}
