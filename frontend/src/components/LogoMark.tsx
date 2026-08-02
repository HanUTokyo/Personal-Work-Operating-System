import logoSrc from "../assets/pf-tasks-logo.png";

export function LogoMark() {
  return (
    <div className="brand-mark" aria-label="Personal Work Operating System">
      <img src={logoSrc} alt="" aria-hidden="true" />
    </div>
  );
}
