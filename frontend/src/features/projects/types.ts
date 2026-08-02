import type { NoteType, TaskNote } from "../../types";

export type TaskFilter = "all" | "active" | "completed" | "stuck" | "recent";
export type SortOrder = "asc" | "desc";
export type EditorTab = "base" | "phases" | "knowledge";
export type KnowledgeField = "recentDecisions" | "recentExperiments" | "knowledgeHighlights";
export type KnowledgeDraft =
  | { mode: "create"; noteType: NoteType; value: string }
  | { mode: "edit-note"; note: TaskNote }
  | { mode: "edit-field"; field: KnowledgeField; noteType: NoteType; title: string; value: string };
export type PhaseDraftMode =
  | { type: "edit"; phaseKey: string }
  | { type: "add-root" }
  | { type: "add-next"; phaseKey: string }
  | { type: "add-child"; parentPhaseKey: string };
