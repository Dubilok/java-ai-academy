export default function HomePage() {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center gap-8 p-8">
      <div className="text-center">
        <h1 className="text-4xl font-bold text-text-primary">
          Java <span className="text-accent-java">AI</span> Academy
        </h1>
        <p className="mt-3 text-lg text-text-muted">
          Learn Java 21+ · Instant verification · AI mentoring
        </p>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <div className="rounded-xl bg-bg-card p-6">
          <div className="text-2xl font-bold text-accent-java">01</div>
          <h2 className="mt-2 font-semibold text-text-primary">Read the lecture</h2>
          <p className="mt-1 text-sm text-text-muted">
            Concise, example-driven content on Java 21+ and the Spring ecosystem.
          </p>
        </div>

        <div className="rounded-xl bg-bg-card p-6">
          <div className="text-2xl font-bold text-accent-java">02</div>
          <h2 className="mt-2 font-semibold text-text-primary">Write the code</h2>
          <p className="mt-1 text-sm text-text-muted">
            Monaco editor in your browser, or your own IntelliJ IDEA via the plugin.
          </p>
        </div>

        <div className="rounded-xl bg-bg-card p-6">
          <div className="text-2xl font-bold text-success">03</div>
          <h2 className="mt-2 font-semibold text-text-primary">Get instant verdict</h2>
          <p className="mt-1 text-sm text-text-muted">
            JUnit 5 runs in a sandbox container. Pass → XP. Fail → Socratic hint.
          </p>
        </div>
      </div>

      <a
        href="/login"
        className="rounded-lg bg-accent-java px-6 py-3 font-semibold text-white hover:opacity-90"
      >
        Start learning
      </a>
    </main>
  );
}
