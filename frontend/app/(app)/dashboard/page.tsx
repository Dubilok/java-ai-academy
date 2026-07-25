"use client";

import { useAuth } from "@/contexts/auth-context";

export default function DashboardPage() {
  const { user } = useAuth();

  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold text-text-primary">Dashboard</h1>
      <p className="mt-2 text-text-muted">Welcome back, {user?.email}</p>
    </div>
  );
}
