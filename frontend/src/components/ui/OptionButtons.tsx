export interface OptionButtonItem<T extends string> {
  value: T;
  label: string;
  className?: string;
}

export function OptionButtons<T extends string>({
  label,
  value,
  options,
  onChange
}: {
  label: string;
  value: T;
  options: { value: T; label: string; className?: string }[];
  onChange: (value: T) => void;
}) {
  return (
    <div className="option-field">
      <span>{label}</span>
      <div className="option-buttons" role="group" aria-label={label}>
        {options.map((option) => (
          <button
            key={option.value}
            type="button"
            className={[option.className, option.value === value ? "active" : ""].filter(Boolean).join(" ")}
            onClick={() => onChange(option.value)}
          >
            {option.label}
          </button>
        ))}
      </div>
    </div>
  );
}
