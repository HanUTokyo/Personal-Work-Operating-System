import { BookOpen, Trash2 } from "lucide-react";
import { dictionaries } from "../../i18n";
import type { Locale, OnboardingResponse } from "../../types";

export function DemoWorkspacePanel({ locale, onboarding, busy, onLoad, onClear }: { locale: Locale; onboarding: OnboardingResponse | null; busy: boolean; onLoad: () => void; onClear: () => void }) {
  const t = dictionaries[locale];
  if (!onboarding || onboarding.status === "ESTABLISHED") return null;
  return <section className="panel demo-workspace-panel">
    <div><h2><BookOpen size={19} />{t.demoWorkspace}</h2><p>{onboarding.hasDemoData ? t.demoWorkspaceLoaded : t.demoWorkspaceEmpty}</p></div>
    {onboarding.hasDemoData ? <button className="ghost-button danger-subtle" type="button" disabled={busy} onClick={onClear}><Trash2 size={16} />{t.clearDemoWorkspace}</button> : <button className="secondary-button" type="button" disabled={busy} onClick={onLoad}>{t.loadDemoWorkspace}</button>}
  </section>;
}
