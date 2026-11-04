"use client";

export default function ErrorPage({ reset }: { reset: () => void }) {
  return <div className="mx-auto max-w-5xl px-6 py-16"><h1 className="text-2xl font-semibold">Não foi possível carregar esta página</h1><button className="mt-6 rounded-md bg-primary px-4 py-3 text-white" onClick={reset}>Tentar novamente</button></div>;
}
