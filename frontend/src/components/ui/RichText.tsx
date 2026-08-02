import { Link, RichTextEditor as MantineRichTextEditor, getTaskListExtension } from "@mantine/tiptap";
import Placeholder from "@tiptap/extension-placeholder";
import TaskItem from "@tiptap/extension-task-item";
import TipTapTaskList from "@tiptap/extension-task-list";
import { Markdown } from "@tiptap/markdown";
import { useEditor } from "@tiptap/react";
import StarterKit from "@tiptap/starter-kit";
import { useEffect, useRef } from "react";
import { dictionaries } from "../../i18n";
import type { Locale } from "../../types";

export function RichTextEditor({
  locale,
  value,
  onChange,
  placeholder,
  maxLength
}: {
  locale: Locale;
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  maxLength?: number;
}) {
  const t = dictionaries[locale];
  const acceptedValueRef = useRef(value);
  const onChangeRef = useRef(onChange);
  const maxLengthRef = useRef(maxLength);
  onChangeRef.current = onChange;
  maxLengthRef.current = maxLength;

  const editor = useEditor({
    shouldRerenderOnTransaction: true,
    extensions: [
      StarterKit.configure({ link: false }),
      Link.configure({
        autolink: true,
        defaultProtocol: "https://",
        openOnClick: false
      }),
      Placeholder.configure({ placeholder: placeholder || t.noteContent }),
      getTaskListExtension(TipTapTaskList),
      TaskItem.configure({ nested: true }),
      Markdown.configure({ markedOptions: { gfm: true, breaks: false } })
    ],
    content: value,
    contentType: "markdown",
    onUpdate: ({ editor: updatedEditor }) => {
      const markdown = updatedEditor.getMarkdown();
      if (maxLengthRef.current && markdown.length > maxLengthRef.current) {
        updatedEditor.commands.setContent(acceptedValueRef.current, { contentType: "markdown", emitUpdate: false });
        return;
      }
      acceptedValueRef.current = markdown;
      onChangeRef.current(markdown);
    }
  });

  useEffect(() => {
    if (!editor || value === acceptedValueRef.current) return;
    editor.commands.setContent(value, { contentType: "markdown", emitUpdate: false });
    acceptedValueRef.current = value;
  }, [editor, value]);

  return (
    <MantineRichTextEditor className="rich-editor" editor={editor} withTypographyStyles={false}>
      <MantineRichTextEditor.Toolbar className="rich-editor-toolbar" aria-label={t.richText}>
        <MantineRichTextEditor.ControlsGroup className="rich-editor-controls">
          <MantineRichTextEditor.Bold />
          <MantineRichTextEditor.Italic />
          <MantineRichTextEditor.Underline />
          <MantineRichTextEditor.Strikethrough />
          <MantineRichTextEditor.ClearFormatting />
          <MantineRichTextEditor.Code />
        </MantineRichTextEditor.ControlsGroup>
        <MantineRichTextEditor.ControlsGroup className="rich-editor-controls">
          <MantineRichTextEditor.H1 />
          <MantineRichTextEditor.H2 />
          <MantineRichTextEditor.H3 />
        </MantineRichTextEditor.ControlsGroup>
        <MantineRichTextEditor.ControlsGroup className="rich-editor-controls">
          <MantineRichTextEditor.BulletList />
          <MantineRichTextEditor.OrderedList />
          <MantineRichTextEditor.TaskList />
          <MantineRichTextEditor.Blockquote />
          <MantineRichTextEditor.CodeBlock />
          <MantineRichTextEditor.Hr />
        </MantineRichTextEditor.ControlsGroup>
        <MantineRichTextEditor.ControlsGroup className="rich-editor-controls">
          <MantineRichTextEditor.Link />
          <MantineRichTextEditor.Unlink />
        </MantineRichTextEditor.ControlsGroup>
        <MantineRichTextEditor.ControlsGroup className="rich-editor-controls">
          <MantineRichTextEditor.Undo />
          <MantineRichTextEditor.Redo />
        </MantineRichTextEditor.ControlsGroup>
      </MantineRichTextEditor.Toolbar>
      <MantineRichTextEditor.Content />
    </MantineRichTextEditor>
  );
}

export function RichTextView({ text, empty }: { text?: string; empty?: string }) {
  const content = (text || "").trim();
  if (!content) return empty ? <p className="rich-text empty-rich-text">{empty}</p> : null;
  return <div className="rich-text" dangerouslySetInnerHTML={{ __html: renderRichText(content) }} />;
}

export function renderRichText(text: string) {
  const lines = text.replace(/\r\n?/g, "\n").split("\n");
  const blocks: string[] = [];
  let index = 0;

  while (index < lines.length) {
    const line = lines[index];
    const trimmed = line.trim();

    if (!trimmed) {
      index += 1;
      continue;
    }

    if (trimmed.startsWith("```")) {
      const language = trimmed.slice(3).trim();
      const codeLines: string[] = [];
      index += 1;
      while (index < lines.length && !lines[index].trim().startsWith("```")) {
        codeLines.push(lines[index]);
        index += 1;
      }
      if (index < lines.length) index += 1;
      blocks.push(`<pre><code${language ? ` class="language-${escapeHtml(language)}"` : ""}>${escapeHtml(codeLines.join("\n"))}</code></pre>`);
      continue;
    }

    const heading = /^(#{1,6})\s+(.+)$/.exec(trimmed);
    if (heading) {
      const level = heading[1].length;
      blocks.push(`<h${level}>${formatInline(heading[2])}</h${level}>`);
      index += 1;
      continue;
    }

    if (/^([-*_])\1{2,}$/.test(trimmed)) {
      blocks.push("<hr />");
      index += 1;
      continue;
    }

    if (/^>\s?/.test(trimmed)) {
      const quoteLines: string[] = [];
      while (index < lines.length && /^>\s?/.test(lines[index].trim())) {
        quoteLines.push(lines[index].trim().replace(/^>\s?/, ""));
        index += 1;
      }
      blocks.push(`<blockquote>${renderRichText(quoteLines.join("\n"))}</blockquote>`);
      continue;
    }

    if (/^[-*+]\s+/.test(trimmed)) {
      const items: string[] = [];
      let hasTasks = false;
      while (index < lines.length && /^[-*+]\s+/.test(lines[index].trim())) {
        const itemText = lines[index].trim().replace(/^[-*+]\s+/, "");
        const task = /^\[( |x|X)\]\s+(.+)$/.exec(itemText);
        if (task) {
          hasTasks = true;
          items.push(`<li class="task-list-item"><input type="checkbox" disabled ${task[1].toLowerCase() === "x" ? "checked " : ""}/><span>${formatInline(task[2])}</span></li>`);
        } else {
          items.push(`<li>${formatInline(itemText)}</li>`);
        }
        index += 1;
      }
      blocks.push(`<ul${hasTasks ? ' class="task-list"' : ""}>${items.join("")}</ul>`);
      continue;
    }

    if (/^\d+[.)]\s+/.test(trimmed)) {
      const items: string[] = [];
      while (index < lines.length && /^\d+[.)]\s+/.test(lines[index].trim())) {
        items.push(`<li>${formatInline(lines[index].trim().replace(/^\d+[.)]\s+/, ""))}</li>`);
        index += 1;
      }
      blocks.push(`<ol>${items.join("")}</ol>`);
      continue;
    }

    const paragraph: string[] = [];
    while (index < lines.length && lines[index].trim() && !isMarkdownBlockStart(lines[index])) {
      paragraph.push(lines[index]);
      index += 1;
    }
    blocks.push(`<p>${paragraph.map((item) => formatInline(item)).join("<br />")}</p>`);
  }

  return blocks.join("");
}

function isMarkdownBlockStart(line: string) {
  const trimmed = line.trim();
  return trimmed.startsWith("```") ||
    /^(#{1,6})\s+/.test(trimmed) ||
    /^([-*_])\1{2,}$/.test(trimmed) ||
    /^>\s?/.test(trimmed) ||
    /^[-*+]\s+/.test(trimmed) ||
    /^\d+[.)]\s+/.test(trimmed);
}

function formatInline(text: string) {
  const codeSpans: string[] = [];
  return escapeHtml(text)
    .replace(/`([^`]+)`/g, (_, code: string) => {
      const token = `@@CODE${codeSpans.length}@@`;
      codeSpans.push(`<code>${code}</code>`);
      return token;
    })
    .replace(/\[([^\]]+)\]\(([^)\s]+)\)/g, (_, label: string, href: string) => {
      const safeHref = sanitizeMarkdownHref(href);
      return `<a href="${escapeHtml(safeHref)}" target="_blank" rel="noreferrer">${label}</a>`;
    })
    .replace(/\*\*([^*]+)\*\*/g, "<strong>$1</strong>")
    .replace(/__([^_]+)__/g, "<strong>$1</strong>")
    .replace(/\+\+([^+]+)\+\+/g, "<u>$1</u>")
    .replace(/~~([^~]+)~~/g, "<del>$1</del>")
    .replace(/(^|[^*])\*([^*]+)\*/g, "$1<em>$2</em>")
    .replace(/(^|[^_])_([^_]+)_/g, "$1<em>$2</em>")
    .replace(/@@CODE(\d+)@@/g, (_, codeIndex: string) => codeSpans[Number(codeIndex)] || "");
}

function sanitizeMarkdownHref(href: string) {
  const normalized = href.replace(/&amp;/g, "&").trim();
  if (/^(https?:|mailto:|\/|#)/i.test(normalized)) return normalized;
  return "#";
}

function escapeHtml(text: string) {
  return text
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}
