import type { FormEvent } from "react";
import { useEffect, useState } from "react";
import { ArrowLeft, Edit3, Plus, Trash2 } from "lucide-react";
import { api } from "../../api";
import { dictionaries } from "../../i18n";
import type { FlashNote, Locale } from "../../types";
import { formatDate } from "../../utils";
import { RichTextEditor, RichTextView } from "../../components/ui/RichText";

export function FlashNotes({ locale, onClose, onError }: { locale: Locale; onClose: () => void; onError: (error: unknown) => void }) {
  const t = dictionaries[locale];
  const [notes, setNotes] = useState<FlashNote[]>([]);
  const [content, setContent] = useState("");
  const [editingId, setEditingId] = useState<number | null>(null);
  const [composerOpen, setComposerOpen] = useState(false);

  async function load() {
    setNotes(await api.flashNotes());
  }

  useEffect(() => {
    load().catch(onError);
  }, []);

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (!content.trim()) return;
    try {
      if (editingId) await api.updateFlashNote(editingId, content.trim());
      else await api.createFlashNote(content.trim());
      setContent("");
      setEditingId(null);
      setComposerOpen(false);
      await load();
    } catch (error) {
      onError(error);
    }
  }

  async function deleteNote(noteId: number) {
    if (!window.confirm(t.confirmDeleteFlash)) return;
    try {
      await api.deleteFlashNote(noteId);
      await load();
    } catch (error) {
      onError(error);
    }
  }

  const editorOpen = composerOpen || editingId !== null;

  return (
    <section className="project-detail-page flash-page">
      {!editorOpen && (
        <div className="project-page-toolbar">
          <button className="ghost-button icon-only transparent-back-button" type="button" onClick={onClose} title={t.back}>
            <ArrowLeft size={18} />
          </button>
        </div>
      )}
      <div className={`panel detail-panel flash-page-panel ${editorOpen ? "editing" : ""}`}>
        <header className="editor-head">
          <div><h2>{t.flashNotes}</h2></div>
          <div className="detail-actions">
            {!editorOpen && <button className="primary-button" type="button" onClick={() => { setContent(""); setEditingId(null); setComposerOpen(true); }}><Plus size={16} />{t.newNote}</button>}
          </div>
        </header>
        {editorOpen ? (
          <form className="note-composer" onSubmit={submit}>
            <RichTextEditor locale={locale} value={content} placeholder={t.noteContent} onChange={setContent} />
            <div className="form-footer compact">
              <button className="ghost-button" type="button" onClick={() => { setEditingId(null); setContent(""); setComposerOpen(false); }}>{t.cancel}</button>
              <button className="primary-button" type="submit" disabled={!content.trim()}>{editingId ? t.save : t.create}</button>
            </div>
          </form>
        ) : (
          <div className="note-list">
            {notes.length ? notes.map((note) => (
              <article className="note-item" key={note.id}>
                <header><span>{formatDate(note.updatedAt || note.createdAt)}</span></header>
                <RichTextView text={note.noteContent} empty={t.emptyKnowledge} />
                <div className="inline-actions">
                  <button className="icon-only" type="button" onClick={() => { setEditingId(note.id); setContent(note.noteContent); setComposerOpen(false); }} title={t.edit}><Edit3 size={17} /></button>
                  <button className="icon-only danger-subtle" type="button" onClick={() => deleteNote(note.id)} title={t.delete}><Trash2 size={17} /></button>
                </div>
              </article>
            )) : <p className="empty-copy">{t.emptyKnowledge}</p>}
          </div>
        )}
      </div>
    </section>
  );
}
