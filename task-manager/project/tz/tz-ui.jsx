// tz-ui.jsx — shared Tzafon UI: phone frame, status bar, bottom nav,
// headers, task rows, chips, progress. Consumes tokens from tz-core.

// ── Android status bar (Den-tinted) ─────────────────────────
function StatusBar({ dark = false }) {
  const c = dark ? TZ.cream : TZ.ink;
  return (
    <div style={{ position: 'absolute', top: 0, left: 0, right: 0, height: 44, zIndex: 40,
      display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0 20px', pointerEvents: 'none' }}>
      <span style={{ fontFamily: TZ.mono, fontSize: 13.5, fontWeight: 600, color: c, letterSpacing: 0.3 }}>9:41</span>
      <div style={{ position: 'absolute', left: '50%', top: 12, transform: 'translateX(-50%)', width: 11, height: 11, borderRadius: 99, background: '#120C08' }} />
      <div style={{ display: 'flex', alignItems: 'center', gap: 5 }}>
        <svg width="15" height="12" viewBox="0 0 16 12" fill="none"><path d="M8 10.5L1 4a9.5 9.5 0 0114 0L8 10.5z" fill={c} opacity="0.9"/></svg>
        <svg width="15" height="12" viewBox="0 0 16 12"><path d="M14 11V1L2 11h12z" fill={c} opacity="0.9"/></svg>
        <svg width="20" height="12" viewBox="0 0 22 12"><rect x="1" y="1.5" width="18" height="9" rx="2.2" stroke={c} strokeWidth="1.2" fill="none" opacity="0.9"/><rect x="2.6" y="3" width="12" height="6" rx="1" fill={c} opacity="0.9"/><rect x="20" y="4.2" width="1.6" height="3.6" rx="0.8" fill={c} opacity="0.9"/></svg>
      </div>
    </div>
  );
}

function GesturePill({ dark = false }) {
  return (
    <div style={{ position: 'absolute', bottom: 0, left: 0, right: 0, height: 22, display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 41, pointerEvents: 'none' }}>
      <div style={{ width: 128, height: 4.5, borderRadius: 3, background: dark ? 'rgba(255,255,255,0.5)' : tzA(TZ.ink, 0.32) }} />
    </div>
  );
}

// ── Bottom navigation — the 5 primary views ─────────────────
const NAV_ITEMS = [
  ['today', 'Today', TI.Today],
  ['planning', 'Planning', TI.Plan],
  ['habits', 'Habits', TI.Habit],
  ['directions', 'Directions', TI.Compass],
  ['journey', 'Journey', TI.Journey],
];

function BottomNav({ active = 'today' }) {
  return (
    <div style={{ position: 'absolute', bottom: 0, left: 0, right: 0, height: 84, zIndex: 35,
      background: tzA(TZ.surface, 0.94), backdropFilter: 'blur(8px)', borderTop: '1px solid ' + TZ.line,
      display: 'flex', paddingBottom: 22 }}>
      {NAV_ITEMS.map(([key, label, Icon]) => {
        const on = key === active;
        return (
          <div key={key} style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 3, paddingTop: 8 }}>
            <div style={{ width: 46, height: 26, borderRadius: 999, display: 'flex', alignItems: 'center', justifyContent: 'center',
              background: on ? tzA(TZ.rust, 0.14) : 'transparent' }}>
              <Icon s={on ? 22 : 21} c={on ? TZ.rust : TZ.faint} w={on ? 2.1 : 1.8} />
            </div>
            <span style={{ fontFamily: TZ.mono, fontSize: 9.5, letterSpacing: 0.4, fontWeight: on ? 700 : 500,
              color: on ? TZ.rust : TZ.faint }}>{label}</span>
          </div>
        );
      })}
    </div>
  );
}

// ── Phone frame ─────────────────────────────────────────────
function Phone({ children, statusDark = false, nav = null, pillDark = false }) {
  return (
    <div style={{ width: 412, height: 892, borderRadius: 48, background: '#241A12', padding: 9, boxSizing: 'border-box',
      boxShadow: '0 30px 64px rgba(60,30,12,0.30), 0 10px 22px rgba(60,30,12,0.16)' }}>
      <div style={{ position: 'relative', width: '100%', height: '100%', borderRadius: 40, overflow: 'hidden', background: TZ.bg }}>
        {children}
        <StatusBar dark={statusDark} />
        {nav ? <BottomNav active={nav} /> : <GesturePill dark={pillDark} />}
        {nav && <GesturePill dark={pillDark} />}
      </div>
    </div>
  );
}

// ── text + label primitives ─────────────────────────────────
function Kicker({ children, c = TZ.faint, style }) {
  return <div style={{ fontFamily: TZ.mono, fontSize: 11, letterSpacing: 2.5, textTransform: 'uppercase', fontWeight: 600, color: c, ...style }}>{children}</div>;
}
function SectionLabel({ children, c = TZ.faint, style }) {
  return <div style={{ fontFamily: TZ.mono, fontSize: 10.5, letterSpacing: 1.4, textTransform: 'uppercase', fontWeight: 600, color: c, ...style }}>{children}</div>;
}
function GroupHeader({ label, count, accent, right }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 9, padding: '18px 0 8px' }}>
      <span style={{ fontFamily: TZ.mono, fontSize: 11, letterSpacing: 1.4, textTransform: 'uppercase', color: accent || TZ.muted, fontWeight: 700 }}>{label}</span>
      {count != null && <span style={{ fontFamily: TZ.mono, fontSize: 11, color: TZ.faint }}>{count}</span>}
      <div style={{ flex: 1, height: 1, background: TZ.line }} />
      {right}
    </div>
  );
}

// ── rust header (Action layer) ──────────────────────────────
function RustHeader({ kicker = "DON'T PANIC", title, meta, progress, right, compass, compact }) {
  return (
    <div style={{ flexShrink: 0, background: TZ.rust, color: TZ.cream, padding: compact ? '48px 20px 14px' : '52px 20px 18px', position: 'relative', overflow: 'hidden' }}>
      {compass && <div style={{ position: 'absolute', top: 38, right: -20, opacity: 0.13 }}><Compass s={140} ring={TZ.cream} needleN={TZ.cream} needleS={TZ.cream} stroke={1.2} /></div>}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', position: 'relative' }}>
        <Kicker c={tzA('#FBF4E6', 0.82)}>{kicker}</Kicker>
        <div style={{ fontSize: 19, display: 'flex', alignItems: 'center' }}>{right || <FoxLogo s={30} ring={tzA('#FBF4E6', 0.9)} />}</div>
      </div>
      <h1 style={{ margin: compact ? '4px 0 0' : '7px 0 0', fontFamily: TZ.serif, fontSize: compact ? 28 : 37, fontWeight: 600, letterSpacing: -0.5, lineHeight: 1, position: 'relative' }}>{title}</h1>
      {(meta || progress) && (
        <div style={{ marginTop: 14, position: 'relative' }}>
          {meta && <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', fontFamily: TZ.mono, fontSize: 11.5, letterSpacing: 0.4, color: tzA('#FBF4E6', 0.92) }}>{meta}</div>}
          {progress != null && (
            <div style={{ marginTop: 9, height: 9, borderRadius: 6, background: 'rgba(255,255,255,0.22)', overflow: 'hidden' }}>
              <div style={{ width: progress + '%', height: '100%', background: TZ.amber, borderRadius: 6 }} />
            </div>
          )}
        </div>
      )}
    </div>
  );
}

// ── layer header (Direction / Reflection) — cream, editorial ─
function LayerHeader({ kicker, title, sub, accent = TZ.rust, compass = false, mascot }) {
  return (
    <div style={{ flexShrink: 0, background: TZ.surface, padding: '52px 20px 16px', borderBottom: '1px solid ' + TZ.line, position: 'relative', overflow: 'hidden' }}>
      {compass && <div style={{ position: 'absolute', top: 34, right: 16, opacity: 0.9 }}><Compass s={46} ring={accent} needleN={accent} needleS={TZ.faint} /></div>}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Kicker c={accent}>{kicker}</Kicker>
        <div style={{ fontSize: 19, display: 'flex', alignItems: 'center' }}>{mascot || (compass ? null : <FoxLogo s={30} ring={tzA(TZ.ink, 0.06)} />)}</div>
      </div>
      <h1 style={{ margin: '6px 0 0', fontFamily: TZ.serif, fontSize: 33, fontWeight: 600, letterSpacing: -0.4, lineHeight: 1.02, color: TZ.ink, maxWidth: 300 }}>{title}</h1>
      {sub && <p style={{ margin: '9px 0 0', fontSize: 13.5, lineHeight: 1.45, color: TZ.muted, maxWidth: 320 }}>{sub}</p>}
    </div>
  );
}

// ── progress bar ────────────────────────────────────────────
function Bar({ pct, fill = TZ.amber, track = tzA(TZ.ink, 0.1), h = 8, radius = 5 }) {
  return (
    <div style={{ height: h, borderRadius: radius, background: track, overflow: 'hidden', width: '100%' }}>
      <div style={{ width: Math.max(0, Math.min(100, pct)) + '%', height: '100%', background: fill, borderRadius: radius }} />
    </div>
  );
}

function Dot({ c, s = 7 }) { return <span style={{ width: s, height: s, borderRadius: 99, background: c, flexShrink: 0, display: 'inline-block' }} />; }

// The Fox Works logo — a bespectacled fox roundel (transparent corners).
function FoxLogo({ s = 32, ring, style }) {
  return <img src="tz/foxworks-logo.png" alt="The Fox Works"
    style={{ width: s, height: s, borderRadius: '50%', objectFit: 'cover', display: 'block',
      boxShadow: ring ? '0 0 0 2px ' + ring : 'none', ...style }} />;
}

// ── chips ───────────────────────────────────────────────────
function Chip({ children, c = TZ.muted, bg = 'transparent', border, icon, style }) {
  return (
    <span style={{ display: 'inline-flex', alignItems: 'center', gap: 5, fontFamily: TZ.mono, fontSize: 11, letterSpacing: 0.2, color: c,
      background: bg, border: border ? '1px solid ' + border : 'none', borderRadius: 5, padding: border || bg !== 'transparent' ? '2px 7px' : '0', whiteSpace: 'nowrap', ...style }}>
      {icon}{children}
    </span>
  );
}

// "serves: <theme>" — the auto-bridge made visible
function Serves({ themeKey, label, plus, c }) {
  const accent = themeKey && THEMES[themeKey] ? THEMES[themeKey].accent : (c || TZ.muted);
  return (
    <span style={{ display: 'inline-flex', alignItems: 'center', gap: 5, fontFamily: TZ.mono, fontSize: 11, color: TZ.muted, whiteSpace: 'nowrap' }}>
      <Dot c={accent} s={7} />
      <span style={{ color: TZ.faint }}>serves</span>
      <span style={{ color: tzA(accent, 1), fontWeight: 600 }}>{label}</span>
      {plus ? <span style={{ color: TZ.faint }}>+{plus}</span> : null}
    </span>
  );
}

function CueChip({ children, icon }) {
  return <Chip c={TZ.muted} icon={icon || <TI.Cue s={12} c={TZ.amber} />}>{children}</Chip>;
}

// state pill for done/closed/frozen/backlog
function StateTag({ state }) {
  const map = {
    done: ['DONE', TZ.green], closed: ['CLOSED', TZ.closed], frozen: ['FROZEN', TZ.frozen], backlog: ['SOMEDAY', TZ.backlog],
  };
  const [t, c] = map[state] || ['', TZ.muted];
  if (!t) return null;
  return <span style={{ fontFamily: TZ.mono, fontSize: 10, letterSpacing: 0.6, color: c, border: '1px solid ' + tzA(c, 0.5), borderRadius: 4, padding: '1px 6px' }}>{t}</span>;
}

// ── the reusable task row ───────────────────────────────────
function Checkbox({ state = 'open' }) {
  if (state === 'done') return <div style={box(TZ.rust, TZ.rust)}><TI.Check s={15} /></div>;
  if (state === 'closed') return <div style={box(tzA(TZ.closed, 0.5), 'transparent')}><TI.Skip s={14} c={TZ.closed} /></div>;
  if (state === 'frozen') return <div style={box(tzA(TZ.frozen, 0.5), tzA(TZ.frozen, 0.12))}><TI.Frozen s={13} c={TZ.frozen} /></div>;
  if (state === 'backlog') return <div style={box(tzA(TZ.backlog, 0.5), 'transparent')}><TI.Moon s={13} c={TZ.backlog} /></div>;
  return <div style={box(tzA(TZ.ink, 0.3), 'transparent')} />;
}
function box(borderC, bg) {
  return { flexShrink: 0, width: 25, height: 25, border: '2px solid ' + borderC, borderRadius: 7, background: bg,
    display: 'flex', alignItems: 'center', justifyContent: 'center' };
}

function TaskRow({ title, state = 'open', serves, servesLabel, plus, cue, recur, due, habit, focus, last, chevron = true }) {
  const struck = state === 'done' || state === 'closed';
  const meta = [];
  if (serves || servesLabel) meta.push(<Serves key="s" themeKey={serves} label={servesLabel || (serves && THEMES[serves] && THEMES[serves].name)} plus={plus} />);
  if (habit) meta.push(<Chip key="h" c={TZ.muted} icon={<TI.Repeat s={12} c={TZ.faint} />}>{habit}</Chip>);
  if (cue) meta.push(<CueChip key="c">{cue}</CueChip>);
  if (recur) meta.push(<Chip key="r" c={TZ.muted} icon={<TI.Repeat s={12} c={TZ.faint} />}>{recur}</Chip>);
  return (
    <div style={{ display: 'flex', alignItems: 'flex-start', gap: 13, padding: '13px 0',
      borderBottom: last ? 'none' : '1px solid ' + TZ.line, opacity: state === 'done' ? 0.62 : 1 }}>
      <div style={{ paddingTop: 1 }}><Checkbox state={state} /></div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <div style={{ flex: 1, minWidth: 0, fontSize: 16, lineHeight: 1.3, color: struck ? TZ.muted : TZ.ink,
            textDecoration: struck ? 'line-through' : 'none', textDecorationColor: tzA(TZ.rust, 0.5),
            fontWeight: focus ? 600 : 400, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{title}</div>
          {due === true && <span style={{ fontFamily: TZ.mono, fontSize: 10, color: '#fff', background: TZ.due, borderRadius: 4, padding: '2px 6px', whiteSpace: 'nowrap' }}>DUE</span>}
          {due === 'today' && <span style={{ display: 'inline-flex', alignItems: 'center', gap: 3, fontFamily: TZ.mono, fontSize: 10, color: '#fff', background: TZ.due, borderRadius: 4, padding: '2px 6px', whiteSpace: 'nowrap' }}><TI.Alert s={10} c="#fff" />DUE TODAY</span>}
          <StateTag state={state === 'open' ? null : state} />
        </div>
        {meta.length > 0 && (
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginTop: 6, flexWrap: 'wrap' }}>{meta}</div>
        )}
      </div>
      {chevron && <div style={{ paddingTop: 3 }}><TI.Chevron s={17} c={tzA(TZ.ink, 0.26)} /></div>}
    </div>
  );
}

function Card({ children, style, pad = 16, onDark }) {
  return <div style={{ background: TZ.card, border: '1px solid ' + TZ.line, borderRadius: 15, padding: pad, ...style }}>{children}</div>;
}

// closeable slippage banner
function Banner({ children, tone = TZ.rust, icon }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 11, background: tzA(tone, 0.08), border: '1px solid ' + tzA(tone, 0.28),
      borderRadius: 12, padding: '11px 12px 11px 14px' }}>
      <div style={{ flexShrink: 0 }}>{icon || <TI.Alert s={16} c={tone} />}</div>
      <div style={{ flex: 1, fontSize: 13, lineHeight: 1.35, color: TZ.ink }}>{children}</div>
      <button style={{ flexShrink: 0, width: 26, height: 26, borderRadius: 7, border: 'none', background: 'transparent', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }}><TI.X s={13} c={TZ.muted} /></button>
    </div>
  );
}

// supportive nudge (overload / stale-goal) — fox voice
function Nudge({ children, action }) {
  return (
    <div style={{ background: tzA(TZ.amber, 0.14), border: '1px solid ' + tzA(TZ.amber, 0.4), borderRadius: 13, padding: '13px 14px' }}>
      <div style={{ display: 'flex', gap: 9 }}>
        <Compass s={17} ring={TZ.rust} needleN={TZ.rust} needleS={TZ.faint} stroke={2} />
        <div style={{ flex: 1, fontSize: 13.5, lineHeight: 1.45, color: TZ.ink }}>{children}</div>
      </div>
      {action && <div style={{ marginTop: 10, display: 'flex', gap: 8 }}>{action}</div>}
    </div>
  );
}

function FAB({ icon, sub, style }) {
  return (
    <button style={{ position: 'absolute', right: 20, bottom: 104, zIndex: 34, height: 58, minWidth: 58, borderRadius: 18, border: 'none',
      background: TZ.rust, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8, padding: sub ? '0 20px 0 18px' : 0,
      boxShadow: '0 12px 26px ' + tzA(TZ.rust, 0.42), ...style }}>
      {icon || <TI.Plus s={25} />}
      {sub && <span style={{ color: '#fff', fontFamily: TZ.body, fontSize: 15, fontWeight: 600 }}>{sub}</span>}
    </button>
  );
}

// scroll region that fills between header and bottom nav
function Body({ children, style, pad = '0 20px', navPad = true }) {
  return <div style={{ flex: 1, overflow: 'hidden', padding: pad, paddingBottom: navPad ? 96 : 24, position: 'relative', ...style }}>{children}</div>;
}

function Screen({ children, statusDark, nav, bg = TZ.bg }) {
  return (
    <div style={{ position: 'absolute', inset: 0, background: bg, color: TZ.ink, fontFamily: TZ.body, display: 'flex', flexDirection: 'column' }}>
      {children}
    </div>
  );
}

Object.assign(window, {
  StatusBar, GesturePill, BottomNav, NAV_ITEMS, Phone, Kicker, SectionLabel, GroupHeader,
  RustHeader, LayerHeader, Bar, Dot, FoxLogo, Chip, Serves, CueChip, StateTag, Checkbox, TaskRow, Card,
  Banner, Nudge, FAB, Body, Screen,
});
