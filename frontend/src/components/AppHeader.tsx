import { Languages, LogOut, Moon, NotepadText, Sun } from "lucide-react";
import { dictionaries } from "../i18n";
import type { Locale, UserResponse } from "../types";
import { LogoMark } from "./LogoMark";

interface AppHeaderProps {
  locale: Locale;
  theme: "dark" | "light";
  user: UserResponse;
  onFlashOpen: () => void;
  onLocaleToggle: () => void;
  onThemeToggle: () => void;
  onLogout: () => void;
}

export function AppHeader({ locale, theme, user, onFlashOpen, onLocaleToggle, onThemeToggle, onLogout }: AppHeaderProps) {
  const t = dictionaries[locale];
  const confirmLogout = () => {
    if (window.confirm(t.confirmLogout)) onLogout();
  };

  return (
    <header className="app-header">
      <div className="brand-lockup">
        <LogoMark />
        <div>
          <h1>{t.appTitle}</h1>
          <p>{t.appSubtitle}</p>
        </div>
      </div>
      <div className="header-actions">
        <div className="header-tool-actions">
          <button className="icon-button" type="button" onClick={onFlashOpen}>
            <NotepadText size={18} />
          </button>
          <button className="icon-button" type="button" onClick={onLocaleToggle} title={t.switchLanguage}>
            <Languages size={18} />
          </button>
          <button className="icon-button" type="button" onClick={onThemeToggle}>
            {theme === "dark" ? <Sun size={18} /> : <Moon size={18} />}
          </button>
        </div>
        <div className="account-actions">
          <button className="icon-button" title={user.displayName || user.username}>
            {user.displayName || user.username}
          </button>
          <button className="icon-button danger-subtle" type="button" onClick={confirmLogout} title={t.logout}>
            <LogOut size={18} />
          </button>
        </div>
      </div>
    </header>
  );
}
