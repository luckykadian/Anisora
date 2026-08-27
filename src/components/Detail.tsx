import { AnimatePresence, motion } from "framer-motion";
import {
  Bookmark,
  CheckCircle2,
  ChevronDown,
  ChevronLeft,
  ExternalLink,
  Heart,
  Minus,
  Pause,
  Play,
  Plus,
  Radio,
  RotateCcw,
  Star,
  Trash2,
  X,
  Clapperboard,
} from "lucide-react";
import { useCallback, useEffect, useRef, useState, type ReactNode } from "react";
import { fetchDetail, FORMAT_LABELS, fmtCountdown, gradFrom, SEASON_LABELS, STATUS_LABELS, titleOf } from "../api";
import { Art, Chip, EmptyState, Skel, SPRING } from "../bits";
import { useApp } from "../store";
import type { DetailRef, Entry, ListStatus, PersonSeed } from "../types";
import { cn } from "../utils/cn";
import { InfoTab } from "./InfoTab";
import { WatchTab } from "./WatchTab";

export interface DetailProps {
  dref: DetailRef;
  depth: number;
  onBack: () => void;
  onCloseAll: () => void;
  onOpenMedia: (m: any, layoutId: string | null) => void;
  onGoHome: () => void;
}

export const STATUS_META: { id: ListStatus; label: string; icon: any }[] = [
  { id: "CURRENT", label: "Set as watching", icon: Play },
  { id: "COMPLETED", label: "Completed", icon: CheckCircle2 },
  { id: "PLANNING", label: "Plan to watch", icon: Bookmark },
  { id: "PAUSED", label: "Paused", icon: Pause },
  { id: "DROPPED", label: "Dropped", icon: X },
  { id: "REPEATING", label: "Rewatching", icon: RotateCcw },
];

const STATUS_SHORT: Record<ListStatus, string> = {
  CURRENT: "Watching",
  COMPLETED: "Completed",
  PLANNING: "Planning",
  PAUSED: "Paused",
  DROPPED: "Dropped",
  REPEATING: "Rewatching",
};

function Pop({ open, onClose, children, right }: { open: boolean; onClose: () => void; children: ReactNode; right?: boolean }) {
  return (
    <AnimatePresence>
      {open && (
        <>
          <motion.div className="fixed inset-0 z-40" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={onClose} />
          <motion.div
            initial={{ opacity: 0, y: -8, scale: 0.97 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -6, scale: 0.97 }}
            transition={SPRING}
            className={cn("absolute top-full z-50 mt-2 min-w-[230px] rounded-2xl border border-line bg-bg1 p-1.5 shadow-pop", right ? "right-0" : "left-0")}
          >
            {children}
          </motion.div>
        </>
      )}
    </AnimatePresence>
  );
}

export function DetailPage({ dref, depth, onBack, onCloseAll, onOpenMedia, onGoHome }: DetailProps) {
  const { library, settings, upsertEntry, removeEntry, toast } = useApp();
  const [d, setD] = useState<any>(null);
  const [err, setErr] = useState(false);
  const [tab, setTab] = useState<"info" | "play">("info");
  const [statusOpen, setStatusOpen] = useState(false);
  const [scoreOpen, setScoreOpen] = useState(false);
  const [fav, setFav] = useState(false);
  const scrollRef = useRef<HTMLDivElement>(null);

  const entry = library[dref.id];

  useEffect(() => {
    let on = true;
    setD(null);
    setErr(false);
    setTab("info");
    scrollRef.current?.scrollTo({ top: 0 });
    fetchDetail(dref.id)
      .then((m) => on && setD(m))
      .catch(() => on && setErr(true));
    return () => {
      on = false;
    };
  }, [dref.id]);

  const title = d ? titleOf(d, settings.titleLang) : entry?.title ?? "…";
  const isAnime = d ? d.type === "ANIME" : entry?.type !== "MANGA";

  const ensure = useCallback((): Entry | null => {
    if (entry) return entry;
    if (!d) return null;
    return {
      id: d.id,
      type: d.type,
      title,
      cover: d.coverImage?.large ?? null,
      color: d.coverImage?.color ?? null,
      status: "PLANNING",
      progress: 0,
      total: d.type === "MANGA" ? d.chapters ?? null : d.episodes ?? null,
      updatedAt: Date.now(),
    };
  }, [entry, d, title]);

  const setStatus = (s: ListStatus) => {
    const e = ensure();
    if (!e) return;
    const progress = s === "COMPLETED" ? e.total ?? e.progress : s === "PLANNING" ? 0 : e.progress;
    upsertEntry({ ...e, status: s, progress });
    toast(`${title.length > 26 ? title.slice(0, 26) + "…" : title} → ${s === "CURRENT" ? (isAnime ? "Watching" : "Reading") : STATUS_SHORT[s]}`);
    setStatusOpen(false);
  };

  const adjust = (delta: number) => {
    const e = ensure();
    if (!e) return;
    const total = e.total ?? null;
    const next = Math.max(0, total != null ? Math.min(total, e.progress + delta) : e.progress + delta);
    const done = total != null && next >= total && delta > 0;
    const status: ListStatus = done ? "COMPLETED" : (e.status === "PLANNING" || e.status === "PAUSED") && delta > 0 ? "CURRENT" : e.status;
    upsertEntry({ ...e, progress: next, status });
    if (done) toast(`Completed ${title.length > 24 ? title.slice(0, 24) + "…" : title} — nicely done`, "check");
  };

  const setScore = (v: number) => {
    const e = ensure();
    if (!e) return;
    upsertEntry({ ...e, score: v });
  };

  const airing = d?.nextAiringEpisode;
  const totalCount = d ? (isAnime ? d.episodes : d.chapters) : entry?.total;

  return (
    <motion.div
      ref={scrollRef}
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      transition={{ duration: 0.25 }}
      className="fixed inset-0 z-40 overflow-y-auto bg-bg0"
    >
      {/* banner */}
      <div className="pointer-events-none absolute inset-x-0 top-0 h-[280px] overflow-hidden md:h-[340px]">
        <div className="absolute inset-0" style={{ background: gradFrom(d?.coverImage?.color ?? entry?.color ?? null, title) }} />
        {d?.bannerImage && <Art src={d.bannerImage} alt="" eager className="absolute inset-0 h-full w-full opacity-90" />}
        <div className="absolute inset-0 bg-gradient-to-t from-bg0 via-bg0/55 to-bg0/25" />
        <div className="noise absolute inset-0" />
      </div>

      {/* nav buttons */}
      <div className="pointer-events-none sticky top-0 z-40 mx-auto flex w-full max-w-6xl items-center justify-between px-4 pt-4 sm:px-8">
        <button
          onClick={depth > 0 ? onBack : onGoHome}
          className="pointer-events-auto flex items-center gap-1.5 rounded-full border border-white/10 bg-black/45 py-2 pl-2.5 pr-4 text-[13px] font-semibold text-white backdrop-blur-md transition hover:bg-black/65"
        >
          <ChevronLeft size={16} /> Back
        </button>
        <button
          onClick={onCloseAll}
          className="pointer-events-auto grid h-9 w-9 place-items-center rounded-full border border-white/10 bg-black/45 text-white backdrop-blur-md transition hover:bg-black/65"
          aria-label="close"
        >
          <X size={16} />
        </button>
      </div>

      <div className="relative mx-auto w-full max-w-6xl px-4 sm:px-8">
        {/* header */}
        <div className="flex flex-col gap-6 pt-24 md:flex-row md:items-end md:pt-32">
          <motion.div
            layoutId={dref.layoutId ?? undefined}
            transition={SPRING}
            className="relative z-10 aspect-[2/3] w-[150px] shrink-0 self-start overflow-hidden rounded-poster border border-line/60 bg-bg2 shadow-pop md:w-[196px]"
          >
            <Art eager src={d?.coverImage?.extraLarge ?? d?.coverImage?.large ?? entry?.cover} alt={title} color={d?.coverImage?.color ?? entry?.color} className="h-full w-full" />
          </motion.div>

          <div className="min-w-0 flex-1 md:pb-1">
            {!d ? (
              err ? null : (
                <div className="space-y-3">
                  <Skel className="h-4 w-48" />
                  <Skel className="h-10 w-full max-w-md" />
                  <Skel className="h-4 w-64" />
                </div>
              )
            ) : (
              <>
                <div className="flex flex-wrap items-center gap-2">
                  {d.status && (
                    <Chip accent className="capitalize">
                      <span className={cn("h-1.5 w-1.5 rounded-full bg-current", d.status === "RELEASING" && "live-dot")} />
                      {STATUS_LABELS[d.status] ?? d.status}
                    </Chip>
                  )}
                  <Chip>{FORMAT_LABELS[d.format] ?? d.format}</Chip>
                  {d.season && d.seasonYear && (
                    <Chip>
                      {SEASON_LABELS[d.season]} {d.seasonYear}
                    </Chip>
                  )}
                  {totalCount != null && <Chip className="font-mono tabular">{isAnime ? `${totalCount} EP` : `${totalCount} CH`}</Chip>}
                  {airing && (
                    <Chip accent className="font-mono tabular">
                      <Radio size={11} /> EP {airing.episode} in {fmtCountdown(airing.timeUntilAiring)}
                    </Chip>
                  )}
                  {d.isAdult && <Chip className="border-rose-400/40 bg-rose-400/10 text-rose-300">18+</Chip>}
                </div>
                <h1 className="mt-3 line-clamp-3 font-disp text-[30px] font-bold leading-[1.04] tracking-tight md:text-[44px]">{title}</h1>
                {(d.title?.native || d.title?.english) && (
                  <p className="mt-2 truncate text-[13px] text-mut">
                    {[d.title?.native, d.title?.english && d.title.english !== title ? d.title.english : null].filter(Boolean).join("  ·  ")}
                  </p>
                )}
              </>
            )}
          </div>
        </div>

        {/* actions */}
        <div className="relative z-20 mt-6 flex flex-wrap items-center gap-2.5">
          {/* status selector */}
          <div className="relative">
            <button
              onClick={() => setStatusOpen((v) => !v)}
              className={cn(
                "flex items-center gap-2 rounded-2xl px-5 py-3 text-[13.5px] font-bold shadow-glow transition active:scale-[0.98]",
                entry ? "bg-accSoft text-acc ring-1 ring-accLine" : "bg-acc text-accInk",
              )}
            >
              {entry ? <CheckCircle2 size={16} /> : <Plus size={16} strokeWidth={3} />}
              {entry ? (entry.status === "CURRENT" ? (isAnime ? "Watching" : "Reading") : STATUS_SHORT[entry.status]) : "Add to List"}
              <ChevronDown size={14} className={cn("transition-transform", statusOpen && "rotate-180")} />
            </button>
            <Pop open={statusOpen} onClose={() => setStatusOpen(false)}>
              <p className="px-3 pb-1.5 pt-2 text-[10.5px] font-semibold uppercase tracking-wider text-mut">List status</p>
              {STATUS_META.map((s) => (
                <button
                  key={s.id}
                  onClick={() => setStatus(s.id)}
                  className={cn(
                    "flex w-full items-center gap-2.5 rounded-xl px-3 py-2.5 text-left text-[13px] font-medium transition hover:bg-bg2",
                    entry?.status === s.id && "text-acc",
                  )}
                >
                  <s.icon size={15} /> {isAnime ? s.label : s.label.replace("watch", "read").replace("watching", "reading")}
                  {entry?.status === s.id && <CheckCircle2 size={13} className="ml-auto" />}
                </button>
              ))}
              {entry && (
                <>
                  <div className="mx-2 my-1 border-t border-line" />
                  <button
                    onClick={() => {
                      removeEntry(entry.id);
                      toast("Removed from your list", "trash");
                      setStatusOpen(false);
                    }}
                    className="flex w-full items-center gap-2.5 rounded-xl px-3 py-2.5 text-left text-[13px] font-medium text-rose-300 transition hover:bg-rose-400/10"
                  >
                    <Trash2 size={15} /> Remove from list
                  </button>
                </>
              )}
            </Pop>
          </div>

          {/* progress stepper */}
          <div className="flex items-center overflow-hidden rounded-2xl border border-line bg-bg1">
            <button onClick={() => adjust(-1)} disabled={!entry || entry.progress <= 0} className="grid h-[46px] w-11 place-items-center text-mut transition hover:bg-bg2 hover:text-txt disabled:pointer-events-none disabled:opacity-30" aria-label="minus">
              <Minus size={15} />
            </button>
            <div className="min-w-[86px] border-x border-line px-3 text-center">
              <p className="font-mono text-[14px] font-bold leading-none tabular">
                {entry?.progress ?? 0}
                <span className="text-mut">/{totalCount ?? "∞"}</span>
              </p>
              <p className="mt-1 text-[9.5px] font-semibold uppercase tracking-[0.14em] text-mut">{isAnime ? "episodes" : "chapters"}</p>
            </div>
            <button onClick={() => adjust(1)} disabled={totalCount != null && (entry?.progress ?? 0) >= totalCount} className="grid h-[46px] w-11 place-items-center text-mut transition hover:bg-bg2 hover:text-txt disabled:pointer-events-none disabled:opacity-30" aria-label="plus">
              <Plus size={15} />
            </button>
          </div>

          {/* score */}
          <div className="relative">
            <button onClick={() => setScoreOpen((v) => !v)} className="flex h-[46px] items-center gap-2 rounded-2xl border border-line bg-bg1 px-4 text-[13px] font-semibold transition hover:border-accLine">
              <Star size={15} className={entry?.score ? "fill-amber-300 text-amber-300" : "text-mut"} />
              <span className="font-mono tabular">{entry?.score ? entry.score : "—"}</span>
              <span className="text-mut">score</span>
            </button>
            <Pop open={scoreOpen} onClose={() => setScoreOpen(false)} right>
              <div className="w-[240px] p-3">
                {settings.scoreFormat === "5" ? (
                  <div className="flex items-center justify-between">
                    {[1, 2, 3, 4, 5].map((n) => (
                      <button key={n} onClick={() => setScore(n)} className="transition hover:scale-125">
                        <Star size={26} className={((entry?.score ?? 0) >= n ? "fill-amber-300 text-amber-300" : "text-mut") + " transition-colors"} />
                      </button>
                    ))}
                  </div>
                ) : (
                  <>
                    <input
                      type="range"
                      min={0}
                      max={settings.scoreFormat === "10" ? 10 : 100}
                      step={settings.scoreFormat === "10" ? 0.5 : 1}
                      value={entry?.score ?? 0}
                      onChange={(e) => setScore(Number(e.target.value))}
                      className="range w-full"
                    />
                    <p className="mt-2 text-center font-mono text-xl font-bold tabular">
                      {entry?.score ?? 0}
                      <span className="text-sm text-mut">/{settings.scoreFormat === "10" ? 10 : 100}</span>
                    </p>
                  </>
                )}
                <button
                  onClick={() => {
                    setScore(0);
                    setScoreOpen(false);
                  }}
                  className="mt-2 w-full rounded-lg py-1.5 text-[11.5px] font-medium text-mut transition hover:bg-bg2"
                >
                  Clear score
                </button>
              </div>
            </Pop>
          </div>

          {/* misc */}
          <button
            onClick={() => {
              setFav((v) => !v);
              if (!fav) toast("Added to favourites", "check");
            }}
            className={cn("grid h-[46px] w-[46px] place-items-center rounded-2xl border transition", fav ? "border-rose-400/40 bg-rose-400/10 text-rose-300" : "border-line bg-bg1 text-mut hover:border-accLine hover:text-txt")}
            aria-label="favourite"
          >
            <Heart size={17} className={fav ? "fill-current" : ""} />
          </button>
          {d?.trailer?.site === "youtube" && (
            <a href={`https://www.youtube.com/watch?v=${d.trailer.id}`} target="_blank" rel="noreferrer" className="grid h-[46px] w-[46px] place-items-center rounded-2xl border border-line bg-bg1 text-mut transition hover:border-red-400/50 hover:text-red-300" aria-label="trailer">
              <Clapperboard size={17} />
            </a>
          )}
          <a href={`https://anilist.co/${isAnime ? "anime" : "manga"}/${dref.id}`} target="_blank" rel="noreferrer" className="grid h-[46px] w-[46px] place-items-center rounded-2xl border border-line bg-bg1 text-mut transition hover:border-accLine hover:text-txt" aria-label="open on anilist">
            <ExternalLink size={16} />
          </a>
        </div>

        {/* tab bar */}
        <div className="sticky top-0 z-30 mt-8 flex gap-1 border-b border-line bg-bg0/85 backdrop-blur-xl">
          {(["info", "play"] as const).map((t) => (
            <button key={t} onClick={() => setTab(t)} className={cn("relative flex items-center gap-2 px-5 py-4 text-[14px] font-semibold transition-colors", tab === t ? "text-txt" : "text-mut hover:text-txt")}>
              {tab === t && <motion.span layoutId={`dtab-${dref.id}`} transition={SPRING} className="absolute inset-x-3 bottom-[-1px] h-[2.5px] rounded-full bg-acc shadow-glow" />}
              {t === "info" ? "Info" : isAnime ? "Watch" : "Read"}
              {t === "play" && entry && totalCount != null && (
                <span className="rounded-md bg-bg2 px-1.5 py-0.5 font-mono text-[10px] font-bold text-acc tabular">
                  {entry.progress}/{totalCount}
                </span>
              )}
            </button>
          ))}
        </div>

        {/* content */}
        <div className="min-h-[50vh] pb-32 pt-10">
          {!d ? (
            err ? (
              <EmptyState icon={X} title="Could not load this entry" sub="Check your connection — the AniList API is unreachable right now." />
            ) : (
              <div className="space-y-4">
                <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
                  <Skel className="h-16" />
                  <Skel className="h-16" />
                  <Skel className="h-16" />
                  <Skel className="h-16" />
                </div>
                <Skel className="h-28" />
                <Skel className="h-44" />
              </div>
            )
          ) : (
            <AnimatePresence mode="wait">
              <motion.div key={tab} initial={{ opacity: 0, y: 14 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -10 }} transition={{ duration: 0.22 }}>
                {tab === "info" ? <InfoTab d={d} onOpenMedia={onOpenMedia} /> : <WatchTab d={d} />}
              </motion.div>
            </AnimatePresence>
          )}
        </div>
      </div>
    </motion.div>
  );
}

export function personSeedFromCharEdge(edge: any): Omit<PersonSeed, "uid"> {
  const node = edge.node;
  return {
    kind: "character",
    id: node.id,
    layoutId: `person-character-${node.id}`,
    name: node.name?.full ?? "Unknown",
    native: node.name?.native ?? null,
    image: node.image?.large ?? null,
    role: edge.role,
    vas: (edge.voiceActors ?? []).map((v: any) => ({ id: v.id, name: v.name?.full ?? "?", image: v.image?.medium ?? null })),
  };
}

export function personSeedFromStaffEdge(edge: any): Omit<PersonSeed, "uid"> {
  const node = edge.node;
  return {
    kind: "staff",
    id: node.id,
    layoutId: `person-staff-${node.id}`,
    name: node.name?.full ?? "Unknown",
    native: node.name?.native ?? null,
    image: node.image?.large ?? null,
    role: edge.role,
  };
}

export { STATUS_SHORT };
