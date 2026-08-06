import type { ReactElement } from "react";
import { Activity, AlertTriangle, BarChart3 } from "lucide-react";
import { dictionaries } from "../../i18n";
import type { GlobalActionGoal, GlobalAiSuggestion, Locale, ProjectMetrics, Task } from "../../types";
import type { PersonalTask } from "../../types";
import { formatDate, formatProgress, isRecent, isStuck } from "../../utils";
import { ProgressBar } from "../../components/ui/Progress";
import { byUpdatedAsc, byUpdatedDesc } from "../projects/projectHelpers";
import { AiSuggestionsPanel } from "./AiSuggestionsPanel";
import { CurrentActionGoalsPanel } from "./CurrentActionGoalsPanel";
import { PersonalTaskList } from "./PersonalTaskList";

export function Dashboard({ tasks, metrics, weeklyTasks, longTermTasks, aiSuggestions, actionGoals, locale, onSelect, onPersonalTasksChanged, onAiSuggestionsChanged, onActionGoalsChanged, onError }: {
  tasks: Task[];
  metrics: ProjectMetrics;
  weeklyTasks: PersonalTask[];
  longTermTasks: PersonalTask[];
  aiSuggestions: GlobalAiSuggestion[];
  actionGoals: GlobalActionGoal[];
  locale: Locale;
  onSelect: (id: number) => void;
  onPersonalTasksChanged: () => Promise<void>;
  onAiSuggestionsChanged: () => Promise<void>;
  onActionGoalsChanged: () => Promise<void>;
  onError: (error: unknown) => void;
}) {
  const t = dictionaries[locale];
  const stuck = tasks.filter(isStuck).sort(byUpdatedAsc).slice(0, 5);
  const recent = tasks.filter(isRecent).sort(byUpdatedDesc).slice(0, 5);

  return (
    <section className="dashboard">
      <div className="kpi-grid compact-kpi-grid">
        <Kpi icon={<BarChart3 />} label={t.totalProjects} value={metrics.total} />
        <Kpi icon={<Activity />} label={t.activeProjects} value={metrics.doing} />
        <Kpi icon={<AlertTriangle />} label={t.stuckProjects} value={metrics.stuck} tone={metrics.stuck ? "risk" : "ok"} />
      </div>

      <div className="personal-task-grid">
        <PersonalTaskList locale={locale} type="WEEKLY" tasks={weeklyTasks} onChanged={onPersonalTasksChanged} onError={onError} />
        <PersonalTaskList locale={locale} type="LONG_TERM" tasks={longTermTasks} onChanged={onPersonalTasksChanged} onError={onError} />
      </div>

      <AiSuggestionsPanel suggestions={aiSuggestions} locale={locale} onChanged={onAiSuggestionsChanged} onError={onError} />

      <CurrentActionGoalsPanel goals={actionGoals} locale={locale} onChanged={onActionGoalsChanged} onError={onError} />

      <div className="insight-grid compact-insight-grid">
        <SummaryList title={t.needsAttention} empty={t.noAttention} tasks={stuck} locale={locale} onSelect={onSelect} />
        <SummaryList title={t.recentMomentum} empty={t.noData} tasks={recent} locale={locale} onSelect={onSelect} />
      </div>
    </section>
  );
}

function Kpi({ icon, label, value, tone }: { icon: ReactElement; label: string; value: string | number; tone?: "risk" | "ok" }) {
  return (
    <article className={`kpi-card ${tone || ""}`}>
      <div className="kpi-icon">{icon}</div>
      <p>{label}</p>
      <strong>{value}</strong>
    </article>
  );
}

function SummaryList({ title, empty, tasks, locale, onSelect }: { title: string; empty: string; tasks: Task[]; locale: Locale; onSelect: (id: number) => void }) {
  return (
    <section className="panel summary-panel">
      <h2>{title}</h2>
      {tasks.length ? (
        <div className="summary-list">
          {tasks.map((task) => (
            <button key={task.id} className={`summary-item ${isStuck(task) ? "risk" : ""}`} type="button" onClick={() => onSelect(task.id)} title={dictionaries[locale].openProject}>
              <span>{task.taskTitle || dictionaries[locale].untitledProject}</span>
              <em>{formatProgress(task.overallProgress)} · {formatDate(task.updatedAt)}</em>
              <ProgressBar value={task.overallProgress} label={formatProgress(task.overallProgress)} />
            </button>
          ))}
        </div>
      ) : (
        <p className="empty-copy">{empty}</p>
      )}
    </section>
  );
}
