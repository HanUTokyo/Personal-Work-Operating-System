package com.taskapp.backend.service;

import com.taskapp.backend.dto.OnboardingResponse;
import com.taskapp.backend.model.AppUser;
import com.taskapp.backend.repository.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class OnboardingService {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private final AuthService authService; private final UserRepository users; private final JdbcTemplate jdbc;
    public OnboardingService(AuthService authService, UserRepository users, JdbcTemplate jdbc) { this.authService = authService; this.users = users; this.jdbc = jdbc; }
    public OnboardingResponse get(String auth) { AppUser user = authService.requireUser(auth); return response(user); }
    public OnboardingResponse skip(String auth) { AppUser user = authService.requireUser(auth); users.updateOnboardingStatus(user.getId(), "SKIPPED", now()); user.setOnboardingStatus("SKIPPED"); return response(user); }
    public OnboardingResponse updateGuide(String auth, String action) { AppUser user = authService.requireUser(auth); if ("close".equals(action)) jdbc.update("UPDATE users SET onboarding_guide_closed = 1 WHERE id = ?", user.getId()); if ("open".equals(action)) jdbc.update("UPDATE users SET onboarding_guide_closed = 0 WHERE id = ?", user.getId()); if ("ai-used".equals(action)) jdbc.update("UPDATE users SET onboarding_ai_used = 1 WHERE id = ?", user.getId()); return response(user); }
    @Transactional public OnboardingResponse loadDemo(String auth, String locale) {
        AppUser user = authService.requireUser(auth);
        if (hasDemo(user.getId())) return response(user);
        String[] words = words(locale);
        seedProject(user.getId(), words[0], words[1], "HIGH", false, "DOING", "TODO", "TODO");
        seedProject(user.getId(), words[2], words[3], "MEDIUM", false, "DOING", "TODO", "TODO");
        seedProject(user.getId(), words[4], words[5], "LOW", true, "DONE", "DONE", "DONE");
        seedPersonal(user.getId(), "WEEKLY", words[6], 1, 0); seedPersonal(user.getId(), "WEEKLY", words[7], 0, 1); seedPersonal(user.getId(), "LONG_TERM", words[8], 0, 0);
        stamp("INSERT INTO global_ai_suggestions (owner_user_id, content, suggestion_type, created_at, updated_at) VALUES (?, ?, 'AI', ?, ?)", user.getId(), words[9]);
        stamp("INSERT INTO global_ai_suggestions (owner_user_id, content, suggestion_type, created_at, updated_at) VALUES (?, ?, 'ACTION_GOAL', ?, ?)", user.getId(), words[10]);
        stamp("INSERT INTO flash_notes (owner_user_id, note_content, created_at, updated_at) VALUES (?, ?, ?, ?)", user.getId(), words[11]);
        users.updateOnboardingStatus(user.getId(), "LOADED", now()); user.setOnboardingStatus("LOADED"); return response(user);
    }
    @Transactional public OnboardingResponse clearDemo(String auth) {
        AppUser user = authService.requireUser(auth); Long id = user.getId();
        jdbc.update("DELETE FROM task_notes WHERE task_id IN (SELECT id FROM tasks WHERE owner_user_id = ? AND is_demo = 1)", id);
        jdbc.update("DELETE FROM task_phases WHERE task_id IN (SELECT id FROM tasks WHERE owner_user_id = ? AND is_demo = 1)", id);
        jdbc.update("DELETE FROM task_knowledge WHERE task_id IN (SELECT id FROM tasks WHERE owner_user_id = ? AND is_demo = 1)", id);
        jdbc.update("DELETE FROM tasks WHERE owner_user_id = ? AND is_demo = 1", id);
        // Demo entries are identified by their onboarding text prefix; converted entries are never removed by this operation.
        jdbc.update("DELETE FROM personal_tasks WHERE owner_user_id = ? AND content LIKE '[demo]%'", id);
        jdbc.update("DELETE FROM global_ai_suggestions WHERE owner_user_id = ? AND content LIKE '[demo]%'", id);
        jdbc.update("DELETE FROM flash_notes WHERE owner_user_id = ? AND note_content LIKE '[demo]%'", id);
        users.updateOnboardingStatus(id, "CLEARED", now()); user.setOnboardingStatus("CLEARED"); return response(user);
    }
    private void seedProject(Long owner, String title, String description, String priority, boolean archived, String p1, String p2, String p3) {
        String time = now().format(TIME); jdbc.update("INSERT INTO tasks (task_title, task_description, owner_user_id, phase1_status, phase2_status, phase3_status, priority, overall_progress, is_archived, archived_at, is_demo, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?)", title, description, owner, p1,p2,p3,priority, archived ? 100d : ("DOING".equals(p1) ? 35d : 10d), archived ? 1 : 0, archived ? time : null, time,time);
        Long taskId = jdbc.queryForObject("SELECT id FROM tasks WHERE owner_user_id = ? AND task_title = ? ORDER BY id DESC LIMIT 1", Long.class, owner, title);
        jdbc.update("INSERT INTO task_phases (task_id, phase_key, parent_phase_key, phase_name, phase_description, phase_status, sort_order, created_at, updated_at) VALUES (?, 'phase-1', NULL, ?, ?, ?, 1, ?, ?)", taskId, "[demo] Phase 1", description, p1, time,time);
        jdbc.update("INSERT INTO task_knowledge (task_id, recent_decisions, knowledge_highlights, created_at, updated_at) VALUES (?, ?, ?, ?, ?)", taskId, "[demo] Review this example to learn the project workspace.", "[demo] Edit any example to make it your own.", time,time);
        jdbc.update("INSERT INTO task_notes (task_id, note_type, note_content, created_at, updated_at) VALUES (?, 'AI_SUGGESTIONS', ?, ?, ?)", taskId, "[demo] This note demonstrates project-level context.", time,time);
    }
    private void seedPersonal(Long owner, String type, String content, int completed, int pinned) { String t = now().format(TIME); jdbc.update("INSERT INTO personal_tasks (owner_user_id, task_type, content, completed, pinned, sort_order, created_at, updated_at) VALUES (?, ?, ?, ?, ?, 999, ?, ?)", owner,type,"[demo] " + content,completed,pinned,t,t); }
    private void stamp(String sql, Long owner, String content) { String t = now().format(TIME); jdbc.update(sql, owner, "[demo] " + content, t,t); }
    private boolean hasDemo(Long id) { Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM tasks WHERE owner_user_id = ? AND is_demo = 1 AND is_deleted = 0", Integer.class, id); return count != null && count > 0; }
    private OnboardingResponse response(AppUser user) {
        Long id = user.getId();
        boolean project = count("SELECT COUNT(*) FROM tasks WHERE owner_user_id = ? AND is_deleted = 0 AND is_demo = 0", id) > 0;
        boolean focus = count("SELECT COUNT(*) FROM personal_tasks WHERE owner_user_id = ? AND is_deleted = 0 AND content NOT LIKE '[demo]%'", id) > 0 || count("SELECT COUNT(*) FROM global_ai_suggestions WHERE owner_user_id = ? AND is_deleted = 0 AND suggestion_type = 'ACTION_GOAL' AND content NOT LIKE '[demo]%'", id) > 0;
        boolean knowledge = count("SELECT COUNT(*) FROM flash_notes WHERE owner_user_id = ? AND is_deleted = 0 AND note_content NOT LIKE '[demo]%'", id) > 0 || count("SELECT COUNT(*) FROM task_notes n JOIN tasks t ON n.task_id = t.id WHERE t.owner_user_id = ? AND n.is_deleted = 0 AND t.is_demo = 0", id) > 0 || count("SELECT COUNT(*) FROM task_knowledge k JOIN tasks t ON k.task_id = t.id WHERE t.owner_user_id = ? AND t.is_deleted = 0 AND t.is_demo = 0", id) > 0;
        boolean ai = count("SELECT onboarding_ai_used FROM users WHERE id = ?", id) > 0 || count("SELECT COUNT(*) FROM global_ai_suggestions WHERE owner_user_id = ? AND is_deleted = 0 AND suggestion_type = 'AI' AND content NOT LIKE '[demo]%'", id) > 0;
        boolean closed = count("SELECT onboarding_guide_closed FROM users WHERE id = ?", id) > 0;
        return new OnboardingResponse(user.getOnboardingStatus() == null ? "ESTABLISHED" : user.getOnboardingStatus(), hasDemo(id), closed, project, focus, knowledge, ai);
    }
    private int count(String sql, Long id) { Integer value = jdbc.queryForObject(sql, Integer.class, id); return value == null ? 0 : value; }
    private LocalDateTime now() { return LocalDateTime.now().withNano(0); }
    private String[] words(String locale) { return switch (locale) { case "ja" -> new String[]{"[demo] 新しいプロジェクト","サンプルを編集して実際の仕事に変えましょう。","[demo] リスク確認","停滞プロジェクトの確認方法を示します。","[demo] 完了済み","アーカイブ画面で確認できます。","今週の優先事項を確認","関係者に進捗を共有","長期の改善テーマ","次の一手を明確にする","今日の行動目標を一つ終える","気づきをここに残す"}; case "en" -> new String[]{"[demo] Launch plan","Edit this sample to turn it into real work.","[demo] Risk review","Shows how a stalled project is surfaced.","[demo] Completed work","Find this example in Archived.","Review this week's priorities","Share progress with stakeholders","Improve the operating rhythm","Clarify the next best action","Finish one meaningful action today","Capture a useful insight here"}; default -> new String[]{"[demo] 新项目推进","编辑此示例即可转为真实工作。","[demo] 风险复盘","展示如何识别需要关注的停滞项目。","[demo] 已完成项目","可在“已归档”筛选中查看。","确认本周优先事项","向相关成员同步进度","持续优化工作节奏","明确下一步最重要的行动","今天完成一个有意义的行动","把新的想法记录在这里"}; }; }
}
