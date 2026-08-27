import type { MediaType } from "./types";

const ENDPOINT = "https://graphql.anilist.co";
const cache = new Map<string, any>();

export async function gql<T = any>(query: string, variables: Record<string, any> = {}): Promise<T> {
  const key = query + JSON.stringify(variables);
  if (cache.has(key)) return cache.get(key);
  const res = await fetch(ENDPOINT, {
    method: "POST",
    headers: { "Content-Type": "application/json", Accept: "application/json" },
    body: JSON.stringify({ query, variables }),
  });
  if (!res.ok) throw new Error(`AniList ${res.status}`);
  const json = await res.json();
  if (json.errors) throw new Error(json.errors?.[0]?.message || "GraphQL error");
  cache.set(key, json.data);
  return json.data as T;
}

const CARD = `
  id type
  title { romaji english native }
  coverImage { extraLarge large medium color }
  bannerImage
  format status season seasonYear averageScore popularity
  episodes chapters
  genres
  nextAiringEpisode { episode timeUntilAiring airingAt }
`;

export function seasonNow(): { season: string; year: number } {
  const d = new Date();
  const m = d.getMonth();
  const year = d.getFullYear();
  const season = m <= 2 ? "WINTER" : m <= 5 ? "SPRING" : m <= 8 ? "SUMMER" : "FALL";
  return { season, year };
}

export async function fetchHome(type: MediaType) {
  const { season, year } = seasonNow();
  const q = `
    query ($type: MediaType, $season: MediaSeason, $seasonYear: Int) {
      trending: Page(page: 1, perPage: 14) {
        media(type: $type, sort: [TRENDING_DESC, POPULARITY_DESC], isAdult: false) { ${CARD} }
      }
      seasonal: Page(page: 1, perPage: 14) {
        media(type: $type, sort: [POPULARITY_DESC], isAdult: false, season: $season, seasonYear: $seasonYear) { ${CARD} }
      }
      top: Page(page: 1, perPage: 14) {
        media(type: $type, sort: [SCORE_DESC], isAdult: false) { ${CARD} }
      }
      loved: Page(page: 1, perPage: 14) {
        media(type: $type, sort: [FAVOURITES_DESC], isAdult: false) { ${CARD} }
      }
    }`;
  return gql(q, { type, season: type === "ANIME" ? season : null, seasonYear: type === "ANIME" ? year : null });
}

export async function searchMedia(qstr: string, type: MediaType | null, nsfw: boolean) {
  const q = `
    query ($q: String, $type: MediaType, $nsfw: Boolean) {
      Page(page: 1, perPage: 30) {
        media(search: $q, type: $type, isAdult: $nsfw, sort: [SEARCH_MATCH, POPULARITY_DESC]) { ${CARD} }
      }
    }`;
  return gql(q, { q: qstr, type, nsfw: nsfw ? null : false });
}

const DETAIL = `
  id type
  title { romaji english native userPreferred }
  coverImage { extraLarge large medium color }
  bannerImage
  description(asHtml: true)
  format status season seasonYear
  episodes chapters volumes duration
  averageScore meanScore popularity favourites
  source isAdult synonyms countryOfOrigin hashtag
  startDate { year month day } endDate { year month day }
  genres
  tags { name rank category isMediaSpoiler }
  studios(isMain: true) { nodes { name } }
  trailer { id site thumbnail }
  nextAiringEpisode { episode timeUntilAiring airingAt }
  streamingEpisodes { title thumbnail url site }
  externalLinks { id url site icon color }
  relations { edges { relationType(version: 2) node { id type title { romaji english native } coverImage { large color } format status averageScore } } }
  characters(sort: [ROLE, RELEVANCE, ID], perPage: 14) {
    edges {
      role
      node { id name { full native } image { large } }
      voiceActors(language: JAPANESE, sort: [RELEVANCE, ID]) { id name { full } image { medium } languageV2 }
    }
  }
  staff(sort: [RELEVANCE, ID], perPage: 10) {
    edges { role node { id name { full native } image { large } } }
  }
  stats {
    scoreDistribution { score amount }
    statusDistribution { status amount }
  }
  recommendations(sort: [RATING_DESC, ID], perPage: 10) {
    nodes { rating mediaRecommendation { id type title { romaji english native } coverImage { large color } averageScore format } }
  }
  reviews(sort: [RATING_DESC], perPage: 4) {
    nodes { id summary rating ratingAmount score user { id name avatar { medium } } }
  }
`;

export async function fetchDetail(id: number) {
  const q = `query ($id: Int) { Media(id: $id) { ${DETAIL} } }`;
  const data = await gql(q, { id });
  const media = data?.Media;
  // streamingEpisodes may be unavailable for some entries — tolerate it
  if (media && !Array.isArray(media.streamingEpisodes)) media.streamingEpisodes = [];
  return media;
}

export async function fetchCharacter(id: number) {
  const q = `
    query ($id: Int) {
      Character(id: $id) {
        id
        name { full native alternative }
        image { large }
        description
        gender age bloodType
        dateOfBirth { year month day }
        favourites siteUrl
        media(perPage: 12, sort: [POPULARITY_DESC, FAVOURITES_DESC]) {
          nodes { id type title { romaji english native } format coverImage { medium color } averageScore }
        }
      }
    }`;
  const data = await gql(q, { id });
  return data?.Character;
}

export async function fetchStaff(id: number) {
  const q = `
    query ($id: Int) {
      Staff(id: $id) {
        id
        name { full native }
        image { large }
        description
        gender age homeTown languageV2
        dateOfBirth { year month day }
        favourites siteUrl
        staffMedia(perPage: 12, sort: [POPULARITY_DESC, FAVOURITES_DESC]) {
          edges { staffRole node { id type title { romaji english native } format coverImage { medium color } averageScore } }
        }
      }
    }`;
  const data = await gql(q, { id });
  return data?.Staff;
}

/* ---------------------------------- helpers ---------------------------------- */

export const STATUS_LABELS: Record<string, string> = {
  RELEASING: "Releasing",
  FINISHED: "Finished",
  NOT_YET_RELEASED: "Not yet released",
  CANCELLED: "Cancelled",
  HIATUS: "Hiatus",
};

export const FORMAT_LABELS: Record<string, string> = {
  TV: "TV",
  TV_SHORT: "TV Short",
  MOVIE: "Movie",
  SPECIAL: "Special",
  OVA: "OVA",
  ONA: "ONA",
  MUSIC: "Music",
  MANGA: "Manga",
  NOVEL: "Light Novel",
  ONE_SHOT: "One Shot",
};

export const RELATION_LABELS: Record<string, string> = {
  SEQUEL: "Sequel",
  PREQUEL: "Prequel",
  SIDE_STORY: "Side Story",
  SPIN_OFF: "Spin-off",
  ALTERNATIVE: "Alternative",
  ADAPTATION: "Adaptation",
  SOURCE: "Source",
  PARENT: "Parent",
  SUMMARY: "Summary",
  CHARACTER: "Shared Universe",
  OTHER: "Related",
};

export const SEASON_LABELS: Record<string, string> = {
  WINTER: "Winter",
  SPRING: "Spring",
  SUMMER: "Summer",
  FALL: "Fall",
};

export function titleOf(m: any, lang: "romaji" | "english" | "native"): string {
  const t = m?.title ?? {};
  const pick = (l: string) => (t as any)?.[l];
  const alt = lang === "romaji" ? ["romaji", "english", "native"] : lang === "english" ? ["english", "romaji", "native"] : ["native", "romaji", "english"];
  for (const k of alt) {
    const v = pick(k);
    if (v) return v;
  }
  return m?.title?.romaji || "Untitled";
}

export function fmt(n?: number | null): string {
  if (n == null) return "—";
  if (n >= 1_000_000) return (n / 1_000_000).toFixed(1).replace(/\.0$/, "") + "M";
  if (n >= 1000) return (n / 1000).toFixed(1).replace(/\.0$/, "") + "k";
  return String(n);
}

export function fmtDate(d?: { year?: number | null; month?: number | null; day?: number | null } | null): string {
  if (!d || !d.year) return "—";
  const months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
  let s = months[(d.month ?? 1) - 1] ?? "";
  if (d.day) s = `${d.day} ${s}`;
  return `${s} ${d.year}`.trim();
}

export function fmtCountdown(sec?: number | null): string {
  if (!sec && sec !== 0) return "soon";
  const d = Math.floor(sec / 86400);
  const h = Math.floor((sec % 86400) / 3600);
  const m = Math.floor((sec % 3600) / 60);
  if (d > 0) return `${d}d ${h}h`;
  if (h > 0) return `${h}h ${m}m`;
  return `${m}m`;
}

export function hashHue(s: string): number {
  let h = 0;
  for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) % 360;
  return h;
}

export function shade(hex: string, amt: number): string {
  const m = /^#?([\da-f]{2})([\da-f]{2})([\da-f]{2})$/i.exec(hex || "");
  if (!m) return hex;
  const c = m.slice(1).map((v) => Math.max(0, Math.min(255, parseInt(v, 16) + amt)));
  return "#" + c.map((v) => v.toString(16).padStart(2, "0")).join("");
}

export function gradFrom(color: string | null | undefined, title: string): string {
  if (color && /^#[\da-f]{6}$/i.test(color)) {
    return `linear-gradient(145deg, ${shade(color, -30)}, ${shade(color, 30)})`;
  }
  const h = hashHue(title || "x");
  return `linear-gradient(145deg, hsl(${h} 55% 30%), hsl(${(h + 50) % 360} 60% 48%))`;
}

/** Convert AniList HTML description: ~!spoiler!~ => blur span */
export function spoilerize(html: string): string {
  return (html || "")
    .replace(/~!([\s\S]*?)!~/g, `<span class="spoiler">$1</span>`)
    .replace(/!~([\s\S]*?)~!/g, `<span class="spoiler">$1</span>`)
    .replace(/__(.*?)__/g, `<strong>$1</strong>`)
    .replace(/\*\*(.*?)\*\*/g, `<strong>$1</strong>`)
    .replace(/_(.*?)_/g, `<em>$1</em>`)
    .replace(/\n/g, "<br>");
}

export const ACCENTS: { name: string; value: string }[] = [
  { name: "AniList Blue", value: "61 180 242" },
  { name: "Iris", value: "129 140 248" },
  { name: "Violet", value: "167 139 250" },
  { name: "Sakura", value: "244 114 182" },
  { name: "Matcha", value: "74 222 128" },
  { name: "Amber", value: "251 191 36" },
  { name: "Crimson", value: "248 113 113" },
  { name: "Mint", value: "45 212 191" },
];

/* ------------------------- offline fallback content ------------------------- */

const F = (id: number, romaji: string, native: string, c: string, score: number, format: string, type: MediaType) => ({
  id,
  type,
  title: { romaji, english: romaji, native },
  coverImage: { large: null, color: c },
  bannerImage: null,
  format,
  averageScore: score,
  status: "FINISHED",
});

export function fallbackHome(type: MediaType) {
  const anime = [
    F(154587, "Sousou no Frieren", "葬送のフリーレン", "#5DA2D5", 91, "TV", "ANIME"),
    F(16498, "Shingeki no Kyojin", "進撃の巨人", "#D64550", 87, "TV", "ANIME"),
    F(113415, "Jujutsu Kaisen", "呪術廻戦", "#6C5CE7", 84, "TV", "ANIME"),
    F(151807, "Solo Leveling", "俺だけレベルアップな件", "#8E44AD", 84, "TV", "ANIME"),
    F(21, "One Piece", "ワンピース", "#F4A261", 87, "TV", "ANIME"),
  ];
  const manga = [
    F(30013, "One Piece", "ワンピース", "#F4A261", 92, "MANGA", "MANGA"),
    F(2, "Berserk", "ベルセルク", "#A26769", 94, "MANGA", "MANGA"),
    F(105778, "Chainsaw Man", "チェンソーマン", "#E85D04", 87, "MANGA", "MANGA"),
    F(656, "Vagabond", "バガボンド", "#606C38", 93, "MANGA", "MANGA"),
    F(108398, "Solo Leveling", "나 혼자만 레벨업", "#5F0F40", 86, "MANGA", "MANGA"),
  ];
  const items = (type === "ANIME" ? anime : manga) as any[];
  return {
    trending: { media: items },
    seasonal: { media: items.slice().reverse() },
    top: { media: items.slice(1).concat(items[0] ?? []) },
    loved: { media: items.slice(2).concat(items.slice(0, 2)) },
    offline: true,
  };
}
