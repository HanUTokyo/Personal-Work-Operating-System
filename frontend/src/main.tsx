import React, { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { MantineProvider } from "@mantine/core";
import { BrowserRouter } from "react-router-dom";
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
const basename = window.location.pathname.startsWith("/task-app/beta3") ? "/task-app/beta3" : undefined;

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <BrowserRouter basename={basename}>
      <MantineProvider>
        <App initialLocale={locale} />
      </MantineProvider>
    </BrowserRouter>
  </StrictMode>
);
