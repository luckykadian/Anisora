import { CloudOff, Film, Flame, Heart, History, LayoutGrid, Search, TrendingUp, Trophy, Wifi } from "lucide-react";
import { useEffect, useMemo, useRef, useState } from "react";
import { fallbackHome, fetchHome, SEASON_LABELS, seasonNow, searchMedia } from "../api";
import { Chip, EmptyState, SectionHead, Seg, Skel } from "../bits";
import { useApp } from "../store";
import type { Entry, ListStatus, MediaType } from "../types";
import { cn } from "../utils/cn";
import { MediaCard, type OpenMediaFn } from "./MediaCard";

/* ------------------------------ small helpers ------------------------------ */

const FILTERS: { id: "ALL" | ListStatus; label: string }[] = [
  { id: "ALL", label: "All" },
  { id: "CURRENT", label: "In Progress" },
  { id: "COMPLETED", label: "Completed" },
  { id: "PLANNING", label: "Planning" },
  { id: "PAUSED", label: "Paused" },
  { id: "DROPPED", label: "Dropped" },
];

function entryToMedia(e: Entry): any {
  return {
    id: e.id,
    type: e.type,
    title: { romaji: e.title },
    coverImage: { large: e.cover ?? null, color: e.color ?? null },
    format: e.type === "MANGA" ? "MANGA" : undefined,
  };
}

/* ----------------------------------- rail ---------------------------------- */

function Rail({ icon, title, sub, items, rowKey, onOpen }: { icon: any; title: string; sub?: string; items: any[]; rowKey: string; onOpen: OpenMediaFn }) {
  const { settings } = useApp();
  const ref = useRef<HTMLDivElement>(null);
  const scroll = (dir: number) => ref.current?.scrollBy({ left: dir * ref.current.clientWidth * 0.75, behavior: "smooth" });
  if (!items?.length) return null;
  return (
    <section className="relative">
      <SectionHead
        icon={icon}
        title={title}
        sub={sub}
        right={
          <div className="hidden gap-1.5 md:flex">
            <button onClick={() => scroll(-1)} className="grid h-8 w-8 place-items-center rounded-xl border border-line bg-bg1 text-mut transition hover:border-accLine hover:text-txt" aria-label="scroll left">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round"><path d="m15 18-6-6 6-6" /></svg>
            </button>
            <button onClick={() => scroll(1)} className="grid h-8 w-8 place-items-center rounded-xl border border-line bg-bg1 text-mut transition hover:border-accLine hover:text-txt" aria-label="scroll right">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round"><path d="m9 18 6-6-6-6" /></svg>
            </button>
          </div>
        }
      />
      <div ref={ref} className={cn("maskx -mx-1 flex snap-x gap-4 overflow-x-auto px-1 pb-3 pt-1 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden")}>
        {items.map((m) => (
          <div key={`${rowKey}-${m.id}`} className={cn("shrink-0 snap-start", settings.density === "compact" ? "w-[118px] sm:w-[128px]" : "w-[136px] sm:w-[152px]")}>
            <MediaCard m={m} rowKey={rowKey} onOpen={onOpen} />
          </div>
        ))}
      </div>
    </section>
  );
}

function RailSkeleton({ icon: Icon, title }: { icon: any; title: string }) {
  const { settings } = useApp();
  return (
    <section>
      <SectionHead icon={Icon} title={title} />
      <div className="-mx-1 flex gap-4 overflow-hidden px-1 pb-3">
        {Array.from({ length: 8 }).map((_, i) => (
          <div key={i} className={cn("shrink-0", settings.density === "compact" ? "w-[118px] sm:w-[128px]" : "w-[136px] sm:w-[152px]")}>
            <Skel className="aspect-[2/3] w-full rounded-poster" />
            <Skel className="mt-2 h-3 w-4/5" />
            <Skel className="mt-1.5 h-2.5 w-3/5" />
          </div>
        ))}
      </div>
    </section>
  );
}

/* ----------------------------------- home ---------------------------------- */

export function Home({ type, onOpen }: { type: MediaType; onOpen: OpenMediaFn }) {
  const { session, library, settings, setOffline } = useApp();
  const [data, setData] = useState<any>(null);
  const [failed, setFailed] = useState(false);
  const [filter, setFilter] = useState<"ALL" | ListStatus>("ALL");

  useEffect(() => {
    let on = true;
    setData(null);
    setFailed(false);
    fetchHome(type)
      .then((d) => {
        if (!on) return;
        setData(d);
        setOffline(false);
      })
      .catch(() => {
        if (!on) return;
        setData(fallbackHome(type));
        setFailed(true);
        setOffline(true);
      });
    return () => {
      on = false;
    };
  }, [type, setOffline]);

  const entries = useMemo(() => Object.values(library).filter((e) => e.type === type).sort((a, b) => b.updatedAt - a.updatedAt), [library, type]);
  const inProgress = entries.filter((e) => e.status === "CURRENT" || e.status === "PAUSED" || e.status === "REPEATING");
  const filtered = filter === "ALL" ? entries : entries.filter((e) => e.status === filter);
  const { season, year } = seasonNow();
  const hour = new Date().getHours();
  const greet = hour < 12 ? "Good morning" : hour < 18 ? "Good afternoon" : "Good evening";
  const dateStr = new Date().toLocaleDateString(undefined, { weekday: "long", month: "long", day: "numeric" });
  const isAnime = type === "ANIME";

  return (
    <div className="space-y-12 pb-28">
      {/* greeting */}
      <header className="flex flex-wrap items-end justify-between gap-4 pt-1">
        <div>
          <p className="font-mono text-[11px] uppercase tracking-[0.22em] text-mut">
            {dateStr} · {SEASON_LABELS[season]} {year}
          </p>
          <h1 className="mt-1.5 font-disp text-3xl font-bold tracking-tight sm:text-[34px]">
            {greet}, <span className="text-acc">{session?.user.name ?? "traveller"}</span>
          </h1>
          <p className="mt-1 text-[13px] text-mut">
            {inProgress.length > 0
              ? `You have ${inProgress.length} ${isAnime ? "series" : "titles"} in progress — pick up where you left off.`
              : `Your ${isAnime ? "anime" : "manga"} universe awaits.`}
          </p>
        </div>
        <div className="flex items-center gap-2">
          {failed ? (
            <Chip className="border-amber-400/30 bg-amber-400/10 text-amber-300">
              <CloudOff size={12} /> Offline mode
            </Chip>
          ) : (
            <Chip>
              <Wifi size={12} className="text-emerald-400" /> AniList connected
            </Chip>
          )}
        </div>
      </header>

      {/* library */}
      {entries.length > 0 && (
        <section>
          <SectionHead
            icon={LayoutGrid}
            title={`My ${isAnime ? "Anime" : "Manga"} List`}
            sub={`${entries.length} tracked on this device`}
            right={
              <Seg
                uid={`libf-${type}`}
                value={filter}
                onChange={setFilter}
                options={FILTERS.filter((f) => f.id === "ALL" || entries.some((e) => e.status === f.id)).map((f) => ({ id: f.id, label: f.label }))}
              />
            }
          />
          {filtered.length ? (
            <div className="grid gap-x-4 gap-y-6" style={{ gridTemplateColumns: `repeat(auto-fill, minmax(${settings.density === "compact" ? "112px" : "142px"}, 1fr))` }}>
              {filtered.map((e) => (
                <MediaCard key={`lib-${type}-${e.id}`} m={entryToMedia(e)} rowKey={`lib-${type}`} entry={e} onOpen={onOpen} />
              ))}
            </div>
          ) : (
            <EmptyState icon={Search} title="Nothing here" sub={`No ${isAnime ? "anime" : "manga"} matches this filter yet.`} />
          )}
        </section>
      )}

      {/* rails */}
      {!data ? (
        <>
          <RailSkeleton icon={Flame} title="Trending now" />
          <RailSkeleton icon={Trophy} title="All-time top rated" />
        </>
      ) : (
        <>
          <Rail icon={Flame} title="Trending now" sub={isAnime ? "What everyone is watching" : "Hot on the charts"} items={data.trending.media} rowKey={`tr-${type}`} onOpen={onOpen} />
          <Rail
            icon={TrendingUp}
            title={isAnime ? `Popular this ${SEASON_LABELS[season].toLowerCase()}` : "All-time favourites"}
            sub={isAnime ? `${SEASON_LABELS[season]} ${year} season` : "Most loved by the community"}
            items={data.seasonal.media}
            rowKey={`ss-${type}`}
            onOpen={onOpen}
          />
          <Rail icon={Trophy} title="Top rated" sub="Critics' darlings" items={data.top.media} rowKey={`tp-${type}`} onOpen={onOpen} />
          <Rail icon={Heart} title="Community favourites" items={data.loved.media} rowKey={`lv-${type}`} onOpen={onOpen} />
        </>
      )}

      <footer className="flex items-center justify-center gap-2 pt-4 text-[11px] text-mut">
        <History size={12} /> Data by the AniList API · Anisora is a demo client
      </footer>
    </div>
  );
}

/* ---------------------------------- search --------------------------------- */

export function SearchView({ q, onOpen }: { q: string; onOpen: OpenMediaFn }) {
  const { settings } = useApp();
  const [tab, setTab] = useState<"ALL" | MediaType>("ALL");
  const [res, setRes] = useState<any[] | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!q.trim()) {
      setRes(null);
      return;
    }
    setLoading(true);
    const t = window.setTimeout(() => {
      searchMedia(q.trim(), tab === "ALL" ? null : tab, settings.nsfw)
        .then((d) => setRes(d?.Page?.media ?? []))
        .catch(() => setRes([]))
        .finally(() => setLoading(false));
    }, 380);
    return () => window.clearTimeout(t);
  }, [q, tab, settings.nsfw]);

  return (
    <div className="space-y-8 pb-28">
      <header className="flex flex-wrap items-center justify-between gap-4 pt-1">
        <div>
          <p className="font-mono text-[11px] uppercase tracking-[0.22em] text-mut">Search AniList</p>
          <h1 className="mt-1.5 font-disp text-3xl font-bold tracking-tight">
            {q ? (
              <>
                Results for <span className="text-acc">“{q}”</span>
              </>
            ) : (
              "Type to search"
            )}
          </h1>
        </div>
        <Seg
          uid="search-tab"
          value={tab}
          onChange={setTab}
          options={[
            { id: "ALL", label: "All" },
            { id: "ANIME", label: "Anime", icon: Film },
            { id: "MANGA", label: "Manga" },
          ]}
        />
      </header>

      {!q ? (
        <EmptyState icon={Search} title="Search the whole AniList database" sub="Try “Frieren”, “Jujutsu Kaisen”, “Berserk”…" />
      ) : loading && !res ? (
        <div className="grid gap-x-4 gap-y-6" style={{ gridTemplateColumns: `repeat(auto-fill, minmax(${settings.density === "compact" ? "112px" : "142px"}, 1fr))` }}>
          {Array.from({ length: 12 }).map((_, i) => (
            <div key={i}>
              <Skel className="aspect-[2/3] w-full rounded-poster" />
              <Skel className="mt-2 h-3 w-4/5" />
            </div>
          ))}
        </div>
      ) : res && res.length ? (
        <div className={cn("grid gap-x-4 gap-y-6 transition-opacity", loading && "opacity-50")} style={{ gridTemplateColumns: `repeat(auto-fill, minmax(${settings.density === "compact" ? "112px" : "142px"}, 1fr))` }}>
          {res.map((m) => (
            <MediaCard key={`sr-${m.id}`} m={m} rowKey="sr" onOpen={onOpen} />
          ))}
        </div>
      ) : (
        <EmptyState icon={Search} title="No results" sub="Try a different spelling, or check the Anime/Manga filter." />
      )}
    </div>
  );
}
