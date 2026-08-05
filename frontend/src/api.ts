import type {
  ApiResponse,
  AuthResponse,
  FlashNote,
  GlobalAiSuggestion,
  GlobalActionGoal,
  PersonalTask,
  PersonalTaskType,
  Task,
  TaskNote,
  TaskPayload,
  TaskShare,
  SharePermission,
  UserResponse,
  NoteType
} from "./types";

const API_BASE_URL =
  import.meta.env.VITE_TASK_API_BASE_URL ||
  (window as unknown as { TASK_API_BASE_URL?: string }).TASK_API_BASE_URL ||
  `${window.location.protocol}//${window.location.hostname || "localhost"}:8080/api`;

let authToken = localStorage.getItem("task-app-auth-token") || "";

export function getAuthToken() {
  return authToken;
}

export function setAuthToken(token: string) {
  authToken = token;
  localStorage.setItem("task-app-auth-token", token);
}

export function clearAuthToken() {
  authToken = "";
  localStorage.removeItem("task-app-auth-token");
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers);
  headers.set("Content-Type", "application/json");
  if (authToken) {
    headers.set("Authorization", `Bearer ${authToken}`);
  }

  const response = await fetch(`${API_BASE_URL}${path}`, { ...options, headers });
  const payload = (await response.json().catch(() => null)) as ApiResponse<T> | null;
  if (!response.ok || !payload?.success) {
    const detail = payload?.errors ? Object.values(payload.errors)[0] : undefined;
    throw new Error(detail || payload?.message || `HTTP ${response.status}`);
  }
  return payload.data;
}

async function requestFile(path: string, fallbackFileName: string) {
  const headers = new Headers();
  headers.set("Accept", "application/json");
  if (authToken) {
    headers.set("Authorization", `Bearer ${authToken}`);
  }

  const response = await fetch(`${API_BASE_URL}${path}`, { headers });
  if (!response.ok) {
    const payload = (await response.json().catch(() => null)) as ApiResponse<unknown> | null;
    const detail = payload?.errors ? Object.values(payload.errors)[0] : undefined;
    throw new Error(detail || payload?.message || `HTTP ${response.status}`);
  }

  const disposition = response.headers.get("Content-Disposition") || "";
  const fileNameMatch = disposition.match(/filename="?([^";]+)"?/i);
  return {
    blob: await response.blob(),
    fileName: fileNameMatch?.[1] || fallbackFileName
  };
}

export const api = {
  async login(username: string, password: string) {
    const data = await request<AuthResponse>("/auth/login", {
      method: "POST",
      body: JSON.stringify({ username, password })
    });
    setAuthToken(data.token);
    return data;
  },

  async register(username: string, password: string, displayName: string) {
    const data = await request<AuthResponse>("/auth/register", {
      method: "POST",
      body: JSON.stringify({ username, password, displayName })
    });
    setAuthToken(data.token);
    return data;
  },

  me() {
    return request<UserResponse>("/auth/me");
  },

  tasks(params: { keyword?: string; sortBy?: string; order?: string; archived?: boolean } = {}) {
    const url = new URLSearchParams();
    if (params.keyword) url.set("keyword", params.keyword);
    if (params.sortBy) url.set("sortBy", params.sortBy);
    if (params.order) url.set("order", params.order);
    if (params.archived) url.set("archived", "true");
    const suffix = url.toString() ? `?${url}` : "";
    return request<Task[]>(`/tasks${suffix}`);
  },

  taskAiExport(taskId: number) {
    return requestFile(`/tasks/${taskId}/ai-export`, `project-${taskId}.json`);
  },

  tasksAiExport() {
    return requestFile("/tasks/ai-export", "projects-ai-export.json");
  },

  createTask(payload: TaskPayload) {
    return request<Task>("/tasks", { method: "POST", body: JSON.stringify(payload) });
  },

  updateTask(taskId: number, payload: TaskPayload) {
    return request<Task>(`/tasks/${taskId}`, { method: "PUT", body: JSON.stringify(payload) });
  },

  deleteTask(taskId: number) {
    return request<void>(`/tasks/${taskId}`, { method: "DELETE" });
  },

  setTaskArchived(taskId: number, archived: boolean) {
    return request<void>(`/tasks/${taskId}/archive`, { method: archived ? "POST" : "DELETE" });
  },

  setTaskPinned(taskId: number, pinned: boolean) {
    return request<void>(`/tasks/${taskId}/pin`, {
      method: "PUT",
      body: JSON.stringify({ pinned })
    });
  },

  addTaskNote(taskId: number, noteType: NoteType, noteContent: string) {
    return request<TaskNote>(`/tasks/${taskId}/notes`, {
      method: "POST",
      body: JSON.stringify({ noteType, noteContent })
    });
  },

  updateTaskNote(taskId: number, noteId: number, noteType: NoteType, noteContent: string) {
    return request<TaskNote>(`/tasks/${taskId}/notes/${noteId}`, {
      method: "PUT",
      body: JSON.stringify({ noteType, noteContent })
    });
  },

  deleteTaskNote(taskId: number, noteId: number) {
    return request<void>(`/tasks/${taskId}/notes/${noteId}`, { method: "DELETE" });
  },

  flashNotes() {
    return request<FlashNote[]>("/flash-notes");
  },

  createFlashNote(noteContent: string) {
    return request<FlashNote>("/flash-notes", {
      method: "POST",
      body: JSON.stringify({ noteContent } satisfies FlashNoteCreateRequest)
    });
  },

  updateFlashNote(noteId: number, noteContent: string) {
    return request<FlashNote>(`/flash-notes/${noteId}`, {
      method: "PUT",
      body: JSON.stringify({ noteContent } satisfies FlashNoteCreateRequest)
    });
  },

  deleteFlashNote(noteId: number) {
    return request<void>(`/flash-notes/${noteId}`, { method: "DELETE" });
  },

  aiSuggestions() {
    return request<GlobalAiSuggestion[]>("/ai-suggestions");
  },

  createAiSuggestion(content: string) {
    return request<GlobalAiSuggestion>("/ai-suggestions", {
      method: "POST",
      body: JSON.stringify({ content })
    });
  },

  updateAiSuggestion(suggestionId: number, content: string) {
    return request<GlobalAiSuggestion>(`/ai-suggestions/${suggestionId}`, {
      method: "PUT",
      body: JSON.stringify({ content })
    });
  },

  deleteAiSuggestion(suggestionId: number) {
    return request<void>(`/ai-suggestions/${suggestionId}`, { method: "DELETE" });
  },

  actionGoals() {
    return request<GlobalActionGoal[]>("/current-action-goals");
  },

  createActionGoal(content: string) {
    return request<GlobalActionGoal>("/current-action-goals", { method: "POST", body: JSON.stringify({ content }) });
  },

  updateActionGoal(goalId: number, content: string) {
    return request<GlobalActionGoal>(`/current-action-goals/${goalId}`, { method: "PUT", body: JSON.stringify({ content }) });
  },

  deleteActionGoal(goalId: number) {
    return request<void>(`/current-action-goals/${goalId}`, { method: "DELETE" });
  },

  personalTasks(type: PersonalTaskType) {
    return request<PersonalTask[]>(`/personal-tasks?type=${type}`);
  },

  createPersonalTask(type: PersonalTaskType, content: string) {
    return request<PersonalTask>("/personal-tasks", {
      method: "POST",
      body: JSON.stringify({ type, content })
    });
  },

  updatePersonalTask(taskId: number, content: string, completed: boolean, pinned: boolean) {
    return request<PersonalTask>(`/personal-tasks/${taskId}`, {
      method: "PUT",
      body: JSON.stringify({ content, completed, pinned })
    });
  },

  reorderPersonalTasks(type: PersonalTaskType, taskIds: number[]) {
    return request<void>("/personal-tasks/reorder", {
      method: "PUT",
      body: JSON.stringify({ type, taskIds })
    });
  },

  deletePersonalTask(taskId: number) {
    return request<void>(`/personal-tasks/${taskId}`, { method: "DELETE" });
  },

  shares(taskId: number) {
    return request<TaskShare[]>(`/tasks/${taskId}/shares`);
  },

  addShare(taskId: number, username: string, permission: SharePermission) {
    return request<TaskShare>(`/tasks/${taskId}/shares`, {
      method: "POST",
      body: JSON.stringify({ username, permission })
    });
  },

  updateShare(taskId: number, shareId: number, username: string, permission: SharePermission) {
    return request<TaskShare>(`/tasks/${taskId}/shares/${shareId}`, {
      method: "PUT",
      body: JSON.stringify({ username, permission })
    });
  },

  deleteShare(taskId: number, shareId: number) {
    return request<void>(`/tasks/${taskId}/shares/${shareId}`, { method: "DELETE" });
  }
};

interface FlashNoteCreateRequest {
  noteContent: string;
}
