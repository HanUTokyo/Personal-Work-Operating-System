import { useEffect, useState } from "react";
import { X } from "lucide-react";
import { dictionaries, noteTypeLabel } from "../../i18n";
import type { Locale, NoteType } from "../../types";
import { useBodyScrollLock } from "../../hooks/useBodyScrollLock";
import { OptionButtons } from "../ui/OptionButtons";
import { RichTextEditor } from "../ui/RichText";

export function KnowledgeEditModal({
  locale,
  title,
  value,
  noteType,
  showTypePicker,
  onClose,
  onSave
}: {
  locale: Locale;
  title: string;
  value: string;
  noteType?: NoteType;
  showTypePicker?: boolean;
  onClose: () => void;
  onSave: (value: string, noteType?: NoteType) => void | Promise<void>;
}) {
  useBodyScrollLock();
  const t = dictionaries[locale];
  const [draft, setDraft] = useState(value);
  const [selectedType, setSelectedType] = useState<NoteType>(noteType || "RECENT_DECISIONS");

  useEffect(() => {
    setDraft(value);
  }, [value]);

  useEffect(() => {
    setSelectedType(noteType || "RECENT_DECISIONS");
  }, [noteType]);

  return (
    <div className="modal-shell nested-modal" role="dialog" aria-modal="true">
      <section className="editor-panel focused-editor-panel">
        <header className="editor-head">
          <div><p className="eyebrow">{t.editKnowledge}</p><h2>{title}</h2></div>
          <button className="icon-only" type="button" onClick={onClose} title={t.close}><X /></button>
        </header>
        <div className="editor-body">
          {showTypePicker && (
            <div className="knowledge-type-picker">
              <OptionButtons
                label={t.noteType}
                value={selectedType}
                options={(["RECENT_DECISIONS", "RECENT_EXPERIMENTS", "KNOWLEDGE_HIGHLIGHTS", "AI_SUGGESTIONS"] as NoteType[]).map((type) => ({ value: type, label: noteTypeLabel(type, locale) }))}
                onChange={setSelectedType}
              />
            </div>
          )}
          <RichTextEditor locale={locale} maxLength={20000} value={draft} onChange={setDraft} />
        </div>
        <footer className="editor-footer">
          <button className="ghost-button" type="button" onClick={onClose}>{t.cancel}</button>
          <button className="primary-button" type="button" onClick={() => onSave(draft, selectedType)} disabled={!draft.trim()}>{t.save}</button>
        </footer>
      </section>
    </div>
  );
}
