function App() {
  const { useToasts, Button } = window.TS;
  const navigation = window.PrototypeNavigation;
  const [route, setRoute] = React.useState(() => navigation.resolvePrototypeRoute(window.location.hash || localStorage.getItem('ts-kit-screen')));
  const [searchQuery, setSearchQuery] = React.useState('');
  const [authMode, setAuthMode] = React.useState('in');
  const [selections, setSelections] = React.useState({});
  const [state, dispatch] = React.useReducer(window.DemoModel.reduceDemoState, null, window.DemoModel.createDemoState);
  const [toast, toaster] = useToasts();
  const timers = React.useRef(new Map());
  React.useEffect(() => {
    const queued = state.solutions.filter(solution => solution.status === 'Queue' && !timers.current.has(solution.id));
    queued.forEach(solution => {
      const checking = setTimeout(() => dispatch({ type: 'checkSolution', solutionId: solution.id }), 600);
      const finished = setTimeout(() => { dispatch({ type: 'finishSolution', solutionId: solution.id, score: solution.fileName.length % 2 ? 64 : 0 }); timers.current.delete(solution.id); }, 1800);
      timers.current.set(solution.id, [checking, finished]);
    });
  }, [state.solutions]);
  React.useEffect(() => () => { timers.current.forEach(ids => ids.forEach(clearTimeout)); timers.current.clear(); }, []);
  React.useEffect(() => {
    const canonical = '#' + route.key;
    if (window.location.hash !== canonical) window.history.replaceState(null, '', canonical);
    localStorage.setItem('ts-kit-screen', route.key);
    const changed = () => {
      const next = navigation.resolvePrototypeRoute(window.location.hash);
      if (window.location.hash !== '#' + next.key) window.history.replaceState(null, '', '#' + next.key);
      setSearchQuery(''); setRoute(next); dispatch({ type: 'clearMessage' }); localStorage.setItem('ts-kit-screen', next.key); window.scrollTo(0, 0);
    };
    window.addEventListener('hashchange', changed); return () => window.removeEventListener('hashchange', changed);
  }, []);
  const go = (value, options = {}) => {
    setSearchQuery('');
    const next = navigation.resolvePrototypeRoute(value);
    dispatch({ type: 'clearMessage' });
    if (next.key === 'login') setAuthMode(options.authMode || 'in');
    if (window.location.hash !== '#' + next.key) window.location.hash = next.key;
    setRoute(next); localStorage.setItem('ts-kit-screen', next.key); window.scrollTo(0, 0);
  };
  const reset = () => { timers.current.forEach(ids => ids.forEach(clearTimeout)); timers.current.clear(); dispatch({ type: 'reset' }); setSearchQuery(''); setSelections({}); toast({ title: 'Демонстрационные данные сброшены' }); };
  const cabinet = navigation.cabinetDefinitions.find(cabinet => cabinet.key === route.cabinetKey);
  const actor = cabinet ? window.DemoModel.getCabinetActor(state, cabinet.role) : null;
  const context = actor ? navigation.selectionContextKey(cabinet.key, actor.id) : null;
  const selection = selections[context] || {};
  const updateSelection = patch => setSelections(current => navigation.updatePrototypeSelection(current, cabinet.key, actor.id, patch));
  const props = { route, go, toast, state, dispatch, actor, selection, updateSelection, searchQuery, onSearch: setSearchQuery };
  let screen;
  if (route.key === 'home') screen = <HomeScreen {...props} />;
  else if (route.key === 'login') screen = <LoginScreen {...props} authMode={authMode} onAuthModeChange={setAuthMode} />;
  else if (route.sectionKey !== 'overview' && route.cabinetKey === 'student') screen = <ProfileScreen key={context} {...props} />;
  else if (route.sectionKey !== 'overview' && route.cabinetKey === 'organizer') screen = <OrganizerScreen key={context} {...props} />;
  else screen = <CabinetScreen key={context} {...props} />;
  return <><div className="ts-demo-bar"><span className="ts-mono">TestSys · демонстрация</span><Button size="sm" variant="secondary" onClick={() => go('login')}>Вход и регистрация</Button><a href="../../components/index.html">Все компоненты</a><span style={{ flex: 1 }} /><Button size="sm" variant="secondary" onClick={reset}>Сбросить данные</Button></div>{screen}{toaster}</>;
}
Promise.all([window.__tsReady, window.__demoReady, window.__navigationReady]).then(([namespace, model, navigation]) => { window.TS = namespace; window.DemoModel = model; window.PrototypeNavigation = navigation; ReactDOM.createRoot(document.getElementById('root')).render(<App />); });
