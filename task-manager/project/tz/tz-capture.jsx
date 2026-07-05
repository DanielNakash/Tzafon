// tz-capture.jsx — Onboarding + Capture & system:
// Sign-in (Tzafon rebrand + Fox Works logo hero), Task editor, Quick-add,
// task-state sheet, Settings, and an alternative nav model.

function SignIn() {
  return (
    <Phone pillDark={false}>
      <div style={{ position: 'absolute', inset: 0, background: TZ.bg, color: TZ.ink, fontFamily: TZ.body, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
        <div style={{ position: 'absolute', top: -110, right: -90, width: 300, height: 300, borderRadius: '50%', background: TZ.amber, opacity: 0.18, filter: 'blur(10px)' }} />
        <div style={{ position: 'absolute', bottom: -130, left: -100, width: 280, height: 280, borderRadius: '50%', background: TZ.rust, opacity: 0.12, filter: 'blur(8px)' }} />
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: '0 40px', textAlign: 'center', position: 'relative', zIndex: 2 }}>
          <div style={{ position: 'relative' }}>
            <FoxLogo s={116} ring={TZ.surface} style={{ boxShadow: '0 18px 34px rgba(120,60,20,0.22), 0 0 0 3px ' + TZ.surface }} />
            <div style={{ position: 'absolute', bottom: -6, right: -10, width: 40, height: 40, borderRadius: '50%', background: TZ.surface, display: 'flex', alignItems: 'center', justifyContent: 'center', boxShadow: '0 4px 12px rgba(120,60,20,0.2)' }}>
              <Compass s={26} ring={TZ.rust} needleN={TZ.rust} needleS={TZ.faint} stroke={2.2} />
            </div>
          </div>
          <div style={{ marginTop: 22, fontFamily: TZ.mono, fontSize: 11.5, letterSpacing: 3.5, color: TZ.rust, fontWeight: 600 }}>THE FOX WORKS · DON'T PANIC</div>
          <h1 style={{ margin: '10px 0 0', fontFamily: TZ.serif, fontSize: 52, fontWeight: 600, lineHeight: 1, letterSpacing: -0.5 }}>Tzafon</h1>
          <div style={{ marginTop: 6, fontFamily: TZ.mono, fontSize: 13, letterSpacing: 2, color: TZ.faint }}>צָפוֹן · “north”</div>
          <p style={{ margin: '18px 0 0', fontSize: 16, lineHeight: 1.5, color: TZ.muted, maxWidth: 290 }}>
            A compass for the life you're actually building. Hold a direction — then do one small thing today.
          </p>
          <button style={{ marginTop: 30, width: '100%', maxWidth: 300, height: 56, borderRadius: 15, border: '1px solid ' + TZ.line, background: TZ.surface, color: TZ.ink, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 12, fontFamily: TZ.body, fontSize: 16.5, fontWeight: 600, boxShadow: '0 8px 20px rgba(120,60,20,0.10)' }}>
            <TI.Google s={20} /> Continue with Google
          </button>
          <p style={{ margin: '18px 0 0', fontSize: 12.5, color: TZ.muted, maxWidth: 260, lineHeight: 1.5 }}>Your directions are yours alone — fenced off behind your account.</p>
        </div>
        <div style={{ padding: '0 0 34px', textAlign: 'center', fontFamily: TZ.mono, fontSize: 10.5, letterSpacing: 1, color: TZ.faint }}>🔒 SECURED OVER HTTPS</div>
      </div>
    </Phone>
  );
}

function EdSection({ children }) { return <div style={{ fontFamily: TZ.mono, fontSize: 10.5, letterSpacing: 1.4, textTransform: 'uppercase', color: TZ.faint, margin: '20px 0 9px' }}>{children}</div>; }
function EdRow({ icon, label, value, faded, right }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 12, background: TZ.card, border: '1px solid ' + TZ.line, borderRadius: 13, padding: '13px 14px' }}>
      {icon}
      <div style={{ flex: 1, minWidth: 0 }}>
        {label && <div style={{ fontFamily: TZ.mono, fontSize: 10, letterSpacing: 1, textTransform: 'uppercase', color: TZ.faint }}>{label}</div>}
        <div style={{ fontSize: 15.5, color: faded ? TZ.faint : TZ.ink, marginTop: label ? 2 : 0 }}>{value}</div>
      </div>
      {right || <TI.Chevron s={17} c={tzA(TZ.ink, 0.28)} />}
    </div>
  );
}

function TaskEditor() {
  return (
    <Phone statusDark>
      <div style={{ position: 'absolute', inset: 0, background: TZ.bg, color: TZ.ink, fontFamily: TZ.body, display: 'flex', flexDirection: 'column' }}>
        <div style={{ flexShrink: 0, background: TZ.rust, color: TZ.cream, padding: '48px 16px 14px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <span style={{ display: 'flex', alignItems: 'center', gap: 3, fontSize: 15.5 }}><TI.Back s={21} c={TZ.cream} /> Cancel</span>
          <span style={{ fontFamily: TZ.mono, fontSize: 11, letterSpacing: 2, opacity: 0.9 }}>EDIT TASK</span>
          <span style={{ background: TZ.cream, color: TZ.rust, borderRadius: 10, padding: '8px 16px', fontSize: 15, fontWeight: 700 }}>Save</span>
        </div>
        <Body pad="6px 18px" navPad={false} style={{ paddingBottom: 30 }}>
          <EdSection>Title</EdSection>
          <input readOnly value="Write 500 words — chapter 14" style={{ width: '100%', boxSizing: 'border-box', border: '1px solid ' + TZ.line, background: TZ.card, borderRadius: 13, padding: '14px', fontSize: 18, fontFamily: TZ.serif, fontWeight: 600, color: TZ.ink, outline: 'none' }} />
          <EdSection>Description · optional</EdSection>
          <div style={{ border: '1px solid ' + TZ.line, background: TZ.card, borderRadius: 13, padding: '13px 14px', fontSize: 14.5, color: TZ.muted, lineHeight: 1.5 }}>Pick up after the cave scene. Don't edit — just get the words down.</div>

          <EdSection>When will you do this? · cue</EdSection>
          <div style={{ display: 'flex', alignItems: 'center', gap: 11, background: tzA(TZ.amber, 0.1), border: '1px solid ' + tzA(TZ.amber, 0.4), borderRadius: 13, padding: '13px 14px' }}>
            <TI.Cue s={17} c={TZ.rust} />
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 15.5 }}>After the first coffee</div>
              <div style={{ fontFamily: TZ.mono, fontSize: 10, color: TZ.faint, marginTop: 2 }}>AFTER-ROUTINE · A TRIGGER BEATS A CLOCK</div>
            </div>
            <TI.Chevron s={17} c={tzA(TZ.ink, 0.28)} />
          </div>

          <EdSection>Serves · alignment (optional)</EdSection>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, background: TZ.card, border: '1px solid ' + TZ.line, borderRadius: 12, padding: '11px 13px' }}>
              <Dot c={TZ.tWrite} s={9} /><span style={{ fontSize: 14.5, flex: 1 }}>Finish the novel</span><span style={{ fontFamily: TZ.mono, fontSize: 9.5, color: TZ.faint }}>THEME</span>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, background: TZ.card, border: '1px solid ' + TZ.line, borderRadius: 12, padding: '11px 13px' }}>
              <TI.Repeat s={14} c={TZ.tWrite} /><span style={{ fontSize: 14.5, flex: 1 }}>Write in the morning</span><span style={{ fontFamily: TZ.mono, fontSize: 9.5, color: TZ.faint }}>HABIT</span>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 9, padding: '9px 13px', border: '1px dashed ' + TZ.line, borderRadius: 12 }}>
              <TI.Plus s={14} c={TZ.muted} /><span style={{ fontSize: 13.5, color: TZ.muted }}>Link a goal</span>
            </div>
          </div>

          <EdSection>Dates · state · recurrence</EdSection>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            <EdRow icon={<TI.Calendar s={18} c={TZ.rust} />} label="To Do date" value="Today · Fri, Jul 5" />
            <EdRow icon={<TI.Flag s={16} c={TZ.due} />} label="Due date" value="None" faded />
            <EdRow icon={<span style={{ width: 22, height: 22, borderRadius: 6, border: '2px solid ' + tzA(TZ.ink, 0.3), display: 'inline-block' }} />} label="State" value="Open" right={<span style={{ fontFamily: TZ.mono, fontSize: 10.5, color: TZ.rust }}>CHANGE</span>} />
          </div>
        </Body>
      </div>
    </Phone>
  );
}

// dimmed ghost list used behind capture / state overlays
function Ghost() {
  return (
    <div style={{ position: 'absolute', inset: 0, background: TZ.bg }}>
      <div style={{ background: TZ.rust, height: 150, padding: '52px 20px 0' }}>
        <div style={{ fontFamily: TZ.serif, fontSize: 34, color: TZ.cream, fontWeight: 600 }}>Today</div>
      </div>
      <div style={{ padding: '18px 20px', opacity: 0.5 }}>
        {['Write 500 words — chapter 14', 'Easy 5K along the river', 'Anki — 20 new cards', 'Reply to the editor'].map((t, i) => (
          <div key={i} style={{ display: 'flex', gap: 12, padding: '13px 0', borderBottom: '1px solid ' + TZ.line }}>
            <span style={{ width: 24, height: 24, borderRadius: 7, border: '2px solid ' + tzA(TZ.ink, 0.25) }} /><span style={{ fontSize: 15.5 }}>{t}</span>
          </div>
        ))}
      </div>
    </div>
  );
}

function QuickAdd() {
  const kbRow = (keys) => <div style={{ display: 'flex', gap: 5, justifyContent: 'center' }}>{keys.split('').map((k, i) => <div key={i} style={{ flex: 1, maxWidth: 34, height: 40, borderRadius: 6, background: TZ.card, display: 'flex', alignItems: 'center', justifyContent: 'center', fontFamily: TZ.body, fontSize: 15, boxShadow: '0 1px 0 rgba(0,0,0,0.12)' }}>{k}</div>)}</div>;
  return (
    <Phone statusDark>
      <div style={{ position: 'absolute', inset: 0 }}>
        <Ghost />
        <div style={{ position: 'absolute', inset: 0, background: 'rgba(36,26,18,0.4)' }} />
        <div style={{ position: 'absolute', left: 0, right: 0, bottom: 0 }}>
          {/* composer */}
          <div style={{ background: TZ.surface, borderTopLeftRadius: 18, borderTopRightRadius: 18, padding: '16px 18px 12px', boxShadow: '0 -12px 30px rgba(0,0,0,0.14)' }}>
            <div style={{ fontFamily: TZ.mono, fontSize: 10, letterSpacing: 1, color: TZ.faint }}>QUICK ADD</div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginTop: 10 }}>
              <span style={{ fontFamily: TZ.serif, fontSize: 18, fontWeight: 600, color: TZ.ink }}>Book the physio appointment</span>
              <span style={{ width: 2, height: 22, background: TZ.rust, animation: 'none' }} />
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginTop: 14 }}>
              <button style={{ display: 'inline-flex', alignItems: 'center', gap: 7, background: 'transparent', border: '1px solid ' + TZ.line, borderRadius: 999, padding: '8px 13px', color: TZ.muted, fontFamily: TZ.mono, fontSize: 11 }}><TI.Pencil s={13} c={TZ.muted} /> EXPAND — dates, cue, serves</button>
              <div style={{ flex: 1 }} />
              <span style={{ fontFamily: TZ.mono, fontSize: 10.5, color: TZ.faint }}>↵ SAVE</span>
            </div>
            <div style={{ fontSize: 12, color: TZ.faint, marginTop: 10, lineHeight: 1.4 }}>Just a title is enough. No date needed — it lands in your inbox to schedule later.</div>
          </div>
          {/* keyboard */}
          <div style={{ background: TZ.surfaceAlt, padding: '10px 6px calc(10px + 22px)', display: 'flex', flexDirection: 'column', gap: 8 }}>
            {kbRow('qwertyuiop')}{kbRow('asdfghjkl')}
            <div style={{ display: 'flex', gap: 5, justifyContent: 'center', alignItems: 'center' }}>
              <div style={{ width: 40, height: 40, borderRadius: 6, background: tzA(TZ.ink, 0.12), display: 'flex', alignItems: 'center', justifyContent: 'center' }}><TI.Chevron s={16} c={TZ.muted} dir="up" /></div>
              {kbRow('zxcvbnm')}
              <div style={{ width: 40, height: 40, borderRadius: 6, background: tzA(TZ.ink, 0.12), display: 'flex', alignItems: 'center', justifyContent: 'center' }}><TI.X s={15} c={TZ.muted} /></div>
            </div>
            <div style={{ display: 'flex', gap: 5, alignItems: 'center' }}>
              <div style={{ width: 60, height: 40, borderRadius: 6, background: tzA(TZ.ink, 0.12), display: 'flex', alignItems: 'center', justifyContent: 'center', fontFamily: TZ.mono, fontSize: 12, color: TZ.muted }}>?123</div>
              <div style={{ flex: 1, height: 40, borderRadius: 6, background: TZ.card }} />
              <div style={{ width: 78, height: 40, borderRadius: 6, background: TZ.rust, color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 14, fontWeight: 600 }}>Save</div>
            </div>
          </div>
        </div>
      </div>
    </Phone>
  );
}

const STATES = [
  ['open', 'Open', 'Active and pending — on your plate now.'],
  ['done', 'Done', 'Finished. Ticks the habit, moves the goal, files into Journey.'],
  ['closed', 'Closed', 'Not done, no longer relevant. A calm skip — no penalty, links kept.'],
  ['frozen', 'Frozen', 'On ice for now. Thaw it any time, right where you left it.'],
  ['backlog', 'Someday', 'Parked in the backlog. Out of Today — pull it when it calls.'],
];

function StateSheet() {
  return (
    <Phone statusDark>
      <div style={{ position: 'absolute', inset: 0 }}>
        <Ghost />
        <div style={{ position: 'absolute', inset: 0, background: 'rgba(36,26,18,0.42)' }} />
        <div style={{ position: 'absolute', left: 0, right: 0, bottom: 0, background: TZ.surface, borderTopLeftRadius: 20, borderTopRightRadius: 20, padding: '10px 20px calc(20px + 22px)', boxShadow: '0 -12px 30px rgba(0,0,0,0.16)' }}>
          <div style={{ width: 40, height: 4, borderRadius: 2, background: tzA(TZ.ink, 0.18), margin: '0 auto 14px' }} />
          <div style={{ fontFamily: TZ.serif, fontSize: 22, fontWeight: 600 }}>Where does this stand?</div>
          <div style={{ fontSize: 13, color: TZ.muted, marginTop: 3, marginBottom: 8 }}>Every state is reversible. Nothing here is a failure.</div>
          {STATES.map(([st, name, desc]) => (
            <div key={st} style={{ display: 'flex', alignItems: 'center', gap: 13, padding: '12px 0', borderBottom: '1px solid ' + TZ.line2 }}>
              <Checkbox state={st} />
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 15.5, fontWeight: 600, color: TZ.ink }}>{name}{st === 'open' && <span style={{ fontFamily: TZ.mono, fontSize: 9.5, color: TZ.rust, border: '1px solid ' + tzA(TZ.rust, 0.4), borderRadius: 4, padding: '1px 6px', marginLeft: 8 }}>CURRENT</span>}</div>
                <div style={{ fontSize: 12.5, color: TZ.muted, marginTop: 2, lineHeight: 1.35 }}>{desc}</div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </Phone>
  );
}

function SettingsScreen() {
  return (
    <Phone nav={null} statusDark>
      <div style={{ position: 'absolute', inset: 0, background: TZ.bg, fontFamily: TZ.body, display: 'flex', flexDirection: 'column' }}>
        <RustHeader kicker="TZAFON" title="Settings" compact right={<TI.X s={20} c={TZ.cream} />} />
        <Body navPad={false} style={{ paddingBottom: 40 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 13, background: TZ.card, border: '1px solid ' + TZ.line, borderRadius: 14, padding: '14px', marginTop: 16 }}>
            <FoxLogo s={44} ring={tzA(TZ.ink, 0.06)} />
            <div style={{ flex: 1 }}>
              <div style={{ fontFamily: TZ.serif, fontSize: 16.5, fontWeight: 600 }}>Signed in with Google</div>
              <div style={{ fontSize: 13, color: TZ.muted }}>you@thefoxworks.net</div>
            </div>
          </div>

          <EdSection>The week</EdSection>
          <div style={{ background: TZ.card, border: '1px solid ' + TZ.line, borderRadius: 13, padding: '13px 14px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <TI.Calendar s={17} c={TZ.rust} /><span style={{ fontSize: 15, flex: 1 }}>Week starts on</span>
            </div>
            <div style={{ display: 'flex', gap: 6, marginTop: 11 }}>
              {['Sun', 'Mon', 'Sat'].map((d, i) => <span key={d} style={{ flex: 1, textAlign: 'center', fontFamily: TZ.mono, fontSize: 12, padding: '7px 0', borderRadius: 8, border: '1px solid ' + (i === 0 ? TZ.rust : TZ.line), background: i === 0 ? TZ.rust : 'transparent', color: i === 0 ? '#fff' : TZ.muted }}>{d}</span>)}
            </div>
            <div style={{ fontSize: 11.5, color: TZ.faint, marginTop: 9, lineHeight: 1.4 }}>Sets your Review day, habit periods and every “fresh start”.</div>
          </div>

          <EdSection>Reminders</EdSection>
          <EdRow icon={<TI.Bell s={17} c={TZ.rust} />} label="Cue-based reminders" value="On — fires on your triggers"
            right={<span style={{ width: 44, height: 26, borderRadius: 999, background: TZ.rust, position: 'relative' }}><span style={{ position: 'absolute', top: 3, right: 3, width: 20, height: 20, borderRadius: 99, background: '#fff' }} /></span>} />

          <EdSection>Account</EdSection>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8, height: 50, borderRadius: 13, border: '1px solid ' + tzA(TZ.due, 0.4), color: TZ.due, fontSize: 15.5, fontWeight: 600 }}>Sign out</div>
        </Body>
      </div>
    </Phone>
  );
}

// Alternative navigation model — for comparison against the 5-tab spec.
function NavAlt() {
  const zones = [['Do', TI.Today, true], ['Aim', TI.Compass, false], ['Look back', TI.Journey, false]];
  return (
    <Phone statusDark>
      <div style={{ position: 'absolute', inset: 0, background: TZ.bg, fontFamily: TZ.body, display: 'flex', flexDirection: 'column' }}>
        <RustHeader kicker="ALTERNATIVE NAV" title="Do · Aim · Look back" compact
          meta={<React.Fragment><span>3 ZONES + CAPTURE</span><span>vs. 5 TABS</span></React.Fragment>} />
        <Body navPad={false} style={{ paddingBottom: 30 }}>
          <p style={{ margin: '16px 0 0', fontSize: 14, lineHeight: 1.5, color: TZ.muted }}>
            A tighter model: the three research layers become three zones, with a center <b style={{ color: TZ.ink }}>capture</b> button always in reach. Today/Planning/Backlog live under <b style={{ color: TZ.ink }}>Do</b>; Directions/Habits under <b style={{ color: TZ.ink }}>Aim</b>; Journey/Review under <b style={{ color: TZ.ink }}>Look back</b>. All Tasks stays in search.
          </p>
          <div style={{ marginTop: 16, display: 'flex', flexDirection: 'column', gap: 9 }}>
            {[['Do', 'Today · Planning · Backlog', TZ.rust], ['Aim', 'Directions · Habits', TZ.tHealth], ['Look back', 'Journey · Review', TZ.tLearn]].map(([z, sub, c]) => (
              <div key={z} style={{ display: 'flex', alignItems: 'center', gap: 12, background: TZ.card, border: '1px solid ' + TZ.line, borderLeft: '4px solid ' + c, borderRadius: 12, padding: '12px 14px' }}>
                <div style={{ flex: 1 }}><div style={{ fontFamily: TZ.serif, fontSize: 16, fontWeight: 600 }}>{z}</div><div style={{ fontFamily: TZ.mono, fontSize: 10.5, color: TZ.muted, marginTop: 2 }}>{sub.toUpperCase()}</div></div>
              </div>
            ))}
          </div>
          <div style={{ fontFamily: TZ.mono, fontSize: 10.5, color: TZ.faint, marginTop: 16, textAlign: 'center', lineHeight: 1.5 }}>TRADE-OFF: FEWER TAPS TO CAPTURE,<br />ONE MORE TAP TO REACH A SUB-VIEW</div>
        </Body>
        {/* alt nav bar with center capture */}
        <div style={{ position: 'absolute', bottom: 0, left: 0, right: 0, height: 84, background: tzA(TZ.surface, 0.96), borderTop: '1px solid ' + TZ.line, display: 'flex', alignItems: 'flex-start', paddingBottom: 22, zIndex: 35 }}>
          {zones.slice(0, 1).map(([l, Icon, on]) => <NavZone key={l} l={l} Icon={Icon} on={on} />)}
          {zones.slice(1, 2).map(([l, Icon, on]) => <NavZone key={l} l={l} Icon={Icon} on={on} />)}
          <div style={{ flex: 1, display: 'flex', justifyContent: 'center', paddingTop: 4 }}>
            <div style={{ width: 52, height: 52, borderRadius: 18, background: TZ.rust, display: 'flex', alignItems: 'center', justifyContent: 'center', marginTop: -18, boxShadow: '0 8px 18px ' + tzA(TZ.rust, 0.4) }}><TI.Plus s={24} /></div>
          </div>
          {zones.slice(2).map(([l, Icon, on]) => <NavZone key={l} l={l} Icon={Icon} on={on} />)}
          <NavZone l="Search" Icon={TI.Search} on={false} />
        </div>
        <GesturePill />
      </div>
    </Phone>
  );
}
function NavZone({ l, Icon, on }) {
  return (
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 3, paddingTop: 10 }}>
      <Icon s={21} c={on ? TZ.rust : TZ.faint} w={on ? 2.1 : 1.8} />
      <span style={{ fontFamily: TZ.mono, fontSize: 9, letterSpacing: 0.3, color: on ? TZ.rust : TZ.faint, fontWeight: on ? 700 : 500 }}>{l.toUpperCase()}</span>
    </div>
  );
}

Object.assign(window, { SignIn, TaskEditor, QuickAdd, StateSheet, SettingsScreen, NavAlt, Ghost });
