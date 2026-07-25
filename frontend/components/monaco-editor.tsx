"use client";

import Editor from "@monaco-editor/react";
import type { editor } from "monaco-editor";
import { useRef } from "react";

interface MonacoEditorProps {
  value: string;
  onChange: (value: string) => void;
  readOnly?: boolean;
}

const EDITOR_OPTIONS: editor.IStandaloneEditorConstructionOptions = {
  minimap: { enabled: false },
  fontSize: 14,
  fontFamily: "JetBrains Mono, monospace",
  fontLigatures: true,
  lineNumbers: "on",
  scrollBeyondLastLine: false,
  automaticLayout: true,
  tabSize: 4,
  wordWrap: "off",
};

export function MonacoEditor({ value, onChange, readOnly = false }: MonacoEditorProps) {
  const editorRef = useRef<editor.IStandaloneCodeEditor | null>(null);

  function handleMount(editor: editor.IStandaloneCodeEditor) {
    editorRef.current = editor;
    editor.focus();
  }

  return (
    <Editor
      height="100%"
      defaultLanguage="java"
      theme="vs-dark"
      value={value}
      onChange={(v) => onChange(v ?? "")}
      onMount={handleMount}
      options={{ ...EDITOR_OPTIONS, readOnly }}
    />
  );
}
