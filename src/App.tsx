import { AnimatePresence, LayoutGroup, motion, MotionConfig } from "framer-motion";
import { Bell, BookOpen, CloudOff, Compass, Film, LogOut, Search, Settings as SettingsIcon, User, X } from "lucide-react";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Kbd, Logo, SPRING, ToastStack } from "./bits";
import { DetailPage } from "./components/Detail";
import { Home, SearchView } from "./components/Home";
import { Onboarding } from "./components/Onboarding";
import { PersonStack } from "./components/PersonOverlay";
import { SettingsPage } from "./components/Settings";
import { OverlayContext } from "./overlay";
import { AppProvider, useApp } from "./store";
import type { DetailRef, PersonSeed } from "./types";
import { cn } from "./utils/cn";

type Route = "anime" | "manga" | "search" | "settings";

const NAV: { id: Route; icon: any; label: string }[] = [
  { id: "anime", icon: Film, label: "Anime" },
  { id: "manga", icon: BookOpen, label: "Manga" },
  { id: "search", icon: Compass, label: "Search" },
  { id: "settings", icon: SettingsIcon, label: "Settings" },
];

function Shell() {
  const { session, settings, library, logout, offline, toast } = useApp();
  const [route, setRoute] = useState<Route>("anime");
  const [q, setQ] = useState("");
  const [detailStack, setDetailStack] = useState<DetailRef[]>([]);
  const [persons, setPersons] = useState<PersonSeed[]>([]);
  const [menuOpen, setMenuOpen] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  /* ---- theme side effects ---- */
  useEffect(() => {
    const el = document.documentElement;
    el.dataset.theme = settings.theme;
    el.style.setProperty("--acc", settings.accent);
    el.style.setProperty("--posterR", `${settings.posterRadius}px`);
  }, [settings.theme, settings.accent, settings.posterRadius]);

  /* ---- overlay navigation ---- */
  const openMedia = useCallback((m: any, layoutId: string | null) => {
    const id = typeof m === "number" ? m : m?.id;
    if (!id) return;
    setDetailStack((s) => (s[s.length - 1]?.id === id ? s : [...s, { id, layoutId }]));
  }, []);

  const openPerson = useCallback((p: Omit<PersonSeed, "uid">) => {
    setPersons((s) => [...s, { ...p, uid: `${p.kind}-${p.id}-${Math.random().toString(36).slice(2, 9)}` }]);
  }, []);

  const hidden = useMemo(() => {
    const set = new Set<string>();
    for (const d of detailStack) if (d.layoutId) set.add(d.layoutId);
    for (const p of persons) set.add(p.layoutId);
    return set;
  }, [detailStack, persons]);

  const anyOverlay = detailStack.length > 0 || persons.length > 0;
  useEffect(() => {
    document.documentElement.style.overflow = anyOverlay ? "hidden" : "";
    return () => {
      document.documentElement.style.overflow = "";
    };
  }, [anyOverlay]);

  useEffect(() => {
    const h = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        if (persons.length) setPersons((s) => s.slice(0, -1));
        else if (detailStack.length) setDetailStack((s) => s.slice(0, -1));
      }
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === "k") {
        e.preventDefault();
        setRoute("search");
        inputRef.current?.focus();
      }
    };
    window.addEventListener("keydown", h);
    return () => window.removeEventListener("keydown", h);
  }, [persons.length, detailStack.length]);

  const openFromPerson = useCallback(
    (m: any) => {
      setPersons([]);
      openMedia(m, null);
    },
    [openMedia],
  );

  const topDetail = detailStack[detailStack.length - 1];
  const ctxValue = useMemo(() => ({ hidden, openPerson }), [hidden, openPerson]);

  if (!session) return <Onboarding />;

  const watching = Object.values(library).filter((e) => e.status === "CURRENT").length;

  return (
    <MotionConfig reducedMotion={settings.reduceMotion ? "always" : "never"}>
      <LayoutGroup>
        <OverlayContext.Provider value={ctxValue}>
          <div className="flex min-h-dvh">
            {/* ------------------------- desktop rail ------------------------- */}
            <aside className="sticky top-0 z-40 hidden h-dvh w-[86px] shrink-0 flex-col items-center border-r border-line bg-bg0/70 py-5 backdrop-blur md:flex">
              <button onClick={() => setRoute("anime")} aria-label="home">
                <Logo size={30} text={false} />
              </button>
              <nav className="mt-10 flex flex-1 flex-col items-center gap-2">
                {NAV.map((n) => {
                  const active = route === n.id;
                  return (
                    <button
                      key={n.id}
                      onClick={() => {
                        setRoute(n.id);
                        if (n.id === "search") window.setTimeout(() => inputRef.current?.focus(), 60);
                      }}
                      className={cn("relative flex h-[60px] w-[62px] flex-col items-center justify-center gap-1 rounded-2xl transition-colors", active ? "text-acc" : "text-mut hover:text-txt")}
                      aria-label={n.label}
                    >
                      {active && <motion.span layoutId="rail-pill" transition={SPRING} className="absolute inset-0 rounded-2xl bg-accSoft ring-1 ring-accLine" />}
                      <n.icon size={18} className="relative z-10" />
                      <span className="relative z-10 text-[9.5px] font-bold uppercase tracking-wider">{n.label}</span>
                    </button>
                  );
                })}
              </nav>
              <div className="relative">
                <button onClick={() => setMenuOpen((v) => !v)} className="grid h-11 w-11 place-items-center rounded-2xl border border-line bg-bg1 font-disp text-[14px] font-bold text-acc transition hover:border-accLine" aria-label="account">
                  {(session.user.name ?? "G").slice(0, 1).toUpperCase()}
                </button>
                <AnimatePresence>
                  {menuOpen && (
                    <>
                      <motion.div className="fixed inset-0 z-40" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={() => setMenuOpen(false)} />
                      <motion.div initial={{ opacity: 0, x: -6, scale: 0.96 }} animate={{ opacity: 1, x: 0, scale: 1 }} exit={{ opacity: 0, x: -4, scale: 0.97 }} transition={SPRING} className="absolute bottom-0 left-14 z-50 w-[230px] overflow-hidden rounded-2xl border border-line bg-bg1 shadow-pop">
                        <div className="border-b border-line p-4">
                          <p className="truncate text-[14px] font-bold">{session.user.name}</p>
                          <p className="mt-0.5 text-[11px] text-mut">{session.guest ? "Guest profile" : "anilist.co user"} · {watching} in progress</p>
                        </div>
                        <div className="p-1.5">
                          <button onClick={() => { setMenuOpen(false); setRoute("settings"); }} className="flex w-full items-center gap-2.5 rounded-xl px-3 py-2.5 text-[13px] font-medium transition hover:bg-bg2">
                            <User size={15} className="text-mut" /> Profile &amp; settings
                          </button>
                          <button onClick={() => { setMenuOpen(false); logout(); toast("Signed out — see you soon", "info"); }} className="flex w-full items-center gap-2.5 rounded-xl px-3 py-2.5 text-[13px] font-medium text-rose-300 transition hover:bg-rose-400/10">
                            <LogOut size={15} /> Log out
                          </button>
                        </div>
                      </motion.div>
                    </>
                  )}
                </AnimatePresence>
              </div>
            </aside>

            {/* ------------------------------ main ------------------------------ */}
            <main className="min-w-0 flex-1">
              <header className="sticky top-0 z-30 border-b border-line bg-bg0/75 backdrop-blur-xl">
                <div className="mx-auto flex h-16 w-full max-w-[1440px] items-center gap-3 px-4 sm:px-6 lg:px-10">
                  <div className="md:hidden">
                    <Logo size={26} text={false} />
                  </div>
                  <div className="relative w-full max-w-xl flex-1">
                    <Search size={15} className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-mut" />
                    <input
                      ref={inputRef}
                      value={q}
                      onChange={(e) => {
                        setQ(e.target.value);
                        if (route !== "search") setRoute("search");
                      }}
                      onFocus={() => route !== "search" && setRoute("search")}
                      placeholder="Search anime, manga — anything on AniList…"
                      className="h-10 w-full rounded-2xl border border-line bg-bg1 pl-10 pr-14 text-[13.5px] outline-none transition placeholder:text-mut/70 focus:border-accLine focus:ring-2 focus:ring-accSoft"
                    />
                    <span className="absolute right-3 top-1/2 hidden -translate-y-1/2 items-center gap-1 sm:flex">
                      {q ? (
                        <button onClick={() => setQ("")} className="grid h-6 w-6 place-items-center rounded-lg text-mut hover:bg-bg2 hover:text-txt" aria-label="clear">
                          <X size={13} />
                        </button>
                      ) : (
                        <Kbd>⌘K</Kbd>
                      )}
                    </span>
                  </div>
                  <div className="ml-auto flex items-center gap-2">
                    {offline && (
                      <span className="hidden items-center gap-1.5 rounded-full border border-amber-400/30 bg-amber-400/10 px-3 py-1.5 text-[11px] font-semibold text-amber-300 sm:flex">
                        <CloudOff size={12} /> Offline
                      </span>
                    )}
                    <button
                      onClick={() => toast("You're all caught up — no new notifications", "info")}
                      className="relative grid h-10 w-10 place-items-center rounded-2xl border border-line bg-bg1 text-mut transition hover:border-accLine hover:text-txt"
                      aria-label="notifications"
                    >
                      <Bell size={16} />
                      <span className="absolute right-2.5 top-2.5 h-1.5 w-1.5 rounded-full bg-acc" />
                    </button>
                  </div>
                </div>
              </header>

              <div className="mx-auto w-full max-w-[1440px] px-4 pt-6 sm:px-6 lg:px-10">
                <AnimatePresence mode="wait">
                  <motion.div key={route} initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -8 }} transition={{ duration: 0.18 }}>
                    {route === "anime" && <Home type="ANIME" onOpen={openMedia} />}
                    {route === "manga" && <Home type="MANGA" onOpen={openMedia} />}
                    {route === "search" && <SearchView q={q} onOpen={openMedia} />}
                    {route === "settings" && <SettingsPage />}
                  </motion.div>
                </AnimatePresence>
              </div>
            </main>

            {/* ------------------------- mobile bottom nav ------------------------- */}
            <nav className="fixed inset-x-0 bottom-0 z-40 border-t border-line bg-bg0/85 backdrop-blur-xl md:hidden" style={{ paddingBottom: "env(safe-area-inset-bottom)" }}>
              <div className="grid grid-cols-4">
                {NAV.map((n) => {
                  const active = route === n.id;
                  return (
                    <button
                      key={n.id}
                      onClick={() => {
                        setRoute(n.id);
                        if (n.id === "search") window.setTimeout(() => inputRef.current?.focus(), 60);
                      }}
                      className={cn("relative flex flex-col items-center gap-1 py-2.5 transition-colors", active ? "text-acc" : "text-mut")}
                    >
                      {active && <motion.span layoutId="mnav-pill" transition={SPRING} className="absolute inset-x-5 top-0 h-[2.5px] rounded-full bg-acc shadow-glow" />}
                      <n.icon size={19} />
                      <span className="text-[9.5px] font-bold uppercase tracking-wider">{n.label}</span>
                    </button>
                  );
                })}
              </div>
            </nav>

            {/* ------------------------------ overlays ------------------------------ */}
            <AnimatePresence>
              {topDetail && (
                <DetailPage
                  key={`detail-${topDetail.id}-${detailStack.length}`}
                  dref={topDetail}
                  depth={detailStack.length - 1}
                  onBack={() => setDetailStack((s) => s.slice(0, -1))}
                  onCloseAll={() => setDetailStack([])}
                  onOpenMedia={openMedia}
                  onGoHome={() => setDetailStack([])}
                />
              )}
            </AnimatePresence>

            <PersonStack persons={persons} onPop={() => setPersons((s) => s.slice(0, -1))} onOpenMedia={openFromPerson} />

            <ToastStack />
          </div>
        </OverlayContext.Provider>
      </LayoutGroup>
    </MotionConfig>
  );
}

export default function App() {
  return (
    <AppProvider>
      <Shell />
    </AppProvider>
  );
}
