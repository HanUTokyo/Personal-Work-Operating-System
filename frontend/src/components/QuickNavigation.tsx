import { useEffect, useId, useRef, useState } from "react";
import { ArrowUpRight, CalendarDays, Compass, FolderKanban, ListTodo, NotepadText, Sparkles, Target, X } from "lucide-react";
import { dictionaries } from "../i18n";
import type { Locale } from "../types";

export function QuickNavigation({ locale, onNavigate }: { locale: Locale; onNavigate: (section: string) => void }) {
  const t = dictionaries[locale];
  const [open, setOpen] = useState(false);
  const panelId = useId();
  const root = useRef<HTMLDivElement>(null);
  const trigger = useRef<HTMLButtonElement>(null);
  const firstLink = useRef<HTMLButtonElement>(null);
  const links = [
    { id: "portfolio", label: t.portfolio, icon: FolderKanban },
    { id: "weekly-tasks", label: t.weeklyTasks, icon: CalendarDays },
    { id: "long-term-tasks", label: t.longTermTasks, icon: ListTodo },
    { id: "ai-suggestions", label: t.aiSuggestionsOverview, icon: Sparkles },
    { id: "current-action-goals", label: t.currentActionGoal, icon: Target },
    { id: "flash-notes", label: t.flashNotes, icon: NotepadText }
  ];

  useEffect(() => {
    if (!open) return;
    firstLink.current?.focus({ preventScroll: true });
    const onPointerDown = (event: PointerEvent) => {
      if (event.target instanceof Node && !root.current?.contains(event.target)) setOpen(false);
    };
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key !== "Escape") return;
      event.preventDefault();
      setOpen(false);
      trigger.current?.focus({ preventScroll: true });
    };
    document.addEventListener("pointerdown", onPointerDown);
    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.removeEventListener("pointerdown", onPointerDown);
      document.removeEventListener("keydown", onKeyDown);
    };
  }, [open]);

  function close() {
    setOpen(false);
    trigger.current?.focus({ preventScroll: true });
  }

  return (
    <div className="quick-navigation" ref={root} onBlur={(event) => {
      if (!event.currentTarget.contains(event.relatedTarget)) setOpen(false);
    }}>
      {open && (
        <nav id={panelId} className="quick-navigation-panel" aria-label={t.quickLinks}>
          <div className="quick-navigation-heading">
            <span>{t.quickLinks}</span>
            <button type="button" className="quick-navigation-close" aria-label={t.close} onClick={close}><X size={16} aria-hidden="true" /></button>
          </div>
          <div className="quick-navigation-links">
            {links.map(({ id, label, icon: Icon }, index) => (
              <button key={id} type="button" ref={index === 0 ? firstLink : undefined} onClick={() => { close(); onNavigate(id); }}>
                <span className="quick-navigation-icon"><Icon size={18} aria-hidden="true" /></span>
                <span className="quick-navigation-label">{label}</span>
                <ArrowUpRight className="quick-navigation-arrow" size={15} aria-hidden="true" />
              </button>
            ))}
          </div>
        </nav>
      )}
      <button ref={trigger} type="button" className="quick-navigation-trigger" aria-expanded={open} aria-controls={open ? panelId : undefined} onClick={() => setOpen((current) => !current)}>
        {open ? <X size={19} aria-hidden="true" /> : <Compass size={19} aria-hidden="true" />}
        <span>{t.quickLinks}</span>
      </button>
    </div>
  );
}
