import type { FormEvent } from "react";
import { useEffect, useState } from "react";
import { X } from "lucide-react";
import { dictionaries, statusLabel } from "../../i18n";
import type { Locale, Phase, PhaseStatus } from "../../types";
import { useBodyScrollLock } from "../../hooks/useBodyScrollLock";
import { OptionButtons } from "../ui/OptionButtons";
import { RichTextEditor } from "../ui/RichText";

export function PhaseEditModal({
  locale,
  phase,
  phases,
  onClose,
  onSave
}: {
  locale: Locale;
  phase: Phase;
  phases: Phase[];
  onClose: () => void;
  onSave: (phase: Phase) => void | Promise<void>;
}) {
  useBodyScrollLock();
  const t = dictionaries[locale];
  const [draft, setDraft] = useState<Phase>(() => ({ ...phase }));

  useEffect(() => {
    setDraft({ ...phase });
  }, [phase.phaseKey]);

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (!draft.phaseName.trim()) return;
    await onSave({ ...draft, phaseName: draft.phaseName.trim() });
  }

  return (
    <div className="modal-shell nested-modal" role="dialog" aria-modal="true">
      <form className="editor-panel focused-editor-panel" onSubmit={submit}>
        <header className="editor-head">
          <div><p className="eyebrow">{t.editPhase}</p><h2>{draft.phaseName || t.stageName}</h2></div>
          <button className="icon-only" type="button" onClick={onClose} title={t.close}><X /></button>
        </header>
        <div className="editor-body">
          <div className="phase-editor-grid">
            <label className="wide">{t.stageName}<input maxLength={100} value={draft.phaseName} onChange={(event) => setDraft({ ...draft, phaseName: event.target.value })} /></label>
            <OptionButtons
              label={t.stageStatus}
              value={draft.phaseStatus}
              options={(["TODO", "DOING", "DONE"] as PhaseStatus[]).map((value) => ({ value, label: statusLabel(value, locale), className: `status-choice status-${value.toLowerCase()}` }))}
              onChange={(value) => setDraft({ ...draft, phaseStatus: value })}
            />
            <label>{t.parentStage}<select value={draft.parentPhaseKey || ""} onChange={(event) => setDraft({ ...draft, parentPhaseKey: event.target.value || null })}>
              <option value="">{t.rootStage}</option>
              {phases.filter((item) => item.phaseKey !== draft.phaseKey).map((item) => <option key={item.phaseKey} value={item.phaseKey}>{item.phaseName}</option>)}
            </select></label>
            <div className="field wide"><span>{t.stageDescription}</span><RichTextEditor locale={locale} value={draft.phaseDescription || ""} maxLength={2000} onChange={(value) => setDraft({ ...draft, phaseDescription: value })} /></div>
          </div>
        </div>
        <footer className="editor-footer">
          <button className="ghost-button" type="button" onClick={onClose}>{t.cancel}</button>
          <button className="primary-button" type="submit" disabled={!draft.phaseName.trim()}>{t.savePhase}</button>
        </footer>
      </form>
    </div>
  );
}
