import { ChevronDown, ChevronUp, CornerDownRight, Edit3, Plus, Trash2 } from "lucide-react";
import { dictionaries } from "../../i18n";
import type { Locale, Phase, PhaseNode } from "../../types";
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
  onMove
}: {
  phases: Phase[];
  locale: Locale;
  editable?: boolean;
  onEdit?: (phase: Phase) => void;
  onAddNext?: (phaseKey: string) => void | Promise<void>;
  onAddChild?: (phaseKey: string) => void | Promise<void>;
  onDelete?: (phaseKey: string) => void | Promise<void>;
  onMove?: (phaseKey: string, direction: -1 | 1) => void | Promise<void>;
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
  onMove
}: {
  node: PhaseNode;
  locale: Locale;
  editable?: boolean;
  onEdit?: (phase: Phase) => void;
  onAddNext?: (phaseKey: string) => void | Promise<void>;
  onAddChild?: (phaseKey: string) => void | Promise<void>;
  onDelete?: (phaseKey: string) => void | Promise<void>;
  onMove?: (phaseKey: string, direction: -1 | 1) => void | Promise<void>;
}) {
  const t = dictionaries[locale];
  return (
    <div className="phase-node">
      <div className="phase-card">
        <div className="phase-card-head">
          <div>
            <span>{node.phase.phaseName}</span>
            <StatusPill status={node.phase.phaseStatus} locale={locale} />
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
            />
          ))}
        </div>
      )}
    </div>
  );
}
