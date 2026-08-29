import { AnimatePresence, motion } from "framer-motion";
import { Captions, Check, ChevronDown, Layers, ListChecks, Maximize2, MonitorPlay, Pause, Play, RefreshCw, SkipForward, Volume2, X } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { titleOf } from "../api";
import { Art, Chip, EmptyState, Seg, SPRING } from "../bits";
import { useApp } from "../store";
import type { Entry, Extension } from "../types";
import { cn } from "../utils/cn";

function mmss(t: number) {
  const m = Math.floor(t / 60);
  const s = Math.floor(t % 60);
  return `${m}:${String(s).padStart(2, "0")}`;
}
function epNum(title?: string | null): number | null {
  if (!title) return null;
  const m = /(\d+)/.exec(title);
  return m ? parseInt(m[1], 10) : null;
}
export interface Ep {
  n: number;
  title: string;
  thumb: string | null;
  site: string | null;
}

function Player({ head, ep, isAnime, ext, onDone, onClose }: { head: string; ep: Ep; isAnime: boolean; ext?: Extension; onDone: (n: number) => void; onClose: () => void }) {
  const { settings } = useApp();
  const DUR = 60;
  const [t, setT] = useState(0);
  const [playing, setPlaying] = useState(true);
  useEffect(() => {
    if (!playing) return;
    const i = window.setInterval(() => setT((s) => Math.min(DUR, s + 0.5)), 500);
    return () => window.clearInterval(i);
  }, [playing]);
  useEffect(() => { if (t >= DUR) onDone(ep.n); }, [t, ep.n, onDone]);
  const showSkip = settings.skipIntro && t >= 3 && t < 12;
  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="fixed inset-0 z-[80] grid place-items-center bg-black/85 p-4 backdrop-blur-sm">
      <motion.div initial={{ scale: 0.94, y: 24 }} animate={{ scale: 1, y: 0 }} exit={{ scale: 0.95, y: 12 }} transition={SPRING} className="relative aspect-video w-full max-w-[880px] overflow-hidden rounded-3xl border border-white/10 bg-black shadow-pop">
        {ep.thumb ? <Art src={ep.thumb} alt="" eager className="absolute inset-0 h-full w-full opacity-70" /> : <div className="absolute inset-0 bg-gradient-to-br from-acc/25 via-bg0 to-bg0" />}
        <div className="absolute inset-0 bg-gradient-to-t from-black/90 via-black/20 to-black/60" />
        <div className="noise absolute inset-0" />
        <div className="absolute inset-x-0 top-0 flex items-center justify-between gap-3 p-4">
          <div className="min-w-0">
            <p className="truncate text-[13px] font-bold text-white">{isAnime ? `E${ep.n}` : `Ch. ${ep.n}`} · {ep.title}</p>
            <p className="mt-0.5 truncate font-mono text-[10px] uppercase tracking-wider text-white/50">{head}</p>
          </div>
          <div className="flex items-center gap-2">
            {ext && <Chip className="border-white/15 bg-white/10 text-white/80 backdrop-blur">{ext.name} · {ext.cfg.server}</Chip>}
            <button onClick={onClose} className="grid h-8 w-8 place-items-center rounded-full border border-white/15 bg-black/50 text-white backdrop-blur transition hover:bg-black/80" aria-label="close player"><X size={14} /></button>
          </div>
        </div>
        <button onClick={() => setPlaying((v) => !v)} className="absolute inset-0 grid place-items-center" aria-label={playing ? "pause" : "play"}>
          <motion.span key={String(playing)} initial={{ scale: 0.6, opacity: 0 }} animate={{ scale: 1, opacity: 1 }} transition={SPRING} className="grid h-[74px] w-[74px] place-items-center rounded-full border border-white/25 bg-black/50 text-white backdrop-blur-md">
            {playing ? <Pause size={26} className="fill-current" /> : <Play size={26} className="ml-1 fill-current" />}
          </motion.span>
        </button>
        <AnimatePresence>
          {showSkip && (
            <motion.button initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: 20 }} onClick={() => setT(16)} className="absolute bottom-24 right-5 flex items-center gap-2 rounded-xl border border-white/20 bg-black/70 px-4 py-2.5 text-[12.5px] font-bold text-white backdrop-blur-md transition hover:bg-acc hover:text-accInk"><SkipForward size={14} /> Skip intro</motion.button>
          )}
        </AnimatePresence>
        <div className="absolute inset-x-0 bottom-0 p-4 pt-8">
          <div className="group relative h-1.5 w-full cursor-pointer overflow-visible rounded-full bg-white/20" onClick={(e) => { const r = (e.currentTarget as HTMLDivElement).getBoundingClientRect(); setT(Math.round(((e.clientX - r.left) / r.width) * DUR * 2) / 2); }}>
            <div className="h-full rounded-full bg-acc shadow-glow" style={{ width: `${(t / DUR) * 100}%` }} />
            <div className="absolute top-1/2 h-3.5 w-3.5 -translate-y-1/2 rounded-full bg-acc opacity-0 shadow-glow transition group-hover:opacity-100" style={{ left: `calc(${(t / DUR) * 100}% - 7px)` }} />
          </div>
          <div className="mt-3 flex items-center gap-3 text-white">
            <button onClick={() => setPlaying((v) => !v)} className="grid h-9 w-9 place-items-center rounded-full transition hover:bg-white/10" aria-label="play pause">{playing ? <Pause size={17} className="fill-current" /> : <Play size={17} className="ml-0.5 fill-current" />}</button>
            <span className="font-mono text-[11.5px] text-white/80 tabular">{mmss(t)} <span className="text-white/40">/ {mmss(DUR)}</span></span>
            <span className="text-white/30">·</span>
            <span className="font-mono text-[10.5px] uppercase tracking-wider text-white/60">demo playback</span>
            <div className="ml-auto flex items-center gap-2.5">
              <span className="flex items-center gap-1.5 text-white/70"><Captions size={15} /><span className="font-mono text-[10px] tabular">{settings.subSize}%</span></span>
              <Volume2 size={15} className="text-white/70" />
              <span className="rounded-md border border-white/20 bg-white/10 px-1.5 py-0.5 font-mono text-[10px] font-bold">{ext?.cfg.quality ?? "1080p"}</span>
              <Maximize2 size={14} className="text-white/70" />
            </div>
          </div>
        </div>
      </motion.div>
    </motion.div>
  );
}

export function WatchTab({ d }: { d: any }) {
  const { settings, library, upsertEntry, updateSettings, toast } = useApp();
  const isAnime = d.type === "ANIME";
  const title = titleOf(d, settings.titleLang);
  const entry = library[d.id];
  const exts = settings.extensions.filter((x) => x.kind === d.type);
  const [extId, setExtId] = useState<string>(() => exts.find((x) => x.enabled)?.id ?? exts[0]?.id ?? "");
  const [srcOpen, setSrcOpen] = useState(false);
  const [rangeStart, setRangeStart] = useState(0);
  const [player, setPlayer] = useState<Ep | null>(null);
  const [wrongOpen, setWrongOpen] = useState(false);
  const [wrongQ, setWrongQ] = useState(title);
  const [boundTitle, setBoundTitle] = useState<string | null>(null);
  const ext = exts.find((x) => x.id === extId && x.enabled) ?? exts.find((x) => x.enabled);

  const total: number | null = isAnime ? d.episodes ?? null : d.chapters ?? null;
  const progress = entry?.progress ?? 0;
  const RANGE = 35;

  const eps: Ep[] = useMemo(() => {
    if (!isAnime) {
      const count = total ?? 40;
      return Array.from({ length: count }, (_, i) => ({ n: i + 1, title: `Chapter ${i + 1}`, thumb: null, site: "Reader" }));
    }
    const map = new Map<number, Ep>();
    for (const s of d.streamingEpisodes ?? []) {
      const n = epNum(s.title);
      if (n && !map.has(n)) map.set(n, { n, title: (s.title ?? `Episode ${n}`).replace(/^.*[Ee]pisode\s*\d+\s*:?\s*/, "") || `Episode ${n}`, thumb: s.thumbnail ?? null, site: s.site ?? null });
    }
    const streamed = map.size ? Math.max(...map.keys()) : 0;
    const count = total ?? Math.max(12, streamed);
    const out: Ep[] = [];
    for (let n = 1; n <= count; n++) {
      const s = map.get(n);
      out.push(s ?? { n, title: `Episode ${n}`, thumb: null, site: null });
    }
    return out;
  }, [d, isAnime, total]);

  const count = eps.length;
  const nextN = Math.min(progress + 1, count || 1);
  const next = eps[nextN - 1] ?? eps[0];

  useEffect(() => {
    const idx = eps.findIndex((e) => e.n >= nextN);
    if (idx >= 0) setRangeStart(Math.floor(idx / RANGE) * RANGE);
  }, [progress, eps, nextN]);

  const ensure = useCallback((): Entry | null => {
    if (entry) return entry;
    return { id: d.id, type: d.type, title, cover: d.coverImage?.large ?? null, color: d.coverImage?.color ?? null, status: "PLANNING", progress: 0, total, updatedAt: Date.now() };
  }, [entry, d, title, total]);

  const mark = useCallback((n: number, silent = false) => {
    const e = ensure();
    if (!e) return;
    const already = e.progress >= n;
    const p = already ? n - 1 : n;
    const t2 = e.total ?? null;
    const status = t2 != null && p >= t2 ? "COMPLETED" : p > 0 && (e.status === "PLANNING" || e.status === "PAUSED") ? "CURRENT" : e.status;
    upsertEntry({ ...e, progress: p, status });
    if (!silent && !already) toast(`${isAnime ? "Episode" : "Chapter"} ${n} ${already ? "unmarked" : "watched"} — synced to AniList`, "sync");
  }, [ensure, upsertEntry, toast, isAnime]);

  const onPlayerDone = useCallback((n: number) => {
    mark(n, true);
    toast(`${isAnime ? "Episode" : "Chapter"} ${n} complete — progress saved`, "check");
    if (settings.autoNext && n < count) setPlayer(eps[n] ?? null);
    else { setPlayer(null); if (n >= count && count > 0) toast("All caught up — enjoy the cliffhanger", "info"); }
  }, [mark, toast, settings.autoNext, count, eps, isAnime]);

  const wrongResults = useMemo(() => {
    if (!wrongQ) return [];
    const q = wrongQ.toLowerCase();
    return [
      { id: 1, title: `${title}`, subtitle: `Exact match on ${ext?.name ?? "source"}` },
      { id: 2, title: `${title} (TV)`, subtitle: `Contains "${q.slice(0, 8)}"` },
      { id: 3, title: `${title} Season 2`, subtitle: `Alternative season` },
    ];
  }, [wrongQ, title, ext]);

  if (!exts.some((x) => x.enabled) && isAnime) {
    return <EmptyState icon={Layers} title="No extension enabled" sub="Enable a source extension in Settings → Extensions to stream episodes, or track progress manually." />;
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="font-disp text-xl font-bold">{isAnime ? "Episodes" : "Chapters"}</h2>
        <span className="font-mono text-[11px] text-mut">{count} on {ext?.name ?? "demo"} {boundTitle ? `· playing as "${boundTitle}"` : ""}</span>
      </div>

      {next && (
        <motion.div initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} transition={SPRING} className="relative overflow-hidden rounded-[18px] border border-line bg-bg2">
          <div className="absolute inset-0">{settings.showThumbs && next.thumb ? <Art src={next.thumb} alt="" className="h-full w-full opacity-55" /> : <div className="h-full w-full bg-gradient-to-br from-acc/20 via-transparent to-transparent" />}</div>
          <div className="absolute inset-0 bg-black/40" />
          <div className="relative flex items-center gap-3 p-3">
            <div className="min-w-0 flex-1">
              <p className="font-bold text-white text-[13px]">Continue : {isAnime ? `Episode ${next.n}` : `Chapter ${next.n}`}</p>
              <p className="mt-0.5 truncate text-[11px] text-white/70">{next.title}</p>
            </div>
            <button onClick={() => setPlayer(next)} className="grid h-9 w-9 place-items-center rounded-full bg-white text-black shadow-glow"><Play size={14} className="ml-0.5 fill-current" /></button>
          </div>
        </motion.div>
      )}

      <div className="flex flex-wrap items-center gap-2">
        {exts.length > 0 && (
          <div className="relative">
            <button onClick={() => setSrcOpen((v) => !v)} className="flex items-center gap-2.5 rounded-xl border border-accLine bg-accSoft py-2 pl-3 pr-3.5 text-[12px] font-semibold text-acc transition">
              <span className="grid h-5 w-5 place-items-center rounded-md text-[10px] font-black text-white" style={{ background: ext?.color ?? "#666" }}>{(ext?.name ?? "?").slice(0, 1)}</span>
              {ext?.name ?? "Manual"}
              <ChevronDown size={12} className={cn("transition-transform", srcOpen && "rotate-180")} />
            </button>
            <AnimatePresence>
              {srcOpen && (
                <>
                  <motion.div className="fixed inset-0 z-40" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={() => setSrcOpen(false)} />
                  <motion.div initial={{ opacity: 0, y: -6, scale: 0.98 }} animate={{ opacity: 1, y: 0, scale: 1 }} exit={{ opacity: 0, y: -4 }} transition={SPRING} className="absolute z-50 mt-2 w-[260px] rounded-2xl border border-line bg-bg1 p-1.5 shadow-pop">
                    <p className="px-3 pb-1 pt-2 text-[10.5px] font-semibold uppercase tracking-wider text-mut">Source extension</p>
                    {exts.map((x) => (
                      <button key={x.id} disabled={!x.enabled} onClick={() => { setExtId(x.id); setSrcOpen(false); }} className={cn("flex w-full items-center gap-2.5 rounded-xl px-3 py-2.5 text-left transition hover:bg-bg2 disabled:opacity-40", x.id === (ext?.id ?? "") && "text-acc")}>
                        <span className="grid h-7 w-7 place-items-center rounded-lg text-[11px] font-black text-white" style={{ background: x.color }}>{x.name.slice(0, 1)}</span>
                        <span className="flex-1 text-[13px] font-medium">{x.name}<span className="ml-1.5 text-[10px] text-mut">v{x.version}</span></span>
                        {x.enabled ? <span className="h-1.5 w-1.5 rounded-full bg-emerald-400" /> : <span className="text-[10px] text-mut">off</span>}
                      </button>
                    ))}
                  </motion.div>
                </>
              )}
            </AnimatePresence>
          </div>
        )}
        <button onClick={() => { setWrongQ(title); setWrongOpen(true); }} className="flex items-center gap-2 rounded-xl border border-line bg-bg1 px-3 py-2 text-[12px] font-semibold hover:border-accLine"><Layers size={12} /> Wrong title</button>
        <button onClick={() => { const e = ensure(); if (!e) return; upsertEntry({ ...e, progress: Math.max(0, nextN - 1) }); toast(`Marked first ${Math.max(0, nextN - 1)} as ${isAnime ? "watched" : "read"}`, "sync"); }} className="flex items-center gap-2 rounded-xl border border-line bg-bg1 px-3 py-2 text-[12px] font-semibold hover:border-accLine"><ListChecks size={12} className="text-acc" /> Mark previous</button>
        <div className="ml-auto"><Seg uid={`wm-${d.id}`} value={settings.autoNext ? "on" : "off"} onChange={(v) => { updateSettings({ autoNext: v === "on" }); toast(`Auto-next ${v === "on" ? "enabled" : "disabled"}`, "info"); }} options={[{ id: "on", label: "Auto-next" }, { id: "off", label: "Manual" }]} /></div>
      </div>

      {count > RANGE && (
        <div className="flex gap-2 overflow-x-auto [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
          {Array.from({ length: Math.ceil(count / RANGE) }, (_, i) => {
            const start = i * RANGE;
            const end = Math.min(count, start + RANGE);
            const active = start === rangeStart;
            return <button key={start} onClick={() => setRangeStart(start)} className={cn("shrink-0 rounded-full border px-4 py-2 text-[12px] font-bold transition", active ? "border-acc bg-acc text-accInk shadow-glow" : "border-accLine bg-accSoft text-acc hover:bg-acc/20")}>{start + 1}–{end}</button>;
          })}
        </div>
      )}

      <div className="space-y-1.5">
        {eps.slice(rangeStart, rangeStart + RANGE).map((ep, i) => {
          const watched = progress >= ep.n;
          const isNext = ep.n === nextN;
          return (
            <motion.div key={ep.n} initial={{ opacity: 0, y: 10 }} whileInView={{ opacity: 1, y: 0 }} viewport={{ once: true, margin: "-10px" }} transition={{ duration: 0.25, delay: Math.min(i, 8) * 0.02 }} onClick={() => setPlayer(ep)} className={cn("group flex cursor-pointer items-center gap-3.5 rounded-2xl border p-2 pr-3 transition-colors", isNext ? "border-accLine bg-accSoft/60" : "border-transparent hover:border-line hover:bg-bg1")}>
              {settings.showThumbs && isAnime ? (
                <div className="relative h-[52px] w-[92px] shrink-0 overflow-hidden rounded-xl border border-line bg-bg2">
                  {ep.thumb ? <Art src={ep.thumb} alt="" className="h-full w-full" /> : <div className="grid h-full w-full place-items-center font-mono text-sm font-bold text-mut tabular">{ep.n}</div>}
                  {watched && <div className="absolute inset-0 bg-bg0/60" />}
                </div>
              ) : <div className={cn("grid h-[52px] w-[52px] shrink-0 place-items-center rounded-xl border font-mono text-sm font-bold tabular", watched ? "border-line bg-bg2 text-mut" : "border-accLine bg-accSoft text-acc")}>{ep.n}</div>}
              <div className="min-w-0 flex-1">
                <p className={cn("truncate text-[13px] font-semibold", watched && "text-mut")}>{ep.title}</p>
                <p className="mt-0.5 flex items-center gap-2 text-[10.5px] text-mut">{isNext && <span className="rounded bg-acc px-1.5 py-px font-bold uppercase tracking-wider text-accInk">Up next</span>}{ep.site && <span className="rounded bg-bg2 px-1.5 py-px font-semibold">{ep.site}</span>}<span>{isAnime ? "24 min" : "~19 pages"}</span></p>
              </div>
              <button onClick={(ev) => { ev.stopPropagation(); setPlayer(ep); }} className="grid h-9 w-9 shrink-0 place-items-center rounded-full border border-line text-mut opacity-0 transition hover:border-accLine hover:text-acc group-hover:opacity-100" aria-label={isAnime ? "play" : "read"}><Play size={14} className="ml-0.5" /></button>
              <button onClick={(ev) => { ev.stopPropagation(); mark(ep.n); }} className={cn("grid h-9 w-9 shrink-0 place-items-center rounded-full border transition", watched ? "border-transparent bg-acc text-accInk shadow-glow" : "border-line text-mut hover:border-accLine hover:text-acc")} aria-label="toggle watched"><Check size={15} strokeWidth={3} /></button>
            </motion.div>
          );
        })}
      </div>

      <p className="text-center text-[10.5px] text-mut">{d.volumes ? `${d.volumes} volumes · ` : ""}Sources via extension {ext?.name ?? "demo"} — {boundTitle ? `playing as "${boundTitle}"` : "tap Wrong title if title mismatch"}.</p>

      <AnimatePresence>
        {wrongOpen && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="fixed inset-0 z-[70] grid place-items-end bg-black/60 p-4 backdrop-blur-sm sm:place-items-center">
            <motion.div initial={{ y: 24, scale: 0.98 }} animate={{ y: 0, scale: 1 }} exit={{ y: 12, scale: 0.98 }} transition={SPRING} className="w-full max-w-[420px] rounded-[22px] border border-line bg-bg1 p-5 shadow-pop">
              <h3 className="font-disp text-lg font-bold">Wrong title</h3>
              <p className="mt-1 text-[11px] text-mut">Search {ext?.name ?? "extension"} and pick the correct result. Anisora will bind it for this AniList entry.</p>
              <div className="mt-4 flex gap-2">
                <input value={wrongQ} onChange={(e) => setWrongQ(e.target.value)} placeholder="Title on this extension" className="flex-1 rounded-xl border border-line bg-bg2 px-3.5 py-2.5 text-[13px] outline-none focus:border-accLine" />
                <button onClick={() => setWrongQ((q) => q)} className="rounded-xl bg-acc px-4 py-2.5 text-[12.5px] font-bold text-accInk">Search</button>
              </div>
              <div className="mt-4 max-h-[280px] space-y-2 overflow-y-auto">
                {wrongResults.map((r) => (
                  <button key={r.id} onClick={() => { setBoundTitle(r.title); setWrongOpen(false); toast(`Bound to "${r.title}" on ${ext?.name ?? "source"}`, "check"); }} className="flex w-full items-center gap-3 rounded-xl border border-line bg-bg2 p-3 text-left hover:border-accLine">
                    <span className="grid h-10 w-10 place-items-center rounded-lg bg-bg1 font-bold text-mut">{r.title.slice(0, 1)}</span>
                    <span className="min-w-0 flex-1"><span className="block truncate text-[13px] font-semibold">{r.title}</span><span className="block text-[11px] text-mut">{r.subtitle}</span></span>
                  </button>
                ))}
              </div>
              <button onClick={() => setWrongOpen(false)} className="mt-4 w-full rounded-xl border border-line bg-bg1 py-2.5 text-[12.5px] font-semibold">Close</button>
            </motion.div>
            <div className="fixed inset-0 -z-10" onClick={() => setWrongOpen(false)} />
          </motion.div>
        )}
      </AnimatePresence>

      <AnimatePresence>{player && <Player key={`player-${player.n}`} head={title} ep={player} isAnime={isAnime} ext={ext} onDone={onPlayerDone} onClose={() => setPlayer(null)} />}</AnimatePresence>
    </div>
  );
}
