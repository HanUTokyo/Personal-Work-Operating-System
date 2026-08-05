export type Locale = "zh" | "en" | "ja";
export type Priority = "HIGH" | "MEDIUM" | "LOW";
export type PhaseStatus = "TODO" | "DOING" | "DONE";
export type NoteType = "RECENT_DECISIONS" | "RECENT_EXPERIMENTS" | "KNOWLEDGE_HIGHLIGHTS" | "AI_SUGGESTIONS";
export type AccessLevel = "OWNER" | "EDIT" | "VIEW";
export type SharePermission = "VIEW" | "EDIT";
export type PersonalTaskType = "WEEKLY" | "LONG_TERM";

export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  errors?: Record<string, string>;
}

export interface UserResponse {
  id: number;
  username: string;
  displayName?: string;
}

export interface AuthResponse {
  token: string;
  user: UserResponse;
}

export interface Phase {
  id?: number;
  phaseKey: string;
  parentPhaseKey?: string | null;
  phaseName: string;
  phaseDescription?: string;
  phaseStatus: PhaseStatus;
  sortOrder?: number;
}

export interface TaskNote {
  id: number;
  noteType: NoteType;
  noteContent: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface Task {
  id: number;
  taskTitle: string;
  taskDescription?: string;
  recentDecisions?: string;
  recentExperiments?: string;
  knowledgeHighlights?: string;
  priority?: Priority;
  ownerUserId?: number;
  ownerUsername?: string;
  ownedByCurrentUser?: boolean;
  sharedWithCurrentUser?: boolean;
  accessLevel?: AccessLevel;
  pinned?: boolean;
  archived?: boolean;
  archivedAt?: string;
  phases?: Phase[];
  notes?: TaskNote[];
  overallProgress: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface TaskPayload {
  taskTitle: string;
  taskDescription?: string;
  recentDecisions?: string;
  recentExperiments?: string;
  knowledgeHighlights?: string;
  priority: Priority;
  phases: Phase[];
}

export interface TaskShare {
  id: number;
  taskId: number;
  sharedWith: UserResponse;
  permission: SharePermission;
}

export interface FlashNote {
  id: number;
  noteContent: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface GlobalAiSuggestion {
  id: number;
  content: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface GlobalActionGoal {
  id: number;
  content: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface PersonalTask {
  id: number;
  type: PersonalTaskType;
  content: string;
  completed: boolean;
  pinned: boolean;
  sortOrder: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface ProjectMetrics {
  total: number;
  doing: number;
  done: number;
  stuck: number;
  recent: number;
  averageProgress: number;
}

export interface PhaseNode {
  phase: Phase;
  children: PhaseNode[];
}
