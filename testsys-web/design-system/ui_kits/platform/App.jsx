function App() {
  const { useToasts } = window.TS;
  const [screen, setScreen] = React.useState(localStorage.getItem('ts-kit-screen') || 'login');
  const [toast, toaster] = useToasts();
  const go = s => { setScreen(s); localStorage.setItem('ts-kit-screen', s); window.scrollTo(0, 0); };
  const tabs = [['login', 'Вход и регистрация'], ['profile', 'Профиль'], ['org', 'Панель организатора']];
  const S = { login: window.LoginScreen, profile: window.ProfileScreen, org: window.OrganizerScreen }[screen];
  return (
    <>
      <div style={{ display: 'flex', alignItems: 'center', gap: 4, height: 40, padding: '0 16px', background: 'var(--ink-900)', minWidth: 'var(--page-min)' }}>
        <span className="ts-mono" style={{ fontSize: 11, color: 'var(--ink-400)', marginRight: 12 }}>UI kit · Platform</span>
        {tabs.map(([k, l]) => <button key={k} onClick={() => go(k)} style={{ height: 26, padding: '0 10px', border: 0, borderRadius: 6, cursor: 'pointer', font: '600 12px var(--font-sans)', background: screen === k ? 'var(--white)' : 'transparent', color: screen === k ? 'var(--ink-900)' : '#C9CBD1' }}>{l}</button>)}
      </div>
      <S go={go} toast={toast} />
      {toaster}
    </>
  );
}
window.__tsReady.then(NS => { window.TS = NS; ReactDOM.createRoot(document.getElementById('root')).render(<App />); });
