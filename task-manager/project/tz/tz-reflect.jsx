// tz-reflect.jsx — Reflection layer: Journey (identity mirror) + Review loop.
// Mirror, not trophy case. No badges, streaks, points, or engagement metrics.

function JourneyScreen() {
  const milestones = [
    ['Ran my first 10K', 'health', 'Move more, feel strong', 'MAY 2026'],
    ['Finished the short story', 'novel', 'Finish the novel', 'APR 2026'],
  ];
  const arcs = [
    ['A writer', '12 weeks of morning pages', TZ.tWrite],
    ['A runner', '9 weeks on the trail', TZ.tHealth],
    ['A beginner again', '3 weeks of Japanese', TZ.tLearn],
  ];
  return (
    <Phone nav="journey">
      <Screen bg={TZ.surface}>
        <LayerHeader kicker="JOURNEY · THE MIRROR" title="How far you've come" accent={TZ.rust} compass
          sub="Not a trophy case — a mirror. Who you're becoming, in your own past and your own words." />
        <Body>
          <div style={{ paddingTop: 14 }}>
            {/* honest aggregate */}
            <div style={{ background: TZ.rust, color: TZ.cream, borderRadius: 16, padding: '18px 18px', position: 'relative', overflow: 'hidden' }}>
              <div style={{ position: 'absolute', right: -24, bottom: -24, opacity: 0.16 }}><Compass s={120} ring={TZ.cream} needleN={TZ.cream} needleS={TZ.cream} stroke={1.2} /></div>
              <div style={{ position: 'relative' }}>
                <Kicker c={tzA('#FBF4E6', 0.75)}>THIS QUARTER</Kicker>
                <div style={{ fontFamily: TZ.serif, fontSize: 21, fontWeight: 500, lineHeight: 1.3, marginTop: 8 }}>
                  <b>47</b> of your completed tasks served a direction you chose.
                </div>
                <div style={{ fontFamily: TZ.serif, fontStyle: 'italic', fontSize: 14, color: tzA('#FBF4E6', 0.85), marginTop: 8 }}>That's not busywork. That's who you're becoming.</div>
              </div>
            </div>

            <GroupHeader label="Milestones" count={2} accent={TZ.rust} />
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
              {milestones.map(([title, tk, theme, date]) => (
                <div key={title} style={{ display: 'flex', alignItems: 'center', gap: 12, background: TZ.card, border: '1px solid ' + TZ.line, borderRadius: 13, padding: '13px 14px' }}>
                  <div style={{ width: 34, height: 34, borderRadius: 99, background: tzA(THEMES[tk].accent, 0.14), display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
                    <TI.Sparkle s={17} c={THEMES[tk].accent} />
                  </div>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontFamily: TZ.serif, fontSize: 16.5, fontWeight: 600 }}>{title}</div>
                    <div style={{ marginTop: 3 }}><Serves themeKey={tk} label={theme} /></div>
                  </div>
                  <span style={{ fontFamily: TZ.mono, fontSize: 9.5, color: TZ.faint, whiteSpace: 'nowrap' }}>{date}</span>
                </div>
              ))}
            </div>

            <GroupHeader label="Who you're becoming" accent={TZ.rust} />
            <div style={{ display: 'flex', flexDirection: 'column', gap: 9 }}>
              {arcs.map(([who, detail, c]) => (
                <div key={who} style={{ display: 'flex', alignItems: 'center', gap: 11 }}>
                  <TI.Sprout s={17} c={c} />
                  <span style={{ fontFamily: TZ.serif, fontSize: 16, fontWeight: 600 }}>{who}</span>
                  <span style={{ fontSize: 13, color: TZ.muted }}>— {detail}</span>
                </div>
              ))}
            </div>

            <GroupHeader label="Directions over time" accent={TZ.rust} />
            <div style={{ paddingLeft: 4 }}>
              {[['Year of Health', 'renewed as', 'Move more, feel strong', TZ.tHealth, true],
                ['Learn to cook', 'set down, with thanks', 'retired', TZ.closed, false]].map(([a, mid, b, c, live], i) => (
                <div key={i} style={{ display: 'flex', gap: 12, marginBottom: i === 0 ? 4 : 0 }}>
                  <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                    <Dot c={c} s={11} />
                    {i === 0 && <div style={{ flex: 1, width: 2, background: TZ.line, marginTop: 3 }} />}
                  </div>
                  <div style={{ paddingBottom: 14, flex: 1 }}>
                    <div style={{ fontSize: 14.5 }}><b style={{ color: TZ.ink }}>{a}</b> <span style={{ color: TZ.faint, fontStyle: 'italic' }}>{mid}</span> <b style={{ color: live ? c : TZ.muted }}>{b}</b></div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </Body>
      </Screen>
    </Phone>
  );
}

// Review is a loop, not a nav tab — a gentle, dismissible modal flow.
function ReviewHeader({ kicker, title, step }) {
  return (
    <div style={{ flexShrink: 0, background: TZ.surface, padding: '48px 20px 16px', borderBottom: '1px solid ' + TZ.line, position: 'relative' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div>
          <Kicker c={TZ.rust}>{kicker}</Kicker>
          <h1 style={{ margin: '6px 0 0', fontFamily: TZ.serif, fontSize: 30, fontWeight: 600, letterSpacing: -0.4, lineHeight: 1.02 }}>{title}</h1>
        </div>
        <button style={{ width: 34, height: 34, borderRadius: 99, border: '1px solid ' + TZ.line, background: TZ.card, display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer', flexShrink: 0 }}><TI.X s={15} c={TZ.muted} /></button>
      </div>
      {step && (
        <div style={{ display: 'flex', gap: 6, marginTop: 14 }}>
          {['Reflect', 'Plan'].map((s, i) => (
            <div key={s} style={{ flex: 1, display: 'flex', alignItems: 'center', gap: 7 }}>
              <span style={{ width: 20, height: 20, borderRadius: 99, background: i + 1 <= step ? TZ.rust : TZ.surfaceAlt, color: i + 1 <= step ? '#fff' : TZ.faint, fontFamily: TZ.mono, fontSize: 10, fontWeight: 700, display: 'flex', alignItems: 'center', justifyContent: 'center', border: i + 1 <= step ? 'none' : '1px solid ' + TZ.line }}>{i + 1}</span>
              <span style={{ fontFamily: TZ.mono, fontSize: 11, letterSpacing: 0.4, color: i + 1 === step ? TZ.ink : TZ.faint, fontWeight: i + 1 === step ? 700 : 500 }}>{s.toUpperCase()}</span>
              {i === 0 && <div style={{ flex: 1, height: 1, background: TZ.line }} />}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function ReviewReflect() {
  const rates = [
    ['Write in the morning', 'novel', '4 of 5 days', 80],
    ['Run', 'health', '2 of 3', 66],
    ['Anki reviews', 'japanese', '5 of 7', 71],
  ];
  return (
    <Phone nav={null}>
      <Screen bg={TZ.surface}>
        <ReviewHeader kicker="SUNDAY · A GENTLE LOOK BACK" title="Your week, reflected" step={1} />
        <Body navPad={false} style={{ paddingBottom: 92 }}>
          <div style={{ paddingTop: 16 }}>
            <div style={{ fontFamily: TZ.serif, fontSize: 19, fontWeight: 500, lineHeight: 1.35, color: TZ.ink }}>
              You finished <b>14 things</b> this week. <span style={{ color: TZ.rust }}>9 of them</span> served a direction you chose.
            </div>

            <SectionLabel style={{ margin: '20px 0 10px' }}>Your habits, honestly</SectionLabel>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
              {rates.map(([name, tk, rate, pct]) => (
                <div key={name} style={{ background: TZ.card, border: '1px solid ' + TZ.line, borderRadius: 12, padding: '11px 13px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <Dot c={THEMES[tk].accent} s={8} />
                    <span style={{ fontSize: 14.5, fontWeight: 500, flex: 1 }}>{name}</span>
                    <span style={{ fontFamily: TZ.mono, fontSize: 12, color: THEMES[tk].accent, fontWeight: 700 }}>{rate}</span>
                  </div>
                  <div style={{ marginTop: 8 }}><Bar pct={pct} fill={THEMES[tk].accent} h={6} /></div>
                </div>
              ))}
            </div>
            <div style={{ fontSize: 13, color: TZ.muted, marginTop: 10, lineHeight: 1.45 }}>Missed a few? Normal. The rate has slack built in — that's the point.</div>

            <SectionLabel style={{ margin: '20px 0 10px' }}>Goals that moved</SectionLabel>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              {[['First draft', '+3,200 words', TZ.tWrite], ['JLPT N5', '+1 step — katakana done', TZ.tLearn]].map(([g, d, c]) => (
                <div key={g} style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                  <TI.Target s={16} c={c} />
                  <span style={{ fontSize: 14.5, fontWeight: 500 }}>{g}</span>
                  <span style={{ fontFamily: TZ.mono, fontSize: 11.5, color: c, marginLeft: 'auto' }}>{d}</span>
                </div>
              ))}
            </div>

            <div style={{ marginTop: 18, background: TZ.card, border: '1px solid ' + TZ.line, borderRadius: 12, padding: '12px 14px' }}>
              <SectionLabel>A line for yourself · optional</SectionLabel>
              <div style={{ fontFamily: TZ.serif, fontStyle: 'italic', fontSize: 15, color: TZ.faint, marginTop: 6 }}>What mattered this week…</div>
            </div>
          </div>
        </Body>
        <div style={{ position: 'absolute', left: 0, right: 0, bottom: 0, padding: '12px 20px calc(12px + 22px)', background: tzA(TZ.surface, 0.96), borderTop: '1px solid ' + TZ.line, display: 'flex', gap: 10, zIndex: 40 }}>
          <button style={{ flex: '0 0 auto', height: 48, padding: '0 18px', borderRadius: 13, border: '1px solid ' + TZ.line, background: 'transparent', color: TZ.muted, fontFamily: TZ.body, fontSize: 15, fontWeight: 600 }}>Later</button>
          <button style={{ flex: 1, height: 48, borderRadius: 13, border: 'none', background: TZ.rust, color: '#fff', fontFamily: TZ.body, fontSize: 15.5, fontWeight: 600, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8 }}>Now, plan the week <TI.Chevron s={17} c="#fff" /></button>
        </div>
      </Screen>
    </Phone>
  );
}

function ReviewPlan() {
  const picks = [
    ['Write 500 words — chapter 15', 'novel', 'Finish the novel', true],
    ['Long run — 12K', 'health', 'Move more, feel strong', true],
    ['Tutor call + review', 'japanese', 'Speak Japanese', true],
    ['Draft the studio homepage', null, 'Redo the studio website', false],
  ];
  return (
    <Phone nav={null}>
      <Screen bg={TZ.surface}>
        <ReviewHeader kicker="A FRESH START" title="Plan the week" step={2} />
        <Body navPad={false} style={{ paddingBottom: 92 }}>
          <div style={{ paddingTop: 16 }}>
            <div style={{ fontFamily: TZ.serif, fontSize: 18, fontWeight: 500, lineHeight: 1.35 }}>
              Clean slate. Pick a few things worth pointing at this week.
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: 8, margin: '18px 0 10px' }}>
              <Compass s={16} ring={TZ.rust} needleN={TZ.rust} needleS={TZ.faint} stroke={2} ticks={false} />
              <SectionLabel c={TZ.rust}>This week's priorities</SectionLabel>
              <div style={{ flex: 1, height: 1, background: TZ.line }} />
              <span style={{ fontFamily: TZ.mono, fontSize: 10.5, color: TZ.green }}>3 OF 3 · A GOOD NUMBER</span>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: 9 }}>
              {picks.map(([title, tk, label, on]) => (
                <div key={title} style={{ display: 'flex', alignItems: 'center', gap: 12, background: on ? TZ.card : 'transparent', border: '1px solid ' + (on ? tzA(TZ.rust, 0.3) : TZ.line), borderRadius: 12, padding: '11px 13px' }}>
                  <span style={{ width: 24, height: 24, borderRadius: 7, border: '2px solid ' + (on ? TZ.rust : tzA(TZ.ink, 0.25)), background: on ? TZ.rust : 'transparent', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>{on && <Compass s={15} ring="#fff" needleN="#fff" needleS={tzA('#fff', 0.5)} stroke={2} ticks={false} />}</span>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontSize: 14.5, fontWeight: on ? 600 : 400, color: on ? TZ.ink : TZ.muted }}>{title}</div>
                    <div style={{ marginTop: 3 }}><Serves themeKey={tk} label={label} /></div>
                  </div>
                </div>
              ))}
            </div>

            <div style={{ marginTop: 16 }}>
              <Nudge>
                <b>Redo the studio website</b> hasn't moved in 3 weeks. Freeze it for now? You can thaw it any time — no harm done.
              </Nudge>
            </div>
          </div>
        </Body>
        <div style={{ position: 'absolute', left: 0, right: 0, bottom: 0, padding: '12px 20px calc(12px + 22px)', background: tzA(TZ.surface, 0.96), borderTop: '1px solid ' + TZ.line, display: 'flex', gap: 10, zIndex: 40 }}>
          <button style={{ flex: 1, height: 48, borderRadius: 13, border: 'none', background: TZ.rust, color: '#fff', fontFamily: TZ.body, fontSize: 15.5, fontWeight: 600 }}>Start the week</button>
        </div>
      </Screen>
    </Phone>
  );
}

Object.assign(window, { JourneyScreen, ReviewHeader, ReviewReflect, ReviewPlan });
