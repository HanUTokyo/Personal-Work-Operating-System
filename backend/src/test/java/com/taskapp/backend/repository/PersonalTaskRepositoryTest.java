package com.taskapp.backend.repository;

import com.taskapp.backend.model.PersonalTask;
import com.taskapp.backend.model.PersonalTaskType;
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
class PersonalTaskRepositoryTest {

    @Autowired
    private PersonalTaskRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<Long> userIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (Long userId : userIds) {
            jdbcTemplate.update("DELETE FROM personal_tasks WHERE owner_user_id = ?", userId);
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
        }
    }

    @Test
    void scopesTypesAndOwnersAndSoftDeletesTasks() {
        Long firstUser = createUser("personal-task-a-");
        Long secondUser = createUser("personal-task-b-");
        LocalDateTime now = LocalDateTime.now();

        PersonalTask weekly = repository.save(firstUser, PersonalTaskType.WEEKLY, "**This week**", now);
        repository.save(firstUser, PersonalTaskType.LONG_TERM, "Long-term", now.plusSeconds(1));
        repository.save(secondUser, PersonalTaskType.WEEKLY, "Other user", now.plusSeconds(2));

        assertThat(repository.findAll(firstUser, PersonalTaskType.WEEKLY))
                .extracting(PersonalTask::getContent)
                .containsExactly("**This week**");
        assertThat(repository.findAll(firstUser, PersonalTaskType.LONG_TERM))
                .extracting(PersonalTask::getContent)
                .containsExactly("Long-term");

        PersonalTask updated = repository.update(firstUser, weekly.getId(), "**Done**", true, true, now.plusSeconds(3));
        assertThat(updated).isNotNull();
        assertThat(updated.isCompleted()).isTrue();
        assertThat(updated.isPinned()).isTrue();
        assertThat(updated.getContent()).isEqualTo("**Done**");

        assertThat(repository.softDelete(firstUser, weekly.getId(), now.plusSeconds(4))).isTrue();
        assertThat(repository.findAll(firstUser, PersonalTaskType.WEEKLY)).isEmpty();
        assertThat(repository.findAll(secondUser, PersonalTaskType.WEEKLY))
                .extracting(PersonalTask::getContent)
                .containsExactly("Other user");
    }

    @Test
    void ordersPinnedTasksBeforeManualSortOrder() {
        Long userId = createUser("personal-task-order-");
        LocalDateTime now = LocalDateTime.now();
        PersonalTask first = repository.save(userId, PersonalTaskType.WEEKLY, "First", now);
        PersonalTask second = repository.save(userId, PersonalTaskType.WEEKLY, "Second", now.plusSeconds(1));
        PersonalTask third = repository.save(userId, PersonalTaskType.WEEKLY, "Third", now.plusSeconds(2));

        repository.updateSortOrder(userId, third.getId(), 1, now.plusSeconds(3));
        repository.updateSortOrder(userId, first.getId(), 2, now.plusSeconds(3));
        repository.updateSortOrder(userId, second.getId(), 3, now.plusSeconds(3));
        repository.update(userId, second.getId(), "Second", false, true, now.plusSeconds(4));

        assertThat(repository.findAll(userId, PersonalTaskType.WEEKLY))
                .extracting(PersonalTask::getContent)
                .containsExactly("Second", "Third", "First");
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
