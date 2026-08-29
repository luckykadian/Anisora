import { createContext, useContext } from "react";
import type { PersonSeed } from "./types";

export interface OverlayCtxValue {
  hidden: Set<string>;
  openPerson: (p: Omit<PersonSeed, "uid">) => void;
}

export const OverlayContext = createContext<OverlayCtxValue>({
  hidden: new Set<string>(),
  openPerson: () => {},
});

export const useOverlay = () => useContext(OverlayContext);
