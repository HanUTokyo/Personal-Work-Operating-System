export function ProgressBar({ value, label, large }: { value: number; label: string; large?: boolean }) {
  return (
    <div className={`progress-bar ${large ? "large" : ""}`}>
      <div style={{ width: `${Math.max(2, Math.min(100, Number(value) || 0))}%` }} />
      <span>{label}</span>
    </div>
  );
}
