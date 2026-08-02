import { FormEvent, useEffect, useState } from "react";
import { Check, ChevronDown, ChevronUp, Edit3, Pin, PinOff, Plus, Trash2, X } from "lucide-react";
import { api } from "../../api";
import { dictionaries } from "../../i18n";
import type { Locale, PersonalTask, PersonalTaskType } from "../../types";
import { useBodyScrollLock } from "../../hooks/useBodyScrollLock";
import { RichTextEditor, RichTextView } from "../../components/ui/RichText";

export function PersonalTaskList({
  locale,
  type,
  tasks,
  onChanged,
  onError
}: {
  locale: Locale;
  type: PersonalTaskType;
  tasks: PersonalTask[];
  onChanged: () => Promise<void>;
  onError: (error: unknown) => void;
}) {
  const t = dictionaries[locale];
  const [draft, setDraft] = useState<PersonalTask | "new" | null>(null);
  const [savingId, setSavingId] = useState<number | null>(null);
  const [reordering, setReordering] = useState(false);
  const title = type === "WEEKLY" ? t.weeklyTasks : t.longTermTasks;

  async function toggle(task: PersonalTask) {
    setSavingId(task.id);
    try {
      await api.updatePersonalTask(task.id, task.content, !task.completed, task.pinned);
      await onChanged();
    } catch (error) {
      onError(error);
    } finally {
      setSavingId(null);
    }
  }

  async function togglePin(task: PersonalTask) {
    setSavingId(task.id);
    try {
      await api.updatePersonalTask(task.id, task.content, task.completed, !task.pinned);
      await onChanged();
    } catch (error) {
      onError(error);
    } finally {
      setSavingId(null);
    }
  }

  function moveTarget(task: PersonalTask, direction: -1 | 1) {
    const sameGroup = tasks.filter((item) => item.pinned === task.pinned);
    const position = sameGroup.findIndex((item) => item.id === task.id);
    return sameGroup[position + direction] || null;
  }

  async function move(task: PersonalTask, direction: -1 | 1) {
    const target = moveTarget(task, direction);
    if (!target) return;
    const taskIndex = tasks.findIndex((item) => item.id === task.id);
    const targetIndex = tasks.findIndex((item) => item.id === target.id);
    const nextTasks = [...tasks];
    [nextTasks[taskIndex], nextTasks[targetIndex]] = [nextTasks[targetIndex], nextTasks[taskIndex]];
    setReordering(true);
    try {
      await api.reorderPersonalTasks(type, nextTasks.map((item) => item.id));
      await onChanged();
    } catch (error) {
      onError(error);
    } finally {
      setReordering(false);
    }
  }

  async function remove(taskId: number) {
    if (!window.confirm(t.confirmDeletePersonalTask)) return;
    setSavingId(taskId);
    try {
      await api.deletePersonalTask(taskId);
      await onChanged();
    } catch (error) {
      onError(error);
    } finally {
      setSavingId(null);
    }
  }

  return (
    <section className="panel summary-panel personal-task-panel">
      <header className="section-head personal-task-head">
        <h2>{title}</h2>
        <button className="primary-button" type="button" onClick={() => setDraft("new")}><Plus size={16} />{t.newPersonalTask}</button>
      </header>
      {tasks.length ? (
        <div className="personal-task-list">
          {tasks.map((task) => {
            const busy = savingId !== null || reordering;
            const canMoveUp = Boolean(moveTarget(task, -1));
            const canMoveDown = Boolean(moveTarget(task, 1));
            return (
            <article className={`personal-task-item ${task.completed ? "completed" : ""}`} key={task.id}>
              <label className="personal-task-check" title={task.completed ? t.completed : t.active}>
                <input type="checkbox" checked={task.completed} disabled={busy} onChange={() => toggle(task)} />
                <span aria-hidden="true"><Check size={14} /></span>
              </label>
              <div className="personal-task-content"><RichTextView text={task.content} /></div>
              <div className="personal-task-actions">
                <button className={`icon-only ${task.pinned ? "active" : ""}`} type="button" disabled={busy} onClick={() => togglePin(task)} title={task.pinned ? t.unpinPersonalTask : t.pinPersonalTask}>
                  {task.pinned ? <PinOff size={16} /> : <Pin size={16} />}
                </button>
                <button className="icon-only" type="button" disabled={busy || !canMoveUp} onClick={() => move(task, -1)} title={t.movePersonalTaskUp}><ChevronUp size={16} /></button>
                <button className="icon-only" type="button" disabled={busy || !canMoveDown} onClick={() => move(task, 1)} title={t.movePersonalTaskDown}><ChevronDown size={16} /></button>
                <button className="icon-only" type="button" disabled={busy} onClick={() => setDraft(task)} title={t.edit}><Edit3 size={16} /></button>
                <button className="icon-only danger-subtle" type="button" disabled={busy} onClick={() => remove(task.id)} title={t.delete}><Trash2 size={16} /></button>
              </div>
            </article>
            );
          })}
        </div>
      ) : <p className="empty-copy">{t.noPersonalTasks}</p>}
      {draft && (
        <PersonalTaskEditor
          locale={locale}
          type={type}
          task={draft === "new" ? null : draft}
          onClose={() => setDraft(null)}
          onSaved={async () => {
            setDraft(null);
            await onChanged();
          }}
          onError={onError}
        />
      )}
    </section>
  );
}

function PersonalTaskEditor({
  locale,
  type,
  task,
  onClose,
  onSaved,
  onError
}: {
  locale: Locale;
  type: PersonalTaskType;
  task: PersonalTask | null;
  onClose: () => void;
  onSaved: () => Promise<void>;
  onError: (error: unknown) => void;
}) {
  useBodyScrollLock();
  const t = dictionaries[locale];
  const [content, setContent] = useState(task?.content || "");
  const [saving, setSaving] = useState(false);

  useEffect(() => setContent(task?.content || ""), [task]);

  async function submit(event: FormEvent) {
    event.preventDefault();
    const nextContent = content.trim();
    if (!nextContent) return;
    setSaving(true);
    try {
      if (task) await api.updatePersonalTask(task.id, nextContent, task.completed, task.pinned);
      else await api.createPersonalTask(type, nextContent);
      await onSaved();
    } catch (error) {
      onError(error);
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="modal-shell nested-modal" role="dialog" aria-modal="true">
      <form className="editor-panel focused-editor-panel personal-task-editor" onSubmit={submit}>
        <header className="editor-head">
          <div><p className="eyebrow">{task ? t.editPersonalTask : t.newPersonalTask}</p><h2>{type === "WEEKLY" ? t.weeklyTasks : t.longTermTasks}</h2></div>
          <button className="icon-only" type="button" onClick={onClose} title={t.close}><X /></button>
        </header>
        <div className="editor-body">
          <RichTextEditor locale={locale} value={content} maxLength={20000} onChange={setContent} />
        </div>
        <footer className="editor-footer">
          <button className="ghost-button" type="button" onClick={onClose}>{t.cancel}</button>
          <button className="primary-button" type="submit" disabled={saving || !content.trim()}>{saving ? t.saving : t.save}</button>
        </footer>
      </form>
    </div>
  );
}
