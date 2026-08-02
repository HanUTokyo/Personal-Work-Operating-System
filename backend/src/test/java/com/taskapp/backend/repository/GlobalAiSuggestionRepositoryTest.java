package com.taskapp.backend.repository;

import com.taskapp.backend.model.GlobalAiSuggestion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class GlobalAiSuggestionRepositoryTest {

    @Autowired
    private GlobalAiSuggestionRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<Long> userIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (Long userId : userIds) {
            jdbcTemplate.update("DELETE FROM global_ai_suggestions WHERE owner_user_id = ?", userId);
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
        }
    }

    @Test
    void scopesSuggestionsToTheirOwnerAndSoftDeletes() {
        Long firstUser = createUser("global-ai-a-");
        Long secondUser = createUser("global-ai-b-");
        LocalDateTime now = LocalDateTime.now();

        GlobalAiSuggestion first = repository.save(firstUser, "**Prioritize** risk review", now);
        repository.save(secondUser, "Other user's suggestion", now.plusSeconds(1));

        assertThat(repository.findAll(firstUser)).extracting(GlobalAiSuggestion::getContent)
                .containsExactly("**Prioritize** risk review");

        GlobalAiSuggestion updated = repository.update(firstUser, first.getId(), "Updated guidance", now.plusSeconds(2));
        assertThat(updated).isNotNull();
        assertThat(updated.getContent()).isEqualTo("Updated guidance");
        assertThat(repository.update(secondUser, first.getId(), "Not allowed", now.plusSeconds(3))).isNull();

        assertThat(repository.softDelete(firstUser, first.getId(), now.plusSeconds(4))).isTrue();
        assertThat(repository.findAll(firstUser)).isEmpty();
        assertThat(repository.findAll(secondUser)).extracting(GlobalAiSuggestion::getContent)
                .containsExactly("Other user's suggestion");
    }

    private Long createUser(String prefix) {
        String suffix = Long.toUnsignedString(System.nanoTime());
        jdbcTemplate.update("""
                INSERT INTO users (username, display_name, password_hash, password_salt, created_at, updated_at)
                VALUES (?, ?, 'hash', 'salt', strftime('%Y-%m-%dT%H:%M:%S','now'), strftime('%Y-%m-%dT%H:%M:%S','now'))
                """, prefix + suffix, "Test User");
        Long userId = jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Long.class);
        userIds.add(userId);
        return userId;
    }
}
