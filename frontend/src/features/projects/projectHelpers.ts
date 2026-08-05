import { dictionaries, priorityLabel } from "../../i18n";
import type { Locale, Phase, Priority, Task } from "../../types";
import { generatePhaseKey, normalizePhases } from "../../utils";
import type { EditorTab, PhaseDraftMode, TaskFilter } from "./types";

export function filterLabel(filter: TaskFilter, locale: Locale) {
  const t = dictionaries[locale];
  return { all: t.all, active: t.active, completed: t.completed, stuck: t.stuck, recent: t.recent, archived: t.archived }[filter];
}

export function editorTabLabel(tab: EditorTab, locale: Locale) {
  const t = dictionaries[locale];
  return { base: t.details, phases: t.phases, knowledge: t.knowledge }[tab];
}

export function priorityWeight(priority?: Priority) {
  return priority === "HIGH" ? 3 : priority === "LOW" ? 1 : 2;
}

export function byUpdatedDesc(a: Task, b: Task) {
  return new Date(b.updatedAt || 0).getTime() - new Date(a.updatedAt || 0).getTime();
}

export function byUpdatedAsc(a: Task, b: Task) {
  return new Date(a.updatedAt || 0).getTime() - new Date(b.updatedAt || 0).getTime();
}

export function priorityOption(value: Priority, locale: Locale) {
  return {
    value,
    label: priorityLabel(value, locale),
    className: `priority-choice priority-${value.toLowerCase()}`
  };
}

export function createRootPhaseDraft(phases: Phase[], locale: Locale): Phase {
  const current = normalizePhases(phases);
  const phaseName = locale === "zh" ? "新阶段" : locale === "ja" ? "新しいフェーズ" : "New phase";
  return {
    phaseKey: generatePhaseKey(current, phaseName),
    parentPhaseKey: null,
    phaseName,
    phaseStatus: "TODO",
    phaseDescription: ""
  };
}

export function createNextPhaseDraft(phases: Phase[], phaseKey: string, locale: Locale): Phase {
  const current = normalizePhases(phases);
  const index = current.findIndex((phase) => phase.phaseKey === phaseKey);
  const source = current[index];
  const phaseName = locale === "zh" ? "新阶段" : locale === "ja" ? "新しいフェーズ" : "New phase";
  return {
    phaseKey: generatePhaseKey(current, `${phaseKey}-${phaseName}`),
    parentPhaseKey: source?.parentPhaseKey || null,
    phaseName,
    phaseStatus: "TODO",
    phaseDescription: ""
  };
}

export function createChildPhaseDraft(phases: Phase[], parentPhaseKey: string, locale: Locale): Phase {
  const current = normalizePhases(phases);
  const phaseName = locale === "zh" ? "新子阶段" : locale === "ja" ? "新しい子フェーズ" : "New child phase";
  return {
    phaseKey: generatePhaseKey(current, `${parentPhaseKey}-${phaseName}`),
    parentPhaseKey,
    phaseName,
    phaseStatus: "TODO",
    phaseDescription: ""
  };
}

export function savePhaseDraftToList(phases: Phase[], mode: PhaseDraftMode, draft: Phase) {
  const current = normalizePhases(phases);
  const cleanDraft: Phase = {
    ...draft,
    phaseName: draft.phaseName.trim(),
    phaseDescription: draft.phaseDescription || "",
    phaseStatus: draft.phaseStatus || "TODO"
  };

  if (mode.type === "edit") {
    return normalizePhases(current.map((phase) => (
      phase.phaseKey === mode.phaseKey ? { ...phase, ...cleanDraft, phaseKey: mode.phaseKey } : phase
    )));
  }

  const withoutDraft = current.filter((phase) => phase.phaseKey !== cleanDraft.phaseKey);

  if (mode.type === "add-root") {
    return normalizePhases([...withoutDraft, { ...cleanDraft, parentPhaseKey: null }]);
  }

  if (mode.type === "add-child") {
    return normalizePhases([...withoutDraft, { ...cleanDraft, parentPhaseKey: mode.parentPhaseKey }]);
  }

  const sourceIndex = withoutDraft.findIndex((phase) => phase.phaseKey === mode.phaseKey);
  const source = withoutDraft[sourceIndex];
  const next = [...withoutDraft];
  next.splice(sourceIndex >= 0 ? sourceIndex + 1 : next.length, 0, {
    ...cleanDraft,
    parentPhaseKey: source?.parentPhaseKey || null
  });
  return normalizePhases(next);
}

export function removePhaseFromList(phases: Phase[], phaseKey: string) {
  const next = normalizePhases(phases)
    .filter((phase) => phase.phaseKey !== phaseKey)
    .map((phase) => ({
      ...phase,
      parentPhaseKey: phase.parentPhaseKey === phaseKey ? null : phase.parentPhaseKey
    }));
  return normalizePhases(next.length ? next : phases);
}
