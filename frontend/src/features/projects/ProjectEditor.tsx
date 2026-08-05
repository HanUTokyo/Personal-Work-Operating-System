import type { FormEvent } from "react";
import { useState } from "react";
import { ChevronDown, ChevronUp, CornerDownRight, Edit3, Plus, Trash2, X } from "lucide-react";
import { api } from "../../api";
import { dictionaries, noteTypeLabel, priorityLabel } from "../../i18n";
import type { Locale, Phase, Priority, Task } from "../../types";
import { clampText, ensurePhases, formatDate, generatePhaseKey, movePhase, normalizePhases, toTaskPayload } from "../../utils";
import { KnowledgeEditModal } from "../../components/knowledge/KnowledgeEditModal";
import { PhaseEditModal } from "../../components/phases/PhaseEditModal";
import { OptionButtons } from "../../components/ui/OptionButtons";
import { StatusPill } from "../../components/ui/Pills";
import { RichTextEditor, RichTextView } from "../../components/ui/RichText";
import { useBodyScrollLock } from "../../hooks/useBodyScrollLock";
import type { EditorTab, KnowledgeField, PhaseDraftMode } from "./types";
import { createChildPhaseDraft, createNextPhaseDraft, editorTabLabel, priorityOption, savePhaseDraftToList } from "./projectHelpers";

export function ProjectEditor({ locale, task, onClose, onSaved, onError }: { locale: Locale; task: Task | null; onClose: () => void; onSaved: () => Promise<void>; onError: (error: unknown) => void }) {
  useBodyScrollLock();
  const t = dictionaries[locale];
  const [tab, setTab] = useState<EditorTab>("base");
  const [form, setForm] = useState<Partial<Task>>(() => task ? { ...task } : { taskTitle: "", priority: "MEDIUM", taskDescription: "" });
  const [phases, setPhases] = useState<Phase[]>(() => task ? ensurePhases(task) : normalizePhases([{ phaseKey: "phase-1", phaseName: "", phaseStatus: "TODO" }]));
  const [phaseDraft, setPhaseDraft] = useState<Phase | null>(null);
  const [phaseDraftMode, setPhaseDraftMode] = useState<PhaseDraftMode | null>(null);
  const [knowledgeDraft, setKnowledgeDraft] = useState<{ field: "recentDecisions" | "recentExperiments" | "knowledgeHighlights" | "currentActionGoal"; title: string; value: string } | null>(null);
  const [saving, setSaving] = useState(false);
  const knowledgeNotes = (task?.notes || [])
    .filter((note) => note.noteContent.trim())
    .sort((a, b) => new Date(b.updatedAt || b.createdAt || 0).getTime() - new Date(a.updatedAt || a.createdAt || 0).getTime());

  async function submit(event: FormEvent) {
    event.preventDefault();
    const payload = toTaskPayload(form, phases);
    if (!payload.taskTitle) return;
    setSaving(true);
    try {
      if (task) await api.updateTask(task.id, payload);
      else await api.createTask(payload);
      await onSaved();
    } catch (error) {
      onError(error);
    } finally {
      setSaving(false);
    }
  }

  function updatePhase(phaseKey: string, patch: Partial<Phase>) {
    setPhases((current) => normalizePhases(current.map((phase) => phase.phaseKey === phaseKey ? { ...phase, ...patch } : phase)));
  }

  function addPhase() {
    const phaseName = locale === "zh" ? `阶段${phases.length + 1}` : locale === "ja" ? `フェーズ${phases.length + 1}` : `Phase ${phases.length + 1}`;
    setPhases((current) => normalizePhases([...current, { phaseKey: generatePhaseKey(current, phaseName), phaseName, phaseStatus: "TODO" }]));
  }

  function addChildPhase(parentPhaseKey: string) {
    setPhaseDraft(createChildPhaseDraft(phases, parentPhaseKey, locale));
    setPhaseDraftMode({ type: "add-child", parentPhaseKey });
  }

  function addNextPhase(phaseKey: string) {
    setPhaseDraft(createNextPhaseDraft(phases, phaseKey, locale));
    setPhaseDraftMode({ type: "add-next", phaseKey });
  }

  function removePhase(phaseKey: string) {
    if (phases.length <= 1) return;
    setPhases((current) => normalizePhases(current.filter((phase) => phase.phaseKey !== phaseKey).map((phase) => ({
      ...phase,
      parentPhaseKey: phase.parentPhaseKey === phaseKey ? null : phase.parentPhaseKey
    }))));
  }

  const tabs: EditorTab[] = ["base", "phases", "knowledge"];
  return (
    <div className="modal-shell" role="dialog" aria-modal="true">
      <form className="editor-panel" onSubmit={submit}>
        <header className="editor-head">
          <div>
            <p className="eyebrow">{t.projectEditor}</p>
            <h2>{task ? `${t.edit}: ${task.taskTitle}` : t.newProject}</h2>
          </div>
          <button className="icon-only" type="button" onClick={onClose} title={t.close}><X /></button>
        </header>
        <nav className="editor-tabs">
          {tabs.map((item) => (
            <button key={item} type="button" className={tab === item ? "active" : ""} onClick={() => setTab(item)}>
              {editorTabLabel(item, locale)}
            </button>
          ))}
        </nav>
        <div className="editor-body">
          {tab === "base" && (
            <div className="form-grid">
              <label className="wide">{t.title}<input maxLength={200} required value={form.taskTitle || ""} onChange={(event) => setForm({ ...form, taskTitle: event.target.value })} /></label>
              <OptionButtons
                label={t.priority}
                value={form.priority || "MEDIUM"}
                options={(["HIGH", "MEDIUM", "LOW"] as Priority[]).map((value) => priorityOption(value, locale))}
                onChange={(value) => setForm({ ...form, priority: value })}
              />
              <div className="field wide"><span>{t.description}</span><RichTextEditor locale={locale} maxLength={2000} value={form.taskDescription || ""} onChange={(value) => setForm({ ...form, taskDescription: value })} /></div>
            </div>
          )}
          {tab === "phases" && (
            <div className="phase-editor">
              <button className="secondary-button fit" type="button" onClick={addPhase}><Plus size={16} />{t.addPhase}</button>
              {phases.map((phase) => (
                <article className="phase-editor-item" key={phase.phaseKey}>
                  <div className="phase-editor-summary">
                    <div className="phase-editor-title-line">
                      <h3>{phase.phaseName || t.stageName}</h3>
                      <StatusPill status={phase.phaseStatus} locale={locale} />
                    </div>
                    <p>{phase.parentPhaseKey ? `${t.parentStage}: ${phases.find((item) => item.phaseKey === phase.parentPhaseKey)?.phaseName || phase.parentPhaseKey}` : t.rootStage}</p>
                  </div>
                  {phase.phaseDescription && <RichTextView text={clampText(phase.phaseDescription, 180)} />}
                  <div className="phase-card-actions">
                    <div className="phase-action-row">
                      <button type="button" onClick={() => setPhases(movePhase(phases, phase.phaseKey, -1))} title={t.updated}><ChevronUp size={15} /></button>
                      <button type="button" onClick={() => setPhases(movePhase(phases, phase.phaseKey, 1))} title={t.updated}><ChevronDown size={15} /></button>
                      <button type="button" className="phase-add-button" onClick={() => addNextPhase(phase.phaseKey)} title={t.addNextPhase} aria-label={t.addNextPhase}><Plus size={15} /></button>
                      <button type="button" className="phase-add-button" onClick={() => addChildPhase(phase.phaseKey)} title={t.addChildPhase} aria-label={t.addChildPhase}><CornerDownRight size={15} /></button>
                    </div>
                    <div className="phase-action-row phase-management-row">
                      <button type="button" onClick={() => { setPhaseDraft({ ...phase }); setPhaseDraftMode({ type: "edit", phaseKey: phase.phaseKey }); }} title={t.editPhase}><Edit3 size={15} /></button>
                      <button type="button" className="phase-delete-button" onClick={() => removePhase(phase.phaseKey)} disabled={phases.length <= 1} title={t.deletePhase}><Trash2 size={15} /></button>
                    </div>
                  </div>
                </article>
              ))}
            </div>
          )}
          {tab === "knowledge" && (
            <div className="knowledge-editor-list">
              {([
                ["recentDecisions", t.recentDecisions, form.recentDecisions || ""],
                ["recentExperiments", t.recentExperiments, form.recentExperiments || ""],
                ["knowledgeHighlights", t.knowledgeHighlights, form.knowledgeHighlights || ""]
                ,["currentActionGoal", t.currentActionGoal, form.currentActionGoal || ""]
              ] as const).map(([field, title, value]) => (
                <article className="knowledge-editor-item" key={field}>
                  <div>
                    <h3>{title}</h3>
                    <RichTextView text={clampText(value, 220)} empty={t.emptyKnowledge} />
                  </div>
                  <button className="secondary-button" type="button" onClick={() => setKnowledgeDraft({ field, title, value })}><Edit3 size={16} />{t.editKnowledge}</button>
                </article>
              ))}
              {knowledgeNotes.map((note, index) => (
                <article className="knowledge-editor-item knowledge-note-preview" key={note.id}>
                  <div>
                    <p>{noteTypeLabel(note.noteType, locale)} · {formatDate(note.updatedAt || note.createdAt)}</p>
                    <RichTextView text={clampText(note.noteContent, 220)} empty={t.emptyKnowledge} />
                  </div>
                </article>
              ))}
            </div>
          )}
        </div>
        <footer className="editor-footer">
          <button className="ghost-button" type="button" onClick={onClose}>{t.cancel}</button>
          <button className="primary-button" type="submit" disabled={saving}>{saving ? t.saving : t.save}</button>
        </footer>
      </form>
      {phaseDraft && (
        <PhaseEditModal
          locale={locale}
          phase={phaseDraft}
          phases={phases}
          onClose={() => { setPhaseDraft(null); setPhaseDraftMode(null); }}
          onSave={(draft) => {
            if (!phaseDraftMode) return;
            if (phaseDraftMode.type === "edit") updatePhase(phaseDraftMode.phaseKey, draft);
            else setPhases((current) => savePhaseDraftToList(current, phaseDraftMode, draft));
            setPhaseDraft(null);
            setPhaseDraftMode(null);
          }}
        />
      )}
      {knowledgeDraft && (
        <KnowledgeEditModal
          locale={locale}
          title={knowledgeDraft.title}
          value={knowledgeDraft.value}
          onClose={() => setKnowledgeDraft(null)}
          onSave={(value) => {
            setForm((current) => ({ ...current, [knowledgeDraft.field]: value }));
            setKnowledgeDraft(null);
          }}
        />
      )}
    </div>
  );
}
