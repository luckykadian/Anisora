import { motion } from "framer-motion";
import {
  Activity,
  AlignLeft,
  BarChart3,
  ChevronDown,
  ChevronRight,
  Clapperboard,
  ExternalLink,
  Heart,
  Layers,
  Link2,
  MessageSquare,
  Mic,
  Sparkles,
  Star,
  Tag,
  ThumbsUp,
  TrendingUp,
  Users,
} from "lucide-react";
import { useState, type ReactNode } from "react";
import { FORMAT_LABELS, fmt, fmtDate, RELATION_LABELS, SEASON_LABELS, STATUS_LABELS, titleOf } from "../api";
import { Art, Chip, RichText, SectionHead, StatPill, SPRING } from "../bits";
import { useOverlay } from "../overlay";
import { useApp } from "../store";
import { cn } from "../utils/cn";
import { personSeedFromCharEdge, personSeedFromStaffEdge } from "./Detail";

const SOURCE_LABELS: Record<string, string> = {
  ORIGINAL: "Original",
  MANGA: "Manga",
  LIGHT_NOVEL: "Light novel",
  VISUAL_NOVEL: "Visual novel",
  VIDEO_GAME: "Video game",
  NOVEL: "Novel",
  DOUJINSHI: "Doujinshi",
  ANIME: "Anime",
  WEB_NOVEL: "Web novel",
  LIVE_ACTION: "Live action",
  GAME: "Game",
  COMIC: "Comic",
  MULTIMEDIA_PROJECT: "Multimedia",
  PICTURE_BOOK: "Picture book",
  OTHER: "Other",
};

const COUNTRY: Record<string, string> = { JP: "Japan", KR: "South Korea", CN: "China", TW: "Taiwan" };

function Sec({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <motion.section initial={{ opacity: 0, y: 22 }} whileInView={{ opacity: 1, y: 0 }} viewport={{ once: true, margin: "-30px" }} transition={{ ...SPRING, delay: 0.03 }} className={className}>
      {children}
    </motion.section>
  );
}

function InfoItem({ label, value, mono }: { label: string; value: ReactNode; mono?: boolean }) {
  return (
    <div className="rounded-xl border border-line bg-bg1 px-3.5 py-2.5">
      <p className="text-[10px] font-semibold uppercase tracking-[0.14em] text-mut">{label}</p>
      <p className={cn("mt-1 truncate text-[13px] font-medium", mono && "font-mono tabular")}>{value ?? "—"}</p>
    </div>
  );
}

/* ------------------------------ character card ------------------------------ */

function CharCard({ edge }: { edge: any }) {
  const { hidden, openPerson } = useOverlay();
  const node = edge.node ?? {};
  const seed = personSeedFromCharEdge(edge);
  const va = edge.voiceActors?.[0];
  return (
    <motion.button
      layoutId={seed.layoutId}
      transition={SPRING}
      style={{ opacity: hidden.has(seed.layoutId) ? 0 : 1 }}
      whileHover={{ y: -3 }}
      onClick={() => openPerson(seed)}
      className="group relative flex items-center gap-3.5 rounded-2xl border border-line bg-bg1 p-3 text-left shadow-card transition-colors hover:border-accLine"
    >
      <Art src={node.image?.large} alt={node.name?.full ?? "?"} className="h-16 w-16 shrink-0 rounded-xl" />
      <div className="min-w-0 flex-1">
        <div className="flex items-center gap-2">
          <p className="truncate text-[13.5px] font-semibold">{node.name?.full}</p>
          <span className={cn("shrink-0 rounded-md px-1.5 py-0.5 text-[9px] font-bold uppercase tracking-wider", edge.role === "MAIN" ? "bg-accSoft text-acc" : "bg-bg2 text-mut")}>
            {edge.role === "MAIN" ? "Main" : "Support"}
          </span>
        </div>
        {node.name?.native && <p className="mt-0.5 truncate font-disp text-[11px] text-mut">{node.name.native}</p>}
        {va && (
          <p className="mt-1.5 flex items-center gap-1.5 text-[11.5px] text-mut">
            <Mic size={11} className="shrink-0 text-acc" />
            <span className="truncate">{va.name?.full}</span>
            <span className="shrink-0 rounded bg-bg2 px-1 py-px text-[8.5px] font-bold uppercase tracking-wider">JP</span>
          </p>
        )}
      </div>
      <ChevronRight size={15} className="shrink-0 text-mut transition-all group-hover:translate-x-0.5 group-hover:text-acc" />
    </motion.button>
  );
}

function StaffCard({ edge }: { edge: any }) {
  const { hidden, openPerson } = useOverlay();
  const node = edge.node ?? {};
  const seed = personSeedFromStaffEdge(edge);
  return (
    <motion.button
      layoutId={seed.layoutId}
      transition={SPRING}
      style={{ opacity: hidden.has(seed.layoutId) ? 0 : 1 }}
      whileHover={{ y: -3 }}
      onClick={() => openPerson(seed)}
      className="group flex items-center gap-3.5 rounded-2xl border border-line bg-bg1 p-3 text-left shadow-card transition-colors hover:border-accLine"
    >
      <Art src={node.image?.large} alt={node.name?.full ?? "?"} className="h-14 w-14 shrink-0 rounded-xl" />
      <div className="min-w-0 flex-1">
        <p className="truncate text-[13.5px] font-semibold">{node.name?.full}</p>
        <p className="mt-1 truncate text-[11px] font-medium text-acc">{edge.role}</p>
      </div>
      <ChevronRight size={15} className="shrink-0 text-mut transition-all group-hover:translate-x-0.5 group-hover:text-acc" />
    </motion.button>
  );
}

/* ------------------------------- flow card ---------------------------------- */

function FlowCard({ node, caption, rowKey, onOpen }: { node: any; caption?: string; rowKey: string; onOpen: (m: any, lid: string | null) => void }) {
  const { settings } = useApp();
  const lid = `${rowKey}-${node.id}`;
  return (
    <motion.button whileHover={{ y: -4 }} transition={SPRING} onClick={() => onOpen(node, lid)} className="group w-[104px] shrink-0 text-left">
      <motion.div layoutId={lid} transition={SPRING} className="relative aspect-[2/3] overflow-hidden rounded-xl border border-line bg-bg2 shadow-card">
        <Art src={node.coverImage?.large} alt={titleOf(node, settings.titleLang)} color={node.coverImage?.color} className="h-full w-full transition-transform duration-500 group-hover:scale-[1.06]" />
        {caption && (
          <span className="absolute inset-x-1.5 bottom-1.5 truncate rounded-lg bg-black/65 px-1.5 py-1 text-center text-[9px] font-bold uppercase tracking-wider text-white backdrop-blur-sm">{caption}</span>
        )}
      </motion.div>
      <p className="mt-1.5 line-clamp-2 text-[11px] font-medium leading-tight">{titleOf(node, settings.titleLang)}</p>
      {node.averageScore && <p className="mt-0.5 font-mono text-[10px] text-mut tabular">★ {node.averageScore}%</p>}
    </motion.button>
  );
}

/* ---------------------------------- tab ------------------------------------ */

export function InfoTab({ d, onOpenMedia }: { d: any; onOpenMedia: (m: any, lid: string | null) => void }) {
  const [expanded, setExpanded] = useState(false);
  const isAnime = d.type === "ANIME";
  const chars = (d.characters?.edges ?? []).filter((e: any) => e.node);
  const staff = (d.staff?.edges ?? []).filter((e: any) => e.node);
  const relations = (d.relations?.edges ?? []).filter((e: any) => e.node);
  const recs = (d.recommendations?.nodes ?? []).map((n: any) => ({ node: n.mediaRecommendation, rating: n.rating })).filter((r: any) => r.node);
  const reviews = (d.reviews?.nodes ?? []).filter((r: any) => r?.user);
  const tags = (d.tags ?? []).filter((t: any) => !t.isMediaSpoiler).slice(0, 16);
  const scoreDist = (d.stats?.scoreDistribution ?? []).slice().sort((a: any, b: any) => a.score - b.score);
  const maxScore = Math.max(1, ...scoreDist.map((x: any) => x.amount));
  const statusDist = d.stats?.statusDistribution ?? [];
  const maxStatus = Math.max(1, ...statusDist.map((x: any) => x.amount));
  const studios = (d.studios?.nodes ?? []).map((s: any) => s.name).filter(Boolean);

  return (
    <div className="space-y-14">
      {/* stats strip */}
      <Sec>
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5">
          {d.averageScore != null && <StatPill icon={Star} label="Average score" value={`${d.averageScore}%`} />}
          {d.meanScore != null && <StatPill icon={Activity} label="Mean score" value={`${d.meanScore}%`} />}
          <StatPill icon={TrendingUp} label="Popularity" value={fmt(d.popularity)} />
          <StatPill icon={Heart} label="Favourites" value={fmt(d.favourites)} />
          <StatPill icon={Clapperboard} label="Status" value={<span className="font-sans text-[13px] font-bold">{STATUS_LABELS[d.status] ?? d.status}</span>} />
        </div>
      </Sec>

      {/* about */}
      {d.description && (
        <Sec>
          <SectionHead icon={AlignLeft} title="Synopsis" />
          <div className="relative">
            <div className={cn("transition-all duration-500", !expanded && "max-h-[190px] overflow-hidden")}>
              <RichText html={d.description} className="text-[14.5px]" />
              {d.hashtag && <p className="mt-2 font-mono text-[12px] text-acc">{d.hashtag}</p>}
            </div>
            {!expanded && <div className="pointer-events-none absolute inset-x-0 bottom-0 h-20 bg-gradient-to-t from-bg0 to-transparent" />}
          </div>
          <button onClick={() => setExpanded((v) => !v)} className="mt-3 flex items-center gap-1.5 text-[12.5px] font-semibold text-acc transition hover:brightness-110">
            {expanded ? "Show less" : "Read more"}
            <ChevronDown size={14} className={cn("transition-transform", expanded && "rotate-180")} />
          </button>
        </Sec>
      )}

      {/* info grid */}
      <Sec>
        <SectionHead icon={Clapperboard} title="Details" />
        <div className="grid grid-cols-2 gap-2.5 sm:grid-cols-3 lg:grid-cols-4">
          <InfoItem label="Format" value={FORMAT_LABELS[d.format] ?? d.format} />
          {isAnime ? (
            <>
              <InfoItem label="Episodes" value={d.episodes ?? "TBA"} mono />
              <InfoItem label="Duration" value={d.duration ? `${d.duration} min` : "—"} mono />
            </>
          ) : (
            <>
              <InfoItem label="Chapters" value={d.chapters ?? "TBA"} mono />
              <InfoItem label="Volumes" value={d.volumes ?? "TBA"} mono />
            </>
          )}
          <InfoItem label="Status" value={STATUS_LABELS[d.status] ?? d.status} />
          {d.season && (
            <InfoItem label="Season" value={`${SEASON_LABELS[d.season]} ${d.seasonYear}`} />
          )}
          <InfoItem label="Start date" value={fmtDate(d.startDate)} />
          <InfoItem label="End date" value={fmtDate(d.endDate)} />
          {studios.length > 0 && <InfoItem label={isAnime ? "Studio" : "Publisher"} value={studios.join(", ")} />}
          <InfoItem label="Source" value={SOURCE_LABELS[d.source] ?? d.source ?? "—"} />
          <InfoItem label="Country" value={COUNTRY[d.countryOfOrigin] ?? "—"} />
          <InfoItem label="Native title" value={d.title?.native} />
          {d.synonyms?.length > 0 && <InfoItem label="Synonyms" value={d.synonyms.join(", ")} />}
        </div>
      </Sec>

      {/* genres + tags */}
      {(d.genres?.length || tags.length) && (
        <Sec>
          <SectionHead icon={Tag} title="Genres &amp; tags" />
          <div className="flex flex-wrap gap-2">
            {(d.genres ?? []).map((g: string) => (
              <Chip key={g} accent className="px-3.5 py-1.5 text-[12px] font-semibold">
                {g}
              </Chip>
            ))}
          </div>
          {tags.length > 0 && (
            <div className="mt-3 flex flex-wrap gap-2">
              {tags.map((t: any) => (
                <span key={t.name} className="relative inline-flex items-center gap-1.5 overflow-hidden rounded-full border border-line bg-bg1 px-3 py-1.5 text-[11.5px] text-mut">
                  <span className="absolute inset-y-0 left-0 bg-accSoft" style={{ width: `${Math.min(100, t.rank ?? 0)}%` }} />
                  <span className="relative">{t.name}</span>
                  <span className="relative font-mono text-[10px] font-bold text-acc tabular">{t.rank}%</span>
                </span>
              ))}
            </div>
          )}
        </Sec>
      )}

      {/* relations */}
      {relations.length > 0 && (
        <Sec>
          <SectionHead icon={Layers} title="Relations" sub="Connected entries in the series" />
          <div className="maskx -mx-1 flex gap-4 overflow-x-auto px-1 pb-3 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
            {relations.map((e: any) => (
              <FlowCard key={`rel-${e.node.id}`} node={e.node} caption={RELATION_LABELS[e.relationType] ?? e.relationType} rowKey={`rel-${d.id}`} onOpen={onOpenMedia} />
            ))}
          </div>
        </Sec>
      )}

      {/* characters */}
      {chars.length > 0 && (
        <Sec>
          <SectionHead icon={Users} title="Cast" sub="Tap a card — it unfolds in place" />
          <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
            {chars.map((e: any) => (
              <CharCard key={`ch-${e.node.id}`} edge={e} />
            ))}
          </div>
        </Sec>
      )}

      {/* staff */}
      {staff.length > 0 && (
        <Sec>
          <SectionHead icon={Clapperboard} title="Staff" sub="The people behind it" />
          <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
            {staff.map((e: any) => (
              <StaffCard key={`st-${e.node.id}`} edge={e} />
            ))}
          </div>
        </Sec>
      )}

      {/* community stats */}
      {scoreDist.length > 0 && (
        <Sec>
          <SectionHead icon={BarChart3} title="Community pulse" />
          <div className="grid gap-4 lg:grid-cols-2">
            <div className="rounded-2xl border border-line bg-bg1 p-5">
              <p className="mb-5 text-[12px] font-semibold uppercase tracking-[0.14em] text-mut">Score distribution</p>
              <div className="flex h-[110px] items-end gap-1 sm:gap-1.5">
                {scoreDist.map((x: any, i: number) => (
                  <div key={x.score} className="group relative flex h-full flex-1 items-end">
                    <motion.div
                      initial={{ scaleY: 0 }}
                      whileInView={{ scaleY: 1 }}
                      viewport={{ once: true }}
                      transition={{ ...SPRING, delay: i * 0.03 }}
                      className={cn("w-full origin-bottom rounded-t-[4px]", x.score >= 70 ? "bg-gradient-to-t from-acc/40 to-acc" : "bg-bg2")}
                      style={{ height: `${(x.amount / maxScore) * 100}%` }}
                    />
                    <span className="pointer-events-none absolute -top-7 left-1/2 -translate-x-1/2 whitespace-nowrap rounded-lg border border-line bg-bg2 px-1.5 py-0.5 font-mono text-[9.5px] text-txt opacity-0 transition group-hover:opacity-100 tabular">
                      {fmt(x.amount)}
                    </span>
                  </div>
                ))}
              </div>
              <div className="mt-2 flex gap-1 sm:gap-1.5">
                {scoreDist.map((x: any) => (
                  <p key={x.score} className="flex-1 text-center font-mono text-[8.5px] text-mut tabular">
                    {x.score}
                  </p>
                ))}
              </div>
            </div>
            <div className="rounded-2xl border border-line bg-bg1 p-5">
              <p className="mb-5 text-[12px] font-semibold uppercase tracking-[0.14em] text-mut">List status</p>
              <div className="space-y-3">
                {statusDist.map((x: any) => (
                  <div key={x.status}>
                    <div className="mb-1 flex items-center justify-between text-[11px]">
                      <span className="font-medium capitalize">{String(x.status).toLowerCase()}</span>
                      <span className="font-mono text-mut tabular">{fmt(x.amount)}</span>
                    </div>
                    <div className="h-1.5 overflow-hidden rounded-full bg-bg2">
                      <motion.div initial={{ width: 0 }} whileInView={{ width: `${(x.amount / maxStatus) * 100}%` }} viewport={{ once: true }} transition={{ ...SPRING }} className="h-full rounded-full bg-acc" />
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </Sec>
      )}

      {/* reviews */}
      {reviews.length > 0 && (
        <Sec>
          <SectionHead icon={MessageSquare} title="Community reviews" />
          <div className="grid gap-3 md:grid-cols-2">
            {reviews.map((r: any) => (
              <a key={r.id} href={`https://anilist.co/review/${r.id}`} target="_blank" rel="noreferrer" className="group rounded-2xl border border-line bg-bg1 p-4 transition hover:border-accLine">
                <div className="flex items-center gap-2.5">
                  <Art src={r.user.avatar?.medium} alt={r.user.name} className="h-8 w-8 rounded-full" />
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-[12.5px] font-semibold">{r.user.name}</p>
                    <p className="text-[10.5px] text-mut">AniList review</p>
                  </div>
                  <span className="flex items-center gap-1 rounded-full bg-accSoft px-2 py-1 font-mono text-[10.5px] font-bold text-acc tabular">
                    <ThumbsUp size={10} /> {fmt(r.rating)}
                  </span>
                </div>
                <p className="mt-3 line-clamp-3 text-[12.5px] italic leading-relaxed text-mut">“{r.summary}”</p>
              </a>
            ))}
          </div>
        </Sec>
      )}

      {/* recommendations */}
      {recs.length > 0 && (
        <Sec>
          <SectionHead icon={Sparkles} title="You may also like" sub="Recommended by the community" />
          <div className="maskx -mx-1 flex gap-4 overflow-x-auto px-1 pb-3 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
            {recs.map((r: any) => (
              <FlowCard key={`rec-${r.node.id}`} node={r.node} rowKey={`rec-${d.id}`} onOpen={onOpenMedia} />
            ))}
          </div>
        </Sec>
      )}

      {/* external links */}
      {(d.externalLinks?.length || d.trailer) && (
        <Sec>
          <SectionHead icon={Link2} title="Around the web" />
          <div className="flex flex-wrap gap-2">
            {d.trailer?.site === "youtube" && (
              <a href={`https://www.youtube.com/watch?v=${d.trailer.id}`} target="_blank" rel="noreferrer" className="flex items-center gap-2 rounded-xl border border-red-400/30 bg-red-400/10 px-3.5 py-2 text-[12px] font-semibold text-red-300 transition hover:bg-red-400/20">
                <Clapperboard size={13} /> Watch trailer
              </a>
            )}
            {(d.externalLinks ?? []).map((l: any) => (
              <a key={l.id} href={l.url} target="_blank" rel="noreferrer" className="flex items-center gap-2 rounded-xl border border-line bg-bg1 px-3.5 py-2 text-[12px] font-medium text-mut transition hover:border-accLine hover:text-txt">
                <ExternalLink size={12} /> {l.site}
              </a>
            ))}
          </div>
        </Sec>
      )}
    </div>
  );
}
