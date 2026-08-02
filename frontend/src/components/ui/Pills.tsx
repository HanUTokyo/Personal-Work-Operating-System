import { statusLabel } from "../../i18n";
import type { Locale, PhaseStatus } from "../../types";

export function StatusPill({ status, locale }: { status: PhaseStatus; locale: Locale }) {
  return <em className={`status status-${status.toLowerCase()}`}>{statusLabel(status, locale)}</em>;
}

export function StatPill({ label, value }: { label: string; value: string }) {
  return <div className="stat-pill"><span>{label}</span><strong>{value}</strong></div>;
}
