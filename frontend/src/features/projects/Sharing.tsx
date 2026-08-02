import type { FormEvent } from "react";
import { useEffect, useState } from "react";
import { Share2, X } from "lucide-react";
import { api } from "../../api";
import { dictionaries, permissionLabel } from "../../i18n";
import type { Locale, SharePermission, Task, TaskShare } from "../../types";
import { canManageShares } from "../../utils";
import { useBodyScrollLock } from "../../hooks/useBodyScrollLock";

export function SharedControls({ locale, task, onError, compact }: { locale: Locale; task: Task; onError: (error: unknown) => void; compact?: boolean }) {
  const t = dictionaries[locale];
  const [shares, setShares] = useState<TaskShare[]>([]);
  const [username, setUsername] = useState("");
  const [permission, setPermission] = useState<SharePermission>("VIEW");

  useEffect(() => {
    if (!canManageShares(task)) return;
    api.shares(task.id).then(setShares).catch(onError);
  }, [task.id]);

  if (!canManageShares(task)) {
    return compact ? <p className="empty-copy">{t.shareUnavailable}</p> : null;
  }

  async function addShare(event: FormEvent) {
    event.preventDefault();
    if (!username.trim()) return;
    try {
      await api.addShare(task.id, username.trim(), permission);
      setUsername("");
      setShares(await api.shares(task.id));
    } catch (error) {
      onError(error);
    }
  }

  async function removeShare(shareId: number) {
    try {
      await api.deleteShare(task.id, shareId);
      setShares((current) => current.filter((share) => share.id !== shareId));
    } catch (error) {
      onError(error);
    }
  }

  return (
    <section className={compact ? "sharing-box compact" : "detail-section sharing-box"}>
      {!compact && <h3>{t.sharing}</h3>}
      <form className="share-form" onSubmit={addShare}>
        <input value={username} placeholder={t.username} onChange={(event) => setUsername(event.target.value)} />
        <div className="option-buttons share-permission-buttons" role="group" aria-label={t.permission}>
          {(["VIEW", "EDIT"] as SharePermission[]).map((item) => (
            <button key={item} type="button" className={permission === item ? "active" : ""} onClick={() => setPermission(item)}>
              {permissionLabel(item, locale)}
            </button>
          ))}
        </div>
        <button className="secondary-button share-submit-button" type="submit"><Share2 size={16} />{t.addShare}</button>
      </form>
      <div className="share-list">
        {shares.map((share) => (
          <div className="share-row" key={share.id}>
            <span>{share.sharedWith.displayName || share.sharedWith.username}</span>
            <em>{permissionLabel(share.permission, locale)}</em>
            <button type="button" onClick={() => removeShare(share.id)}>{t.remove}</button>
          </div>
        ))}
      </div>
    </section>
  );
}

export function ShareModal({ locale, task, onClose, onError }: { locale: Locale; task: Task; onClose: () => void; onError: (error: unknown) => void }) {
  useBodyScrollLock();
  const t = dictionaries[locale];
  return (
    <div className="modal-shell nested-modal" role="dialog" aria-modal="true">
      <section className="flash-panel share-modal-panel">
        <header className="editor-head">
          <div><p className="eyebrow">{t.shareProject}</p><h2>{task.taskTitle}</h2></div>
          <button className="icon-only" type="button" onClick={onClose} title={t.close}><X /></button>
        </header>
        <div className="editor-body">
          <SharedControls locale={locale} task={task} onError={onError} compact />
        </div>
      </section>
    </div>
  );
}
