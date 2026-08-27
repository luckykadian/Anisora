import { AnimatePresence, motion } from "framer-motion";
import { Check, Info, Play, RefreshCw, Star, Trash2, type LucideIcon } from "lucide-react";
import { useState, type ReactNode } from "react";
import { gradFrom, spoilerize } from "./api";
import { useApp } from "./store";
import { cn } from "./utils/cn";

export const SPRING = { type: "spring", stiffness: 340, damping: 32 } as const;

/* ---------------------------------- Logo ---------------------------------- */

export function Logo({ size = 30, text = true }: { size?: number; text?: boolean }) {
  return (
    <span className="flex select-none items-center gap-2.5">
      <svg width={size} height={size} viewBox="0 0 32 32" fill="none">
        <defs>
          <linearGradient id="lg-core" x1="9" y1="9" x2="23" y2="23">
            <stop stopColor="rgb(var(--acc))" />
            <stop offset="1" stopColor="color-mix(in srgb, rgb(var(--acc)) 35%, white)" />
          </linearGradient>
        </defs>
        <circle cx="16" cy="16" r="6.5" fill="url(#lg-core)" />
        <ellipse cx="16" cy="16" rx="14" ry="5.4" stroke="rgb(var(--acc))" strokeWidth="1.5" transform="rotate(-22 16 16)" opacity=".9" />
        <circle cx="27" cy="11" r="1.9" fill="rgb(var(--acc))" />
      </svg>
      {text && (
        <span className="font-disp text-[19px] font-bold tracking-tight">
          Ani<span className="text-acc">sora</span>
        </span>
      )}
    </span>
  );
}

/* ---------------------------- image with fallback ---------------------------- */

export function Art({
  src,
  alt,
  color,
  className,
  eager,
}: {
  src?: string | null;
  alt: string;
  color?: string | null;
  className?: string;
  eager?: boolean;
}) {
  const [err, setErr] = useState(false);
  if (!src || err) {
    return (
      <div className={cn("grid select-none place-items-center overflow-hidden", className)} style={{ background: gradFrom(color ?? null, alt) }}>
        <span className="px-3 text-center font-disp text-[13px] font-semibold leading-snug text-white drop-shadow-md">{alt}</span>
      </div>
    );
  }
  return (
    <img
      src={src}
      alt={alt}
      loading={eager ? "eager" : "lazy"}
      referrerPolicy="no-referrer"
      draggable={false}
      onError={() => setErr(true)}
      className={cn("object-cover", className)}
    />
  );
}

/* ------------------------------- rich html text ------------------------------ */

export function RichText({ html, className }: { html?: string | null; className?: string }) {
  if (!html) return null;
  return (
    <div
      className={cn("richtxt text-sm leading-relaxed text-mut", className)}
      dangerouslySetInnerHTML={{ __html: spoilerize(html) }}
      onClick={(e) => {
        const t = e.target as HTMLElement;
        if (t.classList?.contains("spoiler")) t.classList.toggle("open");
      }}
    />
  );
}

/* ----------------------------------- chip ----------------------------------- */

export function Chip({ children, className, accent }: { children: ReactNode; className?: string; accent?: boolean }) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 whitespace-nowrap rounded-full border px-2.5 py-1 text-[11px] font-medium",
        accent ? "border-transparent bg-accSoft text-acc" : "border-line bg-bg1 text-mut",
        className,
      )}
    >
      {children}
    </span>
  );
}

/* -------------------------------- segmented --------------------------------- */

export interface SegOption<T extends string> {
  id: T;
  label: string;
  icon?: LucideIcon;
}

export function Seg<T extends string>({
  uid,
  options,
  value,
  onChange,
  className,
}: {
  uid: string;
  options: SegOption<T>[];
  value: T;
  onChange: (v: T) => void;
  className?: string;
}) {
  return (
    <div className={cn("inline-flex items-center gap-0.5 rounded-full border border-line bg-bg2 p-1", className)}>
      {options.map((o) => {
        const active = value === o.id;
        const Icon = o.icon;
        return (
          <button
            key={o.id}
            type="button"
            onClick={() => onChange(o.id)}
            className={cn(
              "relative rounded-full px-3.5 py-1.5 text-[12.5px] font-medium transition-colors duration-200",
              active ? "text-accInk" : "text-mut hover:text-txt",
            )}
          >
            {active && <motion.span layoutId={`seg-${uid}`} transition={SPRING} className="absolute inset-0 rounded-full bg-acc shadow-glow" />}
            <span className="relative z-10 flex items-center gap-1.5">
              {Icon && <Icon size={13} />}
              {o.label}
            </span>
          </button>
        );
      })}
    </div>
  );
}

/* ---------------------------------- toggle ---------------------------------- */

export function Toggle({ on, onChange, disabled }: { on: boolean; onChange: (v: boolean) => void; disabled?: boolean }) {
  return (
    <button
      type="button"
      aria-pressed={on}
      onClick={() => !disabled && onChange(!on)}
      className={cn(
        "relative h-6.5 w-11 shrink-0 rounded-full border transition-colors duration-200",
        on ? "border-transparent" : "border-line bg-bg2",
        disabled && "pointer-events-none opacity-40",
      )}
      style={on ? { background: "rgb(var(--acc))" } : undefined}
    >
      <motion.span
        animate={{ x: on ? 18 : 1 }}
        transition={{ type: "spring", stiffness: 520, damping: 34 }}
        className="absolute left-0.5 top-0.5 h-5 w-5 rounded-full bg-white shadow-md"
      />
    </button>
  );
}

/* --------------------------------- section head ------------------------------ */

export function SectionHead({ icon: Icon, title, sub, right }: { icon?: LucideIcon; title: ReactNode; sub?: string; right?: ReactNode }) {
  return (
    <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
      <div className="flex items-center gap-2.5">
        {Icon && (
          <span className="grid h-8 w-8 place-items-center rounded-xl bg-accSoft text-acc">
            <Icon size={15} />
          </span>
        )}
        <div>
          <h3 className="font-disp text-[17px] font-semibold tracking-tight">{title}</h3>
          {sub && <p className="text-[11.5px] text-mut">{sub}</p>}
        </div>
      </div>
      {right}
    </div>
  );
}

/* ---------------------------------- stat pill -------------------------------- */

export function StatPill({ icon: Icon, label, value, className }: { icon: LucideIcon; label: string; value: ReactNode; className?: string }) {
  return (
    <div className={cn("flex items-center gap-3 rounded-2xl border border-line bg-bg1 px-4 py-3", className)}>
      <span className="grid h-9 w-9 shrink-0 place-items-center rounded-xl bg-accSoft text-acc">
        <Icon size={16} />
      </span>
      <div className="min-w-0">
        <p className="font-mono text-[15px] font-bold leading-tight tabular">{value}</p>
        <p className="truncate text-[11px] text-mut">{label}</p>
      </div>
    </div>
  );
}

/* --------------------------------- score pill -------------------------------- */

export function ScorePill({ score }: { score?: number | null }) {
  if (!score) return null;
  const tone = score >= 80 ? "text-emerald-300" : score >= 65 ? "text-amber-300" : "text-rose-300";
  return (
    <span className={cn("inline-flex items-center gap-1 rounded-full border border-white/10 bg-black/55 px-2 py-0.5 font-mono text-[11px] font-bold backdrop-blur-sm tabular", tone)}>
      <Star size={10} fill="currentColor" />
      {score}
    </span>
  );
}

/* --------------------------------- empty state ------------------------------- */

export function EmptyState({ icon: Icon, title, sub, className }: { icon: LucideIcon; title: string; sub?: string; className?: string }) {
  return (
    <div className={cn("flex flex-col items-center justify-center gap-3 rounded-3xl border border-dashed border-line bg-bg1/50 px-6 py-14 text-center", className)}>
      <span className="grid h-14 w-14 place-items-center rounded-2xl bg-bg2 text-mut">
        <Icon size={22} />
      </span>
      <div>
        <p className="font-disp text-base font-semibold">{title}</p>
        {sub && <p className="mt-1 max-w-sm text-[13px] text-mut">{sub}</p>}
      </div>
    </div>
  );
}

export function Kbd({ children }: { children: ReactNode }) {
  return <kbd className="rounded-md border border-line bg-bg2 px-1.5 py-0.5 font-mono text-[10px] font-semibold text-mut">{children}</kbd>;
}

export function Skel({ className }: { className?: string }) {
  return <div className={cn("skel rounded-xl", className)} />;
}

/* ----------------------------------- toasts ---------------------------------- */

const TOAST_ICONS = { check: Check, sync: RefreshCw, trash: Trash2, info: Info, play: Play };

export function ToastStack() {
  const { toasts } = useApp();
  return (
    <div className="pointer-events-none fixed bottom-[calc(env(safe-area-inset-bottom)+86px)] left-1/2 z-[120] flex w-full max-w-sm -translate-x-1/2 flex-col items-center gap-2 px-4 md:bottom-8 md:left-auto md:right-8 md:translate-x-0 md:items-end">
      <AnimatePresence>
        {toasts.map((t) => {
          const Icon = TOAST_ICONS[t.icon];
          return (
            <motion.div
              key={t.id}
              layout
              initial={{ opacity: 0, y: 18, scale: 0.94 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, y: 10, scale: 0.94 }}
              transition={SPRING}
              className="pointer-events-auto flex items-center gap-2.5 rounded-2xl border border-line bg-bg1/95 px-4 py-2.5 shadow-pop backdrop-blur-xl"
            >
              <span className="grid h-6.5 w-6.5 shrink-0 place-items-center rounded-full bg-accSoft text-acc">
                <Icon size={13} />
              </span>
              <span className="text-[13px] font-medium">{t.msg}</span>
            </motion.div>
          );
        })}
      </AnimatePresence>
    </div>
  );
}
