import type { FormEvent } from "react";
import { useEffect, useState } from "react";
import { Copy, Download, Edit3, HelpCircle, Plus, Sparkles, Trash2, X } from "lucide-react";
import { api } from "../../api";
import { dictionaries } from "../../i18n";
import type { GlobalAiSuggestion, Locale } from "../../types";
import { formatDate } from "../../utils";
import { useBodyScrollLock } from "../../hooks/useBodyScrollLock";
import { RichTextEditor, RichTextView } from "../../components/ui/RichText";

export function AiSuggestionsPanel({
  suggestions,
  locale,
  onChanged,
  onAiUsed,
  onExportAllProjects,
  exportingAllProjects,
  onError
}: {
  suggestions: GlobalAiSuggestion[];
  locale: Locale;
  onChanged: () => Promise<void>;
  onAiUsed: () => void;
  onExportAllProjects: () => Promise<void>;
  exportingAllProjects: boolean;
  onError: (error: unknown) => void;
}) {
  const t = dictionaries[locale];
  const [draft, setDraft] = useState<GlobalAiSuggestion | "new" | null>(null);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [guideOpen, setGuideOpen] = useState(false);

  async function remove(suggestionId: number) {
    if (!window.confirm(t.confirmDeleteAiSuggestion)) return;
    setDeletingId(suggestionId);
    try {
      await api.deleteAiSuggestion(suggestionId);
      await onChanged();
    } catch (error) {
      onError(error);
    } finally {
      setDeletingId(null);
    }
  }

  return (
    <section className="panel ai-suggestions-panel" id="ai-suggestions">
      <header className="section-head ai-suggestions-panel-head">
        <h2><Sparkles aria-hidden="true" />{t.aiSuggestionsOverview}</h2>
        <div className="ai-suggestions-head-actions">
          {suggestions.length > 0 && <span>{suggestions.length}</span>}
          <button className="ghost-button compact-button" type="button" onClick={() => setGuideOpen(true)}><HelpCircle size={16} />{t.learnAi}</button>
          <button className="primary-button" type="button" onClick={() => setDraft("new")}><Plus size={16} />{t.newAiSuggestion}</button>
        </div>
      </header>

      {suggestions.length ? (
        <div className="ai-suggestions-grid">
          {suggestions.map((suggestion) => (
            <article className="ai-suggestion-card" key={suggestion.id}>
              <header className="ai-suggestion-card-head">
                <time dateTime={suggestion.updatedAt || suggestion.createdAt}>{formatDate(suggestion.updatedAt || suggestion.createdAt)}</time>
                <div className="inline-actions">
                  <button className="icon-only" type="button" onClick={() => setDraft(suggestion)} title={t.edit}><Edit3 size={16} /></button>
                  <button className="icon-only danger-subtle" type="button" disabled={deletingId === suggestion.id} onClick={() => remove(suggestion.id)} title={t.delete}><Trash2 size={16} /></button>
                </div>
              </header>
              <div className="ai-suggestion-content"><RichTextView text={suggestion.content} /></div>
            </article>
          ))}
        </div>
      ) : <p className="empty-copy">{t.noAiSuggestions}</p>}

      {draft && (
        <AiSuggestionEditor
          locale={locale}
          suggestion={draft === "new" ? null : draft}
          onClose={() => setDraft(null)}
          onSaved={async () => {
            setDraft(null);
            await onChanged();
            onAiUsed();
          }}
          onError={onError}
        />
      )}
      {guideOpen && <AiUsageGuide locale={locale} onClose={() => setGuideOpen(false)} onExport={onExportAllProjects} exporting={exportingAllProjects} onUsed={onAiUsed} />}
    </section>
  );
}

function AiUsageGuide({ locale, onClose, onExport, exporting, onUsed }: { locale: Locale; onClose: () => void; onExport: () => Promise<void>; exporting: boolean; onUsed: () => void }) {
  useBodyScrollLock();
  const t = dictionaries[locale];
  const prompts = locale === "zh" ? [
    ["本周复盘与优先级建议", "请根据以下工作上下文，做一次本周复盘：识别最重要的 3 件事、可延后的事项、主要风险，并给出下周可执行的优先级顺序。"],
    ["停滞项目与风险诊断", "请分析以下项目上下文，找出停滞或高风险项目，说明可能原因，并为每个项目提出一个最小可执行的下一步。"],
    ["项目阶段与下一步规划", "请根据以下项目阶段和笔记，评估当前阶段是否清晰，提出缺失的里程碑、依赖和下一步行动。"],
    ["闪念/知识整理为行动方案", "请将以下闪念与项目知识整理为主题、待验证假设和可执行行动，并标出应归入哪个项目。"]
  ] : locale === "ja" ? [
    ["今週の振り返りと優先順位", "以下の作業コンテキストを基に今週を振り返り、最重要の3件、後回しにできること、主なリスク、来週の実行順を提案してください。"],
    ["停滞プロジェクトとリスク診断", "以下のプロジェクト情報を分析し、停滞または高リスクの案件と理由、各案件の最小の次の一歩を提案してください。"],
    ["フェーズと次の一歩の計画", "以下のフェーズとノートを基に、現在の計画の不足しているマイルストーン、依存関係、次の行動を提案してください。"],
    ["メモと知識を行動に整理", "以下の閃きとプロジェクト知識をテーマ、検証すべき仮説、実行可能な行動に整理し、どのプロジェクトに属するか示してください。"]
  ] : [
    ["Weekly review and priorities", "Using the work context below, review this week: identify the three most important items, what can wait, the key risks, and an executable priority order for next week."],
    ["Stalled projects and risk diagnosis", "Analyze the project context below. Identify stalled or high-risk projects, explain likely causes, and propose one smallest executable next step for each."],
    ["Project phases and next steps", "Using the project phases and notes below, assess whether the current plan is clear and propose missing milestones, dependencies, and next actions."],
    ["Turn notes and knowledge into action", "Organize the flash notes and project knowledge below into themes, hypotheses to validate, and executable actions; identify which project each belongs to."]
  ];
  async function copy(prompt: string) { try { await navigator.clipboard.writeText(prompt); onUsed(); } catch { /* clipboard permission is browser-controlled */ } }
  return <div className="modal-shell nested-modal ai-usage-modal" role="dialog" aria-modal="true"><section className="editor-panel focused-editor-panel ai-usage-guide"><header className="editor-head"><div><p className="eyebrow">{t.learnAi}</p><h2>{t.aiGuideTitle}</h2></div><button className="icon-only" type="button" onClick={onClose} title={t.close}><X /></button></header><div className="editor-body"><section className="ai-guide-intro"><p>{t.aiGuideSuggestions}</p><p>{t.aiGuideGoal}</p><p>{t.aiGuideExport}</p><p className="privacy-copy">{t.aiPrivacy}</p></section><div className="prompt-list">{prompts.map(([title, prompt]) => <article key={title}><strong>{title}</strong><p>{prompt}</p><button className="ghost-button compact-button" type="button" onClick={() => void copy(prompt)}><Copy size={15} />{t.copyPrompt}</button></article>)}</div></div><footer className="editor-footer"><button className="ghost-button" type="button" onClick={onClose}>{t.close}</button><button className="primary-button" type="button" disabled={exporting} onClick={() => void onExport()}><Download size={16} />{exporting ? t.exportingAllAiJson : t.exportWorkspace}</button></footer></section></div>;
}

function AiSuggestionEditor({
  locale,
  suggestion,
  onClose,
  onSaved,
  onError
}: {
  locale: Locale;
  suggestion: GlobalAiSuggestion | null;
  onClose: () => void;
  onSaved: () => Promise<void>;
  onError: (error: unknown) => void;
}) {
  useBodyScrollLock();
  const t = dictionaries[locale];
  const [content, setContent] = useState(suggestion?.content || "");
  const [saving, setSaving] = useState(false);

  useEffect(() => setContent(suggestion?.content || ""), [suggestion]);

  async function submit(event: FormEvent) {
    event.preventDefault();
    const nextContent = content.trim();
    if (!nextContent) return;
    setSaving(true);
    try {
      if (suggestion) await api.updateAiSuggestion(suggestion.id, nextContent);
      else await api.createAiSuggestion(nextContent);
      await onSaved();
    } catch (error) {
      onError(error);
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="modal-shell nested-modal" role="dialog" aria-modal="true">
      <form className="editor-panel focused-editor-panel" onSubmit={submit}>
        <header className="editor-head">
          <div><p className="eyebrow">{suggestion ? t.editAiSuggestion : t.newAiSuggestion}</p><h2>{t.aiSuggestionsOverview}</h2></div>
          <button className="icon-only" type="button" onClick={onClose} title={t.close}><X /></button>
        </header>
        <div className="editor-body"><RichTextEditor locale={locale} value={content} maxLength={20000} onChange={setContent} /></div>
        <footer className="editor-footer">
          <button className="ghost-button" type="button" onClick={onClose}>{t.cancel}</button>
          <button className="primary-button" type="submit" disabled={saving || !content.trim()}>{saving ? t.saving : t.save}</button>
        </footer>
      </form>
    </div>
  );
}
