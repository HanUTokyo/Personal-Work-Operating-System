import type { Phase, PhaseNode, Priority, ProjectMetrics, Task, TaskPayload } from "./types";

export function formatDate(value?: string) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "-";
  return date.toLocaleDateString();
}

export function formatProgress(value: number | undefined) {
  return `${Math.round(Math.max(0, Math.min(100, Number(value) || 0)))}%`;
}

export function isCompleted(task: Task) {
  return Number(task.overallProgress) >= 100;
}

export function isStuck(task: Task) {
  if (isCompleted(task)) return false;
  const updated = task.updatedAt ? new Date(task.updatedAt) : null;
  if (!updated || Number.isNaN(updated.getTime())) return false;
  return Date.now() - updated.getTime() > 30 * 24 * 60 * 60 * 1000;
}

export function isRecent(task: Task) {
  const updated = task.updatedAt ? new Date(task.updatedAt) : null;
  if (!updated || Number.isNaN(updated.getTime())) return false;
  return Date.now() - updated.getTime() <= 7 * 24 * 60 * 60 * 1000;
}

export function computeMetrics(tasks: Task[]): ProjectMetrics {
  const total = tasks.length;
  const done = tasks.filter(isCompleted).length;
  const doing = tasks.filter((task) => Number(task.overallProgress) > 0 && !isCompleted(task)).length;
  const stuck = tasks.filter(isStuck).length;
  const recent = tasks.filter(isRecent).length;
  const averageProgress = total
    ? tasks.reduce((sum, task) => sum + (Number(task.overallProgress) || 0), 0) / total
    : 0;
  return { total, doing, done, stuck, recent, averageProgress };
}

export function normalizePriority(priority?: Priority): Priority {
  return priority || "MEDIUM";
}

export function ensurePhases(task: Task): Phase[] {
  if (Array.isArray(task.phases) && task.phases.length) {
    return normalizePhases(task.phases);
  }
  return normalizePhases([
    { phaseKey: "phase-1", phaseName: "Phase 1", phaseStatus: "TODO" },
    { phaseKey: "phase-2", phaseName: "Phase 2", phaseStatus: "TODO" },
    { phaseKey: "phase-3", phaseName: "Phase 3", phaseStatus: "TODO" }
  ]);
}

export function normalizePhases(phases: Phase[]): Phase[] {
  const used = new Set<string>();
  return phases.map((phase, index) => {
    const rawKey = normalizeKey(phase.phaseKey || phase.phaseName || `phase-${index + 1}`);
    let phaseKey = rawKey || `phase-${index + 1}`;
    while (used.has(phaseKey)) phaseKey = `${phaseKey}-${index + 1}`;
    used.add(phaseKey);
    return {
      ...phase,
      phaseKey,
      parentPhaseKey: phase.parentPhaseKey || null,
      phaseName: (phase.phaseName || `Phase ${index + 1}`).trim(),
      phaseDescription: phase.phaseDescription || "",
      phaseStatus: phase.phaseStatus || "TODO",
      sortOrder: index + 1
    };
  }).map((phase, _, all) => {
    const keys = new Set(all.map((item) => item.phaseKey));
    return {
      ...phase,
      parentPhaseKey: phase.parentPhaseKey && keys.has(phase.parentPhaseKey) && phase.parentPhaseKey !== phase.phaseKey
        ? phase.parentPhaseKey
        : null
    };
  });
}

export function normalizeKey(value: string) {
  return value
    .toLowerCase()
    .trim()
    .replace(/[^a-z0-9\u4e00-\u9fa5]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .slice(0, 100);
}

export function generatePhaseKey(phases: Phase[], name: string) {
  const keys = new Set(phases.map((phase) => phase.phaseKey));
  const base = normalizeKey(name) || `phase-${phases.length + 1}`;
  let key = base;
  let count = 1;
  while (keys.has(key)) {
    count += 1;
    key = `${base}-${count}`;
  }
  return key;
}

export function buildPhaseTree(phases: Phase[]): PhaseNode[] {
  const normalized = normalizePhases(phases);
  const map = new Map<string, PhaseNode>();
  normalized.forEach((phase) => map.set(phase.phaseKey, { phase, children: [] }));
  const roots: PhaseNode[] = [];
  map.forEach((node) => {
    const parent = node.phase.parentPhaseKey ? map.get(node.phase.parentPhaseKey) : undefined;
    if (parent) parent.children.push(node);
    else roots.push(node);
  });
  return roots;
}

export function movePhase(phases: Phase[], phaseKey: string, direction: -1 | 1) {
  const next = normalizePhases(phases);
  const index = next.findIndex((phase) => phase.phaseKey === phaseKey);
  if (index < 0) return next;
  const phase = next[index];
  const siblingIndexes = next
    .map((item, itemIndex) => ({ item, itemIndex }))
    .filter(({ item }) => (item.parentPhaseKey || null) === (phase.parentPhaseKey || null))
    .map(({ itemIndex }) => itemIndex);
  const siblingPosition = siblingIndexes.indexOf(index);
  const targetIndex = siblingIndexes[siblingPosition + direction];
  if (targetIndex === undefined) return next;
  const [removed] = next.splice(index, 1);
  next.splice(targetIndex, 0, removed);
  return normalizePhases(next);
}

export function toTaskPayload(task: Partial<Task>, phases: Phase[]): TaskPayload {
  return {
    taskTitle: (task.taskTitle || "").trim(),
    taskDescription: task.taskDescription || "",
    recentDecisions: task.recentDecisions || "",
    recentExperiments: task.recentExperiments || "",
    knowledgeHighlights: task.knowledgeHighlights || "",
    priority: normalizePriority(task.priority),
    phases: normalizePhases(phases),
    expectedRevision: task.revision
  };
}

export function canEdit(task?: Task | null) {
  return Boolean(task && (task.accessLevel === "OWNER" || task.accessLevel === "EDIT" || task.ownedByCurrentUser));
}

export function canManageShares(task?: Task | null) {
  return Boolean(task && (task.accessLevel === "OWNER" || task.ownedByCurrentUser));
}

export function clampText(text = "", max = 180) {
  const normalized = text.trim();
  if (normalized.length <= max) return normalized;
  return `${normalized.slice(0, max)}...`;
}
