import React, { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { MantineProvider } from "@mantine/core";
import "@mantine/core/styles.css";
import "@mantine/tiptap/styles.css";
import { App } from "./App";
import "./styles.css";
import type { Locale } from "./types";

function isLocale(value: string | null | undefined): value is Locale {
  return value === "zh" || value === "en" || value === "ja";
}

function systemLocale(): Locale {
  const language = navigator.language.toLowerCase();
  if (language.startsWith("ja")) return "ja";
  if (language.startsWith("zh")) return "zh";
  return "en";
}

const savedLocale = localStorage.getItem("task-app-locale");
const locale = isLocale(savedLocale) ? savedLocale : systemLocale();

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <MantineProvider>
      <App initialLocale={locale} />
    </MantineProvider>
  </StrictMode>
);
