import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from "react";
import type { Entry, Extension, ListStatus, Session, SettingsState, Toast } from "./types";

const LS_SETTINGS = "anisora.settings.v1";
const LS_SESSION = "anisora.session.v1";
const LS_LIBRARY = "anisora.library.v1";

export const DEFAULT_EXTENSIONS: Extension[] = [
  {
    id: "aniwatch",
    name: "AniWatch",
    lang: "EN",
    version: "1.4.2",
    kind: "ANIME",
    enabled: true,
    color: "#6C5CE7",
    servers: ["HD-1", "HD-2", "VidStream"],
    cfg: { quality: "1080p", server: "HD-1", subDub: "sub" },
    downloads: 128400,
  },
  {
    id: "senpai",
    name: "Senpai Stream",
    lang: "EN",
    version: "0.9.8",
    kind: "ANIME",
    enabled: true,
    color: "#00B4D8",
    servers: ["Kappa", "Beta", "Delta"],
    cfg: { quality: "720p", server: "Kappa", subDub: "sub" },
    downloads: 86300,
  },
  {
    id: "jellyfin",
    name: "Jellyfin Local",
    lang: "Multi",
    version: "2.1.0",
    kind: "ANIME",
    enabled: false,
    color: "#5bc0de",
    servers: ["Home NAS"],
    cfg: { quality: "1080p", server: "Home NAS", subDub: "sub" },
    downloads: 12400,
  },
  {
    id: "mangadex",
    name: "MangaDex",
    lang: "Multi",
    version: "3.0.1",
    kind: "MANGA",
    enabled: true,
    color: "#FF6740",
    servers: ["Main", "EU Mirror"],
    cfg: { quality: "1080p", server: "Main", subDub: "sub" },
    downloads: 214800,
  },
  {
    id: "asura",
    name: "Asura Scans",
    lang: "EN",
    version: "1.1.6",
    kind: "MANGA",
    enabled: false,
    color: "#F4A261",
    servers: ["Main", "CDN-2"],
    cfg: { quality: "720p", server: "Main", subDub: "sub" },
    downloads: 74100,
  },
];

export const defaultSettings: SettingsState = {
  theme: "dark",
  accent: "61 180 242",
  density: "cozy",
  posterRadius: 16,
  reduceMotion: false,
  showThumbs: true,
  titleLang: "romaji",
  nsfw: false,
  autoNext: true,
  skipIntro: true,
  subSize: 100,
  syncAuto: true,
  syncOnStart: true,
  autoProgress: true,
  scoreFormat: "100",
  extensions: DEFAULT_EXTENSIONS,
};

function safeParse<T>(raw: string | null, fallback: T): T {
  if (!raw) return fallback;
  try {
    return { ...fallback, ...JSON.parse(raw) };
  } catch {
    return fallback;
  }
}

function seedLibrary(): Record<number, Entry> {
  const mk = (
    id: number,
    type: "ANIME" | "MANGA",
    title: string,
    status: ListStatus,
    progress: number,
    total: number | null,
    color: string,
    score?: number,
  ): Entry => ({ id, type, title, status, progress, total, color, score, updatedAt: Date.now() - Math.random() * 86400000 });
  const list: Entry[] = [
    mk(154587, "ANIME", "Sousou no Frieren", "CURRENT", 11, 28, "#5DA2D5", 64),
    mk(113415, "ANIME", "Jujutsu Kaisen", "CURRENT", 17, 23, "#6C5CE7", 71),
    mk(151807, "ANIME", "Solo Leveling", "PLANNING", 0, 12, "#8E44AD"),
    mk(16498, "ANIME", "Attack on Titan", "COMPLETED", 25, 25, "#D64550", 92),
    mk(21, "ANIME", "One Piece", "PAUSED", 112, null, "#F4A261", 84),
    mk(30013, "MANGA", "One Piece", "CURRENT", 1098, null, "#F4A261", 88),
    mk(2, "MANGA", "Berserk", "PAUSED", 122, 364, "#A26769"),
    mk(105778, "MANGA", "Chainsaw Man", "CURRENT", 132, null, "#E85D04", 80),
    mk(656, "MANGA", "Vagabond", "PLANNING", 0, 327, "#606C38"),
  ];
  return Object.fromEntries(list.map((e) => [e.id, e]));
}

interface AppCtx {
  session: Session | null;
  settings: SettingsState;
  library: Record<number, Entry>;
  toasts: Toast[];
  offline: boolean;
  login: (s: Session) => void;
  logout: () => void;
  updateSettings: (patch: Partial<SettingsState>) => void;
  upsertEntry: (e: Entry) => void;
  patchEntry: (id: number, patch: Partial<Entry>) => void;
  removeEntry: (id: number) => void;
  bumpProgress: (id: number) => void;
  toast: (msg: string, icon?: Toast["icon"]) => void;
  setOffline: (v: boolean) => void;
}

const Ctx = createContext<AppCtx | null>(null);

export function useApp(): AppCtx {
  const v = useContext(Ctx);
  if (!v) throw new Error("useApp outside provider");
  return v;
}

export function AppProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(() => {
    try {
      const raw = localStorage.getItem(LS_SESSION);
      return raw ? (JSON.parse(raw) as Session) : null;
    } catch {
      return null;
    }
  });
  const [settings, setSettings] = useState<SettingsState>(() => safeParse(localStorage.getItem(LS_SETTINGS), defaultSettings));
  const [library, setLibrary] = useState<Record<number, Entry>>(() => {
    try {
      const raw = localStorage.getItem(LS_LIBRARY);
      return raw ? JSON.parse(raw) : {};
    } catch {
      return {};
    }
  });
  const [toasts, setToasts] = useState<Toast[]>([]);
  const [offline, setOffline] = useState(false);
  const toastId = useRef(0);

  useEffect(() => localStorage.setItem(LS_SETTINGS, JSON.stringify(settings)), [settings]);
  useEffect(() => localStorage.setItem(LS_LIBRARY, JSON.stringify(library)), [library]);

  const toast = useCallback((msg: string, icon: Toast["icon"] = "check") => {
    const id = ++toastId.current;
    setToasts((t) => [...t.slice(-3), { id, msg, icon }]);
    window.setTimeout(() => setToasts((t) => t.filter((x) => x.id !== id)), 3000);
  }, []);

  const login = useCallback(
    (s: Session) => {
      setSession(s);
      localStorage.setItem(LS_SESSION, JSON.stringify(s));
      setLibrary((lib) => {
        if (Object.keys(lib).length > 0) return lib;
        return seedLibrary();
      });
    },
    [],
  );

  const logout = useCallback(() => {
    setSession(null);
    localStorage.removeItem(LS_SESSION);
  }, []);

  const updateSettings = useCallback((patch: Partial<SettingsState>) => {
    setSettings((s) => ({ ...s, ...patch }));
  }, []);

  const upsertEntry = useCallback((e: Entry) => {
    setLibrary((lib) => ({ ...lib, [e.id]: { ...e, updatedAt: Date.now() } }));
  }, []);

  const patchEntry = useCallback((id: number, patch: Partial<Entry>) => {
    setLibrary((lib) => (lib[id] ? { ...lib, [id]: { ...lib[id], ...patch, updatedAt: Date.now() } } : lib));
  }, []);

  const removeEntry = useCallback((id: number) => {
    setLibrary((lib) => {
      const n = { ...lib };
      delete n[id];
      return n;
    });
  }, []);

  const bumpProgress = useCallback(
    (id: number) => {
      setLibrary((lib) => {
        const e = lib[id];
        if (!e) return lib;
        const next = e.progress + 1;
        const done = e.total != null && next >= e.total;
        return {
          ...lib,
          [id]: {
            ...e,
            progress: done ? (e.total ?? next) : next,
            status: done ? "COMPLETED" : e.status === "PLANNING" || e.status === "PAUSED" ? "CURRENT" : e.status,
            updatedAt: Date.now(),
          },
        };
      });
    },
    [],
  );

  const value = useMemo<AppCtx>(
    () => ({ session, settings, library, toasts, offline, login, logout, updateSettings, upsertEntry, patchEntry, removeEntry, bumpProgress, toast, setOffline }),
    [session, settings, library, toasts, offline, login, logout, updateSettings, upsertEntry, patchEntry, removeEntry, bumpProgress, toast],
  );

  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}
