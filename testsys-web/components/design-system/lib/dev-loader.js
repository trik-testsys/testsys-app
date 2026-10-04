// Preview helper: resolves the TestSys component namespace.
// Uses the compiled design-system bundle when present; otherwise transpiles components/*.jsx in the browser (needs React + Babel standalone loaded first).
(function () {
  var ORDER = ["core/Icon","actions/Button","actions/IconButton","actions/DownloadButton","forms/Checkbox","forms/Field","forms/Input","forms/Textarea","forms/Select","forms/MultiSelect","forms/Radio","forms/Switch","forms/SegmentedControl","display/ProgressBar","forms/FileDrop","forms/DateRangeCalendar","display/StatusBadge","display/Verdict","display/Tag","display/Counter","display/Avatar","display/AvatarGroup","display/Difficulty","display/Timer","layout/Block","layout/Row","layout/Footer","layout/Page","display/StatCard","display/ContestCard","data/DataTable","data/Leaderboard","data/SortableList","navigation/Header","navigation/Tabs","navigation/PillTabs","navigation/Breadcrumbs","layout/PageHead","navigation/Pagination","navigation/FilterChip","navigation/Stepper","feedback/Toast","feedback/Alert","feedback/EmptyState","feedback/Skeleton","overlays/Popover","overlays/Menu","overlays/Dialog","overlays/Drawer","overlays/Tooltip","quiz/QuizOption","quiz/QuestionNav","forms/CodeEditor"];
  function findBundle() {
    for (var k of Object.keys(window)) { try { var v = window[k]; if (v && typeof v === 'object' && v.Button && v.Block && v.Header) return v; } catch (e) {} }
    return null;
  }
  var base = (document.currentScript && document.currentScript.src || '').replace(/lib\/dev-loader\.js.*$/, '');
  window.__tsReady = (async function () {
    var ns = findBundle();
    if (!ns) {
      var has = false; try { has = (await fetch(base + '_ds_bundle.js', { method: 'HEAD' })).ok; } catch (e) {}
      if (has) await new Promise(function (res) { var s = document.createElement('script'); s.src = base + '_ds_bundle.js'; s.onload = res; s.onerror = res; document.head.appendChild(s); });
      ns = findBundle();
    }
    if (ns) return ns;
    var scope = {};
    Object.assign(scope, await import(base + 'lib/search-text.mjs'), await import(base + 'lib/popover-geometry.mjs'), await import(base + 'lib/brand-assets.mjs'));
    for (var p of ORDER) {
      var src = await (await fetch(base + 'components/' + p + '.jsx')).text();
      src = src.replace(/^import[^\n]*\n/gm, '').replace(/^export function /gm, 'function ');
      var names = Array.from(src.matchAll(/^function ([A-Z]\w*|use\w+)/gm)).map(function (m) { return m[1]; });
      var code = Babel.transform(src, { presets: ['react'] }).code;
      var keys = Object.keys(scope).filter(function (k) { return names.indexOf(k) < 0; });
      var fn = new Function('React', 'scope', 'const {' + keys.join(',') + '} = scope;\n' + code + '\nreturn {' + names.join(',') + '};');
      Object.assign(scope, fn(React, scope));
    }
    window.TestSys = scope;
    return scope;
  })();
})();
