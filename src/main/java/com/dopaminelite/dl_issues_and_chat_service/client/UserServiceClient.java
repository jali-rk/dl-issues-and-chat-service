package com.dopaminelite.dl_issues_and_chat_service.client;

import com.dopaminelite.dl_issues_and_chat_service.dto.UserBatchRequest;
import com.dopaminelite.dl_issues_and_chat_service.dto.UserBatchResponse;
import com.dopaminelite.dl_issues_and_chat_service.dto.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserServiceClient {

    private final RestTemplate restTemplate;
    private final String userServiceUrl;
    private final String serviceToken;
    private final String serviceName;

    public UserServiceClient(
            RestTemplate restTemplate,
            @Value("${user.service.url:https://dev-api.gingerbreaddopamine.com}") String userServiceUrl,
            @Value("${user.service.token:change-me-in-production}") String serviceToken,
            @Value("${user.service.name:issues-service}") String serviceName) {
        this.restTemplate = restTemplate;
        this.userServiceUrl = userServiceUrl;
        this.serviceToken = serviceToken;
        this.serviceName = serviceName;
    }

    public Map<UUID, UserInfo> fetchUsersByIds(List<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            log.warn("No user IDs provided to fetch");
            return Collections.emptyMap();
        }

        try {
            String url = userServiceUrl + "/users/public/batch";

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Service-Token", serviceToken);
            headers.set("X-Service-Name", serviceName);
            headers.set("Content-Type", "application/json");

            UserBatchRequest request = new UserBatchRequest(userIds);
            HttpEntity<UserBatchRequest> entity = new HttpEntity<>(request, headers);

            log.info("Fetching {} users from user service: {}", userIds.size(), userIds);
            log.info("Using URL: {}", url);
            ResponseEntity<UserBatchResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    UserBatchResponse.class
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                Map<UUID, UserInfo> userMap = response.getBody().getData().stream()
                        .collect(Collectors.toMap(UserInfo::getId, user -> user));
                log.info("Successfully fetched {} users from API", userMap.size());
                userMap.forEach((id, user) -> log.info("User fetched: {} -> {}", id, user.getFullName()));
                return userMap;
            }

            log.error("Failed to fetch users: response was not successful. Response body: {}", response.getBody());
            return Collections.emptyMap();
        } catch (Exception e) {
            log.error("Error fetching users from user service: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }
}
