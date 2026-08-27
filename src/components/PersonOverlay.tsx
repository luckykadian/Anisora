import { AnimatePresence, motion } from "framer-motion";
import { Cake, Droplets, ExternalLink, Heart, Home, Languages, Mic, User, Users, X } from "lucide-react";
import { useEffect, useState, type ReactNode } from "react";
import { fetchCharacter, fetchStaff, fmt, fmtDate, hashHue, titleOf } from "../api";
import { Art, RichText, Skel, SPRING } from "../bits";
import { useOverlay } from "../overlay";
import { useApp } from "../store";
import type { PersonSeed, VA } from "../types";
import { cn } from "../utils/cn";

function Fact({ icon: Icon, label, value }: { icon: any; label: string; value?: ReactNode }) {
  if (value == null || value === "") return null;
  return (
    <span className="flex items-center gap-2 rounded-xl border border-line bg-bg2/70 px-3 py-2 text-[12px]">
      <Icon size={13} className="shrink-0 text-acc" />
      <span className="text-mut">{label}</span>
      <span className="truncate font-semibold">{value}</span>
    </span>
  );
}

function VaChip({ va }: { va: VA }) {
  const { hidden, openPerson } = useOverlay();
  const lid = `person-staff-${va.id}`;
  return (
    <motion.button
      layoutId={lid}
      transition={SPRING}
      style={{ opacity: hidden.has(lid) ? 0 : 1 }}
      whileHover={{ y: -2 }}
      onClick={() => openPerson({ kind: "staff", id: va.id, layoutId: lid, name: va.name, image: va.image ?? null, role: "Voice actor" })}
      className="flex items-center gap-2.5 rounded-full border border-line bg-bg2/70 py-1 pl-1 pr-3.5 transition hover:border-accLine"
    >
      <Art src={va.image} alt={va.name} className="h-8 w-8 rounded-full" />
      <span className="text-[12px] font-semibold">{va.name}</span>
      <span className="flex items-center gap-1 text-[10px] font-bold uppercase tracking-wide text-mut">
        <Mic size={10} className="text-acc" /> JA
      </span>
    </motion.button>
  );
}

export function PersonOverlay({ item, idx, total, onClose, onOpenMedia }: { item: PersonSeed; idx: number; total: number; onClose: () => void; onOpenMedia: (m: any) => void }) {
  const { settings } = useApp();
  const isTop = idx === total - 1;
  const [data, setData] = useState<any>(null);
  const [failed, setFailed] = useState(false);
  const [expanded, setExpanded] = useState(false);

  useEffect(() => {
    let on = true;
    setData(null);
    setFailed(false);
    const load = item.kind === "character" ? fetchCharacter : fetchStaff;
    load(item.id)
      .then((r) => on && setData(r))
      .catch(() => on && setFailed(true));
    return () => {
      on = false;
    };
  }, [item.uid, item.id, item.kind]);

  const name = data?.name?.full ?? item.name;
  const native = data?.name?.native ?? item.native;
  const alts: string[] = data?.name?.alternative ?? [];
  const hue = hashHue(name);
  const isChar = item.kind === "character";
  const mediaList: { node: any; role?: string }[] = isChar
    ? (data?.media?.nodes ?? []).map((n: any) => ({ node: n }))
    : (data?.staffMedia?.edges ?? []).map((e: any) => ({ node: e.node, role: e.staffRole }));

  return (
    <div className="fixed inset-0" style={{ zIndex: 60 + idx * 2 }}>
      {/* backdrop */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        onClick={isTop ? onClose : undefined}
        className={cn("absolute inset-0 backdrop-blur-[5px]", idx === 0 ? "bg-black/65" : "bg-black/35")}
      />
      {/* scroll wrapper */}
      <div className="pointer-events-none absolute inset-0 overflow-y-auto p-3 pt-[6vh] sm:p-6" style={{ pointerEvents: isTop ? "auto" : "none" }}>
        <motion.div
          layoutId={item.layoutId}
          transition={SPRING}
          initial={false}
          animate={{ scale: isTop ? 1 : 0.95, y: isTop ? 0 : -14, filter: isTop ? "brightness(1)" : "brightness(0.45)" }}
          exit={{ opacity: 0 }}
          className="relative mx-auto w-full max-w-[820px] overflow-hidden rounded-3xl border border-line bg-bg1 shadow-pop"
        >
          <motion.div key={`c-${item.uid}`} initial={{ opacity: 0 }} animate={{ opacity: 1, transition: { delay: 0.16 } }} exit={{ opacity: 0, transition: { duration: 0.08 } }}>
            {/* header band */}
            <div className="absolute inset-x-0 top-0 h-36" style={{ background: `linear-gradient(125deg, hsl(${hue} 55% 24% / .85), hsl(${(hue + 60) % 360} 45% 14% / .35) 55%, transparent)` }} />
            <div className="noise absolute inset-x-0 top-0 h-36" />
            {isTop && (
              <button onClick={onClose} className="absolute right-4 top-4 z-20 grid h-9 w-9 place-items-center rounded-full border border-white/15 bg-black/40 text-white backdrop-blur-md transition hover:bg-black/70" aria-label="close">
                <X size={15} />
              </button>
            )}

            <div className="relative px-5 pb-7 pt-12 sm:px-8">
              {/* head */}
              <div className="flex flex-col gap-5 sm:flex-row sm:items-end">
                <div className="h-[126px] w-[126px] shrink-0 overflow-hidden rounded-3xl border-4 border-bg1 shadow-pop">
                  <Art eager src={data?.image?.large ?? item.image} alt={name} className="h-full w-full" />
                </div>
                <div className="min-w-0 flex-1 sm:pb-1">
                  <p className="flex items-center gap-1.5 font-mono text-[10px] font-bold uppercase tracking-[0.22em] text-acc">
                    {isChar ? <User size={11} /> : <Users size={11} />}
                    {isChar ? "Character" : "Staff"}
                    {item.role && <span className="ml-1 normal-case tracking-normal text-mut">· {item.role === "MAIN" ? "Main role" : item.role === "SUPPORTING" ? "Supporting role" : item.role}</span>}
                  </p>
                  <h2 className="mt-1.5 font-disp text-[26px] font-bold leading-[1.05] tracking-tight sm:text-[34px]">{name}</h2>
                  <p className="mt-1.5 truncate text-[13px] text-mut">
                    {native}
                    {alts.length > 0 && <span className="opacity-70"> · {alts.slice(0, 3).join(" · ")}</span>}
                  </p>
                </div>
                <a
                  href={data?.siteUrl ?? `https://anilist.co/${isChar ? "character" : "staff"}/${item.id}`}
                  target="_blank"
                  rel="noreferrer"
                  className="flex shrink-0 items-center gap-1.5 self-start rounded-xl border border-line bg-bg2/70 px-3 py-2 text-[11.5px] font-semibold text-mut transition hover:border-accLine hover:text-txt sm:self-auto"
                >
                  AniList <ExternalLink size={11} />
                </a>
              </div>

              {/* facts */}
              <div className="mt-6 flex flex-wrap gap-2">
                {!data && !failed ? (
                  <>
                    <Skel className="h-9 w-28" />
                    <Skel className="h-9 w-24" />
                    <Skel className="h-9 w-32" />
                  </>
                ) : (
                  <>
                    <Fact icon={User} label="Gender" value={data?.gender} />
                    <Fact icon={Cake} label="Age" value={data?.age} />
                    {isChar && <Fact icon={Droplets} label="Blood" value={data?.bloodType} />}
                    <Fact icon={Cake} label="Birthday" value={data?.dateOfBirth?.year || data?.dateOfBirth?.month ? fmtDate(data.dateOfBirth) : null} />
                    {!isChar && <Fact icon={Home} label="Hometown" value={data?.homeTown} />}
                    {!isChar && <Fact icon={Languages} label="Language" value={data?.languageV2} />}
                    <Fact icon={Heart} label="Favourites" value={data?.favourites != null ? fmt(data.favourites) : null} />
                  </>
                )}
              </div>

              {/* description */}
              <div className="mt-6">
                {!data && !failed ? (
                  <div className="space-y-2">
                    <Skel className="h-3.5 w-full" />
                    <Skel className="h-3.5 w-11/12" />
                    <Skel className="h-3.5 w-4/5" />
                  </div>
                ) : failed ? (
                  <p className="text-[13px] text-mut">Couldn't load full details — you're offline. The essentials are above.</p>
                ) : data?.description ? (
                  <>
                    <div className={cn("relative transition-all", !expanded && "max-h-[150px] overflow-hidden")}>
                      <RichText html={data.description} className="text-[13.5px]" />
                      {!expanded && <div className="pointer-events-none absolute inset-x-0 bottom-0 h-16 bg-gradient-to-t from-bg1 to-transparent" />}
                    </div>
                    <button onClick={() => setExpanded((v) => !v)} className="mt-2 text-[12px] font-semibold text-acc">
                      {expanded ? "Show less" : "Read more"} <span className="ml-0.5 text-[9px] text-mut">(hover blurred text to reveal spoilers)</span>
                    </button>
                  </>
                ) : (
                  <p className="text-[13px] italic text-mut">No biography available yet.</p>
                )}
              </div>

              {/* voice actors */}
              {isChar && (item.vas?.length ?? 0) > 0 && (
                <div className="mt-7">
                  <p className="mb-3 flex items-center gap-2 text-[11px] font-bold uppercase tracking-[0.18em] text-mut">
                    <Mic size={12} className="text-acc" /> Voiced by
                  </p>
                  <div className="flex flex-wrap gap-2.5">
                    {item.vas!.map((va) => (
                      <VaChip key={`va-${va.id}`} va={va} />
                    ))}
                  </div>
                </div>
              )}

              {/* media */}
              <div className="mt-8">
                <p className="mb-3 flex items-center gap-2 text-[11px] font-bold uppercase tracking-[0.18em] text-mut">
                  {isChar ? "Appears in" : "Known for"}
                  {mediaList.length > 0 && <span className="rounded-md bg-bg2 px-1.5 py-px font-mono text-[10px]">{mediaList.length}</span>}
                </p>
                {!data && !failed ? (
                  <div className="grid grid-cols-3 gap-3 sm:grid-cols-4">
                    {Array.from({ length: 4 }).map((_, i) => (
                      <Skel key={i} className="aspect-[2/3] w-full rounded-xl" />
                    ))}
                  </div>
                ) : (
                  <div className="grid grid-cols-3 gap-3 sm:grid-cols-4">
                    {mediaList.map(({ node, role }) => (
                      <motion.button key={`pm-${node.id}-${role ?? ""}`} whileHover={{ y: -3 }} transition={SPRING} onClick={() => onOpenMedia(node)} className="group min-w-0 text-left">
                        <div className="relative aspect-[2/3] overflow-hidden rounded-xl border border-line bg-bg2 shadow-card">
                          <Art src={node.coverImage?.medium} alt={titleOf(node, settings.titleLang)} color={node.coverImage?.color} className="h-full w-full transition-transform duration-500 group-hover:scale-[1.06]" />
                          {role && <span className="absolute inset-x-1 bottom-1 truncate rounded-md bg-black/65 px-1.5 py-0.5 text-center text-[8.5px] font-bold uppercase tracking-wide text-white backdrop-blur-sm">{role}</span>}
                        </div>
                        <p className="mt-1.5 truncate text-[11px] font-medium">{titleOf(node, settings.titleLang)}</p>
                      </motion.button>
                    ))}
                  </div>
                )}
              </div>
            </div>
          </motion.div>
        </motion.div>
      </div>
    </div>
  );
}

export function PersonStack({ persons, onPop, onOpenMedia }: { persons: PersonSeed[]; onPop: () => void; onOpenMedia: (m: any) => void }) {
  return (
    <AnimatePresence>
      {persons.map((p, i) => (
        <PersonOverlay key={p.uid} item={p} idx={i} total={persons.length} onClose={onPop} onOpenMedia={onOpenMedia} />
      ))}
    </AnimatePresence>
  );
}
