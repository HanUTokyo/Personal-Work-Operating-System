import { Edit3, Trash2 } from "lucide-react";
import { dictionaries } from "../../i18n";
import type { Locale } from "../../types";
import { RichTextView } from "../ui/RichText";

export function KnowledgeBlock({
  title,
  meta,
  text,
  empty,
  locale,
  editable,
  onEdit,
  onDelete
}: {
  title: string;
  meta?: string;
  text?: string;
  empty: string;
  locale?: Locale;
  editable?: boolean;
  onEdit?: () => void;
  onDelete?: () => void;
}) {
  const t = dictionaries[locale || "zh"];
  return (
    <article className="knowledge-block">
      <header className="knowledge-block-head">
        <div>
          {meta && <em>{meta}</em>}
        </div>
        {editable && (
          <div className="knowledge-actions">
            <button className="icon-only knowledge-action-button" type="button" onClick={onEdit} title={t.edit}><Edit3 size={15} /></button>
            <button className="icon-only knowledge-action-button danger-subtle" type="button" onClick={onDelete} title={t.delete}><Trash2 size={15} /></button>
          </div>
        )}
      </header>
      <RichTextView text={text} empty={empty} />
    </article>
  );
}
