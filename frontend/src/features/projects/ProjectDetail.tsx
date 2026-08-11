import { useEffect, useState } from "react";
import { ArrowLeft, BookOpen, Check, Download, Edit3, Plus, Share2, X } from "lucide-react";
import { api } from "../../api";
import { dictionaries, noteTypeLabel, priorityLabel } from "../../i18n";
import type { Locale, NoteType, Phase, Priority, Task } from "../../types";
import { canEdit, canManageShares, ensurePhases, formatDate, formatProgress, movePhase, toTaskPayload } from "../../utils";
import { KnowledgeBlock } from "../../components/knowledge/KnowledgeBlock";
import { KnowledgeEditModal } from "../../components/knowledge/KnowledgeEditModal";
import { PhaseEditModal } from "../../components/phases/PhaseEditModal";
import { PhaseTree } from "../../components/phases/PhaseTree";
import { OptionButtons } from "../../components/ui/OptionButtons";
import { StatPill } from "../../components/ui/Pills";
import { ProgressBar } from "../../components/ui/Progress";
import { RichTextView } from "../../components/ui/RichText";
import { ShareModal } from "./Sharing";
import type { KnowledgeDraft, KnowledgeField, PhaseDraftMode } from "./types";
import { createChildPhaseDraft, createNextPhaseDraft, createRootPhaseDraft, priorityOption, removePhaseFromList, savePhaseDraftToList } from "./projectHelpers";

export function ProjectDetail({
  locale,
  task,
  onClose,
  onChanged,
  onError
}: {
  locale: Locale;
  task: Task | null;
  onClose: () => void;
  onChanged: () => Promise<void>;
  onError: (error: unknown) => void;
}) {
  const t = dictionaries[locale];
  const [phaseDraft, setPhaseDraft] = useState<Phase | null>(null);
  const [phaseDraftMode, setPhaseDraftMode] = useState<PhaseDraftMode | null>(null);
  const [descriptionDraft, setDescriptionDraft] = useState<string | null>(null);
  const [titleDraft, setTitleDraft] = useState<string | null>(null);
  const [knowledgeDraft, setKnowledgeDraft] = useState<KnowledgeDraft | null>(null);
  const [shareOpen, setShareOpen] = useState(false);
  const [exportingAiJson, setExportingAiJson] = useState(false);

  useEffect(() => {
    setPhaseDraft(null);
    setPhaseDraftMode(null);
    setDescriptionDraft(null);
    setTitleDraft(null);
    setKnowledgeDraft(null);
    setShareOpen(false);
    setExportingAiJson(false);
  }, [task?.id]);

  if (!task) {
    return <section className="panel detail-panel project-detail-page empty-detail"><BookOpen /><p>{t.selectProject}</p></section>;
  }

  async function savePhases(nextPhases: Phase[]) {
    if (!task || !canEdit(task)) return;
    try {
      await api.updateTask(task.id, toTaskPayload(task, nextPhases));
      await onChanged();
    } catch (error) {
      onError(error);
    }
  }

  async function saveTaskPatch(patch: Partial<Task>) {
    if (!task || !canEdit(task)) return;
    try {
      await api.updateTask(task.id, toTaskPayload({ ...task, ...patch }, ensurePhases(task)));
      await onChanged();
    } catch (error) {
      onError(error);
    }
  }

  function startPhaseEdit(phase: Phase) {
    setPhaseDraft({ ...phase });
    setPhaseDraftMode({ type: "edit", phaseKey: phase.phaseKey });
  }

  function addChildPhase(parentPhaseKey: string) {
    if (!task || !canEdit(task)) return;
    setPhaseDraft(createChildPhaseDraft(ensurePhases(task), parentPhaseKey, locale));
    setPhaseDraftMode({ type: "add-child", parentPhaseKey });
  }

  function addRootPhase() {
    if (!task || !canEdit(task)) return;
    setPhaseDraft(createRootPhaseDraft(ensurePhases(task), locale));
    setPhaseDraftMode({ type: "add-root" });
  }

  function addNextPhase(phaseKey: string) {
    if (!task || !canEdit(task)) return;
    setPhaseDraft(createNextPhaseDraft(ensurePhases(task), phaseKey, locale));
    setPhaseDraftMode({ type: "add-next", phaseKey });
  }

  async function deletePhase(phaseKey: string) {
    if (!task || !canEdit(task)) return;
    if (!window.confirm(t.confirmDeletePhase)) return;
    await savePhases(removePhaseFromList(ensurePhases(task), phaseKey));
  }

  async function moveDetailPhase(phaseKey: string, direction: -1 | 1) {
    if (!task || !canEdit(task)) return;
    await savePhases(movePhase(ensurePhases(task), phaseKey, direction));
  }

  async function saveKnowledgeField(field: KnowledgeField, value: string) {
    if (!task || !canEdit(task)) return;
    try {
      await api.updateTask(task.id, toTaskPayload({ ...task, [field]: value }, ensurePhases(task)));
      setKnowledgeDraft(null);
      await onChanged();
    } catch (error) {
      onError(error);
    }
  }

  async function deleteKnowledgeField(field: KnowledgeField) {
    if (!task || !canEdit(task)) return;
    if (!window.confirm(t.confirmDeleteKnowledge)) return;
    await saveKnowledgeField(field, "");
  }

  async function saveKnowledgeNote(value: string, noteType?: NoteType) {
    if (!task || !canEdit(task) || !knowledgeDraft) return;
    const cleanValue = value.trim();
    const nextType = noteType || (knowledgeDraft.mode === "edit-note" ? knowledgeDraft.note.noteType : knowledgeDraft.noteType);
    if (!cleanValue) return;
    try {
      if (knowledgeDraft.mode === "create") {
        await api.addTaskNote(task.id, nextType, cleanValue);
      } else if (knowledgeDraft.mode === "edit-note") {
        await api.updateTaskNote(task.id, knowledgeDraft.note.id, nextType, cleanValue);
      } else {
        await saveKnowledgeField(knowledgeDraft.field, cleanValue);
        return;
      }
      setKnowledgeDraft(null);
      await onChanged();
    } catch (error) {
      onError(error);
    }
  }

  async function deleteKnowledgeNote(noteId: number) {
    if (!task || !canEdit(task)) return;
    if (!window.confirm(t.confirmDeleteKnowledge)) return;
    try {
      await api.deleteTaskNote(task.id, noteId);
      await onChanged();
    } catch (error) {
      onError(error);
    }
  }

  async function saveDescription(value: string) {
    await saveTaskPatch({ taskDescription: value });
    setDescriptionDraft(null);
  }

  async function saveTitle() {
    if (!titleDraft?.trim()) return;
    await saveTaskPatch({ taskTitle: titleDraft.trim() });
    setTitleDraft(null);
  }

  async function savePriority(priority: Priority) {
    if (!task) return;
    if (priority === (task.priority || "MEDIUM")) return;
    await saveTaskPatch({ priority });
  }

  async function downloadAiJson() {
    if (!task || exportingAiJson) return;
    setExportingAiJson(true);
    let objectUrl: string | null = null;
    try {
      const { blob, fileName } = await api.taskAiExport(task.id);
      objectUrl = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = objectUrl;
      link.download = fileName;
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (error) {
      onError(error);
    } finally {
      if (objectUrl) {
        const urlToRevoke = objectUrl;
        window.setTimeout(() => URL.revokeObjectURL(urlToRevoke), 0);
      }
      setExportingAiJson(false);
    }
  }

  const knowledgeFields = [
    { field: "recentDecisions" as const, title: t.recentDecisions, type: "RECENT_DECISIONS" as NoteType, value: task.recentDecisions || "" },
    { field: "recentExperiments" as const, title: t.recentExperiments, type: "RECENT_EXPERIMENTS" as NoteType, value: task.recentExperiments || "" },
    { field: "knowledgeHighlights" as const, title: t.knowledgeHighlights, type: "KNOWLEDGE_HIGHLIGHTS" as NoteType, value: task.knowledgeHighlights || "" }
  ];
  const noteTypes: NoteType[] = ["RECENT_DECISIONS", "RECENT_EXPERIMENTS", "KNOWLEDGE_HIGHLIGHTS", "AI_SUGGESTIONS"];

  return (
    <section className="project-detail-page">
      <div className="project-page-toolbar">
        <button className="ghost-button icon-only transparent-back-button" type="button" onClick={onClose} title={t.back}>
          <ArrowLeft size={18} />
        </button>
      </div>

      <div className="panel detail-panel">
      <div className="detail-head project-detail-hero">
        <div>
          <p className="eyebrow">{t.manageProject}</p>
          {titleDraft === null ? (
            <h2>{task.taskTitle}</h2>
          ) : (
            <input
              className="project-title-edit-input"
              value={titleDraft}
              maxLength={200}
              autoFocus
              onChange={(event) => setTitleDraft(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === "Enter") void saveTitle();
                if (event.key === "Escape") setTitleDraft(null);
              }}
            />
          )}
        </div>
        <div className="detail-actions">
          {titleDraft === null ? (
            <>
              <button className="secondary-button" type="button" onClick={downloadAiJson} disabled={exportingAiJson || task.demo} title={task.demo ? t.demoExportUnavailable : undefined}>
                <Download size={17} />{exportingAiJson ? t.exportingAiJson : t.exportAiJson}
              </button>
              <button className="secondary-button" type="button" onClick={() => setShareOpen(true)} disabled={!canManageShares(task)}><Share2 size={17} />{t.shareProject}</button>
              <button className="primary-button" type="button" onClick={() => setTitleDraft(task.taskTitle || "")} disabled={!canEdit(task)}><Edit3 size={17} />{t.edit}</button>
            </>
          ) : (
            <>
              <button className="ghost-button" type="button" onClick={() => setTitleDraft(null)}><X size={17} />{t.cancel}</button>
              <button className="primary-button" type="button" onClick={saveTitle} disabled={!titleDraft.trim()}><Check size={17} />{t.save}</button>
            </>
          )}
        </div>
      </div>
      <div className="detail-stats">
        {canEdit(task) ? (
          <OptionButtons
            label={t.priority}
            value={task.priority || "MEDIUM"}
            options={(["HIGH", "MEDIUM", "LOW"] as Priority[]).map((value) => priorityOption(value, locale))}
            onChange={savePriority}
          />
        ) : (
          <StatPill label={t.priority} value={priorityLabel(task.priority, locale)} />
        )}
        <StatPill label={t.updated} value={formatDate(task.updatedAt)} />
        <StatPill label={t.progress} value={formatProgress(task.overallProgress)} />
      </div>
      <ProgressBar value={task.overallProgress} label={formatProgress(task.overallProgress)} large />
      <section className="detail-section">
        <div className="detail-section-head">
          <h3>{t.description}</h3>
          <button className="secondary-button" type="button" onClick={() => setDescriptionDraft(task.taskDescription || "")} disabled={!canEdit(task)}><Edit3 size={16} />{t.edit}</button>
        </div>
        <RichTextView text={task.taskDescription} empty={t.emptyKnowledge} />
      </section>
      <section className="detail-section">
        <div className="detail-section-head">
          <h3>{t.phases}</h3>
          <button className="secondary-button" type="button" onClick={addRootPhase} disabled={!canEdit(task)}>
            <Plus size={16} />{t.addPhase}
          </button>
        </div>
        <PhaseTree
          phases={ensurePhases(task)}
          locale={locale}
          editable={canEdit(task)}
          onEdit={startPhaseEdit}
          onAddNext={addNextPhase}
          onAddChild={addChildPhase}
          onDelete={deletePhase}
          onMove={moveDetailPhase}
        />
      </section>
      <section className="detail-section">
        <div className="detail-section-head">
          <h3>{t.knowledge}</h3>
          <button className="secondary-button" type="button" onClick={() => setKnowledgeDraft({ mode: "create", noteType: "RECENT_DECISIONS", value: "" })} disabled={!canEdit(task)}>
            <Plus size={16} />{t.addKnowledge}
          </button>
        </div>
        <div className="knowledge-groups">
          {noteTypes.map((type) => {
            const fieldItem = knowledgeFields.find((item) => item.type === type);
            const legacyText = (fieldItem?.value || "").trim();
            const notes = (task.notes || [])
              .filter((note) => note.noteType === type && note.noteContent.trim())
              .sort((a, b) => new Date(b.updatedAt || b.createdAt || 0).getTime() - new Date(a.updatedAt || a.createdAt || 0).getTime());
            const count = notes.length + (legacyText ? 1 : 0);
            return (
              <section className="knowledge-group" key={type}>
                <header className="knowledge-group-head">
                  <h4>{noteTypeLabel(type, locale)}</h4>
                </header>
                {count ? (
                  <div className="knowledge-grid">
                    {legacyText && fieldItem && (
                      <KnowledgeBlock
                        title={t.legacyKnowledge}
                        meta={fieldItem.title}
                        text={legacyText}
                        empty={t.emptyKnowledge}
                        locale={locale}
                        editable={canEdit(task)}
                        onEdit={() => setKnowledgeDraft({ mode: "edit-field", field: fieldItem.field, noteType: fieldItem.type, title: fieldItem.title, value: legacyText })}
                        onDelete={() => deleteKnowledgeField(fieldItem.field)}
                      />
                    )}
                    {notes.map((note, index) => (
                      <KnowledgeBlock
                        key={note.id}
                        title={`${t.knowledgeItem} ${notes.length - index}`}
                        meta={formatDate(note.updatedAt || note.createdAt)}
                        text={note.noteContent}
                        empty={t.emptyKnowledge}
                        locale={locale}
                        editable={canEdit(task)}
                        onEdit={() => setKnowledgeDraft({ mode: "edit-note", note })}
                        onDelete={() => deleteKnowledgeNote(note.id)}
                      />
                    ))}
                  </div>
                ) : (
                  <p className="empty-copy">{t.emptyKnowledge}</p>
                )}
              </section>
            );
          })}
        </div>
      </section>
      </div>
      {shareOpen && <ShareModal locale={locale} task={task} onClose={() => setShareOpen(false)} onError={onError} />}
      {descriptionDraft !== null && (
        <KnowledgeEditModal
          locale={locale}
          title={t.description}
          value={descriptionDraft}
          onClose={() => setDescriptionDraft(null)}
          onSave={saveDescription}
        />
      )}
      {knowledgeDraft && (
        <KnowledgeEditModal
          locale={locale}
          title={knowledgeDraft.mode === "create" ? t.addKnowledge : knowledgeDraft.mode === "edit-note" ? t.editKnowledge : knowledgeDraft.title}
          value={knowledgeDraft.mode === "edit-note" ? knowledgeDraft.note.noteContent : knowledgeDraft.value}
          noteType={knowledgeDraft.mode === "edit-note" ? knowledgeDraft.note.noteType : knowledgeDraft.noteType}
          showTypePicker={knowledgeDraft.mode !== "edit-field"}
          onClose={() => setKnowledgeDraft(null)}
          onSave={saveKnowledgeNote}
        />
      )}
      {phaseDraft && (
        <PhaseEditModal
          locale={locale}
          phase={phaseDraft}
          phases={ensurePhases(task)}
          onClose={() => { setPhaseDraft(null); setPhaseDraftMode(null); }}
          onSave={async (draft) => {
            if (!phaseDraftMode) return;
            await savePhases(savePhaseDraftToList(ensurePhases(task), phaseDraftMode, draft));
            setPhaseDraft(null);
            setPhaseDraftMode(null);
          }}
        />
      )}
    </section>
  );
}
