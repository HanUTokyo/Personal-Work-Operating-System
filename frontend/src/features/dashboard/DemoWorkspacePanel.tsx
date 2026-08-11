import { BookOpen, CheckCircle2, Trash2, X } from "lucide-react";
import { useState } from "react";
import { dictionaries } from "../../i18n";
import type { Locale, OnboardingResponse } from "../../types";

export function DemoWorkspacePanel({ locale, onboarding, busy, onLoad, onClear, onFinish }: { locale: Locale; onboarding: OnboardingResponse | null; busy: boolean; onLoad: () => void; onClear: () => void; onFinish: (clearDemo: boolean) => void }) {
  const t = dictionaries[locale];
  const [finishOpen, setFinishOpen] = useState(false);
  if (!onboarding || onboarding.status === "ESTABLISHED" || onboarding.status === "COMPLETED") return null;
  return <section className="panel demo-workspace-panel">
    <div><h2><BookOpen size={19} />{t.demoWorkspace}</h2><p>{onboarding.hasDemoData ? t.demoWorkspaceLoaded : t.demoWorkspaceEmpty}</p></div>
    <div className="demo-workspace-actions">{onboarding.hasDemoData ? <button className="ghost-button danger-subtle" type="button" disabled={busy} onClick={onClear}><Trash2 size={16} />{t.clearDemoWorkspace}</button> : <button className="secondary-button" type="button" disabled={busy} onClick={onLoad}>{t.loadDemoWorkspace}</button>}<button className="ghost-button compact-button" type="button" disabled={busy} onClick={() => setFinishOpen(true)}><CheckCircle2 size={16} />{t.finishQuickStart}</button></div>
    {finishOpen && <div className="modal-shell nested-modal" role="dialog" aria-modal="true"><section className="editor-panel focused-editor-panel finish-onboarding-panel"><header className="editor-head"><div><p className="eyebrow">{t.quickStart}</p><h2>{t.finishQuickStart}</h2></div><button className="icon-only" type="button" onClick={() => setFinishOpen(false)} title={t.close}><X /></button></header><div className="editor-body"><p>{onboarding.hasDemoData ? t.finishQuickStartWithDemo : t.finishQuickStartEmpty}</p></div><footer className="editor-footer">{onboarding.hasDemoData && <button className="ghost-button danger-subtle" type="button" disabled={busy} onClick={() => onFinish(true)}>{t.clearAndFinish}</button>}<button className="primary-button" type="button" disabled={busy} onClick={() => onFinish(false)}>{t.keepAndFinish}</button></footer></section></div>}
  </section>;
}
