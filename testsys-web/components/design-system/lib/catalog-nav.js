// Shared plain-link navigation for the reference catalogue.
(function () {
  const script = document.currentScript;
  const base = script.src.replace(/lib\/catalog-nav\.js.*$/, '');
  const nav = document.createElement('nav');
  nav.setAttribute('aria-label', 'Каталог компонентов'); nav.className = 'ts-hstack';
  nav.style.cssText = 'padding:12px;margin-bottom:16px;background:var(--surface-card);border:1px solid var(--line);border-radius:var(--radius-lg)';
  const groups = [['core/icons', 'Иконки'], ['actions/actions', 'Действия'], ['forms/forms', 'Поля'], ['display/display', 'Отображение'], ['data/data', 'Данные'], ['layout/layout', 'Компоновка'], ['navigation/navigation', 'Навигация'], ['feedback/feedback', 'Обратная связь'], ['overlays/overlays', 'Наложения'], ['quiz/quiz', 'Квиз']];
  const links = [['ui_kits/platform/index.html', 'Прототип TestSys'], ['components/index.html', 'Все компоненты'], ...groups.map(([path, label]) => ['components/' + path + '.card.html', label])];
  links.forEach(([path, label]) => { const link = document.createElement('a'); link.href = base + path; link.textContent = label; nav.appendChild(link); });
  document.body.prepend(nav);
})();
