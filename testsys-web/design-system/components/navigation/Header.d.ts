export interface HeaderMenuLink { key?: string; target?: string; t: string; d?: string; }
export interface HeaderMenu { cols: { key?: string; title: string; /** Optional heading destination; child links are then visually nested. */ target?: string; links: HeaderMenuLink[] }[]; feature?: { tag: string; title: string; text: string; cta: string }; }
export interface HeaderItem { key: string; label: string; /** With menu, opts into separate navigation label and disclosure arrow. */ target?: string; /** Mega-menu content; items without a menu navigate directly. */ menu?: HeaderMenu; }
/**
 * @startingPoint section="Navigation" subtitle="Top navigation with mega-menu, search, user" viewport="1440x380"
 */
export interface HeaderProps {
  brand?: string;
  /** Optional prototype route; legacy brand still sends home. */
  brandTarget?: string;
  /** Shorter bar gaps for a compact prototype navigation. */
  compact?: boolean;
  /** Opt-in: keep the bar at the viewport top while its page scrolls. */
  sticky?: boolean;
  /** 1–2 characters in the dark square mark (placeholder until a logo exists). */
  brandMark?: string;
  items?: HeaderItem[];
  /** Key of the current section (accent underline). */
  active?: string;
  /** Current stable route target marks the corresponding menu link. */
  currentTarget?: string;
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
  /** Local searchable targets and local panels; actions remain owned by the caller. */
  searchItems?: { label: string; onClick?: () => void }[];
  notificationItems?: { label: string; onClick?: () => void }[];
  userMenuItems?: { label: string; onClick?: () => void }[];
  /** Opt-in: input focus opens this same menu instead of the legacy search panel. */
  searchMenuKey?: string;
  /** Controlled query; callers may filter items and clear it on navigation/reset. */
  searchValue?: string;
  onSearch?: (query: string) => void;
}
