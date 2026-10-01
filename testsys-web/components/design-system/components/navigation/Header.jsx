import React from 'react';
import { Icon } from '../core/Icon.jsx';
import { Avatar } from '../display/Avatar.jsx';
import { Button } from '../actions/Button.jsx';
import { splitSearchMatches } from '../../lib/search-text.mjs';
import { getPopoverBounds } from '../../lib/popover-geometry.mjs';
import { brandAssets } from '../../lib/brand-assets.mjs';
const cx = (...a) => a.filter(Boolean).join(' ');

const DEFAULT_ITEMS = [
  { key: 'contests', label: 'Соревнования', menu: { cols: [
    { title: 'Участвовать', links: [{ t: 'Идут сейчас', d: 'Можно присоединиться до конца' }, { t: 'Предстоящие', d: 'Открыта регистрация' }, { t: 'Архив', d: 'Завершённые соревнования и разборы' }] },
    { title: 'Организовать', links: [{ t: 'Создать соревнование', d: 'Олимпиада, контест или квиз' }, { t: 'Мои соревнования', d: 'Черновики и опубликованные' }, { t: 'Шаблоны', d: 'Готовые форматы и правила' }] },
    { title: 'Форматы', links: [{ t: 'ICPC', d: 'Командный, штрафное время' }, { t: 'IOI', d: 'Частичные баллы за подзадачи' }, { t: 'Квиз', d: 'Вопросы с вариантами ответа' }] }
  ], feature: { tag: 'Через 2 дня', title: 'Весенний кубок 2026', text: '5 часов, 8 задач, командный зачёт', cta: 'Зарегистрироваться' } } },
  { key: 'problems', label: 'Задачи', menu: { cols: [
    { title: 'Архив', links: [{ t: 'Все задачи', d: '4 200 задач с проверкой' }, { t: 'По темам', d: 'Графы, ДП, строки, геометрия' }, { t: 'Избранное', d: 'Сохранённые задачи' }] },
    { title: 'Практика', links: [{ t: 'Тренировки', d: 'Подборки по уровню' }, { t: 'Виртуальное участие', d: 'Прошедший контест в реальном времени' }] }
  ] } },
  { key: 'rating', label: 'Рейтинг' },
  { key: 'learn', label: 'Обучение', menu: { cols: [
    { title: 'Материалы', links: [{ t: 'Курсы', d: 'Структурированные программы' }, { t: 'Разборы', d: 'Решения задач прошедших туров' }, { t: 'Справочник', d: 'Алгоритмы и структуры данных' }] }
  ] } },
  { key: 'community', label: 'Сообщество' }
];

export function Header({ brand, brandMark, brandTarget, items = DEFAULT_ITEMS, active, currentTarget, user, notifications = false, searchPlaceholder = 'Поиск задач, соревнований…', compact = false, sticky = false, onNavigate, onSignIn, onSignUp, pinned, searchItems = [], notificationItems = [], userMenuItems = [], searchMenuKey, searchValue, onSearch }) {
  const brandName = brand ?? 'TestSys';
  const customBrand = brand != null || brandMark != null;
  const [open, setOpen] = React.useState(pinned || null);
  const [panel, setPanel] = React.useState(null);
  const [innerQuery, setInnerQuery] = React.useState('');
  const query = searchValue != null ? searchValue : innerQuery;
  const [searchAnchor, setSearchAnchor] = React.useState(null);
  const searchField = React.useRef(null);
  const identityOpener = React.useRef(null);
  const identityPane = React.useRef(null);
  const pendingIdentityFocus = React.useRef(null);
  const [identityAnchor, setIdentityAnchor] = React.useState(null);
  const root = React.useRef(null);
  const opener = React.useRef(null);
  const suppressFocus = React.useRef(false);
  const activated = React.useRef(null);
  const pendingFocus = React.useRef(null);
  const menuId = React.useId();
  const cur = items.find(item => item.key === open && item.menu);
  const unified = searchMenuKey != null && cur?.key === searchMenuKey;
  const identityPanel = panel === 'user' || panel === 'notifications';
  const closePanels = () => { const returnTo = identityPanel ? identityOpener.current : opener.current; pendingIdentityFocus.current = null; pendingFocus.current = null; setPanel(null); setOpen(pinned || null); activated.current = null; suppressFocus.current = true; returnTo?.focus(); suppressFocus.current = false; };
  const navigate = target => { onNavigate?.(target); closePanels(); };
  const openMenu = (item, trigger, focus) => {
    opener.current = trigger; setPanel(null); setOpen(item.key);
    if (focus) {
      activated.current = item.key;
      const rendered = open === item.key && !panel;
      const links = rendered ? Array.from(root.current.querySelectorAll('[role="menuitem"]')) : [];
      if (rendered) { (focus === 'last' ? links[links.length - 1] : links[0])?.focus(); pendingFocus.current = null; }
      else pendingFocus.current = focus;
    }
  };
  const openIdentity = (kind, trigger, focus) => {
    identityOpener.current = trigger; opener.current = trigger; pendingFocus.current = null; activated.current = null; setOpen(pinned || null);
    if (!focus) pendingIdentityFocus.current = null;
    if (!focus && panel === kind) { pendingIdentityFocus.current = null; setPanel(null); return; }
    setPanel(kind);
    if (focus) {
      const rows = panel === kind ? Array.from(identityPane.current?.querySelectorAll('[role="menuitem"]') || []) : [];
      if (panel === kind) { (focus === 'last' ? rows[rows.length - 1] : rows[0])?.focus(); pendingIdentityFocus.current = null; }
      else pendingIdentityFocus.current = focus;
    }
  };
  const identityKey = (event, kind) => {
    if (['ArrowDown', 'ArrowUp'].includes(event.key)) { event.preventDefault(); event.stopPropagation(); openIdentity(kind, event.currentTarget, event.key === 'ArrowDown' ? 'first' : 'last'); }
    if (event.key === 'Tab' && !event.shiftKey && panel === kind) {
      const first = identityPane.current?.querySelector('[role="menuitem"]');
      if (first) { event.preventDefault(); first.focus(); }
    }
  };
  const menuKey = (event, item) => {
    if (!item.menu || !['ArrowDown', 'ArrowUp'].includes(event.key)) return;
    event.preventDefault(); openMenu(item, event.currentTarget, event.key === 'ArrowDown' ? 'first' : 'last');
  };
  React.useEffect(() => {
    const outside = event => { if (!root.current?.contains(event.target)) { pendingFocus.current = null; pendingIdentityFocus.current = null; setPanel(null); setOpen(pinned || null); activated.current = null; } };
    document.addEventListener('mousedown', outside); return () => document.removeEventListener('mousedown', outside);
  }, [pinned]);
  React.useEffect(() => {
    if (!pendingFocus.current || !cur || panel) return;
    const links = Array.from(root.current.querySelectorAll('[role="menuitem"]'));
    (pendingFocus.current === 'last' ? links[links.length - 1] : links[0])?.focus(); pendingFocus.current = null;
  });
  React.useEffect(() => {
    if (!identityPanel || !pendingIdentityFocus.current) return;
    const rows = Array.from(identityPane.current?.querySelectorAll('[role="menuitem"]') || []);
    (pendingIdentityFocus.current === 'last' ? rows[rows.length - 1] : rows[0])?.focus(); pendingIdentityFocus.current = null;
  });
  React.useLayoutEffect(() => {
    const measure = () => {
      if (!identityPanel || !identityOpener.current || !root.current) { setIdentityAnchor(null); return; }
      const next = getPopoverBounds(root.current.getBoundingClientRect(), identityOpener.current.getBoundingClientRect());
      setIdentityAnchor(previous => previous?.right === next.right && previous.width === next.width ? previous : next);
    };
    measure(); window.addEventListener('resize', measure); return () => window.removeEventListener('resize', measure);
  }, [panel, identityPanel, user?.short, user?.name]);
  React.useLayoutEffect(() => {
    const measure = () => {
      const groupCount = cur?.menu.cols.length || 0;
      if (!unified || !query.trim() || groupCount > 3 || !root.current || !searchField.current) { setSearchAnchor(null); return; }
      const header = root.current.getBoundingClientRect();
      const field = searchField.current.getBoundingClientRect();
      const width = Math.min(header.width - 24, Math.max(1, groupCount) * 280 + 24 + Math.max(0, groupCount - 1) * 8);
      const right = Math.min(Math.max(12, header.right - field.right), Math.max(12, header.width - width - 12));
      setSearchAnchor(previous => previous?.right === right && previous.width === width ? previous : { right, width });
    };
    measure(); window.addEventListener('resize', measure); return () => window.removeEventListener('resize', measure);
  }, [unified, query, cur?.menu.cols.length]);
  const renderMenuText = text => unified ? splitSearchMatches(text, query).map((part, index) => part.matched ? <mark key={index} className="ts-search-match">{part.text}</mark> : <React.Fragment key={index}>{part.text}</React.Fragment>) : text;
  const moveInMenu = event => {
    if (!['ArrowDown', 'ArrowUp', 'Home', 'End'].includes(event.key)) return;
    const links = Array.from(event.currentTarget.querySelectorAll('[role="menuitem"]'));
    if (!links.length) return;
    event.preventDefault(); const index = links.indexOf(document.activeElement);
    const next = event.key === 'Home' ? 0 : event.key === 'End' ? links.length - 1 : (index + (event.key === 'ArrowDown' ? 1 : -1) + links.length) % links.length;
    links[next]?.focus();
  };
  return <div ref={root} className={cx('ts-header', compact && 'ts-header--compact', sticky && 'ts-header--sticky', searchMenuKey && 'ts-header--unified-search')} onKeyDown={event => { if (event.key === 'Escape' && (panel || cur)) { event.stopPropagation(); closePanels(); } }} onMouseLeave={() => { if (!root.current.querySelector('.ts-mega')?.contains(document.activeElement) && !(searchMenuKey && searchField.current?.contains(document.activeElement))) setOpen(pinned || null); }}>
    <div className="ts-header__bar">
      <a href={brandTarget ? '#' + brandTarget : '#'} className="ts-brand" aria-label={brandName} onClick={event => { event.preventDefault(); navigate(brandTarget || 'home'); }}>{customBrand ? <><span className="ts-brand__mark">{brandMark ?? 'T'}</span>{brandName}</> : <><img className="ts-brand__emblem" src={brandAssets.emblem} alt="" /><img className="ts-brand__wordmark" src={brandAssets.wordmark} alt="" /></>}</a>
      <nav className="ts-nav" aria-label="Разделы">
        {items.map(item => {
          const split = item.target != null && item.menu;
          const isActive = item.key === active || (item.key === searchMenuKey && open === item.key && !panel);
          const classes = cx('ts-nav__item', isActive && 'ts-nav__item--active');
          const hover = event => { if (identityPanel) return; if (item.menu) { activated.current = null; openMenu(item, event.currentTarget.querySelector('[data-menu-toggle]') || event.currentTarget, null); } else setOpen(null); };
          return split ? <span key={item.key} className={cx('ts-nav__group', isActive && 'ts-nav__group--active')} onMouseEnter={hover}>
            <button type="button" className={classes} onClick={() => navigate(item.target)} onKeyDown={event => menuKey(event, item)}>{item.label}</button>
            <button type="button" data-menu-toggle className={cx(classes, 'ts-nav__toggle')} aria-label={'Разделы: ' + item.label} aria-expanded={open === item.key && !panel} aria-haspopup="menu" aria-controls={menuId} onKeyDown={event => menuKey(event, item)} onClick={event => { opener.current = event.currentTarget; setPanel(null); if (activated.current === item.key && open === item.key) { setOpen(pinned || null); activated.current = null; } else { setOpen(item.key); activated.current = item.key; } }}><Icon name="chevron-down" size={14} /></button>
          </span> : <button type="button" key={item.key} className={classes} aria-expanded={item.menu ? open === item.key && !panel : undefined} aria-haspopup={item.menu ? 'menu' : undefined} aria-controls={item.menu ? menuId : undefined} onMouseEnter={event => { if (identityPanel) return; if (item.menu) { if (item.key === searchMenuKey) activated.current = null; openMenu(item, event.currentTarget, null); } else setOpen(null); }} onKeyDown={event => menuKey(event, item)} onClick={event => { opener.current = event.currentTarget; if (item.menu) { setPanel(null); if (item.key === searchMenuKey) { if (!panel && activated.current === item.key && open === item.key) { setOpen(pinned || null); activated.current = null; } else { setOpen(item.key); activated.current = item.key; } } else setOpen(open === item.key ? pinned || null : item.key); } else navigate(item.target ?? item.key); }}>{item.label}{item.menu ? <Icon name="chevron-down" size={14} /> : null}</button>;
        })}
      </nav>
      {!searchMenuKey ? <div style={{ flex: '1 1 0', minWidth: 0 }} /> : null}
      <label ref={searchField} className="ts-header__search"><Icon name="search" /><input aria-label={searchPlaceholder} aria-controls={searchMenuKey ? menuId : undefined} aria-expanded={searchMenuKey ? open === searchMenuKey && !panel : undefined} placeholder={searchPlaceholder} value={query} onFocus={event => { opener.current = event.currentTarget; if (!suppressFocus.current) { if (searchMenuKey) { activated.current = null; setPanel(null); setOpen(searchMenuKey); } else setPanel('search'); } }} onChange={event => { setInnerQuery(event.target.value); onSearch?.(event.target.value); if (searchMenuKey) { setPanel(null); setOpen(searchMenuKey); } }} onKeyDown={event => { if (searchMenuKey && event.key === 'ArrowDown') { event.preventDefault(); event.stopPropagation(); const item = items.find(item => item.key === searchMenuKey); if (item) openMenu(item, event.currentTarget, 'first'); } }} /></label>
      {searchMenuKey ? <div style={{ flex: '1 1 0', minWidth: 0 }} /> : null}
      {user ? <div className="ts-header__identity">
        <button type="button" onClick={event => openIdentity('notifications', event.currentTarget)} onKeyDown={event => identityKey(event, 'notifications')} aria-haspopup="menu" aria-controls={panel === 'notifications' ? menuId + '-identity' : undefined} aria-expanded={panel === 'notifications'} className="ts-btn ts-btn--ghost ts-btn--icon ts-bell" aria-label="Уведомления" style={{ width: 30, height: 30 }}><Icon name="bell" size={18} strokeWidth={1.8} />{notifications ? <span className="ts-bell__dot" /> : null}</button>
        <button type="button" onClick={event => openIdentity('user', event.currentTarget)} onKeyDown={event => identityKey(event, 'user')} aria-haspopup="menu" aria-controls={panel === 'user' ? menuId + '-identity' : undefined} aria-expanded={panel === 'user'} className="ts-header__user"><Avatar name={user.name} size={24} tone={0} />{user.short || user.name}<Icon name="chevron-down" size={14} /></button>
      </div> : <div style={{ display: 'flex', gap: 8 }}><Button variant="secondary" style={{ height: 30 }} onClick={onSignIn}>Войти</Button><Button variant="primary" style={{ height: 30 }} onClick={onSignUp}>Регистрация</Button></div>}
    </div>
    {panel === 'search' ? <div className="ts-mega"><div className="ts-mega__grid" style={{ display: 'flex', flexDirection: 'column' }}>
      {searchItems.filter(item => item.label.toLowerCase().includes(query.toLowerCase())).map((item, index) => <button key={index} type="button" className="ts-mega__link" onClick={() => { item.onClick?.(); closePanels(); }}>{item.label}</button>)}
      {searchItems.filter(item => item.label.toLowerCase().includes(query.toLowerCase())).length === 0 ? <span className="ts-muted">Ничего не найдено</span> : null}
    </div></div> : null}
    {identityPanel ? <div ref={identityPane} id={menuId + '-identity'} className="ts-header-popover ts-header-floating" style={identityAnchor || { right: 12, width: 320 }} role="menu" aria-label={panel === 'notifications' ? 'Уведомления' : 'Меню пользователя'} onKeyDown={event => {
      if (event.key === 'Tab' && event.shiftKey && event.target === identityPane.current?.querySelector('[role="menuitem"]')) { event.preventDefault(); identityOpener.current?.focus(); }
      else moveInMenu(event);
    }}>
      <div className="ts-header-popover__title">{panel === 'notifications' ? 'Уведомления' : user?.short || user?.name}</div>
      <div className="ts-header-popover__rows">{(panel === 'notifications' ? notificationItems : userMenuItems).map((item, index) => <button key={index} type="button" role="menuitem" className="ts-header-popover__item" onClick={() => { item.onClick?.(); closePanels(); }}>{item.label}</button>)}
        {(panel === 'notifications' ? notificationItems : userMenuItems).length === 0 ? <span className="ts-header-popover__empty" role="status">Нет элементов</span> : null}
      </div>
    </div> : null}
    {cur && !panel ? <div id={menuId} className={cx('ts-mega', unified && 'ts-mega--unified', unified && 'ts-header-floating', unified && searchAnchor && 'ts-mega--filtered')} style={unified ? { '--ts-menu-columns': Math.max(1, Math.min(4, cur.menu.cols.length)), ...(searchAnchor ? { left: 'auto', right: searchAnchor.right, width: searchAnchor.width } : {}) } : undefined} role="menu" aria-label={cur.label} onKeyDown={moveInMenu}>
      <div className={cx('ts-mega__grid', !unified && cur.menu.cols.length === 1 && 'ts-mega__grid--single')}>
        {cur.menu.cols.length === 0 ? <div className="ts-mega__empty" role="status">Ничего не найдено</div> : null}
        {cur.menu.cols.map(column => {
          const renderLink = link => <a key={link.key || link.t} role="menuitem" aria-current={link.target != null && link.target === currentTarget ? 'page' : undefined} href={link.target ? '#' + link.target : '#'} className={cx('ts-mega__link', link.target != null && link.target === currentTarget && 'ts-option--selected')} onClick={event => { event.preventDefault(); navigate(link.target ?? link.t); }}><b>{renderMenuText(link.t)}</b>{link.d ? <span>{link.d}</span> : null}</a>;
          return <div key={column.key || column.title} className={cx('ts-mega__col', column.target && 'ts-mega__col--linked')}>
            {column.target ? <a role="menuitem" href={'#' + column.target} aria-current={column.target === currentTarget ? 'page' : undefined} className={cx('ts-mega__title', 'ts-mega__title-link', column.target === currentTarget && 'ts-option--selected')} onClick={event => { event.preventDefault(); navigate(column.target); }}>{renderMenuText(column.title)}</a> : <div className="ts-mega__title">{renderMenuText(column.title)}</div>}
            {column.target ? <div className="ts-mega__children">{column.links.map(renderLink)}</div> : column.links.map(renderLink)}
          </div>;
        })}
        {cur.menu.feature ? <div className="ts-mega__feat"><span style={{ alignSelf: 'flex-start', font: '500 12px var(--font-sans)', padding: '3px 8px', borderRadius: 999, background: 'rgba(255,255,255,.12)' }}>{cur.menu.feature.tag}</span><div style={{ font: '700 18px/24px var(--font-sans)' }}>{cur.menu.feature.title}</div><div style={{ font: '400 13px/18px var(--font-sans)', color: '#C9CBD1' }}>{cur.menu.feature.text}</div><button type="button" role="menuitem" className="ts-btn ts-btn--sm" style={{ marginTop: 6, alignSelf: 'flex-start', background: 'var(--white)', color: 'var(--ink-900)' }} onClick={() => navigate(cur.target ?? cur.key)}>{cur.menu.feature.cta}</button></div> : null}
      </div>
    </div> : null}
  </div>;
}
