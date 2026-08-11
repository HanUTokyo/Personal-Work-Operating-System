import { CheckCircle2, ChevronDown, ChevronUp, Rocket } from "lucide-react";
import { useState } from "react";
import { dictionaries } from "../../i18n";
import type { Locale, OnboardingResponse } from "../../types";

export function QuickStartPanel({ locale, onboarding, onOpen, onCreateProject, onOpenFocus, onOpenKnowledge, onOpenAi }: {
  locale: Locale;
  onboarding: OnboardingResponse | null;
  onOpen: () => void;
  onCreateProject: () => void;
  onOpenFocus: () => void;
  onOpenKnowledge: () => void;
  onOpenAi: () => void;
}) {
  const t = dictionaries[locale];
  const [open, setOpen] = useState(Boolean(onboarding && onboarding.status === "PENDING" && !onboarding.guideClosed));
  if (!onboarding || onboarding.status === "ESTABLISHED" || onboarding.status === "COMPLETED") return null;
  const steps = [
    [onboarding.projectDone, t.guideProject, t.guideProjectText, t.newProject, onCreateProject],
    [onboarding.focusDone, t.guideFocus, t.guideFocusText, t.guideFocusAction, onOpenFocus],
    [onboarding.knowledgeDone, t.guideKnowledge, t.guideKnowledgeText, t.guideKnowledgeAction, onOpenKnowledge],
    [onboarding.aiDone, t.guideAi, t.guideAiText, t.guideAiAction, onOpenAi]
  ] as const;
  const done = steps.filter(([complete]) => complete).length;
  function toggle() {
    const next = !open;
    setOpen(next);
    if (next) onOpen();
  }
  return <section className={`panel quick-start-panel ${open ? "is-open" : ""}`}>
    <header className="section-head quick-start-head">
      <div><h2><Rocket aria-hidden="true" />{t.quickStartChecklist}</h2><p>{t.guideProgress.replace("{done}", String(done))}</p></div>
      <div className="inline-actions"><button className="icon-only" type="button" onClick={toggle} title={open ? t.collapse : t.expand}>{open ? <ChevronUp /> : <ChevronDown />}</button></div>
    </header>
    {open && <ol className="quick-start-steps">
      {steps.map(([complete, title, copy, action, onClick], index) => <li key={title} className={complete ? "done" : ""}>
        <CheckCircle2 aria-hidden="true" />
        <div><strong>{index + 1}. {title}</strong><p>{copy}</p></div>
        <button className="ghost-button compact-button" type="button" onClick={onClick}>{complete ? t.review : action}</button>
      </li>)}
    </ol>}
  </section>;
}
