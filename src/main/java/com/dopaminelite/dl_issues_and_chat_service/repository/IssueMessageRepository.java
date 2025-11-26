package com.dopaminelite.dl_issues_and_chat_service.repository;

import com.dopaminelite.dl_issues_and_chat_service.entity.IssueMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IssueMessageRepository extends JpaRepository<IssueMessage, UUID> {

    Page<IssueMessage> findByIssueIdOrderByCreatedAtAsc(UUID issueId, Pageable pageable);

    // Convenience method to fetch all messages (used by PDF/report generation)
    List<IssueMessage> findByIssueIdOrderByCreatedAtAsc(UUID issueId);

}
