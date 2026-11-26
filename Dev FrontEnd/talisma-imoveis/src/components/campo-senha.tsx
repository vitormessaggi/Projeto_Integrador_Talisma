"use client";

import { useId, useState, type ComponentProps } from "react";
import { Eye, EyeOff } from "lucide-react";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";

export function CampoSenha({ id, className, "aria-label": rotulo, ...props }: Omit<ComponentProps<"input">, "type">) {
  const idGerado = useId();
  const idCampo = id ?? idGerado;
  const [visivel, setVisivel] = useState(false);
  const acao = visivel ? "Ocultar senha" : "Mostrar senha";
  const Icone = visivel ? EyeOff : Eye;

  return <div className="relative">
    <Input {...props} id={idCampo} aria-label={rotulo} type={visivel ? "text" : "password"}
      className={cn("min-h-11 bg-white pr-12", className)} />
    <button type="button" aria-label={acao} aria-controls={idCampo}
      className="absolute inset-y-0 right-0 grid w-11 place-items-center rounded-md text-muted-foreground hover:text-primary"
      onClick={() => setVisivel(valor => !valor)}>
      <Icone size={20} aria-hidden="true" />
    </button>
  </div>;
}
