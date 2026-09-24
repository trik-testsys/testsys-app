export interface HeaderMenuLink { t: string; d?: string; }
export interface HeaderMenu { cols: { title: string; links: HeaderMenuLink[] }[]; feature?: { tag: string; title: string; text: string; cta: string }; }
export interface HeaderItem { key: string; label: string; /** Mega-menu content; items without a menu navigate directly. */ menu?: HeaderMenu; }
/**
 * @startingPoint section="Navigation" subtitle="Top navigation with mega-menu, search, user" viewport="1440x380"
 */
export interface HeaderProps {
  brand?: string;
  /** 1–2 characters in the dark square mark (placeholder until a logo exists). */
  brandMark?: string;
  items?: HeaderItem[];
  /** Key of the current section (accent underline). */
  active?: string;
  /** Signed-in user; omit for guest state (Войти / Регистрация). */
  user?: { name: string; short?: string };
  /** Red dot on the bell. */
  notifications?: boolean;
  searchPlaceholder?: string;
  onNavigate?: (key: string) => void;
  onSignIn?: () => void;
  onSignUp?: () => void;
  /** Keep a mega-menu open (docs, previews). */
  pinned?: string;
}
