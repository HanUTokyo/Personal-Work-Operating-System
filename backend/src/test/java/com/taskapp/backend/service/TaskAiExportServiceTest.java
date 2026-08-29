package com.taskapp.backend.service;

import com.taskapp.backend.dto.TaskAiBulkExportResponse;
import com.taskapp.backend.dto.TaskAiExportResponse;
import com.taskapp.backend.dto.GlobalAiSuggestionResponse;
import com.taskapp.backend.dto.PersonalTaskResponse;
import com.taskapp.backend.dto.FlashNoteResponse;
import com.taskapp.backend.exception.TaskNotFoundException;
import com.taskapp.backend.model.AppUser;
import com.taskapp.backend.model.NoteType;
import com.taskapp.backend.model.PersonalTaskType;
import com.taskapp.backend.model.PhaseStatus;
import com.taskapp.backend.model.ProjectPriority;
import com.taskapp.backend.model.SharePermission;
import com.taskapp.backend.model.Task;
import com.taskapp.backend.model.TaskKnowledge;
import com.taskapp.backend.model.TaskNote;
import com.taskapp.backend.model.TaskPhase;
import com.taskapp.backend.model.TaskShare;
import com.taskapp.backend.repository.TaskKnowledgeRepository;
import com.taskapp.backend.repository.TaskNoteRepository;
import com.taskapp.backend.repository.TaskPhaseRepository;
import com.taskapp.backend.repository.TaskPinRepository;
import com.taskapp.backend.repository.TaskRepository;
import com.taskapp.backend.repository.TaskShareRepository;
import com.taskapp.backend.repository.TaskVersionRepository;
import com.taskapp.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskAiExportServiceTest {

    private static final Long TASK_ID = 42L;
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 7, 1, 10, 0);
    private static final LocalDateTime UPDATED_AT = LocalDateTime.of(2026, 8, 1, 12, 30);

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private TaskPhaseRepository taskPhaseRepository;
    @Mock
    private TaskKnowledgeRepository taskKnowledgeRepository;
    @Mock
    private TaskNoteRepository taskNoteRepository;
    @Mock
    private TaskShareRepository taskShareRepository;
    @Mock
    private TaskPinRepository taskPinRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthService authService;
    @Mock
    private TaskVersionRepository taskVersionRepository;
    @Mock
    private PersonalTaskService personalTaskService;
    @Mock
    private GlobalAiSuggestionService globalAiSuggestionService;
    @Mock
    private FlashNoteService flashNoteService;

    private TaskAiExportService exportService;

    @BeforeEach
    void setUp() {
        TaskService taskService = new TaskService(
                taskRepository,
                taskPhaseRepository,
                taskKnowledgeRepository,
                taskNoteRepository,
                taskShareRepository,
                taskPinRepository,
                userRepository,
                authService,
                taskVersionRepository,
                new ObjectMapper()
        );
        exportService = new TaskAiExportService(taskService, personalTaskService, globalAiSuggestionService, flashNoteService);
    }

    @Test
    void ownerExportBuildsNestedPhasesAndGroupedKnowledge() {
        AppUser owner = user(7L, "owner");
        stubProject(owner, owner, null);

        TaskAiExportResponse response = exportService.exportTask("Bearer owner-token", TASK_ID);

        assertThat(response.schemaVersion()).isEqualTo("1.1");
        assertThat(response.exportedAt()).isNotNull();
        assertThat(response.project().id()).isEqualTo(TASK_ID);
        assertThat(response.project().title()).isEqualTo("中文项目");
        assertThat(response.project().phases()).hasSize(1);
        assertThat(response.project().phases().getFirst().key()).isEqualTo("research");
        assertThat(response.project().phases().getFirst().children())
                .extracting(TaskAiExportResponse.Phase::key)
                .containsExactly("prototype");
        assertThat(response.project().knowledge().recentDecisions().summary()).isEqualTo("采用方案 A");
        assertThat(response.project().knowledge().recentDecisions().entries())
                .extracting(TaskAiExportResponse.KnowledgeEntry::content)
                .containsExactly("决定先完成数据验证");
        assertThat(response.project().knowledge().recentExperiments().entries())
                .extracting(TaskAiExportResponse.KnowledgeEntry::content)
                .containsExactly("实验结果稳定");
        assertThat(response.project().knowledge().knowledgeHighlights().entries()).isEmpty();
        assertThat(response.project().knowledge().aiSuggestions().entries())
                .extracting(TaskAiExportResponse.KnowledgeEntry::content)
                .containsExactly("AI 建议先验证边界条件");
    }

    @ParameterizedTest
    @EnumSource(value = SharePermission.class, names = {"VIEW", "EDIT"})
    void sharedUserWithViewOrEditPermissionCanExport(SharePermission permission) {
        AppUser currentUser = user(8L, "shared-user");
        AppUser owner = user(7L, "owner");
        stubProject(currentUser, owner, permission);

        TaskAiExportResponse response = exportService.exportTask("Bearer shared-token", TASK_ID);

        assertThat(response.project().id()).isEqualTo(TASK_ID);
    }

    @Test
    void userWithoutProjectAccessCannotExport() {
        AppUser currentUser = user(9L, "outsider");
        AppUser owner = user(7L, "owner");
        Task task = task(owner.getId());
        when(authService.requireUser("Bearer outsider-token")).thenReturn(currentUser);
        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
        when(taskShareRepository.findByTaskIdAndUserId(TASK_ID, currentUser.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> exportService.exportTask("Bearer outsider-token", TASK_ID))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void bulkExportContainsEveryProjectVisibleToCurrentUser() {
        AppUser owner = user(7L, "owner");
        Task visibleTask = task(owner.getId());
        when(authService.requireUser("Bearer owner-token")).thenReturn(owner);
        when(taskRepository.findAllForUser(owner.getId(), null, "taskTitle", "asc"))
                .thenReturn(List.of(visibleTask));
        when(taskPhaseRepository.findByTaskIds(List.of(TASK_ID))).thenReturn(Map.of(TASK_ID, phases()));
        when(taskKnowledgeRepository.findByTaskIds(List.of(TASK_ID))).thenReturn(Map.of(TASK_ID, knowledge()));
        when(taskNoteRepository.findByTaskIds(List.of(TASK_ID))).thenReturn(Map.of(TASK_ID, notes()));
        when(userRepository.findMapByIds(List.of(owner.getId()))).thenReturn(Map.of(owner.getId(), owner));
        when(personalTaskService.getAll("Bearer owner-token", PersonalTaskType.WEEKLY))
                .thenReturn(List.of(personalTask(PersonalTaskType.WEEKLY, "本周完成导出")));
        when(personalTaskService.getAll("Bearer owner-token", PersonalTaskType.LONG_TERM))
                .thenReturn(List.of(personalTask(PersonalTaskType.LONG_TERM, "长期优化流程")));
        when(globalAiSuggestionService.getAll("Bearer owner-token"))
                .thenReturn(List.of(globalAiSuggestion("优先处理停滞项目")));
        when(globalAiSuggestionService.getAll("Bearer owner-token", "ACTION_GOAL"))
                .thenReturn(List.of(globalAiSuggestion("完成发布前验证")));
        when(flashNoteService.getAllFlashNotes("Bearer owner-token"))
                .thenReturn(List.of(flashNote("检查导出结构")));

        TaskAiBulkExportResponse response = exportService.exportAllTasks("Bearer owner-token");

        assertThat(response.schemaVersion()).isEqualTo("1.3");
        assertThat(response.projectCount()).isEqualTo(1);
        assertThat(response.projects())
                .extracting(TaskAiExportResponse.Project::id)
                .containsExactly(TASK_ID);
        assertThat(response.projects().getFirst().knowledge().aiSuggestions().entries())
                .extracting(TaskAiExportResponse.KnowledgeEntry::content)
                .containsExactly("AI 建议先验证边界条件");
        assertThat(response.weeklyTasks()).extracting(PersonalTaskResponse::getContent).containsExactly("本周完成导出");
        assertThat(response.longTermTasks()).extracting(PersonalTaskResponse::getContent).containsExactly("长期优化流程");
        assertThat(response.aiSuggestions()).extracting(GlobalAiSuggestionResponse::getContent).containsExactly("优先处理停滞项目");
        assertThat(response.currentActionGoals()).extracting(GlobalAiSuggestionResponse::getContent).containsExactly("完成发布前验证");
        assertThat(response.flashNotes()).extracting(FlashNoteResponse::getNoteContent).containsExactly("检查导出结构");
    }

    private void stubProject(AppUser currentUser, AppUser owner, SharePermission permission) {
        when(authService.requireUser(org.mockito.ArgumentMatchers.anyString())).thenReturn(currentUser);
        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task(owner.getId())));
        when(taskPhaseRepository.findByTaskId(TASK_ID)).thenReturn(phases());
        when(taskKnowledgeRepository.findByTaskId(TASK_ID)).thenReturn(Optional.of(knowledge()));
        when(taskNoteRepository.findByTaskId(TASK_ID)).thenReturn(notes());
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));

        if (!currentUser.getId().equals(owner.getId())) {
            TaskShare share = new TaskShare();
            share.setTaskId(TASK_ID);
            share.setSharedWithUserId(currentUser.getId());
            share.setPermission(permission);
            when(taskShareRepository.findByTaskIdAndUserId(TASK_ID, currentUser.getId()))
                    .thenReturn(Optional.of(share));
        }
    }

    private Task task(Long ownerId) {
        Task task = new Task();
        task.setId(TASK_ID);
        task.setTaskTitle("中文项目");
        task.setTaskDescription("用于测试 UTF-8 导出");
        task.setOwnerUserId(ownerId);
        task.setPriority(ProjectPriority.HIGH);
        task.setOverallProgress(50.0);
        task.setCreatedAt(CREATED_AT);
        task.setUpdatedAt(UPDATED_AT);
        return task;
    }

    private List<TaskPhase> phases() {
        TaskPhase root = phase("research", null, "调研", PhaseStatus.DONE, 1);
        TaskPhase child = phase("prototype", "research", "原型", PhaseStatus.DOING, 2);
        return List.of(child, root);
    }

    private TaskPhase phase(String key, String parentKey, String name, PhaseStatus status, int order) {
        TaskPhase phase = new TaskPhase();
        phase.setTaskId(TASK_ID);
        phase.setPhaseKey(key);
        phase.setParentPhaseKey(parentKey);
        phase.setPhaseName(name);
        phase.setPhaseDescription(name + "说明");
        phase.setPhaseStatus(status);
        phase.setSortOrder(order);
        return phase;
    }

    private TaskKnowledge knowledge() {
        TaskKnowledge knowledge = new TaskKnowledge();
        knowledge.setTaskId(TASK_ID);
        knowledge.setRecentDecisions("采用方案 A");
        knowledge.setRecentExperiments(null);
        knowledge.setKnowledgeHighlights("保留核心知识");
        return knowledge;
    }

    private List<TaskNote> notes() {
        TaskNote decision = note(NoteType.RECENT_DECISIONS, "决定先完成数据验证");
        TaskNote experiment = note(NoteType.RECENT_EXPERIMENTS, "实验结果稳定");
        TaskNote aiSuggestion = note(NoteType.AI_SUGGESTIONS, "AI 建议先验证边界条件");
        return List.of(decision, experiment, aiSuggestion);
    }

    private TaskNote note(NoteType type, String content) {
        TaskNote note = new TaskNote();
        note.setTaskId(TASK_ID);
        note.setNoteType(type);
        note.setNoteContent(content);
        note.setCreatedAt(CREATED_AT);
        note.setUpdatedAt(UPDATED_AT);
        return note;
    }

    private AppUser user(Long id, String username) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    private PersonalTaskResponse personalTask(PersonalTaskType type, String content) {
        PersonalTaskResponse task = new PersonalTaskResponse();
        task.setId(1L);
        task.setType(type);
        task.setContent(content);
        task.setCreatedAt(CREATED_AT);
        task.setUpdatedAt(UPDATED_AT);
        return task;
    }

    private GlobalAiSuggestionResponse globalAiSuggestion(String content) {
        GlobalAiSuggestionResponse suggestion = new GlobalAiSuggestionResponse();
        suggestion.setId(2L);
        suggestion.setContent(content);
        suggestion.setCreatedAt(CREATED_AT);
        suggestion.setUpdatedAt(UPDATED_AT);
        return suggestion;
    }

    private FlashNoteResponse flashNote(String content) {
        FlashNoteResponse note = new FlashNoteResponse();
        note.setId(3L);
        note.setNoteContent(content);
        note.setCreatedAt(CREATED_AT);
        note.setUpdatedAt(UPDATED_AT);
        return note;
    }
}
