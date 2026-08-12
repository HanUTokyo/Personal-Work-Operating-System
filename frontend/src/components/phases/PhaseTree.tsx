import { ChevronDown, ChevronUp, CornerDownRight, Edit3, Plus, Trash2 } from "lucide-react";
import { useState } from "react";
import { dictionaries, statusLabel } from "../../i18n";
import type { Locale, Phase, PhaseNode, PhaseStatus } from "../../types";
import { buildPhaseTree } from "../../utils";
import { RichTextView } from "../ui/RichText";
import { StatusPill } from "../ui/Pills";

export function PhaseTree({
  phases,
  locale,
  editable,
  onEdit,
  onAddNext,
  onAddChild,
  onDelete,
  onMove,
  onStatusChange,
  changingPhaseKey
}: {
  phases: Phase[];
  locale: Locale;
  editable?: boolean;
  onEdit?: (phase: Phase) => void;
  onAddNext?: (phaseKey: string) => void | Promise<void>;
  onAddChild?: (phaseKey: string) => void | Promise<void>;
  onDelete?: (phaseKey: string) => void | Promise<void>;
  onMove?: (phaseKey: string, direction: -1 | 1) => void | Promise<void>;
  onStatusChange?: (phaseKey: string, status: PhaseStatus) => void | Promise<void>;
  changingPhaseKey?: string | null;
}) {
  const roots = buildPhaseTree(phases);
  return (
    <div className="phase-tree">
      {roots.map((node) => (
        <PhaseNodeView
          key={node.phase.phaseKey}
          node={node}
          locale={locale}
          editable={editable}
          onEdit={onEdit}
          onAddNext={onAddNext}
          onAddChild={onAddChild}
          onDelete={onDelete}
          onMove={onMove}
          onStatusChange={onStatusChange}
          changingPhaseKey={changingPhaseKey}
        />
      ))}
    </div>
  );
}

function PhaseNodeView({
  node,
  locale,
  editable,
  onEdit,
  onAddNext,
  onAddChild,
  onDelete,
  onMove,
  onStatusChange,
  changingPhaseKey
}: {
  node: PhaseNode;
  locale: Locale;
  editable?: boolean;
  onEdit?: (phase: Phase) => void;
  onAddNext?: (phaseKey: string) => void | Promise<void>;
  onAddChild?: (phaseKey: string) => void | Promise<void>;
  onDelete?: (phaseKey: string) => void | Promise<void>;
  onMove?: (phaseKey: string, direction: -1 | 1) => void | Promise<void>;
  onStatusChange?: (phaseKey: string, status: PhaseStatus) => void | Promise<void>;
  changingPhaseKey?: string | null;
}) {
  const t = dictionaries[locale];
  const [statusMenuOpen, setStatusMenuOpen] = useState(false);
  const statusOptions = ["TODO", "DOING", "DONE"] as PhaseStatus[];
  return (
    <div className="phase-node">
      <div className="phase-card">
        <div className="phase-card-head">
          <div className="phase-title-status">
            <span>{node.phase.phaseName}</span>
            {editable ? <div className="phase-status-menu"><button className={`status status-${node.phase.phaseStatus.toLowerCase()} phase-status-trigger`} type="button" disabled={changingPhaseKey === node.phase.phaseKey} onClick={() => setStatusMenuOpen((open) => !open)} aria-label={`${node.phase.phaseName} ${t.phaseStatus}`} aria-expanded={statusMenuOpen}>{statusLabel(node.phase.phaseStatus, locale)}<ChevronDown size={13} /></button>{statusMenuOpen && <div className="phase-status-options" role="menu">{statusOptions.map((status) => <button key={status} className={`status status-${status.toLowerCase()}`} type="button" role="menuitem" onClick={() => { setStatusMenuOpen(false); void onStatusChange?.(node.phase.phaseKey, status); }}>{statusLabel(status, locale)}</button>)}</div>}</div> : <StatusPill status={node.phase.phaseStatus} locale={locale} />}
          </div>
          {editable && (
            <div className="phase-card-actions">
              <div className="phase-action-row">
                <button type="button" onClick={() => onMove?.(node.phase.phaseKey, -1)} title={t.updated}><ChevronUp size={15} /></button>
                <button type="button" onClick={() => onMove?.(node.phase.phaseKey, 1)} title={t.updated}><ChevronDown size={15} /></button>
                <button type="button" className="phase-add-button" onClick={() => onAddNext?.(node.phase.phaseKey)} title={t.addNextPhase} aria-label={t.addNextPhase}><Plus size={15} /></button>
                <button type="button" className="phase-add-button" onClick={() => onAddChild?.(node.phase.phaseKey)} title={t.addChildPhase} aria-label={t.addChildPhase}><CornerDownRight size={15} /></button>
              </div>
              <div className="phase-action-row phase-management-row">
                <button type="button" onClick={() => onEdit?.(node.phase)} title={t.editPhase}><Edit3 size={15} /></button>
                <button type="button" className="phase-delete-button" onClick={() => onDelete?.(node.phase.phaseKey)} title={t.deletePhase}><Trash2 size={15} /></button>
              </div>
            </div>
          )}
        </div>
        {node.phase.phaseDescription && <RichTextView text={node.phase.phaseDescription} />}
      </div>
      {node.children.length > 0 && (
        <div className="phase-children">
          {node.children.map((child) => (
            <PhaseNodeView
              key={child.phase.phaseKey}
              node={child}
              locale={locale}
              editable={editable}
              onEdit={onEdit}
              onAddNext={onAddNext}
              onAddChild={onAddChild}
              onDelete={onDelete}
              onMove={onMove}
              onStatusChange={onStatusChange}
              changingPhaseKey={changingPhaseKey}
            />
          ))}
        </div>
      )}
    </div>
  );
}
