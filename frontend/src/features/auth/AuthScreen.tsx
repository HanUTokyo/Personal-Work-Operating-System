import type { FormEvent } from "react";
import { useState } from "react";
import { Eye, EyeOff } from "lucide-react";
import { dictionaries } from "../../i18n";
import { LogoMark } from "../../components/LogoMark";

export function AuthScreen({
  t,
  mode,
  form,
  busy,
  onChange,
  onModeChange,
  onSubmit
}: {
  t: typeof dictionaries.zh;
  mode: "login" | "register";
  form: { username: string; password: string; confirmPassword: string; displayName: string };
  busy: boolean;
  onChange: (value: { username: string; password: string; confirmPassword: string; displayName: string }) => void;
  onModeChange: (mode: "login" | "register") => void;
  onSubmit: (event: FormEvent) => void;
}) {
  const [passwordVisible, setPasswordVisible] = useState(false);

  return (
    <main className="auth-screen">
      <section className="auth-panel">
        <div className="brand-lockup auth-brand-lockup">
          <LogoMark />
          <div>
            <h1>{mode === "login" ? t.loginTitle : t.registerTitle}</h1>
            <p>{t.loginHint}</p>
          </div>
        </div>
        <form className="stack-form" onSubmit={onSubmit}>
          <label>
            {t.username}
            <input value={form.username} maxLength={60} required onChange={(event) => onChange({ ...form, username: event.target.value })} />
          </label>
          {mode === "register" && (
            <label>
              {t.displayName}
              <input value={form.displayName} maxLength={100} onChange={(event) => onChange({ ...form, displayName: event.target.value })} />
            </label>
          )}
          <label>
            {t.password}
            <div className="password-field">
              <input type={passwordVisible ? "text" : "password"} value={form.password} maxLength={200} required onChange={(event) => onChange({ ...form, password: event.target.value })} />
              <button
                className="password-toggle"
                type="button"
                onClick={() => setPasswordVisible((current) => !current)}
                title={passwordVisible ? t.hidePassword : t.showPassword}
                aria-label={passwordVisible ? t.hidePassword : t.showPassword}
              >
                {passwordVisible ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>
          </label>
          {mode === "register" && (
            <label>
              {t.confirmPassword}
              <div className="password-field">
                <input type={passwordVisible ? "text" : "password"} value={form.confirmPassword} maxLength={200} required onChange={(event) => onChange({ ...form, confirmPassword: event.target.value })} />
                <button
                  className="password-toggle"
                  type="button"
                  onClick={() => setPasswordVisible((current) => !current)}
                  title={passwordVisible ? t.hidePassword : t.showPassword}
                  aria-label={passwordVisible ? t.hidePassword : t.showPassword}
                >
                  {passwordVisible ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </label>
          )}
          <div className="form-footer">
            <button className="primary-button" disabled={busy} type="submit">{mode === "login" ? t.login : t.register}</button>
            <button className="ghost-button" type="button" onClick={() => onModeChange(mode === "login" ? "register" : "login")}>
              {mode === "login" ? t.register : t.switchToLogin}
            </button>
          </div>
        </form>
      </section>
    </main>
  );
}
