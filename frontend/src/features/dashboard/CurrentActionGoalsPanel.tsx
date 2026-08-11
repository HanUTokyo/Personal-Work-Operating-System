import type { FormEvent } from "react";
import { useEffect, useState } from "react";
import { Edit3, Plus, Target, Trash2, X } from "lucide-react";
import { api } from "../../api";
import { RichTextEditor, RichTextView } from "../../components/ui/RichText";
import { useBodyScrollLock } from "../../hooks/useBodyScrollLock";
import { dictionaries } from "../../i18n";
import type { GlobalActionGoal, Locale } from "../../types";
import { formatDate } from "../../utils";

export function CurrentActionGoalsPanel({ goals, locale, onChanged, onError }: { goals: GlobalActionGoal[]; locale: Locale; onChanged: () => Promise<void>; onError: (error: unknown) => void }) {
  const t = dictionaries[locale];
  const [draft, setDraft] = useState<GlobalActionGoal | "new" | null>(null);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  async function remove(id: number) {
    if (!window.confirm(t.confirmDeleteCurrentActionGoal)) return;
    setDeletingId(id);
    try { await api.deleteActionGoal(id); await onChanged(); } catch (error) { onError(error); } finally { setDeletingId(null); }
  }
  return <section className="panel ai-suggestions-panel" id="current-action-goals">
    <header className="section-head ai-suggestions-panel-head"><h2><Target aria-hidden="true" />{t.currentActionGoal}</h2><div className="ai-suggestions-head-actions">{goals.length > 0 && <span>{goals.length}</span>}<button className="primary-button" type="button" onClick={() => setDraft("new")}><Plus size={16} />{t.newCurrentActionGoal}</button></div></header>
    {goals.length ? <div className="ai-suggestions-grid">{goals.map((goal) => <article className="ai-suggestion-card" key={goal.id}><header className="ai-suggestion-card-head"><time dateTime={goal.updatedAt || goal.createdAt}>{formatDate(goal.updatedAt || goal.createdAt)}</time><div className="inline-actions"><button className="icon-only" type="button" onClick={() => setDraft(goal)} title={t.edit}><Edit3 size={16} /></button><button className="icon-only danger-subtle" type="button" disabled={deletingId === goal.id} onClick={() => remove(goal.id)} title={t.delete}><Trash2 size={16} /></button></div></header><div className="ai-suggestion-content"><RichTextView text={goal.content} /></div></article>)}</div> : <p className="empty-copy">{t.noCurrentActionGoals}</p>}
    {draft && <ActionGoalEditor locale={locale} goal={draft === "new" ? null : draft} onClose={() => setDraft(null)} onSaved={async () => { setDraft(null); await onChanged(); }} onError={onError} />}
  </section>;
}

function ActionGoalEditor({ locale, goal, onClose, onSaved, onError }: { locale: Locale; goal: GlobalActionGoal | null; onClose: () => void; onSaved: () => Promise<void>; onError: (error: unknown) => void }) {
  useBodyScrollLock(); const t = dictionaries[locale]; const [content, setContent] = useState(goal?.content || ""); const [saving, setSaving] = useState(false);
  useEffect(() => setContent(goal?.content || ""), [goal]);
  async function submit(event: FormEvent) { event.preventDefault(); if (!content.trim()) return; setSaving(true); try { if (goal) await api.updateActionGoal(goal.id, content.trim()); else await api.createActionGoal(content.trim()); await onSaved(); } catch (error) { onError(error); } finally { setSaving(false); } }
  return <div className="modal-shell nested-modal" role="dialog" aria-modal="true"><form className="editor-panel focused-editor-panel" onSubmit={submit}><header className="editor-head"><div><p className="eyebrow">{goal ? t.editCurrentActionGoal : t.newCurrentActionGoal}</p><h2>{t.currentActionGoal}</h2></div><button className="icon-only" type="button" onClick={onClose} title={t.close}><X /></button></header><div className="editor-body"><RichTextEditor locale={locale} value={content} maxLength={20000} onChange={setContent} /></div><footer className="editor-footer"><button className="ghost-button" type="button" onClick={onClose}>{t.cancel}</button><button className="primary-button" type="submit" disabled={saving || !content.trim()}>{saving ? t.saving : t.save}</button></footer></form></div>;
}
