package com.dopaminelite.dl_issues_and_chat_service.entity;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IssueNumberGenerator {

    private final JdbcTemplate jdbcTemplate;

    public Long next() {
        return jdbcTemplate.queryForObject(
                "SELECT nextval('issue_number_seq')",
                Long.class
        );
    }
}

