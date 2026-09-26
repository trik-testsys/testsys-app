import React from 'react';
import { Icon } from '../core/Icon.jsx';
import { Avatar } from '../display/Avatar.jsx';
import { Button } from '../actions/Button.jsx';
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

export function Header({ brand = 'TestSys', brandMark = 'T', items = DEFAULT_ITEMS, active, user, notifications = false, searchPlaceholder = 'Поиск задач, соревнований…', onNavigate, onSignIn, onSignUp, pinned }) {
  const [open, setOpen] = React.useState(pinned || null);
  const cur = items.find(i => i.key === open && i.menu);
  return (
    <div className="ts-header" onMouseLeave={() => setOpen(pinned || null)}>
      <div className="ts-header__bar">
        <a href="#" className="ts-brand" onClick={e => { e.preventDefault(); onNavigate && onNavigate('home'); }}><span className="ts-brand__mark">{brandMark}</span>{brand}</a>
        <nav className="ts-nav">
          {items.map(it => (
            <button type="button" key={it.key} className={cx('ts-nav__item', it.key === active && 'ts-nav__item--active')}
              onMouseEnter={() => setOpen(it.menu ? it.key : null)}
              onClick={() => { if (it.menu) setOpen(open === it.key ? null : it.key); else onNavigate && onNavigate(it.key); }}>
              {it.label}{it.menu ? <Icon name="chevron-down" size={14} /> : null}
            </button>
          ))}
        </nav>
        <div style={{ flex: '1 1 0', minWidth: 0 }} />
        <label className="ts-header__search"><Icon name="search" /><input placeholder={searchPlaceholder} /><span className="ts-kbd">⌘K</span></label>
        {user ? (
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <button type="button" className="ts-btn ts-btn--ghost ts-btn--icon ts-bell" aria-label="Уведомления" style={{ width: 30, height: 30 }}>
              <Icon name="bell" size={18} strokeWidth={1.8} />{notifications ? <span className="ts-bell__dot" /> : null}
            </button>
            <button type="button" className="ts-header__user"><Avatar name={user.name} size={24} tone={0} />{user.short || user.name}<Icon name="chevron-down" size={14} /></button>
          </div>
        ) : (
          <div style={{ display: 'flex', gap: 8 }}>
            <Button variant="secondary" style={{ height: 30 }} onClick={onSignIn}>Войти</Button>
            <Button variant="primary" style={{ height: 30 }} onClick={onSignUp}>Регистрация</Button>
          </div>
        )}
      </div>
      {cur ? (
        <div className="ts-mega">
          <div className="ts-mega__grid">
            {cur.menu.cols.map(col => (
              <div key={col.title} className="ts-mega__col">
                <div className="ts-mega__title">{col.title}</div>
                {col.links.map(l => <a key={l.t} href="#" className="ts-mega__link" onClick={e => { e.preventDefault(); onNavigate && onNavigate(l.t); }}><b>{l.t}</b><span>{l.d}</span></a>)}
              </div>
            ))}
            {cur.menu.feature ? (
              <div className="ts-mega__feat">
                <span style={{ alignSelf: 'flex-start', font: '500 12px var(--font-sans)', padding: '3px 8px', borderRadius: 999, background: 'rgba(255,255,255,.12)' }}>{cur.menu.feature.tag}</span>
                <div style={{ font: '700 18px/24px var(--font-sans)' }}>{cur.menu.feature.title}</div>
                <div style={{ font: '400 13px/18px var(--font-sans)', color: '#C9CBD1' }}>{cur.menu.feature.text}</div>
                <button type="button" className="ts-btn ts-btn--sm" style={{ marginTop: 6, alignSelf: 'flex-start', background: 'var(--white)', color: 'var(--ink-900)' }}>{cur.menu.feature.cta}</button>
              </div>
            ) : null}
          </div>
        </div>
      ) : null}
    </div>
  );
}
