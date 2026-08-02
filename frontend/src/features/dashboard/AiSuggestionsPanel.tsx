import type { FormEvent } from "react";
import { useEffect, useState } from "react";
import { Edit3, Plus, Sparkles, Trash2, X } from "lucide-react";
import { api } from "../../api";
import { dictionaries } from "../../i18n";
import type { GlobalAiSuggestion, Locale } from "../../types";
import { formatDate } from "../../utils";
import { useBodyScrollLock } from "../../hooks/useBodyScrollLock";
import { RichTextEditor, RichTextView } from "../../components/ui/RichText";

export function AiSuggestionsPanel({
  suggestions,
  locale,
  onChanged,
  onError
}: {
  suggestions: GlobalAiSuggestion[];
  locale: Locale;
  onChanged: () => Promise<void>;
  onError: (error: unknown) => void;
}) {
  const t = dictionaries[locale];
  const [draft, setDraft] = useState<GlobalAiSuggestion | "new" | null>(null);
  const [deletingId, setDeletingId] = useState<number | null>(null);

  async function remove(suggestionId: number) {
    if (!window.confirm(t.confirmDeleteAiSuggestion)) return;
    setDeletingId(suggestionId);
    try {
      await api.deleteAiSuggestion(suggestionId);
      await onChanged();
    } catch (error) {
      onError(error);
    } finally {
      setDeletingId(null);
    }
  }

  return (
    <section className="panel ai-suggestions-panel">
      <header className="section-head ai-suggestions-panel-head">
        <h2><Sparkles aria-hidden="true" />{t.aiSuggestionsOverview}</h2>
        <div className="ai-suggestions-head-actions">
          {suggestions.length > 0 && <span>{suggestions.length}</span>}
          <button className="primary-button" type="button" onClick={() => setDraft("new")}><Plus size={16} />{t.newAiSuggestion}</button>
        </div>
      </header>

      {suggestions.length ? (
        <div className="ai-suggestions-grid">
          {suggestions.map((suggestion) => (
            <article className="ai-suggestion-card" key={suggestion.id}>
              <header className="ai-suggestion-card-head">
                <time dateTime={suggestion.updatedAt || suggestion.createdAt}>{formatDate(suggestion.updatedAt || suggestion.createdAt)}</time>
                <div className="inline-actions">
                  <button className="icon-only" type="button" onClick={() => setDraft(suggestion)} title={t.edit}><Edit3 size={16} /></button>
                  <button className="icon-only danger-subtle" type="button" disabled={deletingId === suggestion.id} onClick={() => remove(suggestion.id)} title={t.delete}><Trash2 size={16} /></button>
                </div>
              </header>
              <div className="ai-suggestion-content"><RichTextView text={suggestion.content} /></div>
            </article>
          ))}
        </div>
      ) : <p className="empty-copy">{t.noAiSuggestions}</p>}

      {draft && (
        <AiSuggestionEditor
          locale={locale}
          suggestion={draft === "new" ? null : draft}
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

function AiSuggestionEditor({
  locale,
  suggestion,
  onClose,
  onSaved,
  onError
}: {
  locale: Locale;
  suggestion: GlobalAiSuggestion | null;
  onClose: () => void;
  onSaved: () => Promise<void>;
  onError: (error: unknown) => void;
}) {
  useBodyScrollLock();
  const t = dictionaries[locale];
  const [content, setContent] = useState(suggestion?.content || "");
  const [saving, setSaving] = useState(false);

  useEffect(() => setContent(suggestion?.content || ""), [suggestion]);

  async function submit(event: FormEvent) {
    event.preventDefault();
    const nextContent = content.trim();
    if (!nextContent) return;
    setSaving(true);
    try {
      if (suggestion) await api.updateAiSuggestion(suggestion.id, nextContent);
      else await api.createAiSuggestion(nextContent);
      await onSaved();
    } catch (error) {
      onError(error);
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="modal-shell nested-modal" role="dialog" aria-modal="true">
      <form className="editor-panel focused-editor-panel" onSubmit={submit}>
        <header className="editor-head">
          <div><p className="eyebrow">{suggestion ? t.editAiSuggestion : t.newAiSuggestion}</p><h2>{t.aiSuggestionsOverview}</h2></div>
          <button className="icon-only" type="button" onClick={onClose} title={t.close}><X /></button>
        </header>
        <div className="editor-body"><RichTextEditor locale={locale} value={content} maxLength={20000} onChange={setContent} /></div>
        <footer className="editor-footer">
          <button className="ghost-button" type="button" onClick={onClose}>{t.cancel}</button>
          <button className="primary-button" type="submit" disabled={saving || !content.trim()}>{saving ? t.saving : t.save}</button>
        </footer>
      </form>
    </div>
  );
}
