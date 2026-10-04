Header: navigation, optional mega-menu/search, guest actions or a signed-in identity. Caller-owned targets and
callbacks keep the component independent from application routes; no unbound shortcut is displayed.

```jsx
<Header compact brandTarget="home" active="menu" currentTarget={route}
  items={getHeaderItems(query)} onNavigate={navigate}
  searchMenuKey="menu" searchValue={query} onSearch={setQuery}
  searchPlaceholder="Найти кабинет или раздел…"
  user={{ name: 'Анна' }}
  notificationItems={[{ label: 'Проверка завершена', onClick: openResult }]}
  userMenuItems={[{ label: 'Мой кабинет', onClick: openCabinet }]} />
```

The complete prototype example and its route configuration are in
[PrototypeHeader.jsx](../../ui_kits/platform/PrototypeHeader.jsx) and
[PrototypeNavigation.mjs](../../ui_kits/platform/PrototypeNavigation.mjs).

- Item `target` + `menu` splits navigation and disclosure: label navigates, arrow/hover opens. Without target,
  click-toggle and legacy link captions remain compatible; `brandTarget` routes the brand.
- A column `target` turns its role heading into an overview link. Heading and children participate in keyboard
  navigation; `currentTarget` sets aria-current. Linked headings use weight 600, nested children 400.
- `searchMenuKey` opens that same menu on input focus. `searchValue`/`onSearch` let the caller filter `items` and
  clear the query on navigation/reset. Visible matching substrings use safe text nodes and inherit font weight.
  Without this opt-in, `searchItems` retains the separate legacy search behavior.
- Unified DOM order is navigation → search → flexible spacer → identity/guest actions. Its content-width panel
  has a complete token border, radius-xl and shadow-popover; short results align to the search field.
- User and notification panels open on click, are bounded and right-aligned to their own trigger, and reposition
  on resize. Click preserves focus; ArrowDown/ArrowUp/Tab enter rows, Escape returns focus, outside/action closes.
  Titles use weight 600, rows 400. Navigation hover cannot replace an active identity panel.

Optional `sticky` keeps the bar at top:0 as the page scrolls, using the existing layer order. Its dropdown panels are height-bounded and scroll internally on short viewports. The prototype enables it only for its main header; the demonstration toolbar and default Header instances remain in normal flow.

The default TestSys brand uses the selected horizontal PNG (split-cream variant 05), with token-rounded
corners, preserved proportions, a single accessible name and one decorative image. Custom `brand` or `brandMark` opts into text branding.
