"use client";

import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import rehypeHighlight from "rehype-highlight";
import "highlight.js/styles/github-dark.css";

interface MarkdownProps {
  content: string;
  className?: string;
}

export function Markdown({ content, className = "" }: MarkdownProps) {
  return (
    <div
      className={[
        "prose prose-invert max-w-none",
        // headings
        "prose-headings:font-bold prose-headings:text-text-primary prose-headings:tracking-tight",
        "prose-h1:text-2xl prose-h1:mb-4 prose-h1:mt-0",
        "prose-h2:text-xl prose-h2:mt-8 prose-h2:mb-3 prose-h2:border-b prose-h2:border-white/10 prose-h2:pb-2",
        "prose-h3:text-base prose-h3:mt-6 prose-h3:mb-2 prose-h3:text-accent-blue",
        // body
        "prose-p:text-text-muted prose-p:leading-7 prose-p:my-3",
        "prose-li:text-text-muted prose-li:leading-7",
        "prose-strong:text-text-primary prose-strong:font-semibold",
        "prose-em:text-text-muted",
        // inline code
        "prose-code:text-accent-java prose-code:bg-white/5 prose-code:rounded prose-code:px-1 prose-code:py-0.5",
        "prose-code:text-sm prose-code:font-mono prose-code:before:content-none prose-code:after:content-none",
        // code blocks
        "prose-pre:bg-transparent prose-pre:p-0 prose-pre:my-4",
        // tables
        "prose-table:text-sm prose-table:w-full",
        "prose-th:text-text-primary prose-th:bg-white/5 prose-th:px-4 prose-th:py-2 prose-th:text-left prose-th:font-semibold",
        "prose-td:text-text-muted prose-td:px-4 prose-td:py-2 prose-td:border-t prose-td:border-white/5",
        "prose-thead:border-b prose-thead:border-white/10",
        // links
        "prose-a:text-accent-blue prose-a:no-underline hover:prose-a:underline",
        // blockquote
        "prose-blockquote:border-l-accent-java prose-blockquote:text-text-muted prose-blockquote:bg-white/3 prose-blockquote:rounded-r-lg prose-blockquote:py-1",
        // hr
        "prose-hr:border-white/10",
        className,
      ].join(" ")}
    >
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        rehypePlugins={[rehypeHighlight]}
        components={{
          pre({ children }) {
            return (
              <pre className="overflow-x-auto rounded-xl bg-[#0d1117] p-4 my-4 text-sm leading-relaxed border border-white/10">
                {children}
              </pre>
            );
          },
          table({ children }) {
            return (
              <div className="overflow-x-auto my-4 rounded-xl border border-white/10">
                <table className="w-full border-collapse">{children}</table>
              </div>
            );
          },
        }}
      >
        {content}
      </ReactMarkdown>
    </div>
  );
}
