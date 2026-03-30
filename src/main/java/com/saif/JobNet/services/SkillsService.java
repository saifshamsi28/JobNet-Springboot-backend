package com.saif.JobNet.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SkillsService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private final String clientId;
    private final String clientSecret;
    private final String tokenUrl;
    private final String skillsBaseUrl;
    private final String scope;

    private volatile String cachedToken;
    private volatile long tokenExpiryEpochMs;

    public SkillsService(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${app.skills.client-id:}") String clientId,
            @Value("${app.skills.client-secret:}") String clientSecret,
            @Value("${app.skills.token-url:https://auth.emsicloud.com/connect/token}") String tokenUrl,
            @Value("${app.skills.base-url:https://emsiservices.com/skills/versions/latest/skills}") String skillsBaseUrl,
            @Value("${app.skills.scope:emsi_open}") String scope
    ) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tokenUrl = tokenUrl;
        this.skillsBaseUrl = skillsBaseUrl;
        this.scope = scope;
    }

    public List<String> getSkillsByType(String type, int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 500));
        try {
            String token = getAccessToken();
            URI uri = UriComponentsBuilder
                    .fromUriString(skillsBaseUrl)
                    .queryParam("typeIds", type)
                    .queryParam("limit", boundedLimit)
                    .build(true)
                    .toUri();

            List<String> skills = fetchSkillNames(token, uri);
            if (!skills.isEmpty()) {
                return skills;
            }
        } catch (Exception ignored) {
            // Fall through to curated fallback list.
        }

        return getFallbackSkillsByType(type, boundedLimit);
    }

    public List<String> searchSkills(String query, int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 100));
        try {
            String token = getAccessToken();
            URI uri = UriComponentsBuilder
                    .fromUriString(skillsBaseUrl)
                    .queryParam("q", query)
                    .queryParam("limit", boundedLimit)
                    .build(true)
                    .toUri();

            List<String> skills = fetchSkillNames(token, uri);
            if (!skills.isEmpty()) {
                return skills;
            }
        } catch (Exception ignored) {
            // Fall through to curated fallback list.
        }

        return searchFallbackSkills(query, boundedLimit);
    }

    public List<String> getFallbackSkillsByType(String type, int limit) {
        List<String> backendSkills = List.of(
                "Java", "Spring Boot", "Microservices", "REST API", "Hibernate", "JPA", "SQL", "MongoDB",
                "Redis", "RabbitMQ", "Kafka", "JUnit", "Mockito", "Docker", "Kubernetes", "AWS",
                "CI/CD", "Git", "Design Patterns", "System Design"
        );

        List<String> dataSkills = List.of(
                "Python", "Pandas", "NumPy", "SQL", "Power BI", "Tableau", "Excel", "Statistics",
                "A/B Testing", "Data Cleaning", "Data Visualization", "ETL", "BigQuery", "Data Warehousing",
                "Machine Learning", "Scikit-learn", "Forecasting", "Reporting"
        );

        List<String> webSkills = List.of(
                "HTML", "CSS", "JavaScript", "TypeScript", "React", "Angular", "Vue.js", "Node.js",
                "Express.js", "Next.js", "Redux", "Tailwind CSS", "Webpack", "Jest", "Playwright",
                "Responsive Design", "Accessibility", "GraphQL", "REST"
        );

        List<String> mobileSkills = List.of(
                "Android", "Kotlin", "Java", "Jetpack Compose", "XML Layout", "Room", "Retrofit", "OkHttp",
                "MVVM", "Coroutines", "Flow", "Firebase", "Material Design", "WorkManager", "Unit Testing",
                "UI Testing"
        );

        List<String> mlSkills = List.of(
                "Python", "TensorFlow", "PyTorch", "Scikit-learn", "Deep Learning", "NLP", "Computer Vision",
                "Feature Engineering", "Model Evaluation", "MLOps", "MLflow", "Docker", "Kubernetes", "Spark",
                "Data Pipelines"
        );

        List<String> selected;
        String normalizedType = type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
        if ("ST2".equals(normalizedType)) {
            selected = dataSkills;
        } else if ("ST3".equals(normalizedType)) {
            selected = mlSkills;
        } else {
            Set<String> union = new LinkedHashSet<>();
            union.addAll(backendSkills);
            union.addAll(webSkills);
            union.addAll(mobileSkills);
            selected = new ArrayList<>(union);
        }

        return selected.stream().limit(limit).collect(Collectors.toList());
    }

    public List<String> searchFallbackSkills(String query, int limit) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (normalizedQuery.isEmpty()) {
            return getFallbackSkillsByType("ST1", limit);
        }

        Set<String> all = new LinkedHashSet<>();
        all.addAll(getFallbackSkillsByType("ST1", 100));
        all.addAll(getFallbackSkillsByType("ST2", 100));
        all.addAll(getFallbackSkillsByType("ST3", 100));

        return all.stream()
                .filter(skill -> skill.toLowerCase(Locale.ROOT).contains(normalizedQuery))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private List<String> fetchSkillNames(String token, URI uri) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, requestEntity, String.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            return Collections.emptyList();
        }

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode data = root.path("data");
            if (!data.isArray()) {
                return Collections.emptyList();
            }

            List<String> skills = new ArrayList<>();
            for (JsonNode node : data) {
                String name = node.path("name").asText("").trim();
                if (!name.isEmpty()) {
                    skills.add(name);
                }
            }
            return skills;
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    private String getAccessToken() {
        String activeToken = getCachedToken();
        if (activeToken != null) {
            return activeToken;
        }

        synchronized (this) {
            activeToken = getCachedToken();
            if (activeToken != null) {
                return activeToken;
            }

            if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
                throw new IllegalStateException("Skills API credentials are missing");
            }

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("client_id", clientId);
            form.add("client_secret", clientSecret);
            form.add("grant_type", "client_credentials");
            form.add("scope", scope);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(form, headers);

            ResponseEntity<String> tokenResponse = restTemplate.postForEntity(tokenUrl, requestEntity, String.class);
            if (!tokenResponse.getStatusCode().is2xxSuccessful() || tokenResponse.getBody() == null) {
                throw new IllegalStateException("Failed to fetch skills access token");
            }

            try {
                JsonNode tokenJson = objectMapper.readTree(tokenResponse.getBody());
                String token = tokenJson.path("access_token").asText("");
                long expiresIn = tokenJson.path("expires_in").asLong(3600L);
                if (token.isEmpty()) {
                    throw new IllegalStateException("Skills token response missing access_token");
                }
                cacheToken(token, expiresIn);
                return token;
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to parse skills access token", ex);
            }
        }
    }

    private String getCachedToken() {
        if (cachedToken == null || cachedToken.isBlank()) {
            return null;
        }
        if (System.currentTimeMillis() >= tokenExpiryEpochMs) {
            return null;
        }
        return cachedToken;
    }

    private void cacheToken(String token, long expiresInSeconds) {
        this.cachedToken = token;
        long safeMarginMs = 10_000L;
        this.tokenExpiryEpochMs = System.currentTimeMillis() + Math.max(0L, (expiresInSeconds * 1000L) - safeMarginMs);
    }
}
