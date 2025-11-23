package com.dopaminelite.dl_issues_and_chat_service.service;

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
        IssueMessage msg = IssueMessage.builder()
                .issueId(issueId)
                .content(content)
                .attachment(attachments != null && !attachments.isEmpty() ? attachments.get(0) : null)
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
