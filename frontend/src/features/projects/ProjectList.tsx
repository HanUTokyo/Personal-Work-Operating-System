import { AlertTriangle, Archive, ArchiveRestore, ArrowDownNarrowWide, ArrowUpNarrowWide, Download, Edit3, Pin, PinOff, Plus, Search, Trash2 } from "lucide-react";
import { useState } from "react";
import { dictionaries, priorityLabel } from "../../i18n";
import type { Locale, Task } from "../../types";
import { canEdit, canManageShares, clampText, ensurePhases, formatDate, formatProgress, isStuck } from "../../utils";
import { ProgressBar } from "../../components/ui/Progress";
import type { SortOrder, TaskFilter } from "./types";
import { filterLabel } from "./projectHelpers";

export function ProjectList({
  locale,
  tasks,
  selectedTaskId,
  keyword,
  filter,
  sortBy,
  sortOrder,
  onKeywordChange,
  onFilterChange,
  onSortChange,
  onSortOrderToggle,
  onSelect,
  onOpenDetail,
  exportingAllProjects,
  onExportAll,
  onCreate,
  onEdit,
  onDelete,
  onArchive,
  onPin
}: {
  locale: Locale;
  tasks: Task[];
  selectedTaskId: number | null;
  keyword: string;
  filter: TaskFilter;
  sortBy: string;
  sortOrder: SortOrder;
  onKeywordChange: (value: string) => void;
  onFilterChange: (value: TaskFilter) => void;
  onSortChange: (value: string) => void;
  onSortOrderToggle: () => void;
  onSelect: (id: number) => void;
  onOpenDetail: (task: Task) => void;
  exportingAllProjects: boolean;
  onExportAll: () => void;
  onCreate: () => void;
  onEdit: (task: Task) => void;
  onDelete: (task: Task) => void;
  onArchive: (task: Task) => void;
  onPin: (task: Task) => Promise<void>;
}) {
  const t = dictionaries[locale];
  const filters: TaskFilter[] = ["all", "active", "completed", "stuck", "recent", "archived"];
  const [pinningId, setPinningId] = useState<number | null>(null);

  async function togglePin(task: Task) {
    setPinningId(task.id);
    try {
      await onPin(task);
    } finally {
      setPinningId(null);
    }
  }

  return (
    <section className="panel portfolio-panel">
      <div className="section-head">
        <h1>{t.portfolio}</h1>
        <div className="section-head-actions">
          <button className="secondary-button" type="button" onClick={onExportAll} disabled={exportingAllProjects}>
            <Download size={18} />{exportingAllProjects ? t.exportingAllAiJson : t.exportAllAiJson}
          </button>
          <button className="primary-button" type="button" onClick={onCreate}><Plus size={18} />{t.newProject}</button>
        </div>
      </div>
      <div className="portfolio-toolbar">
        <label className="search-box">
          <Search size={18} />
          <input value={keyword} placeholder={t.searchPlaceholder} onChange={(event) => onKeywordChange(event.target.value)} />
        </label>
        <div className="portfolio-sort-controls">
          <select value={sortBy} onChange={(event) => onSortChange(event.target.value)}>
            <option value="updatedAt">{t.updated}</option>
            <option value="progress">{t.progress}</option>
            <option value="priority">{t.priority}</option>
          </select>
          <button className="secondary-button sort-order-button" type="button" onClick={onSortOrderToggle} title={sortOrder === "asc" ? t.sortAscending : t.sortDescending}>
            {sortOrder === "asc" ? <ArrowUpNarrowWide size={17} /> : <ArrowDownNarrowWide size={17} />}
            <span>{sortOrder === "asc" ? t.sortAscending : t.sortDescending}</span>
          </button>
        </div>
      </div>
      <div className="segmented">
        {filters.map((item) => (
          <button key={item} className={`filter-button filter-${item} ${filter === item ? "active" : ""}`} type="button" onClick={() => onFilterChange(item)}>
            {filterLabel(item, locale)}
          </button>
        ))}
      </div>
      <div className="project-list">
        {tasks.length ? tasks.map((task) => (
          <article key={task.id} className={`project-card ${selectedTaskId === task.id ? "selected" : ""}`}>
            <div className="project-card-main">
              <div className="project-title-row">
                <button className="project-title-line" type="button" onClick={() => onSelect(task.id)}>
                  {isStuck(task) && <AlertTriangle size={16} className="risk-icon" />}
                  <h3>{task.taskTitle || "Untitled project"}</h3>
                  {task.sharedWithCurrentUser && <span className="tag">{task.ownerUsername}</span>}
                </button>
                <div className="card-actions">
                  <button className={`icon-only ${task.pinned ? "active" : ""}`} type="button" onClick={() => void togglePin(task)} disabled={pinningId === task.id} title={task.pinned ? t.unpinProject : t.pinProject}>
                    {task.pinned ? <PinOff size={17} /> : <Pin size={17} />}
                  </button>
                  <button className="icon-only" type="button" onClick={() => onEdit(task)} disabled={!canEdit(task)} title={t.edit}><Edit3 size={17} /></button>
                  <button className="icon-only" type="button" onClick={() => onArchive(task)} disabled={!canManageShares(task)} title={task.archived ? t.restore : t.archive}>
                    {task.archived ? <ArchiveRestore size={17} /> : <Archive size={17} />}
                  </button>
                  <button className="icon-only danger-subtle" type="button" onClick={() => onDelete(task)} disabled={!canManageShares(task)} title={t.delete}><Trash2 size={17} /></button>
                </div>
              </div>

              <button className="project-card-body" type="button" onClick={() => onSelect(task.id)}>
                <p>{clampText(task.taskDescription || "", 120) || dictionaries[locale].emptyKnowledge}</p>
                <div className="project-meta-row">
                  <span>{dictionaries[locale].phaseCount}: {ensurePhases(task).length}</span>
                  <span>{dictionaries[locale].noteCount}: {task.notes?.length || 0}</span>
                  <span>{dictionaries[locale].created}: {formatDate(task.createdAt)}</span>
                  <span>{dictionaries[locale].updated}: {formatDate(task.updatedAt)}</span>
                </div>
                <div className="project-progress-row">
                  <span className={`priority priority-${(task.priority || "MEDIUM").toLowerCase()}`}>{priorityLabel(task.priority, locale)}</span>
                  <ProgressBar value={task.overallProgress} label={formatProgress(task.overallProgress)} />
                </div>
              </button>
            </div>
          </article>
        )) : <p className="empty-copy">{t.noProjects}</p>}
      </div>
    </section>
  );
}
