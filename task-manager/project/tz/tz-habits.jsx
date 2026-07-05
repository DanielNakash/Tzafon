// tz-habits.jsx — Direction layer: Habits view. Forgiving rate + cue + arc.
// No streaks, no adherence score (DEC-2/4). "One miss is normal."

// 5-week history grid (deterministic sample history)
function HistoryGrid({ accent, weeks }) {
  return (
    <div style={{ display: 'flex', gap: 5 }}>
      {weeks.map((wk, wi) => (
        <div key={wi} style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
          {wk.map((v, di) => (
            <span key={di} style={{ width: 11, height: 11, borderRadius: 3,
              background: v === 2 ? accent : v === 1 ? tzA(accent, 0.4) : tzA(TZ.ink, 0.08) }} />
          ))}
        </div>
      ))}
    </div>
  );
}

function WeekDots({ done, total, accent }) {
  const labels = ['S', 'M', 'T', 'W', 'T', 'F', 'S'];
  return (
    <div style={{ display: 'flex', gap: 7 }}>
      {Array.from({ length: total }).map((_, i) => (
        <div key={i} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4 }}>
          <span style={{ width: 22, height: 22, borderRadius: 7, background: i < done ? accent : tzA(TZ.ink, 0.08),
            display: 'flex', alignItems: 'center', justifyContent: 'center' }}>{i < done && <TI.Check s={13} />}</span>
        </div>
      ))}
    </div>
  );
}

function HabitCard({ h, history, lapse }) {
  const accent = THEMES[h.theme].accent;
  const isQuant = h.kind === 'quantitative';
  const total = isQuant ? h.targetDays : h.target;
  return (
    <div style={{ background: TZ.card, border: '1px solid ' + TZ.line, borderRadius: 16, padding: '15px 16px', marginBottom: 12 }}>
      <div style={{ display: 'flex', alignItems: 'flex-start', gap: 10 }}>
        <div style={{ flex: 1 }}>
          <div style={{ fontFamily: TZ.serif, fontSize: 19, fontWeight: 600, lineHeight: 1.1 }}>{h.name}</div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginTop: 5, flexWrap: 'wrap' }}>
            {h.serves.map((s, i) => <Serves key={i} themeKey={h.theme} label={s} />)}
          </div>
        </div>
        <span style={{ fontFamily: TZ.mono, fontSize: 10, color: accent, background: tzA(accent, 0.1), borderRadius: 999, padding: '4px 9px', whiteSpace: 'nowrap' }}>
          {isQuant ? h.target + ' ' + h.unit + '/day' : h.target + '× / week'}
        </span>
      </div>

      {/* cue — front and centre */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 12, padding: '9px 12px', background: tzA(TZ.amber, 0.1), borderRadius: 10 }}>
        <TI.Cue s={14} c={TZ.rust} />
        <span style={{ fontSize: 13.5, color: TZ.ink }}>When <b>{h.cue.replace(/^After /, '')}</b>{h.cue.startsWith('After') ? '' : ''}</span>
      </div>

      {/* this-period rate — forgiving, endowed */}
      <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', marginTop: 14 }}>
        <div>
          <div style={{ fontFamily: TZ.mono, fontSize: 10.5, letterSpacing: 0.5, color: TZ.faint }}>THIS WEEK</div>
          <div style={{ fontFamily: TZ.serif, fontSize: 17, fontWeight: 600, marginTop: 2 }}>
            {isQuant ? <span>{(h.week.reduce((a, b) => a + b, 0)).toLocaleString()} words<span style={{ fontSize: 13, color: TZ.muted, fontWeight: 400 }}> · {h.doneThis} of {total} days</span></span>
              : <span>{h.doneThis} of {h.target} <span style={{ fontSize: 13, color: TZ.muted, fontWeight: 400 }}>done</span></span>}
          </div>
        </div>
        {!isQuant && <WeekDots done={h.doneThis} total={h.target} accent={accent} />}
      </div>
      {isQuant && (
        <div style={{ display: 'flex', gap: 5, marginTop: 10, alignItems: 'flex-end', height: 34 }}>
          {h.week.map((v, i) => (
            <div key={i} style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 3 }}>
              <div style={{ width: '100%', height: Math.max(3, v / 700 * 30), background: v > 0 ? accent : tzA(TZ.ink, 0.1), borderRadius: 2 }} />
              <span style={{ fontFamily: TZ.mono, fontSize: 8, color: TZ.faint }}>{['S', 'M', 'T', 'W', 'T', 'F', 'S'][i]}</span>
            </div>
          ))}
        </div>
      )}

      <button style={{ marginTop: 12, width: '100%', height: 40, borderRadius: 11, border: '1px solid ' + tzA(accent, 0.5), background: tzA(accent, 0.1), color: accent, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8, fontFamily: TZ.body, fontSize: 14, fontWeight: 600, cursor: 'pointer' }}>
        <TI.Check s={16} c={accent} w={2.6} /> {isQuant ? "Log today's words" : 'Mark today done'}
      </button>

      {lapse && (
        <div style={{ display: 'flex', gap: 8, marginTop: 12, padding: '9px 12px', background: TZ.surface, borderRadius: 10 }}>
          <TI.Sprout s={16} c={TZ.green} />
          <span style={{ fontSize: 12.5, color: TZ.muted, lineHeight: 1.4 }}>Missed a couple this week? Normal. <b style={{ color: TZ.ink }}>New week, clean slate.</b></span>
        </div>
      )}

      {/* history + long arc */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 14, marginTop: 14, paddingTop: 12, borderTop: '1px solid ' + TZ.line2 }}>
        <HistoryGrid accent={accent} weeks={history} />
        <div style={{ flex: 1 }}>
          <div style={{ display: 'inline-flex', alignItems: 'center', gap: 6, fontFamily: TZ.mono, fontSize: 11, color: TZ.muted }}>
            <TI.Sprout s={15} c={TZ.green} />{h.arc}
          </div>
          <div style={{ fontFamily: TZ.mono, fontSize: 9, color: TZ.faint, marginTop: 3, letterSpacing: 0.3 }}>LAST 5 WEEKS</div>
        </div>
      </div>
    </div>
  );
}

const HIST = {
  run: [[2,0,2,0,0,0,0],[2,0,2,0,2,0,0],[0,2,0,2,0,0,0],[2,0,2,0,2,0,0],[2,0,1,0,0,0,0]],
  write: [[2,2,0,2,2,0,0],[2,2,2,0,2,0,0],[2,2,0,2,2,1,0],[2,0,2,2,2,0,0],[2,2,0,2,1,0,0]],
  anki: [[0,0,0,0,0,0,0],[0,0,0,0,0,0,0],[2,2,2,0,2,0,0],[2,2,0,2,2,1,0],[2,2,2,0,2,2,0]],
};

function HabitsScreen() {
  return (
    <Phone nav="habits">
      <Screen bg={TZ.bg}>
        <LayerHeader kicker="HABITS · RHYTHM" title="Habits" accent={TZ.green}
          sub="Aim for the rate, not perfection. Miss one — that's normal. It's the long arc that counts." />
        <Body>
          <div style={{ paddingTop: 14 }}>
            <HabitCard h={HABITS[2]} history={HIST.write} />
            <HabitCard h={HABITS[0]} history={HIST.run} />
            <HabitCard h={HABITS[3]} history={HIST.anki} lapse />
          </div>
        </Body>
        <FAB sub="Habit" />
      </Screen>
    </Phone>
  );
}

Object.assign(window, { HabitCard, HistoryGrid, WeekDots, HabitsScreen });
