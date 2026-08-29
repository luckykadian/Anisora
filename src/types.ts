export type MediaType = "ANIME" | "MANGA";

export interface SessionUser {
  name: string;
  avatar?: string | null;
}
export interface Session {
  user: SessionUser;
  guest: boolean;
}

export type ListStatus =
  | "CURRENT"
  | "COMPLETED"
  | "PLANNING"
  | "PAUSED"
  | "DROPPED"
  | "REPEATING";

export interface Entry {
  id: number;
  type: MediaType;
  title: string;
  cover?: string | null;
  color?: string | null;
  status: ListStatus;
  progress: number;
  total?: number | null;
  score?: number;
  updatedAt: number;
}

export interface ExtensionCfg {
  quality: "1080p" | "720p" | "480p";
  server: string;
  subDub: "sub" | "dub";
}
export interface Extension {
  id: string;
  name: string;
  lang: string;
  version: string;
  kind: MediaType;
  enabled: boolean;
  color: string;
  servers: string[];
  cfg: ExtensionCfg;
  downloads: number;
}

export interface SettingsState {
  theme: "dark" | "amoled" | "light";
  accent: string; // "r g b"
  density: "cozy" | "compact";
  posterRadius: number;
  reduceMotion: boolean;
  showThumbs: boolean;
  titleLang: "romaji" | "english" | "native";
  nsfw: boolean;
  autoNext: boolean;
  skipIntro: boolean;
  subSize: number;
  syncAuto: boolean;
  syncOnStart: boolean;
  autoProgress: boolean;
  scoreFormat: "100" | "10" | "5";
  extensions: Extension[];
}

export interface Toast {
  id: number;
  msg: string;
  icon: "check" | "sync" | "trash" | "info" | "play";
}

export interface VA {
  id: number;
  name: string;
  image?: string | null;
}

export interface PersonSeed {
  uid: string;
  kind: "character" | "staff";
  id: number;
  layoutId: string;
  name: string;
  native?: string | null;
  image?: string | null;
  role?: string | null;
  vas?: VA[];
}

export interface DetailRef {
  id: number;
  layoutId: string | null;
}
