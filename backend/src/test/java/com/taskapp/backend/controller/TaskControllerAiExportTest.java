package com.taskapp.backend.controller;

import com.taskapp.backend.dto.TaskAiBulkExportResponse;
import com.taskapp.backend.dto.TaskAiExportResponse;
import com.taskapp.backend.service.TaskAiExportService;
import com.taskapp.backend.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
class TaskControllerAiExportTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private TaskAiExportService taskAiExportService;

    @Test
    void returnsUtf8JsonAttachmentWithoutApiEnvelope() throws Exception {
        TaskAiExportResponse.KnowledgeSection emptySection =
                new TaskAiExportResponse.KnowledgeSection(null, List.of());
        TaskAiExportResponse export = new TaskAiExportResponse(
                "1.1",
                Instant.parse("2026-08-01T03:00:00Z"),
                new TaskAiExportResponse.Project(
                        42L,
                        "中文项目",
                        null,
                        "MEDIUM",
                        50.0,
                        LocalDateTime.parse("2026-07-01T10:00:00"),
                        LocalDateTime.parse("2026-08-01T12:00:00"),
                        List.of(),
                        new TaskAiExportResponse.Knowledge(emptySection, emptySection, emptySection, emptySection)
                )
        );
        when(taskAiExportService.exportTask("Bearer token", 42L)).thenReturn(export);

        mockMvc.perform(get("/api/tasks/42/ai-export")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"project-42.json\""))
                .andExpect(jsonPath("$.schemaVersion").value("1.1"))
                .andExpect(jsonPath("$.project.title").value("中文项目"))
                .andExpect(jsonPath("$.project.description").value(nullValue()))
                .andExpect(jsonPath("$.project.phases").isArray())
                .andExpect(jsonPath("$.project.phases").isEmpty())
                .andExpect(jsonPath("$.project.knowledge.recentDecisions.entries").isEmpty())
                .andExpect(jsonPath("$.project.knowledge.aiSuggestions.entries").isEmpty())
                .andExpect(jsonPath("$.success").doesNotExist());
    }

    @Test
    void returnsAllVisibleProjectsAsOneJsonAttachment() throws Exception {
        TaskAiExportResponse.KnowledgeSection emptySection =
                new TaskAiExportResponse.KnowledgeSection(null, List.of());
        TaskAiExportResponse.Project project = new TaskAiExportResponse.Project(
                42L,
                "中文项目",
                null,
                "MEDIUM",
                50.0,
                LocalDateTime.parse("2026-07-01T10:00:00"),
                LocalDateTime.parse("2026-08-01T12:00:00"),
                List.of(),
                new TaskAiExportResponse.Knowledge(emptySection, emptySection, emptySection, emptySection)
        );
        TaskAiBulkExportResponse export = new TaskAiBulkExportResponse(
                "1.2",
                Instant.parse("2026-08-01T03:00:00Z"),
                1,
                List.of(project),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
        when(taskAiExportService.exportAllTasks("Bearer token")).thenReturn(export);

        mockMvc.perform(get("/api/tasks/ai-export")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"projects-ai-export.json\""))
                .andExpect(jsonPath("$.schemaVersion").value("1.2"))
                .andExpect(jsonPath("$.projectCount").value(1))
                .andExpect(jsonPath("$.projects[0].id").value(42))
                .andExpect(jsonPath("$.projects[0].title").value("中文项目"))
                .andExpect(jsonPath("$.weeklyTasks").isEmpty())
                .andExpect(jsonPath("$.longTermTasks").isEmpty())
                .andExpect(jsonPath("$.aiSuggestions").isEmpty())
                .andExpect(jsonPath("$.currentActionGoals").isEmpty())
                .andExpect(jsonPath("$.flashNotes").isEmpty())
                .andExpect(jsonPath("$.success").doesNotExist());
    }
}
