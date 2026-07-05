// tz-action.jsx — Action layer: Today (×3 variations), Planning, All Tasks, Backlog.

// A Focus item — the capped "now", elevated with a north tick.
function FocusItem({ t, last }) {
  const accent = t.serves && THEMES[t.serves] ? THEMES[t.serves].accent : TZ.rust;
  return (
    <div style={{ display: 'flex', gap: 12, padding: '13px 0', borderBottom: last ? 'none' : '1px solid ' + TZ.line }}>
      <div style={{ flexShrink: 0, width: 25, height: 25, border: '2px solid ' + tzA(TZ.ink, 0.3), borderRadius: 7, marginTop: 1 }} />
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 16.5, fontWeight: 600, color: TZ.ink, lineHeight: 1.3 }}>{t.title}</div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginTop: 6, flexWrap: 'wrap' }}>
          <Serves themeKey={t.serves} label={t.servesLabel} />
          {t.cue && <CueChip>{t.cue}</CueChip>}
        </div>
      </div>
    </div>
  );
}

function TodayScreen() {
  return (
    <Phone nav="today" statusDark>
      <Screen>
        <RustHeader kicker="FRIDAY · JUL 5" title="Today" compass
          meta={<React.Fragment><span>YOUR NORTH FOR THE DAY</span><span>2 / 6 DONE</span></React.Fragment>}
          progress={33} />
        <Body>
          <div style={{ paddingTop: 14 }}>
            <Banner tone={TZ.rust}>2 tasks slipped past — tidy them up in <b>Planning</b>.</Banner>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '20px 0 4px' }}>
            <Compass s={17} ring={TZ.rust} needleN={TZ.rust} needleS={TZ.faint} stroke={2} />
            <SectionLabel c={TZ.rust}>Today's focus · 3</SectionLabel>
            <div style={{ flex: 1, height: 1, background: TZ.line }} />
          </div>
          <Card pad="2px 15px" style={{ background: TZ.surface, borderColor: tzA(TZ.rust, 0.18) }}>
            {TASKS.focus.map((t, i) => <FocusItem key={t.id} t={t} last={i === TASKS.focus.length - 1} />)}
          </Card>

          <GroupHeader label="Also today" count={3} right={<span style={{ display: 'inline-flex', alignItems: 'center', gap: 4, fontFamily: TZ.mono, fontSize: 10, letterSpacing: 0.4, color: TZ.faint }}>COLLAPSE <TI.Chevron s={14} c={TZ.faint} dir="up" /></span>} />
          <div>
            {TASKS.today.map((t, i) => <TaskRow key={t.id} {...t} last={i === TASKS.today.length - 1} />)}
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '16px 0 0', color: TZ.faint }}>
            <TI.Check s={14} c={TZ.green} w={2.6} />
            <span style={{ fontFamily: TZ.mono, fontSize: 11.5, letterSpacing: 0.4 }}>2 DONE TODAY</span>
            <div style={{ flex: 1, height: 1, background: TZ.line2 }} />
            <TI.Chevron s={16} c={TZ.faint} dir="down" />
          </div>
        </Body>
        <FAB />
      </Screen>
    </Phone>
  );
}

// Variation A — "Points north": the 3 focus items as theme-coloured cards.
function TodayFocusVar() {
  return (
    <Phone nav="today" statusDark>
      <Screen>
        <RustHeader kicker="FRIDAY · JUL 5" title="Today" compass
          meta={<React.Fragment><span>3 THINGS · POINTED NORTH</span><span>2 / 6 DONE</span></React.Fragment>} progress={33} />
        <Body>
          <p style={{ margin: '16px 0 4px', fontFamily: TZ.serif, fontSize: 17, fontStyle: 'italic', color: TZ.muted, lineHeight: 1.4 }}>
            If nothing else, these three move you where you said you wanted to go.
          </p>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 11, marginTop: 12 }}>
            {TASKS.focus.map((t) => {
              const th = THEMES[t.serves];
              return (
                <div key={t.id} style={{ display: 'flex', background: TZ.card, border: '1px solid ' + TZ.line, borderRadius: 14, overflow: 'hidden' }}>
                  <div style={{ width: 5, background: th.accent, flexShrink: 0 }} />
                  <div style={{ flex: 1, padding: '13px 14px', minWidth: 0 }}>
                    <div style={{ display: 'flex', alignItems: 'flex-start', gap: 11 }}>
                      <div style={{ flexShrink: 0, width: 25, height: 25, border: '2px solid ' + tzA(TZ.ink, 0.3), borderRadius: 7, marginTop: 1 }} />
                      <div style={{ flex: 1, minWidth: 0 }}>
                        <div style={{ fontSize: 16.5, fontWeight: 600, lineHeight: 1.3 }}>{t.title}</div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginTop: 7, flexWrap: 'wrap' }}>
                          <Serves themeKey={t.serves} label={t.servesLabel} />
                          {t.cue && <CueChip>{t.cue}</CueChip>}
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
          <div style={{ marginTop: 16, display: 'flex', alignItems: 'center', gap: 8, padding: '13px 15px', background: TZ.surface, border: '1px dashed ' + TZ.line, borderRadius: 12 }}>
            <TI.Chevron s={16} c={TZ.muted} dir="down" />
            <span style={{ fontSize: 13.5, color: TZ.muted }}>3 more on the plate today</span>
            <div style={{ flex: 1 }} />
            <span style={{ fontFamily: TZ.mono, fontSize: 11, color: TZ.faint }}>SHOW</span>
          </div>
        </Body>
        <FAB />
      </Screen>
    </Phone>
  );
}

// Variation B — "By cue": grouped by routine trigger, cue over clock.
function TodayAgendaVar() {
  const groups = [
    ['After the first coffee', TI.Coffee, [TASKS.focus[0]]],
    ['After school drop-off', TI.Clock, [TASKS.focus[1], { id: 'x', title: 'Book the physio appointment', serves: 'health', servesLabel: 'Move more' }]],
    ['After breakfast', TI.Cue, [TASKS.focus[2]]],
    ['No cue yet', null, [{ id: 'y', title: 'Reply to the editor', serves: 'novel', servesLabel: 'Finish the novel', due: 'today' }]],
  ];
  return (
    <Phone nav="today" statusDark>
      <Screen>
        <RustHeader kicker="FRIDAY · JUL 5" title="Today" compact
          meta={<React.Fragment><span>BY CUE, NOT BY CLOCK</span><span>2 / 6 DONE</span></React.Fragment>} progress={33} />
        <Body>
          <div style={{ paddingTop: 14 }}>
            {groups.map(([label, Icon, items], gi) => (
              <div key={label} style={{ display: 'flex', gap: 12, marginBottom: 6 }}>
                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', flexShrink: 0 }}>
                  <div style={{ width: 30, height: 30, borderRadius: 99, background: Icon ? tzA(TZ.amber, 0.16) : TZ.surface, border: '1px solid ' + (Icon ? tzA(TZ.amber, 0.4) : TZ.line), display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    {Icon ? <Icon s={15} c={TZ.rust} /> : <Dot c={TZ.faint} s={6} />}
                  </div>
                  {gi < groups.length - 1 && <div style={{ flex: 1, width: 2, background: TZ.line2, marginTop: 4 }} />}
                </div>
                <div style={{ flex: 1, minWidth: 0, paddingBottom: 10 }}>
                  <div style={{ fontFamily: TZ.mono, fontSize: 11, letterSpacing: 0.5, color: TZ.muted, fontWeight: 600, paddingTop: 6 }}>{label.toUpperCase()}</div>
                  <div style={{ marginTop: 2 }}>
                    {items.map((t, i) => <TaskRow key={t.id} {...t} chevron={false} last={i === items.length - 1} />)}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </Body>
        <FAB />
      </Screen>
    </Phone>
  );
}

function RangeControl({ active = '7 days' }) {
  return (
    <div style={{ display: 'flex', gap: 7, marginTop: 4 }}>
      {['7 days', '14 days', '30 days', 'Month'].map(p => (
        <span key={p} style={{ fontFamily: TZ.mono, fontSize: 11, letterSpacing: 0.3, padding: '5px 11px', borderRadius: 999,
          border: '1px solid ' + (p === active ? TZ.rust : TZ.line), background: p === active ? TZ.rust : 'transparent',
          color: p === active ? '#fff' : TZ.muted }}>{p}</span>
      ))}
    </div>
  );
}

function PlanningScreen() {
  return (
    <Phone nav="planning" statusDark>
      <Screen>
        <RustHeader kicker="WHAT'S ON THE TABLE" title="Planning" compact
          meta={<React.Fragment><span>SAT, JUL 5</span><span>3 UNDATED</span></React.Fragment>} />
        <div style={{ background: TZ.rust, padding: '0 20px 16px' }}><RangeControl /></div>
        <Body>
          <GroupHeader label="Overdue" count={2} accent={TZ.due} right={<span style={{ fontFamily: TZ.mono, fontSize: 10, color: TZ.due }}>DECIDE →</span>} />
          <div style={{ background: tzA(TZ.due, 0.05), border: '1px solid ' + tzA(TZ.due, 0.2), borderRadius: 13, padding: '4px 14px', marginBottom: 4 }}>
            {TASKS.overdue.map((t, i) => (
              <div key={t.id} style={{ padding: '11px 0', borderBottom: i === TASKS.overdue.length - 1 ? 'none' : '1px solid ' + TZ.line2 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 11 }}>
                  <div style={{ flexShrink: 0, width: 24, height: 24, border: '2px solid ' + tzA(TZ.ink, 0.28), borderRadius: 7 }} />
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontSize: 15.5, lineHeight: 1.25, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{t.title}</div>
                    <div style={{ fontFamily: TZ.mono, fontSize: 10.5, color: TZ.due, marginTop: 3 }}>SLIPPED {t.slipped}D AGO</div>
                  </div>
                </div>
                <div style={{ display: 'flex', gap: 7, marginTop: 9, paddingLeft: 35 }}>
                  {['Today', 'Reschedule', 'Someday', 'Drop'].map(a => (
                    <span key={a} style={{ fontFamily: TZ.mono, fontSize: 10.5, padding: '4px 9px', borderRadius: 999, border: '1px solid ' + TZ.line, color: TZ.muted, background: TZ.card }}>{a}</span>
                  ))}
                </div>
              </div>
            ))}
          </div>

          <GroupHeader label="Today" count={3} accent={TZ.rust} />
          {TASKS.today.map((t, i) => <TaskRow key={t.id} {...t} last={i === TASKS.today.length - 1} />)}

          <GroupHeader label="Tue, Jul 8" count={1} />
          <TaskRow title="Plan the Kyoto itinerary" serves="japanese" servesLabel="Speak Japanese" last />

          <GroupHeader label="Inbox — needs a date" count={3} accent={TZ.muted} />
          {TASKS.inbox.map((t, i) => <TaskRow key={t.id} title={t.title} last={i === TASKS.inbox.length - 1} chevron />)}
        </Body>
        <FAB />
      </Screen>
    </Phone>
  );
}

function Toggle({ label, on, c = TZ.rust }) {
  return (
    <span style={{ display: 'inline-flex', alignItems: 'center', gap: 6, fontFamily: TZ.mono, fontSize: 10.5, letterSpacing: 0.3,
      padding: '5px 10px', borderRadius: 999, border: '1px solid ' + (on ? c : TZ.line), background: on ? tzA(c, 0.1) : 'transparent', color: on ? c : TZ.faint }}>
      <span style={{ width: 6, height: 6, borderRadius: 99, background: on ? c : TZ.faint }} />{label}
    </span>
  );
}

function AllTasksScreen() {
  return (
    <Phone nav={null} statusDark>
      <Screen>
        <RustHeader kicker="COMPLETE INDEX" title="All Tasks" compact right={<TI.Menu s={20} c={TZ.cream} />} />
        <div style={{ background: TZ.rust, padding: '0 20px 16px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 9, background: 'rgba(255,255,255,0.16)', borderRadius: 11, padding: '10px 12px' }}>
            <TI.Search s={16} c={tzA('#fff', 0.8)} />
            <span style={{ fontSize: 14, color: tzA('#fff', 0.72) }}>Search title or description…</span>
          </div>
          <div style={{ display: 'flex', gap: 6, marginTop: 11, flexWrap: 'wrap' }}>
            <span style={{ fontFamily: TZ.mono, fontSize: 10, color: tzA('#fff', 0.6), alignSelf: 'center' }}>SHOW:</span>
            <Toggle label="DONE" on c={TZ.cream} /><Toggle label="FROZEN" on c={TZ.cream} /><Toggle label="CLOSED" /><Toggle label="BACKLOG" />
          </div>
        </div>
        <Body>
          <GroupHeader label="Overdue" count={2} accent={TZ.due} />
          {TASKS.overdue.map((t, i) => <TaskRow key={t.id} title={t.title} serves={t.serves} servesLabel={t.servesLabel} last={i === 1} />)}
          <GroupHeader label="Today" count={3} accent={TZ.rust} />
          <TaskRow title="Write 500 words — chapter 14" serves="novel" servesLabel="Finish the novel" />
          <TaskRow title="Water the studio plants 🪴" recur="Every 3 days" />
          <TaskRow title="Morning pages" serves="novel" state="done" last />
          <GroupHeader label="Fri, Jul 12" count={2} />
          <TaskRow title="Strength work — legs" habit="Strength work" state="frozen" />
          <TaskRow title="Old draft cleanup" state="closed" last />
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, margin: '20px 0 6px' }}>
            <div style={{ flex: 1, height: 1, background: TZ.line }} />
            <span style={{ fontFamily: TZ.mono, fontSize: 9.5, letterSpacing: 0.4, color: TZ.faint, textAlign: 'center', lineHeight: 1.4 }}>SCHEDULED THROUGH AUG 22<br />RECURRING TASKS REPEAT BEYOND THIS</span>
            <div style={{ flex: 1, height: 1, background: TZ.line }} />
          </div>
          <GroupHeader label="No date" count={3} accent={TZ.muted} />
          {TASKS.inbox.map((t, i) => <TaskRow key={t.id} title={t.title} last={i === 2} />)}
        </Body>
        {/* edge date-scrubber */}
        <div style={{ position: 'absolute', right: 3, top: 210, bottom: 100, width: 16, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'space-between', paddingTop: 8, paddingBottom: 8, zIndex: 30 }}>
          {['JUL', '•', '•', 'AUG', '•', '•', 'SEP'].map((m, i) => (
            <span key={i} style={{ fontFamily: TZ.mono, fontSize: 8, color: m === '•' ? tzA(TZ.ink, 0.25) : TZ.muted, fontWeight: m === '•' ? 400 : 700 }}>{m}</span>
          ))}
        </div>
      </Screen>
    </Phone>
  );
}

function BacklogScreen() {
  return (
    <Phone nav={null} statusDark>
      <Screen>
        <RustHeader kicker="SOMEDAY / MAYBE" title="Backlog" compact right={<TI.Moon s={19} c={TZ.cream} />}
          meta={<React.Fragment><span>PARKED — NOT SCHEDULED</span><span>3 ITEMS</span></React.Fragment>} />
        <Body>
          <p style={{ margin: '16px 0 4px', fontSize: 13.5, lineHeight: 1.5, color: TZ.muted }}>
            A quiet pool for things you might do. They stay out of Today and Planning, and don't count toward your day. Pull one over whenever it calls.
          </p>
          <div style={{ marginTop: 12 }}>
            {TASKS.backlog.map((t, i) => (
              <div key={t.id} style={{ display: 'flex', alignItems: 'center', gap: 13, padding: '15px 0', borderBottom: i === TASKS.backlog.length - 1 ? 'none' : '1px solid ' + TZ.line }}>
                <Checkbox state="backlog" />
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 16, lineHeight: 1.3 }}>{t.title}</div>
                  {t.id === 't15' && <div style={{ marginTop: 5 }}><span style={{ display: 'inline-flex', alignItems: 'center', gap: 5, fontFamily: TZ.mono, fontSize: 10.5, color: TZ.faint }}><Dot c={tzA(TZ.tLearn, 0.5)} s={6} />links dormant · Speak Japanese</span></div>}
                </div>
                <span style={{ fontFamily: TZ.mono, fontSize: 10, color: TZ.rust, border: '1px solid ' + tzA(TZ.rust, 0.35), borderRadius: 999, padding: '4px 10px', whiteSpace: 'nowrap' }}>PULL →</span>
              </div>
            ))}
          </div>
          <div style={{ marginTop: 20, textAlign: 'center', color: TZ.faint }}>
            <TI.Moon s={26} c={tzA(TZ.backlog, 0.5)} />
            <div style={{ fontFamily: TZ.mono, fontSize: 10.5, letterSpacing: 0.4, marginTop: 6 }}>NO RUSH · NO GUILT</div>
          </div>
        </Body>
        <FAB />
      </Screen>
    </Phone>
  );
}

Object.assign(window, { TodayScreen, TodayFocusVar, TodayAgendaVar, PlanningScreen, AllTasksScreen, BacklogScreen });
