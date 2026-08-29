import { AnimatePresence, motion } from "framer-motion";
import { CheckCircle2, ChevronRight, Layers, Loader2, Lock, ShieldCheck, Sparkles, TrendingUp, User, X } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import onboardingArt from "../assets/onboarding.jpg";
import { fetchHome } from "../api";
import { Art, Logo, SPRING } from "../bits";
import { useApp } from "../store";

type OauthStep = null | "form" | "authorizing" | "done";

export function Onboarding() {
  const { login } = useApp();
  const [posters, setPosters] = useState<any[]>([]);
  const [step, setStep] = useState<OauthStep>(null);
  const [tilt, setTilt] = useState({ x: 0, y: 0 });
  const wrapRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    let on = true;
    fetchHome("ANIME")
      .then((d) => {
        if (on) setPosters((d?.trending?.media ?? []).slice(0, 9));
      })
      .catch(() => {});
    return () => {
      on = false;
    };
  }, []);

  const finish = (guest: boolean) => {
    login({ user: { name: guest ? "Guest" : "Nova" }, guest });
  };

  const startOauth = () => setStep("form");
  const authorize = () => {
    setStep("authorizing");
    window.setTimeout(() => setStep("done"), 1500);
    window.setTimeout(() => finish(false), 2300);
  };

  const onMouse = (e: React.MouseEvent) => {
    const r = wrapRef.current?.getBoundingClientRect();
    if (!r) return;
    setTilt({ x: (e.clientX - r.left) / r.width - 0.5, y: (e.clientY - r.top) / r.height - 0.5 });
  };

  const feats = [
    { icon: TrendingUp, t: "Live AniList sync", s: "Lists & progress stay in orbit" },
    { icon: Layers, t: "Extensions", s: "Pick sources for every episode" },
    { icon: Sparkles, t: "Yours, visually", s: "Themes, accents, density" },
  ];

  return (
    <div ref={wrapRef} onMouseMove={onMouse} className="relative min-h-dvh overflow-hidden bg-bg0">
      {/* backdrop */}
      <img src={onboardingArt} alt="" className="pointer-events-none absolute inset-0 h-full w-full object-cover opacity-60" />
      <div className="pointer-events-none absolute inset-0 bg-gradient-to-r from-bg0 via-bg0/80 to-bg0/30" />
      <div className="pointer-events-none absolute inset-0 bg-gradient-to-t from-bg0 via-transparent to-bg0/70" />
      <div className="noise pointer-events-none absolute inset-0" />

      <div className="relative z-10 mx-auto flex min-h-dvh max-w-[1400px] flex-col px-6 py-8 lg:px-12">
        <header className="flex items-center justify-between">
          <Logo size={32} />
          <span className="hidden items-center gap-2 rounded-full border border-line bg-bg1/60 px-3 py-1.5 text-[11px] text-mut backdrop-blur-md sm:flex">
            <ShieldCheck size={13} className="text-emerald-400" /> OAuth 2.0 · tokens stay on-device
          </span>
        </header>

        <div className="flex flex-1 items-center">
          <div className="max-w-xl py-16">
            <motion.div initial={{ opacity: 0, y: 26 }} animate={{ opacity: 1, y: 0 }} transition={{ ...SPRING, delay: 0.05 }}>
              <span className="inline-flex items-center gap-2 rounded-full border border-accLine bg-accSoft px-3.5 py-1.5 text-[11.5px] font-semibold tracking-wide text-acc">
                <Sparkles size={13} /> THE ANILIST TRACKER, REIMAGINED
              </span>
            </motion.div>

            <motion.h1
              initial={{ opacity: 0, y: 30 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ ...SPRING, delay: 0.12 }}
              className="mt-6 font-disp text-[44px] font-bold leading-[1.02] tracking-tight sm:text-[58px]"
            >
              Log the stories
              <br />
              <span className="bg-gradient-to-r from-acc via-acc to-white/60 bg-clip-text text-transparent">that move you.</span>
            </motion.h1>

            <motion.p initial={{ opacity: 0, y: 26 }} animate={{ opacity: 1, y: 0 }} transition={{ ...SPRING, delay: 0.2 }} className="mt-5 max-w-md text-[15px] leading-relaxed text-mut">
              Track every episode and chapter, discover what's next, and bend the interface to your taste — powered by the AniList universe.
            </motion.p>

            <motion.div initial={{ opacity: 0, y: 24 }} animate={{ opacity: 1, y: 0 }} transition={{ ...SPRING, delay: 0.28 }} className="mt-9 flex flex-wrap items-center gap-3">
              <button
                onClick={startOauth}
                className="group flex items-center gap-3 rounded-2xl bg-acc px-6 py-3.5 text-[14.5px] font-bold text-accInk shadow-glow transition-transform duration-200 hover:scale-[1.03] active:scale-[0.98]"
              >
                <span className="grid h-6 w-6 place-items-center rounded-lg bg-white/25 font-black text-[12px]">AL</span>
                Continue with AniList
                <ChevronRight size={16} className="transition-transform duration-200 group-hover:translate-x-0.5" />
              </button>
              <button
                onClick={() => finish(true)}
                className="flex items-center gap-2 rounded-2xl border border-line bg-bg1/70 px-6 py-3.5 text-[14.5px] font-semibold text-txt backdrop-blur-md transition hover:border-accLine"
              >
                <User size={16} /> Browse as guest
              </button>
            </motion.div>

            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.5, duration: 0.8 }} className="mt-12 grid max-w-lg gap-2.5 sm:grid-cols-3">
              {feats.map((f, i) => (
                <motion.div key={f.t} initial={{ opacity: 0, y: 18 }} animate={{ opacity: 1, y: 0 }} transition={{ ...SPRING, delay: 0.5 + i * 0.09 }} className="rounded-2xl border border-line bg-bg1/50 p-3.5 backdrop-blur-md">
                  <f.icon size={16} className="text-acc" />
                  <p className="mt-2 text-[12.5px] font-semibold leading-tight">{f.t}</p>
                  <p className="mt-1 text-[11px] leading-snug text-mut">{f.s}</p>
                </motion.div>
              ))}
            </motion.div>
          </div>
        </div>

        <footer className="flex items-center justify-between text-[11px] text-mut">
          <span>Anisora demo · not affiliated with AniList</span>
          <span className="hidden sm:block">Free forever · 10 seconds to sign in</span>
        </footer>
      </div>

      {/* floating poster collage (desktop) */}
      {posters.length > 0 && (
        <div className="pointer-events-none absolute right-[-6%] top-1/2 hidden w-[560px] -translate-y-1/2 lg:block" style={{ perspective: "1200px" }}>
          <motion.div
            initial={{ opacity: 0, rotate: 14 }}
            animate={{ opacity: 1 }}
            transition={{ duration: 1.2, delay: 0.3 }}
            className="grid grid-cols-3 gap-5"
            style={{ transform: `rotateY(${tilt.x * -6}deg) rotateX(${tilt.y * 5}deg) rotate(9deg)` }}
          >
            {posters.map((m, i) => (
              <motion.div
                key={m.id}
                initial={{ opacity: 0, y: 40 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.35 + i * 0.06, duration: 0.8 }}
                style={{ animation: `float-y ${7 + (i % 3)}s ease-in-out ${i * 0.4}s infinite` }}
                className="overflow-hidden rounded-2xl border border-white/10 shadow-pop"
              >
                <Art src={m.coverImage?.large} alt={m.title?.romaji ?? ""} color={m.coverImage?.color} className="aspect-[2/3] w-full" />
              </motion.div>
            ))}
          </motion.div>
        </div>
      )}

      {/* oauth modal */}
      <AnimatePresence>
        {step && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="fixed inset-0 z-50 grid place-items-center bg-black/60 p-4 backdrop-blur-sm">
            <motion.div
              initial={{ opacity: 0, y: 34, scale: 0.95 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, y: 20, scale: 0.96 }}
              transition={SPRING}
              className="w-full max-w-[430px] overflow-hidden rounded-3xl border border-line bg-bg1 shadow-pop"
            >
              {/* browser chrome */}
              <div className="flex items-center gap-3 border-b border-line bg-bg2/70 px-4 py-3">
                <span className="flex gap-1.5">
                  <i className="h-2.5 w-2.5 rounded-full bg-rose-400/80" />
                  <i className="h-2.5 w-2.5 rounded-full bg-amber-400/80" />
                  <i className="h-2.5 w-2.5 rounded-full bg-emerald-400/80" />
                </span>
                <span className="flex min-w-0 flex-1 items-center gap-1.5 truncate rounded-lg border border-line bg-bg0 px-2.5 py-1 font-mono text-[10.5px] text-mut">
                  <Lock size={10} className="shrink-0 text-emerald-400" />
                  anilist.co/api/v2/oauth/authorize?client_id=27594
                </span>
                <button onClick={() => setStep(null)} className="grid h-7 w-7 place-items-center rounded-lg text-mut transition hover:bg-bg2 hover:text-txt" aria-label="cancel">
                  <X size={14} />
                </button>
              </div>

              <div className="p-6">
                <AnimatePresence mode="wait">
                  {step === "form" && (
                    <motion.div key="form" initial={{ opacity: 0, x: 18 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -14 }} transition={{ duration: 0.22 }}>
                      <div className="flex items-center gap-2.5">
                        <span className="grid h-10 w-10 place-items-center rounded-2xl bg-[#02A9FF] font-black text-white shadow-glow">A</span>
                        <div>
                          <p className="font-disp text-lg font-bold leading-tight">AniList</p>
                          <p className="text-[11px] text-mut">wants to connect with Anisora</p>
                        </div>
                      </div>
                      <div className="mt-5 space-y-2.5">
                        <label className="block">
                          <span className="mb-1 block text-[11px] font-medium text-mut">Username</span>
                          <input readOnly value="nova" className="w-full rounded-xl border border-line bg-bg2 px-3.5 py-2.5 text-sm outline-none focus:border-accLine" />
                        </label>
                        <label className="block">
                          <span className="mb-1 block text-[11px] font-medium text-mut">Password</span>
                          <input readOnly type="password" value="••••••••••" className="w-full rounded-xl border border-line bg-bg2 px-3.5 py-2.5 text-sm tracking-widest outline-none focus:border-accLine" />
                        </label>
                      </div>
                      <button onClick={authorize} className="mt-5 flex w-full items-center justify-center gap-2 rounded-xl bg-[#02A9FF] py-3 text-sm font-bold text-white shadow-glow transition hover:brightness-110 active:scale-[0.98]">
                        Authorize Anisora <ChevronRight size={15} />
                      </button>
                      <p className="mt-3 text-center text-[10.5px] text-mut">Read &amp; write access to your lists. Revoke anytime at anilist.co</p>
                    </motion.div>
                  )}
                  {step === "authorizing" && (
                    <motion.div key="auth" initial={{ opacity: 0, x: 18 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -14 }} className="flex flex-col items-center gap-3 py-8">
                      <Loader2 size={34} className="animate-spin text-acc" />
                      <p className="text-sm font-medium">Exchanging token…</p>
                      <p className="font-mono text-[10.5px] text-mut">redirect_uri: anisora://auth/callback</p>
                    </motion.div>
                  )}
                  {step === "done" && (
                    <motion.div key="done" initial={{ opacity: 0, scale: 0.9 }} animate={{ opacity: 1, scale: 1 }} className="flex flex-col items-center gap-3 py-8">
                      <motion.span initial={{ scale: 0.4 }} animate={{ scale: 1 }} transition={SPRING}>
                        <CheckCircle2 size={44} className="text-emerald-400" />
                      </motion.span>
                      <p className="text-sm font-semibold">Connected as Nova</p>
                      <p className="text-[11px] text-mut">Pulling your lists into orbit…</p>
                    </motion.div>
                  )}
                </AnimatePresence>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
