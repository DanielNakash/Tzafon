// tz-direction.jsx — Direction layer: Directions hub (Compass board).
// Theme → Goal | Habit hierarchy, the "serves:" auto-bridge made visible.

function Ring({ pct, s = 46, sw = 5, c = TZ.rust, track = tzA(TZ.ink, 0.12) }) {
  const r = (s - sw) / 2, C = 2 * Math.PI * r;
  return (
    <svg width={s} height={s} viewBox={`0 0 ${s} ${s}`}>
      <circle cx={s / 2} cy={s / 2} r={r} fill="none" stroke={track} strokeWidth={sw} />
      <circle cx={s / 2} cy={s / 2} r={r} fill="none" stroke={c} strokeWidth={sw} strokeLinecap="round"
        strokeDasharray={C} strokeDashoffset={C * (1 - pct / 100)} transform={`rotate(-90 ${s / 2} ${s / 2})`} />
    </svg>
  );
}

function TypeTag({ type, c = TZ.faint }) {
  return <span style={{ fontFamily: TZ.mono, fontSize: 9.5, letterSpacing: 0.8, color: c, border: '1px solid ' + tzA(c, 0.4), borderRadius: 4, padding: '1px 6px', textTransform: 'uppercase' }}>{type}</span>;
}

function GoalCard({ g }) {
  const th = g.theme ? THEMES[g.theme] : null;
  const accent = th ? th.accent : TZ.muted;
  const pct = g.type === 'Stepped'
    ? Math.round(g.steps.filter(s => s[1]).length / g.steps.length * 100)
    : g.type === 'Accumulative' ? Math.round(g.cur / g.target * 100) : 40;
  return (
    <div style={{ background: TZ.card, border: '1px solid ' + TZ.line, borderRadius: 13, padding: '13px 14px' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 7 }}>
        <TI.Target s={15} c={accent} />
        <TypeTag type={g.type} c={accent} />
        <div style={{ flex: 1 }} />
        {th ? <Serves themeKey={g.theme} label={th.name.length > 16 ? th.name.slice(0, 15) + '…' : th.name} /> : <span style={{ fontFamily: TZ.mono, fontSize: 10, color: TZ.faint }}>NO THEME</span>}
      </div>
      <div style={{ fontFamily: TZ.serif, fontSize: 17, fontWeight: 600, color: TZ.ink, lineHeight: 1.15 }}>{g.title}</div>

      {g.type === 'Accumulative' && (
        <div style={{ marginTop: 10 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', fontFamily: TZ.mono, fontSize: 11, color: TZ.muted, marginBottom: 5 }}>
            <span style={{ color: accent, fontWeight: 700 }}>{g.cur.toLocaleString()}</span>
            <span>{g.target.toLocaleString()} {g.unit}</span>
          </div>
          <Bar pct={pct} fill={accent} />
          {g.note && <div style={{ fontFamily: TZ.mono, fontSize: 10.5, color: TZ.amber, marginTop: 7 }}>↗ {g.note}</div>}
        </div>
      )}
      {g.type === 'Stepped' && (
        <div style={{ marginTop: 10 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', fontFamily: TZ.mono, fontSize: 11, color: TZ.muted, marginBottom: 7 }}>
            <span>{g.steps.filter(s => s[1]).length} of {g.steps.length} steps</span><span style={{ color: accent, fontWeight: 700 }}>{pct}%</span>
          </div>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '5px 12px' }}>
            {g.steps.map(([label, done], i) => (
              <span key={i} style={{ display: 'inline-flex', alignItems: 'center', gap: 5, fontSize: 12, color: done ? TZ.muted : TZ.ink }}>
                {done ? <span style={{ width: 14, height: 14, borderRadius: 4, background: accent, display: 'inline-flex', alignItems: 'center', justifyContent: 'center' }}><TI.Check s={10} /></span>
                  : <span style={{ width: 14, height: 14, borderRadius: 4, border: '1.5px solid ' + tzA(TZ.ink, 0.28) }} />}
                <span style={{ textDecoration: done ? 'line-through' : 'none', textDecorationColor: tzA(accent, 0.5) }}>{label}</span>
              </span>
            ))}
          </div>
        </div>
      )}
      {g.type === 'Generic' && g.note && <div style={{ marginTop: 8, fontSize: 12.5, color: TZ.muted, fontStyle: 'italic' }}>{g.note}</div>}

      {g.habits && g.habits.length > 0 && (
        <div style={{ display: 'flex', alignItems: 'center', gap: 7, marginTop: 11, paddingTop: 10, borderTop: '1px solid ' + TZ.line2, flexWrap: 'wrap' }}>
          <span style={{ fontFamily: TZ.mono, fontSize: 9.5, color: TZ.faint, letterSpacing: 0.5 }}>ENGINE</span>
          {g.habits.map(h => <span key={h} style={{ display: 'inline-flex', alignItems: 'center', gap: 4, fontFamily: TZ.mono, fontSize: 10.5, color: accent, background: tzA(accent, 0.1), borderRadius: 999, padding: '3px 9px' }}><TI.Repeat s={11} c={accent} />{h}</span>)}
        </div>
      )}
    </div>
  );
}

function HabitChip({ h }) {
  const accent = THEMES[h.theme].accent;
  const done = h.doneThis, tot = h.kind === 'frequency' ? h.target : h.targetDays;
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 9, padding: '9px 12px', background: TZ.card, border: '1px solid ' + TZ.line, borderRadius: 10 }}>
      <TI.Repeat s={13} c={accent} />
      <span style={{ fontSize: 13.5, fontWeight: 500 }}>{h.name}</span>
      <div style={{ display: 'flex', gap: 3, marginLeft: 2 }}>
        {Array.from({ length: tot }).map((_, i) => <span key={i} style={{ width: 6, height: 6, borderRadius: 99, background: i < done ? accent : tzA(TZ.ink, 0.14) }} />)}
      </div>
      <span style={{ fontFamily: TZ.mono, fontSize: 10, color: TZ.muted, marginLeft: 'auto' }}>{done}/{tot}</span>
    </div>
  );
}

// Active / Upcoming / Archive selector — how non-active themes are reached.
function ThemeScope({ active = 'active' }) {
  const opts = [['active', 'ACTIVE · 3'], ['upcoming', 'UPCOMING · 1'], ['archive', 'ARCHIVE · 4']];
  return (
    <div style={{ display: 'flex', background: TZ.surfaceAlt, borderRadius: 999, padding: 3, border: '1px solid ' + TZ.line }}>
      {opts.map(([k, label]) => {
        const on = k === active;
        return <span key={k} style={{ flex: 1, textAlign: 'center', fontFamily: TZ.mono, fontSize: 10.5, letterSpacing: 0.2, padding: '6px 4px', borderRadius: 999, background: on ? TZ.rust : 'transparent', color: on ? '#fff' : TZ.muted, fontWeight: on ? 700 : 500 }}>{label}</span>;
      })}
    </div>
  );
}

// the compass rose with three bearing dots
function BearingRose({ s = 132 }) {
  const keys = ['health', 'novel', 'japanese'];
  return (
    <div style={{ position: 'relative', width: s, height: s }}>
      <Compass s={s} ring={tzA(TZ.rust, 0.55)} needleN={TZ.rust} needleS={TZ.faint} stroke={1.2} />
      {keys.map((tk, i) => {
        const rad = (-90 + i * 120) * Math.PI / 180;
        const x = s / 2 + Math.cos(rad) * (s / 2 - 16), y = s / 2 + Math.sin(rad) * (s / 2 - 16);
        return <span key={tk} style={{ position: 'absolute', left: x - 8, top: y - 8, width: 16, height: 16, borderRadius: 99, background: THEMES[tk].accent, border: '2.5px solid ' + TZ.surface, boxShadow: '0 2px 5px rgba(0,0,0,0.15)' }} />;
      })}
    </div>
  );
}

// collapsed theme row (tap to open)
function ThemeRow({ tk, pct, meta, hint }) {
  const th = THEMES[tk];
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 14, background: TZ.card, border: '1px solid ' + TZ.line, borderLeft: '4px solid ' + th.accent, borderRadius: 13, padding: '12px 14px' }}>
      <div style={{ position: 'relative', width: 44, height: 44, flexShrink: 0 }}>
        <Ring pct={pct} s={44} c={th.accent} />
        <span style={{ position: 'absolute', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', fontFamily: TZ.mono, fontSize: 10.5, fontWeight: 700, color: th.accent }}>{pct}%</span>
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontFamily: TZ.serif, fontSize: 16.5, fontWeight: 600, lineHeight: 1.1 }}>{th.name}</div>
        <div style={{ fontFamily: TZ.serif, fontStyle: 'italic', fontSize: 12.5, color: TZ.muted, marginTop: 2, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>“{th.why}”</div>
        <div style={{ fontFamily: TZ.mono, fontSize: 9.5, color: TZ.faint, marginTop: 5 }}>{th.window.toUpperCase()} · {meta}</div>
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 3 }}>
        <TI.Chevron s={18} c={tzA(TZ.ink, 0.3)} />
        {hint && <span style={{ fontFamily: TZ.mono, fontSize: 8, color: TZ.faint }}>OPEN</span>}
      </div>
    </div>
  );
}

function AddRow({ label, accent = TZ.rust }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 9, padding: '10px 13px', background: tzA(accent, 0.06), border: '1px dashed ' + tzA(accent, 0.5), borderRadius: 11 }}>
      <TI.Plus s={15} c={accent} />
      <span style={{ fontSize: 13, color: accent, fontWeight: 600 }}>{label}</span>
    </div>
  );
}

function DirectionsBoard() {
  return (
    <Phone nav="directions">
      <Screen bg={TZ.surface}>
        <LayerHeader kicker="DIRECTIONS" title="Your three bearings" accent={TZ.rust}
          sub="The most you hold at once. Tap a bearing to open its goals and habits." />
        <Body>
          <div style={{ paddingTop: 14 }}>
            <ThemeScope active="active" />
            <div style={{ display: 'flex', justifyContent: 'center', padding: '16px 0 10px' }}><BearingRose s={128} /></div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
              <ThemeRow tk="health" pct={38} meta="1 GOAL · 2 HABITS" hint />
              <ThemeRow tk="novel" pct={78} meta="1 GOAL · 1 HABIT" />
              <ThemeRow tk="japanese" pct={50} meta="1 GOAL · 2 HABITS" />
            </div>

            <GroupHeader label="Goals without a theme" count={1} accent={TZ.muted} />
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
              <GoalCard g={GOALS[3]} />
              <AddRow label="Add a goal" accent={TZ.muted} />
            </div>
          </div>
        </Body>
        <FAB sub="Theme" />
      </Screen>
    </Phone>
  );
}

// One theme expanded inline — full detail, collapsible; add-goal/habit affordances.
function DirectionsBoardExpanded() {
  const th = THEMES.health;
  return (
    <Phone nav="directions">
      <Screen bg={TZ.surface}>
        <LayerHeader kicker="DIRECTIONS" title="Your three bearings" accent={TZ.rust}
          sub="Tap a bearing to open it — goals and habits unfold in place." />
        <Body>
          <div style={{ paddingTop: 14 }}>
            <ThemeScope active="active" />

            {/* expanded theme */}
            <div style={{ marginTop: 14, background: th.wash, border: '1px solid ' + tzA(th.accent, 0.28), borderLeft: '4px solid ' + th.accent, borderRadius: 15, overflow: 'hidden' }}>
              <div style={{ display: 'flex', alignItems: 'flex-start', gap: 13, padding: '14px 15px 12px' }}>
                <div style={{ position: 'relative', width: 46, height: 46, flexShrink: 0, marginTop: 2 }}>
                  <Ring pct={38} s={46} c={th.accent} />
                  <span style={{ position: 'absolute', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', fontFamily: TZ.mono, fontSize: 11, fontWeight: 700, color: th.accent }}>38%</span>
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontFamily: TZ.serif, fontSize: 19, fontWeight: 600, lineHeight: 1.05 }}>{th.name}</div>
                  <div style={{ fontFamily: TZ.serif, fontStyle: 'italic', fontSize: 13, color: TZ.muted, marginTop: 3 }}>“{th.why}”</div>
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2 }}>
                  <TI.Chevron s={18} c={th.accent} dir="up" />
                  <span style={{ fontFamily: TZ.mono, fontSize: 8, color: tzA(th.accent, 0.8) }}>CLOSE</span>
                </div>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 6, padding: '0 15px 12px', fontFamily: TZ.mono, fontSize: 10, color: tzA(th.accent, 0.95) }}>
                <Compass s={13} ring={th.accent} needleN={th.accent} needleS={tzA(th.accent, 0.4)} stroke={2} ticks={false} />
                <span style={{ color: TZ.muted }}>{th.window.toUpperCase()} · {th.mirror}</span>
              </div>

              <div style={{ background: TZ.surface, borderTop: '1px solid ' + tzA(th.accent, 0.16), padding: '13px 15px', display: 'flex', flexDirection: 'column', gap: 10 }}>
                <SectionLabel c={th.accent}>Goals</SectionLabel>
                <GoalCard g={GOALS[0]} />
                <AddRow label="Add a goal to this theme" accent={th.accent} />

                <SectionLabel c={th.accent} style={{ marginTop: 4 }}>Habits</SectionLabel>
                <HabitChip h={HABITS[0]} />
                <HabitChip h={HABITS[1]} />
                <AddRow label="Add a habit to this theme" accent={th.accent} />
              </div>
            </div>

            {/* other bearings, collapsed */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10, marginTop: 12 }}>
              <ThemeRow tk="novel" pct={78} meta="1 GOAL · 1 HABIT" />
              <ThemeRow tk="japanese" pct={50} meta="1 GOAL · 2 HABITS" />
            </div>
          </div>
        </Body>
        <FAB sub="Theme" />
      </Screen>
    </Phone>
  );
}

Object.assign(window, { Ring, GoalCard, HabitChip, ThemeScope, BearingRose, ThemeRow, AddRow, DirectionsBoard, DirectionsBoardExpanded });
