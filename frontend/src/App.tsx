import { FormEvent, useEffect, useMemo, useState } from "react";
import { api, clearAuthToken, getAuthToken } from "./api";
import { dictionaries } from "./i18n";
import type { GlobalAiSuggestion, Locale, PersonalTask, Task, UserResponse } from "./types";
import { computeMetrics, isCompleted, isRecent, isStuck } from "./utils";
import { AppHeader } from "./components/AppHeader";
import { AuthScreen } from "./features/auth/AuthScreen";
import { Dashboard } from "./features/dashboard/Dashboard";
import { FlashNotes } from "./features/flash/FlashNotes";
import { ProjectDetail } from "./features/projects/ProjectDetail";
import { ProjectEditor } from "./features/projects/ProjectEditor";
import { ProjectList } from "./features/projects/ProjectList";
import { priorityWeight } from "./features/projects/projectHelpers";
import type { SortOrder, TaskFilter } from "./features/projects/types";

interface AppProps {
  initialLocale: Locale;
}

const localeOrder: Locale[] = ["zh", "en", "ja"];
const localeLang: Record<Locale, string> = { zh: "zh-CN", en: "en", ja: "ja" };

export function App({ initialLocale }: AppProps) {
  const [locale, setLocale] = useState<Locale>(initialLocale);
  const t = dictionaries[locale];
  const [theme, setTheme] = useState<"dark" | "light">(() => {
    const savedTheme = localStorage.getItem("task-app-theme");
    return savedTheme === "light" ? "light" : "dark";
  });
  const [user, setUser] = useState<UserResponse | null>(null);
  const [authMode, setAuthMode] = useState<"login" | "register">("login");
  const [authForm, setAuthForm] = useState({ username: "", password: "", confirmPassword: "", displayName: "" });
  const [tasks, setTasks] = useState<Task[]>([]);
  const [weeklyTasks, setWeeklyTasks] = useState<PersonalTask[]>([]);
  const [longTermTasks, setLongTermTasks] = useState<PersonalTask[]>([]);
  const [aiSuggestions, setAiSuggestions] = useState<GlobalAiSuggestion[]>([]);
  const [selectedTaskId, setSelectedTaskId] = useState<number | null>(null);
  const [keyword, setKeyword] = useState("");
  const [filter, setFilter] = useState<TaskFilter>("all");
  const [sortBy, setSortBy] = useState("updatedAt");
  const [sortOrder, setSortOrder] = useState<SortOrder>("desc");
  const [busy, setBusy] = useState(false);
  const [toast, setToast] = useState<string | null>(null);
  const [editorTask, setEditorTask] = useState<Task | null | "new">(null);
  const [flashOpen, setFlashOpen] = useState(false);
  const [detailOpen, setDetailOpen] = useState(false);
  const [exportingAllProjects, setExportingAllProjects] = useState(false);

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    document.documentElement.dataset.mantineColorScheme = theme;
    localStorage.setItem("task-app-theme", theme);
  }, [theme]);

  useEffect(() => {
    document.documentElement.dataset.locale = locale;
    document.documentElement.lang = localeLang[locale];
    document.title = dictionaries[locale].appTitle;
    localStorage.setItem("task-app-locale", locale);
  }, [locale]);

  useEffect(() => {
    if (!getAuthToken()) return;
    api.me()
      .then((currentUser) => {
        setUser(currentUser);
        return loadHomeData();
      })
      .catch(() => clearSession());
  }, []);

  const selectedTask = useMemo(
    () => tasks.find((task) => task.id === selectedTaskId) || tasks[0] || null,
    [selectedTaskId, tasks]
  );
  const mainViewKey = !user
    ? "auth"
    : flashOpen
      ? "flash"
      : detailOpen && selectedTask
        ? `detail-${selectedTask.id}`
        : "home";

  useEffect(() => {
    if (selectedTask && selectedTask.id !== selectedTaskId) setSelectedTaskId(selectedTask.id);
    if (!tasks.length) setSelectedTaskId(null);
  }, [selectedTask, selectedTaskId, tasks.length]);

  useEffect(() => {
    window.scrollTo({ top: 0, left: 0 });
  }, [mainViewKey]);

  const metrics = useMemo(() => computeMetrics(tasks), [tasks]);
  const displayedTasks = useMemo(() => {
    const query = keyword.trim().toLowerCase();
    const visible = tasks.filter((task) => {
      const matchesQuery = !query || `${task.taskTitle} ${task.taskDescription || ""}`.toLowerCase().includes(query);
      const matchesFilter =
        filter === "all" ||
        (filter === "active" && !isCompleted(task)) ||
        (filter === "completed" && isCompleted(task)) ||
        (filter === "stuck" && isStuck(task)) ||
        (filter === "recent" && isRecent(task));
      return matchesQuery && matchesFilter;
    });
    return visible.sort((a, b) => {
      if (Boolean(a.pinned) !== Boolean(b.pinned)) {
        return a.pinned ? -1 : 1;
      }
      const comparison =
        sortBy === "progress"
          ? Number(b.overallProgress) - Number(a.overallProgress)
          : sortBy === "priority"
            ? priorityWeight(b.priority) - priorityWeight(a.priority)
            : new Date(b.updatedAt || 0).getTime() - new Date(a.updatedAt || 0).getTime();
      return sortOrder === "desc" ? comparison : -comparison;
    });
  }, [filter, keyword, sortBy, sortOrder, tasks]);

  async function loadTasks() {
    const nextTasks = await api.tasks({ sortBy: "updatedAt", order: "desc" });
    setTasks(nextTasks);
    setSelectedTaskId((current) => current || nextTasks[0]?.id || null);
  }

  async function loadPersonalTasks() {
    const [weekly, longTerm] = await Promise.all([
      api.personalTasks("WEEKLY"),
      api.personalTasks("LONG_TERM")
    ]);
    setWeeklyTasks(weekly);
    setLongTermTasks(longTerm);
  }

  async function loadAiSuggestions() {
    setAiSuggestions(await api.aiSuggestions());
  }

  async function loadHomeData() {
    await Promise.all([loadTasks(), loadPersonalTasks(), loadAiSuggestions()]);
  }

  function clearSession() {
    clearAuthToken();
    setUser(null);
    setTasks([]);
    setWeeklyTasks([]);
    setLongTermTasks([]);
    setAiSuggestions([]);
    setSelectedTaskId(null);
  }

  async function handleAuth(event: FormEvent) {
    event.preventDefault();
    if (authMode === "register" && authForm.password !== authForm.confirmPassword) {
      showToast(t.passwordMismatch);
      return;
    }
    setBusy(true);
    try {
      const response = authMode === "login"
        ? await api.login(authForm.username.trim(), authForm.password)
        : await api.register(authForm.username.trim(), authForm.password, authForm.displayName.trim());
      setUser(response.user);
      await loadHomeData();
      setAuthForm({ username: "", password: "", confirmPassword: "", displayName: "" });
    } catch (error) {
      showToast(error);
    } finally {
      setBusy(false);
    }
  }

  async function handleDeleteTask(task: Task) {
    if (!window.confirm(t.confirmDeleteProject)) return;
    setBusy(true);
    try {
      await api.deleteTask(task.id);
      await loadTasks();
    } catch (error) {
      showToast(error);
    } finally {
      setBusy(false);
    }
  }

  async function handleToggleTaskPin(task: Task) {
    try {
      await api.setTaskPinned(task.id, !task.pinned);
      await loadTasks();
    } catch (error) {
      showToast(error);
    }
  }

  async function handleExportAllProjects() {
    if (exportingAllProjects) return;
    setExportingAllProjects(true);
    let objectUrl: string | null = null;
    try {
      const { blob, fileName } = await api.tasksAiExport();
      objectUrl = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = objectUrl;
      link.download = fileName;
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (error) {
      showToast(error);
    } finally {
      if (objectUrl) {
        const urlToRevoke = objectUrl;
        window.setTimeout(() => URL.revokeObjectURL(urlToRevoke), 0);
      }
      setExportingAllProjects(false);
    }
  }

  function showToast(error: unknown) {
    setToast(error instanceof Error ? error.message : String(error));
    window.setTimeout(() => setToast(null), 3200);
  }

  if (!user) {
    return (
      <AuthScreen
        t={t}
        mode={authMode}
        form={authForm}
        busy={busy}
        onChange={setAuthForm}
        onModeChange={setAuthMode}
        onSubmit={handleAuth}
      />
    );
  }

  return (
    <div className="app-shell">
      <AppHeader
        locale={locale}
        theme={theme}
        user={user}
        onFlashOpen={() => setFlashOpen(true)}
        onLocaleToggle={() => setLocale((current) => localeOrder[(localeOrder.indexOf(current) + 1) % localeOrder.length])}
        onThemeToggle={() => setTheme(theme === "dark" ? "light" : "dark")}
        onLogout={clearSession}
      />

      <main className={(flashOpen || (detailOpen && selectedTask)) ? "project-page-shell" : "workspace home-workspace"}>
        {flashOpen ? (
          <FlashNotes locale={locale} onClose={() => setFlashOpen(false)} onError={showToast} />
        ) : detailOpen && selectedTask ? (
          <ProjectDetail
            locale={locale}
            task={selectedTask}
            onClose={() => setDetailOpen(false)}
            onChanged={loadTasks}
            onError={showToast}
          />
        ) : (
          <section className="operations-column">
            <Dashboard
              tasks={tasks}
              metrics={metrics}
              weeklyTasks={weeklyTasks}
              longTermTasks={longTermTasks}
              aiSuggestions={aiSuggestions}
              locale={locale}
              onPersonalTasksChanged={loadPersonalTasks}
              onAiSuggestionsChanged={loadAiSuggestions}
              onError={showToast}
              onSelect={(id) => {
                setSelectedTaskId(id);
                setFlashOpen(false);
                setDetailOpen(true);
              }}
            />

            <ProjectList
              locale={locale}
              tasks={displayedTasks}
              selectedTaskId={selectedTaskId}
              keyword={keyword}
              filter={filter}
              sortBy={sortBy}
              sortOrder={sortOrder}
              onKeywordChange={setKeyword}
              onFilterChange={setFilter}
              onSortChange={setSortBy}
              onSortOrderToggle={() => setSortOrder((current) => current === "desc" ? "asc" : "desc")}
              onSelect={(id) => {
                setSelectedTaskId(id);
                setFlashOpen(false);
                setDetailOpen(true);
              }}
              onOpenDetail={(task) => {
                setSelectedTaskId(task.id);
                setFlashOpen(false);
                setDetailOpen(true);
              }}
              exportingAllProjects={exportingAllProjects}
              onExportAll={handleExportAllProjects}
              onCreate={() => setEditorTask("new")}
              onEdit={(task) => setEditorTask(task)}
              onDelete={handleDeleteTask}
              onPin={handleToggleTaskPin}
            />
          </section>
        )}
      </main>

      {editorTask && (
        <ProjectEditor
          locale={locale}
          task={editorTask === "new" ? null : editorTask}
          onClose={() => setEditorTask(null)}
          onSaved={async () => {
            setEditorTask(null);
            await loadTasks();
          }}
          onError={showToast}
        />
      )}

      {toast && <div className="toast">{toast}</div>}
      {busy && <div className="busy-bar">{t.loading}</div>}
    </div>
  );
}
