import { motion } from "framer-motion";
import { Plus } from "lucide-react";
import { FORMAT_LABELS, STATUS_LABELS, titleOf } from "../api";
import { Art, ScorePill, SPRING } from "../bits";
import { useOverlay } from "../overlay";
import { useApp } from "../store";
import type { Entry } from "../types";
import { cn } from "../utils/cn";

export type OpenMediaFn = (m: any, layoutId: string | null) => void;

const DOT: Record<string, string> = {
  RELEASING: "bg-emerald-400",
  FINISHED: "bg-sky-400",
  NOT_YET_RELEASED: "bg-amber-400",
  CANCELLED: "bg-rose-400",
  HIATUS: "bg-orange-400",
};

export function MediaCard({
  m,
  rowKey,
  entry,
  onOpen,
}: {
  m: any;
  rowKey: string;
  entry?: Entry;
  onOpen: OpenMediaFn;
}) {
  const { settings, library, bumpProgress, toast } = useApp();
  const { hidden: hiddenIds } = useOverlay();
  const e = entry ?? library[m.id];
  const layoutId = `${rowKey}-${m.id}`;
  const hidden = hiddenIds.has(layoutId);
  const title = titleOf(m, settings.titleLang);
  const aired = m.nextAiringEpisode?.episode ? m.nextAiringEpisode.episode - 1 : null;
  const total = m.episodes ?? m.chapters ?? e?.total ?? null;
  const progTotal = total ?? (aired ? Math.max(aired, e?.progress ?? 0) : e?.progress && e.progress > 0 ? e.progress : null);
  const pct = e && progTotal ? Math.min(100, (e.progress / progTotal) * 100) : 0;
  const canBump = !!e && e.status !== "COMPLETED" && (total == null || e.progress < total);
  const meta = [FORMAT_LABELS[m.format ?? ""] ?? m.format, m.seasonYear, m.status ? STATUS_LABELS[m.status] : undefined].filter(Boolean).slice(0, 2);

  return (
    <motion.button
      type="button"
      onClick={() => onOpen(m, layoutId)}
      whileHover={{ y: -5 }}
      whileTap={{ scale: 0.97 }}
      transition={SPRING}
      className="group relative block w-full text-left outline-none"
    >
      <motion.div
        layoutId={layoutId}
        style={hidden ? { opacity: 0 } : undefined}
        className="relative aspect-[2/3] w-full overflow-hidden rounded-poster border border-line bg-bg2 shadow-card transition-[box-shadow,border-color] duration-300 group-hover:border-accLine group-hover:shadow-glow"
      >
        <Art
          src={m.coverImage?.large ?? m.coverImage?.extraLarge}
          alt={title}
          color={m.coverImage?.color}
          className="h-full w-full transition-transform duration-500 ease-out group-hover:scale-[1.07]"
        />
        <div className="pointer-events-none absolute inset-x-0 bottom-0 h-2/5 bg-gradient-to-t from-black/75 via-black/20 to-transparent" />

        {/* format badge */}
        {m.format && (
          <span className="absolute left-2 top-2 rounded-lg border border-white/10 bg-black/55 px-1.5 py-0.5 font-mono text-[9.5px] font-bold uppercase tracking-wider text-white/85 backdrop-blur-sm">
            {FORMAT_LABELS[m.format] ?? m.format}
          </span>
        )}

        {/* quick +1 */}
        {canBump && (
          <span
            role="button"
            tabIndex={-1}
            onClick={(ev) => {
              ev.stopPropagation();
              bumpProgress(m.id);
              toast(`${m.type === "MANGA" ? "Ch." : "Ep."} ${(e?.progress ?? 0) + 1} logged — ${title.length > 22 ? title.slice(0, 22) + "…" : title}`, "check");
            }}
            className="absolute right-2 top-2 grid h-7.5 w-7.5 place-items-center rounded-full border border-white/15 bg-black/60 text-white opacity-0 backdrop-blur-md transition-all duration-200 hover:scale-110 hover:bg-acc hover:text-accInk group-hover:opacity-100"
          >
            <Plus size={15} strokeWidth={2.5} />
          </span>
        )}

        {m.averageScore ? (
          <span className="absolute bottom-2 right-2">
            <ScorePill score={m.averageScore} />
          </span>
        ) : null}

        {/* progress bar */}
        {e && progTotal ? (
          <div className="absolute inset-x-0 bottom-0 h-1 bg-black/50">
            <div className="h-full rounded-r-full bg-acc shadow-glow transition-all duration-500" style={{ width: `${pct}%` }} />
          </div>
        ) : null}
      </motion.div>

      <div className="mt-2 px-0.5">
        <p className={cn("truncate font-semibold leading-tight", settings.density === "compact" ? "text-[12px]" : "text-[13px]")}>{title}</p>
        <div className="mt-1 flex items-center gap-1.5 text-[11px] text-mut">
          {m.status && <span className={cn("h-1.5 w-1.5 shrink-0 rounded-full", DOT[m.status] ?? "bg-mut", m.status === "RELEASING" && "live-dot")} />}
          <span className="truncate">{e && meta.length === 0 && m.status == null ? `${e.progress}/${e.total ?? "?"} ${m.type === "MANGA" ? "ch" : "ep"}` : meta.join(" · ") || (e ? `${e.progress}/${e.total ?? "?"}` : "—")}</span>
        </div>
      </div>
    </motion.button>
  );
}
