import { Target } from "lucide-react";
import { RichTextView } from "../../components/ui/RichText";
import { dictionaries } from "../../i18n";
import type { Locale, Task } from "../../types";

export function CurrentActionGoalsPanel({ tasks, locale, onSelect }: { tasks: Task[]; locale: Locale; onSelect: (id: number) => void }) {
  const t = dictionaries[locale];
  const goals = tasks
    .filter((task) => task.currentActionGoal?.trim())
    .sort((a, b) => new Date(b.updatedAt || 0).getTime() - new Date(a.updatedAt || 0).getTime());

  return (
    <section className="panel current-action-goals-panel">
      <header className="section-head current-action-goals-head">
        <h2><Target aria-hidden="true" />{t.currentActionGoal}</h2>
      </header>
      {goals.length ? (
        <div className="current-action-goals-grid">
          {goals.map((task) => (
            <article className="current-action-goal-card" key={task.id}>
              <button type="button" onClick={() => onSelect(task.id)} title={t.openProject}>{task.taskTitle || "Untitled"}</button>
              <RichTextView text={task.currentActionGoal} />
            </article>
          ))}
        </div>
      ) : <p className="empty-copy">{t.noCurrentActionGoals}</p>}
    </section>
  );
}
