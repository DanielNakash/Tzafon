// task-app.jsx — Fox Works Task Manager POC
// One themeable mobile app: SSO sign-in, task list, create, inline edit,
// toggle open/done, delete + undo. Persists to localStorage per instance.
// Exports to window: TaskApp, THEMES

// ─────────────────────────────────────────────────────────────
// Theme tokens — all "warm rust & amber on cream/paper", 3 personalities
// ─────────────────────────────────────────────────────────────
const THEMES = {
  paper: {
    id: 'paper', variant: 'paper', name: 'Foxpaper',
    bg: '#F4EAD8', surface: '#FFFDF7', surfaceAlt: '#FBF3E5',
    ink: '#2C2118', muted: '#97866F',
    rust: '#BC5128', amber: '#E0982F', line: 'rgba(44,33,24,0.10)',
    serif: '"Newsreader", Georgia, serif',
    body: '"Hanken Grotesk", system-ui, sans-serif',
    mono: '"Spline Sans Mono", ui-monospace, monospace',
  },
  den: {
    id: 'den', variant: 'den', name: 'The Den',
    bg: '#F1E5CF', surface: '#FBF4E6', surfaceAlt: '#F1E5CF',
    ink: '#241A12', muted: '#7C6A52',
    rust: '#A6421E', amber: '#D9913A', line: 'rgba(36,26,18,0.14)',
    serif: '"Gloock", Georgia, serif',
    body: '"Hanken Grotesk", system-ui, sans-serif',
    mono: '"Spline Sans Mono", ui-monospace, monospace',
  },
  // The Den structure + Foxpaper (Newsreader) typography + Pop progress bar
  denx: {
    id: 'denx', variant: 'den', name: 'The Den',
    bg: '#F1E5CF', surface: '#FBF4E6', surfaceAlt: '#F1E5CF',
    ink: '#241A12', muted: '#7C6A52',
    rust: '#A6421E', amber: '#D9913A', line: 'rgba(36,26,18,0.14)',
    serif: '"Newsreader", Georgia, serif', serifWeight: 600,
    body: '"Hanken Grotesk", system-ui, sans-serif',
    mono: '"Spline Sans Mono", ui-monospace, monospace',
    progress: true,
  },
  pop: {
    id: 'pop', variant: 'pop', name: 'Pop Fox',
    bg: '#FBEFD6', surface: '#FFFFFF', surfaceAlt: '#FFF3DE',
    ink: '#2A1C10', muted: '#8B775C',
    rust: '#D2552A', amber: '#F2A93B', line: 'rgba(42,28,16,0.10)',
    serif: '"Bricolage Grotesque", system-ui, sans-serif',
    body: '"Hanken Grotesk", system-ui, sans-serif',
    mono: '"Spline Sans Mono", ui-monospace, monospace',
  },
};

// ─────────────────────────────────────────────────────────────
// Icons
// ─────────────────────────────────────────────────────────────
const GoogleG = ({ s = 20 }) => (
  <svg width={s} height={s} viewBox="0 0 48 48" aria-hidden="true">
    <path fill="#EA4335" d="M24 9.5c3.5 0 6.6 1.2 9 3.6l6.7-6.7C35.6 2.4 30.2 0 24 0 14.6 0 6.4 5.4 2.5 13.3l7.8 6.1C12.2 13.2 17.6 9.5 24 9.5z"/>
    <path fill="#4285F4" d="M46.5 24.5c0-1.6-.1-3.1-.4-4.5H24v9h12.7c-.5 3-2.2 5.5-4.7 7.2l7.3 5.7c4.3-3.9 6.8-9.8 6.8-17.4z"/>
    <path fill="#FBBC05" d="M10.3 28.6c-.5-1.5-.8-3-.8-4.6s.3-3.1.8-4.6l-7.8-6.1C.9 16.5 0 20.1 0 24s.9 7.5 2.5 10.7l7.8-6.1z"/>
    <path fill="#34A853" d="M24 48c6.2 0 11.4-2 15.2-5.5l-7.3-5.7c-2 1.4-4.7 2.3-7.9 2.3-6.4 0-11.8-3.7-13.7-9.1l-7.8 6.1C6.4 42.6 14.6 48 24 48z"/>
  </svg>
);

const Check = ({ s = 16, c = '#fff', w = 3 }) => (
  <svg width={s} height={s} viewBox="0 0 24 24" fill="none">
    <path d="M5 12.5l4.5 4.5L19 7" stroke={c} strokeWidth={w} strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
);
const Plus = ({ s = 22, c = '#fff', w = 2.6 }) => (
  <svg width={s} height={s} viewBox="0 0 24 24" fill="none">
    <path d="M12 5v14M5 12h14" stroke={c} strokeWidth={w} strokeLinecap="round"/>
  </svg>
);
const XMark = ({ s = 16, c, w = 2.4 }) => (
  <svg width={s} height={s} viewBox="0 0 24 24" fill="none">
    <path d="M6 6l12 12M18 6L6 18" stroke={c} strokeWidth={w} strokeLinecap="round"/>
  </svg>
);
const Trash = ({ s = 17, c }) => (
  <svg width={s} height={s} viewBox="0 0 24 24" fill="none">
    <path d="M4 7h16M9 7V5a1 1 0 011-1h4a1 1 0 011 1v2m-9 0l1 13a1 1 0 001 1h6a1 1 0 001-1l1-13" stroke={c} strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
);
const Pencil = ({ s = 16, c }) => (
  <svg width={s} height={s} viewBox="0 0 24 24" fill="none">
    <path d="M14.5 5.5l4 4M3.5 20.5l4.2-.9L19 8.3a1.5 1.5 0 000-2.1l-1.2-1.2a1.5 1.5 0 00-2.1 0L4.4 16.3l-.9 4.2z" stroke={c} strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
);

// ─────────────────────────────────────────────────────────────
// Seed + persistence
// ─────────────────────────────────────────────────────────────
const SEED = [
  { id: 's1', title: 'Finish the third Japan travelogue post', done: false },
  { id: 's2', title: 'Edit the SciFi short — “The Long Burrow”', done: false },
  { id: 's3', title: 'Reply to Roey about the podcast', done: false },
  { id: 's4', title: 'Back up the manuscript drafts', done: true },
  { id: 's5', title: 'Water the office plant 🪴', done: true },
];

function load(key) {
  try { return JSON.parse(localStorage.getItem(key)); } catch (e) { return null; }
}

// ─────────────────────────────────────────────────────────────
// Main app
// ─────────────────────────────────────────────────────────────
function TaskApp({ theme, instanceId, initialScreen = 'signin' }) {
  const t = theme;
  const KEY = 'foxtask:' + instanceId;
  const saved = React.useMemo(() => load(KEY), [KEY]);

  const [screen, setScreen] = React.useState(
    saved && saved.signedIn ? 'app' : initialScreen
  );
  const [signing, setSigning] = React.useState(false);
  const [tasks, setTasks] = React.useState(saved && saved.tasks ? saved.tasks : SEED);
  const [composerOpen, setComposerOpen] = React.useState(false);
  const [draft, setDraft] = React.useState('');
  const [editingId, setEditingId] = React.useState(null);
  const [editDraft, setEditDraft] = React.useState('');
  const [undo, setUndo] = React.useState(null); // {task, index}
  const [bursts, setBursts] = React.useState({}); // taskId -> nonce
  const composerRef = React.useRef(null);
  const undoTimer = React.useRef(null);

  // persist
  React.useEffect(() => {
    localStorage.setItem(KEY, JSON.stringify({ tasks, signedIn: screen === 'app' }));
  }, [tasks, screen, KEY]);

  React.useEffect(() => {
    if (composerOpen && composerRef.current) composerRef.current.focus();
  }, [composerOpen]);

  // handlers
  const signIn = () => {
    setSigning(true);
    setTimeout(() => { setSigning(false); setScreen('app'); }, 950);
  };
  const signOut = () => { setScreen('signin'); };

  const addTask = () => {
    const v = draft.trim();
    if (!v) return;
    setTasks(prev => [{ id: 'n' + Date.now(), title: v, done: false, fresh: true }, ...prev]);
    setDraft('');
    setComposerOpen(false);
  };

  const toggle = (id) => {
    setTasks(prev => prev.map(x => {
      if (x.id !== id) return x;
      const nowDone = !x.done;
      if (nowDone && t.variant === 'pop') {
        setBursts(b => ({ ...b, [id]: Date.now() }));
        setTimeout(() => setBursts(b => { const n = { ...b }; delete n[id]; return n; }), 700);
      }
      return { ...x, done: nowDone, fresh: false };
    }));
  };

  const startEdit = (task) => { setEditingId(task.id); setEditDraft(task.title); };
  const commitEdit = () => {
    const v = editDraft.trim();
    if (v) setTasks(prev => prev.map(x => x.id === editingId ? { ...x, title: v } : x));
    setEditingId(null);
  };
  const cancelEdit = () => { setEditingId(null); };

  const remove = (id) => {
    const index = tasks.findIndex(x => x.id === id);
    const task = tasks[index];
    setTasks(prev => prev.filter(x => x.id !== id));
    setUndo({ task, index });
    clearTimeout(undoTimer.current);
    undoTimer.current = setTimeout(() => setUndo(null), 4200);
  };
  const doUndo = () => {
    if (!undo) return;
    setTasks(prev => {
      const next = [...prev];
      next.splice(Math.min(undo.index, next.length), 0, undo.task);
      return next;
    });
    setUndo(null);
    clearTimeout(undoTimer.current);
  };

  const open = tasks.filter(x => !x.done);
  const done = tasks.filter(x => x.done);

  // ── Sign-in screen ──────────────────────────────────────────
  if (screen === 'signin') {
    return (
      <div className="ft-screen" style={{
        height: '100%', background: t.bg, color: t.ink,
        fontFamily: t.body, display: 'flex', flexDirection: 'column',
        position: 'relative', overflow: 'hidden',
      }}>
        {/* warm wash */}
        <div style={{
          position: 'absolute', top: -120, right: -90, width: 320, height: 320,
          borderRadius: '50%', background: t.amber, opacity: 0.16, filter: 'blur(8px)',
        }} />
        <div style={{
          position: 'absolute', bottom: -140, left: -100, width: 300, height: 300,
          borderRadius: '50%', background: t.rust, opacity: 0.12, filter: 'blur(6px)',
        }} />

        <div style={{
          flex: 1, display: 'flex', flexDirection: 'column',
          alignItems: 'center', justifyContent: 'center',
          padding: '0 40px', textAlign: 'center', position: 'relative', zIndex: 2,
        }}>
          <div style={{
            width: 104, height: 104, borderRadius: t.variant === 'pop' ? 30 : '50%',
            background: t.surface, border: '2px solid ' + t.rust,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontSize: 54, boxShadow: '0 16px 30px rgba(120,60,20,0.18)',
            transform: 'rotate(-4deg)',
          }}>🦊</div>

          <div style={{
            marginTop: 16, fontFamily: t.mono, fontSize: 12, letterSpacing: 4,
            color: t.rust, fontWeight: 600, whiteSpace: 'nowrap',
          }}>DON'T PANIC</div>

          <h1 style={{
            margin: '12px 0 0', fontFamily: t.serif, whiteSpace: 'nowrap',
            fontSize: t.variant === 'den' ? 37 : 38,
            fontWeight: t.variant === 'pop' ? 800 : 600,
            lineHeight: 1.1, letterSpacing: t.variant === 'pop' ? -1 : -0.2,
          }}>The Fox Works</h1>

          <p style={{
            margin: '16px 0 0', fontSize: 16, lineHeight: 1.5,
            color: t.muted, maxWidth: 264,
          }}>Your quiet little burrow for everything you mean to get done.</p>

          <button onClick={signIn} disabled={signing} className="ft-press" style={{
            marginTop: 34, width: '100%', maxWidth: 300, height: 56,
            borderRadius: t.variant === 'pop' ? 18 : 14, border: '1px solid ' + t.line,
            background: t.surface, color: t.ink, cursor: 'pointer',
            display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 12,
            fontFamily: t.body, fontSize: 16.5, fontWeight: 600,
            boxShadow: '0 8px 20px rgba(120,60,20,0.10)',
          }}>
            {signing
              ? <span className="ft-spin" style={{
                  width: 20, height: 20, borderRadius: '50%',
                  border: '2.5px solid ' + t.line, borderTopColor: t.rust,
                }} />
              : <GoogleG s={20} />}
            <span style={{ whiteSpace: 'nowrap' }}>{signing ? 'Signing in…' : 'Continue with Google'}</span>
          </button>

          <p style={{ margin: '20px 0 0', fontSize: 12.5, color: t.muted, maxWidth: 250, lineHeight: 1.5 }}>
            Sign in to see your tasks. They're yours alone — fenced off behind your account.
          </p>
        </div>

        <div style={{
          padding: '0 0 30px', textAlign: 'center',
          fontFamily: t.mono, fontSize: 11, letterSpacing: 1, color: t.muted, opacity: 0.7,
        }}>🔒 SECURED OVER HTTPS</div>
      </div>
    );
  }

  // ── App screen ──────────────────────────────────────────────
  const total = tasks.length;
  const doneCount = done.length;

  return (
    <div className="ft-screen" style={{
      height: '100%', background: t.bg, color: t.ink, fontFamily: t.body,
      display: 'flex', flexDirection: 'column', position: 'relative', overflow: 'hidden',
    }}>
      <Header t={t} doneCount={doneCount} total={total} onSignOut={signOut} onAdd={() => setComposerOpen(true)} />

      {/* list */}
      <div className="ft-scroll" style={{ flex: 1, overflowY: 'auto', padding: '6px 18px 120px' }}>
        {open.length > 0 && (
          <GroupLabel t={t} text="To do" count={open.length} />
        )}
        {open.map(task => (
          <TaskRow key={task.id} task={task} t={t}
            editing={editingId === task.id} editDraft={editDraft} setEditDraft={setEditDraft}
            onToggle={() => toggle(task.id)} onStartEdit={() => startEdit(task)}
            onCommit={commitEdit} onCancel={cancelEdit} onDelete={() => remove(task.id)} burst={bursts[task.id]} />
        ))}

        {open.length === 0 && (
          <EmptyState t={t} onAdd={() => setComposerOpen(true)} />
        )}

        {done.length > 0 && (
          <React.Fragment>
            <GroupLabel t={t} text="Done" count={done.length} dim />
            {done.map(task => (
              <TaskRow key={task.id} task={task} t={t}
                editing={editingId === task.id} editDraft={editDraft} setEditDraft={setEditDraft}
                onToggle={() => toggle(task.id)} onStartEdit={() => startEdit(task)}
                onCommit={commitEdit} onCancel={cancelEdit} onDelete={() => remove(task.id)} burst={bursts[task.id]} />
            ))}
          </React.Fragment>
        )}
      </div>

      {/* FAB — add task */}
      {(
        <button onClick={() => setComposerOpen(true)} className="ft-press" title="New task" style={{
          position: 'absolute', right: 20, bottom: 30, zIndex: 30,
          width: 60, height: 60, borderRadius: t.variant === 'pop' ? 20 : '50%',
          border: 'none', background: t.rust, cursor: 'pointer',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          boxShadow: '0 12px 26px ' + hexA(t.rust, 0.45),
        }}><Plus s={26} /></button>
      )}

      {/* undo toast */}
      {undo && (
        <div className="ft-toast" style={{
          position: 'absolute', left: 18, right: 18, bottom: 30, zIndex: 40,
          height: 52, borderRadius: 14, background: t.ink, color: t.surface,
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          padding: '0 8px 0 18px', fontSize: 14.5, fontFamily: t.body,
          boxShadow: '0 14px 30px rgba(0,0,0,0.25)',
        }}>
          <span>Task deleted</span>
          <button onClick={doUndo} style={{
            border: 'none', background: 'transparent', color: t.amber,
            fontWeight: 700, fontSize: 14.5, padding: '10px 14px', cursor: 'pointer',
            fontFamily: t.body, letterSpacing: 0.3,
          }}>UNDO</button>
        </div>
      )}

      {/* composer sheet */}
      {composerOpen && (
        <div style={{ position: 'absolute', inset: 0, zIndex: 50 }}>
          <div onClick={() => setComposerOpen(false)} className="ft-fade" style={{
            position: 'absolute', inset: 0, background: 'rgba(30,18,8,0.34)',
          }} />
          <div className="ft-sheet" style={{
            position: 'absolute', left: 0, right: 0, bottom: 0,
            background: t.surface, borderTopLeftRadius: 26, borderTopRightRadius: 26,
            padding: '14px 18px 26px', boxShadow: '0 -10px 40px rgba(0,0,0,0.18)',
          }}>
            <div style={{ width: 40, height: 5, borderRadius: 3, background: t.line, margin: '0 auto 16px' }} />
            <div style={{
              fontFamily: t.serif, fontSize: 22,
              fontWeight: t.variant === 'pop' ? 800 : 600, marginBottom: 12,
            }}>New task</div>
            <input ref={composerRef} value={draft} onChange={e => setDraft(e.target.value)}
              onKeyDown={e => { if (e.key === 'Enter') addTask(); }}
              placeholder="What needs doing?" style={{
                width: '100%', boxSizing: 'border-box', height: 52, borderRadius: 13,
                border: '1.5px solid ' + t.line, background: t.surfaceAlt,
                padding: '0 16px', fontSize: 16.5, fontFamily: t.body, color: t.ink,
                outline: 'none',
              }} />
            <div style={{ display: 'flex', gap: 10, marginTop: 14 }}>
              <button onClick={() => setComposerOpen(false)} className="ft-press" style={{
                flex: 1, height: 50, borderRadius: 13, border: '1.5px solid ' + t.line,
                background: 'transparent', color: t.muted, fontSize: 16, fontWeight: 600,
                fontFamily: t.body, cursor: 'pointer',
              }}>Cancel</button>
              <button onClick={addTask} className="ft-press" style={{
                flex: 2, height: 50, borderRadius: 13, border: 'none',
                background: t.rust, color: '#fff', fontSize: 16, fontWeight: 700,
                fontFamily: t.body, cursor: 'pointer',
                opacity: draft.trim() ? 1 : 0.5,
              }}>Add task</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// Header (per variant)
// ─────────────────────────────────────────────────────────────
function Header({ t, doneCount, total, onSignOut, onAdd }) {
  const pct = total ? Math.round((doneCount / total) * 100) : 0;
  const today = new Date().toLocaleDateString('en-US', { weekday: 'long', month: 'short', day: 'numeric' });

  if (t.variant === 'den') {
    return (
      <div style={{ flexShrink: 0 }}>
        <div style={{
          background: t.rust, color: '#FBF4E6', padding: '54px 18px 16px',
          position: 'relative',
        }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontFamily: t.mono, fontSize: 11, letterSpacing: 3, opacity: 0.85, whiteSpace: 'nowrap' }}>DON'T PANIC</span>
            <button onClick={onSignOut} title="Sign out" style={ghostBtn('#FBF4E6')}>
              <span style={{ fontSize: 20 }}>🦊</span>
            </button>
          </div>
          <h1 style={{ margin: '6px 0 0', fontFamily: t.serif, fontSize: 38, fontWeight: t.serifWeight || 400, letterSpacing: t.serifWeight ? -0.4 : 0, lineHeight: 1 }}>Today</h1>
          {t.progress ? (
            <div style={{ marginTop: 14 }}>
              <div style={{
                display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end',
                fontFamily: t.mono, fontSize: 11.5, letterSpacing: 0.5, opacity: 0.92,
              }}>
                <span style={{ whiteSpace: 'nowrap' }}>{today.toUpperCase()}</span>
                <span style={{ whiteSpace: 'nowrap' }}>{doneCount}/{total} DONE</span>
              </div>
              <div style={{ marginTop: 8, height: 10, borderRadius: 6, background: 'rgba(255,255,255,0.22)', overflow: 'hidden' }}>
                <div style={{ width: pct + '%', height: '100%', background: t.amber, borderRadius: 6, transition: 'width .45s cubic-bezier(.2,.8,.2,1)' }} />
              </div>
            </div>
          ) : (
            <div style={{
              display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', marginTop: 8,
              fontFamily: t.mono, fontSize: 12, letterSpacing: 0.5, opacity: 0.9,
            }}>
              <span style={{ whiteSpace: 'nowrap' }}>{today.toUpperCase()}</span>
              <span style={{ whiteSpace: 'nowrap' }}>{doneCount}/{total} DONE</span>
            </div>
          )}
        </div>
      </div>
    );
  }

  if (t.variant === 'pop') {
    return (
      <div style={{ flexShrink: 0, padding: '52px 20px 6px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span style={{ fontFamily: t.mono, fontSize: 11, letterSpacing: 3, color: t.rust, fontWeight: 700 }}>DON'T PANIC</span>
          <button onClick={onSignOut} title="Sign out" className="ft-press" style={{
            width: 40, height: 40, borderRadius: 14, border: 'none', background: t.surfaceAlt,
            display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer', fontSize: 20,
          }}>🦊</button>
        </div>
        <h1 style={{ margin: '10px 0 0', fontFamily: t.serif, fontSize: 40, fontWeight: 800, letterSpacing: -1.2, lineHeight: 1 }}>
          Hey, Daniel
        </h1>
        <div style={{ marginTop: 14, display: 'flex', alignItems: 'center', gap: 12 }}>
          <div style={{ flex: 1, height: 12, borderRadius: 8, background: t.surfaceAlt, overflow: 'hidden' }}>
            <div style={{ width: pct + '%', height: '100%', background: t.rust, borderRadius: 8, transition: 'width .4s cubic-bezier(.2,.8,.2,1)' }} />
          </div>
          <span style={{ fontFamily: t.mono, fontSize: 12, color: t.muted, fontWeight: 600 }}>{doneCount}/{total}</span>
        </div>
      </div>
    );
  }

  // paper
  return (
    <div style={{ flexShrink: 0, padding: '52px 22px 6px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <div style={{
            width: 38, height: 38, borderRadius: '50%', background: t.amber,
            display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 20,
          }}>🦊</div>
          <div>
            <div style={{ fontFamily: t.mono, fontSize: 10, letterSpacing: 2.2, color: t.rust, lineHeight: 1.4, whiteSpace: 'nowrap' }}>DON'T PANIC</div>
            <div style={{ fontSize: 13.5, color: t.muted, marginTop: 2, whiteSpace: 'nowrap' }}>{today}</div>
          </div>
        </div>
        <button onClick={onSignOut} title="Sign out" style={ghostBtn(t.muted)}>
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="5" r="1.6" fill={t.muted}/><circle cx="12" cy="12" r="1.6" fill={t.muted}/><circle cx="12" cy="19" r="1.6" fill={t.muted}/></svg>
        </button>
      </div>
      <h1 style={{ margin: '14px 0 2px', fontFamily: t.serif, fontSize: 38, fontWeight: 600, letterSpacing: -0.3, lineHeight: 1 }}>Today</h1>
      <div style={{ fontSize: 14.5, color: t.muted }}>
        {doneCount === total && total > 0 ? 'All done — go read a book.' : `${doneCount} of ${total} done`}
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// Group label
// ─────────────────────────────────────────────────────────────
function GroupLabel({ t, text, count, dim }) {
  return (
    <div style={{
      display: 'flex', alignItems: 'center', gap: 8,
      padding: '20px 4px 10px', opacity: dim ? 0.6 : 1,
    }}>
      <span style={{ fontFamily: t.mono, fontSize: 11.5, letterSpacing: 2, textTransform: 'uppercase', color: t.muted, fontWeight: 600 }}>{text}</span>
      <span style={{ fontFamily: t.mono, fontSize: 11.5, color: t.muted, opacity: 0.7 }}>{count}</span>
      <div style={{ flex: 1, height: 1, background: t.line }} />
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// Task row (per variant)
// ─────────────────────────────────────────────────────────────
function TaskRow({ task, t, editing, editDraft, setEditDraft, onToggle, onStartEdit, onCommit, onCancel, onDelete, burst }) {
  const editRef = React.useRef(null);
  React.useEffect(() => { if (editing && editRef.current) { editRef.current.focus(); editRef.current.select(); } }, [editing]);

  const checkbox = (
    <button onClick={onToggle} className="ft-press" style={{
      position: 'relative', flexShrink: 0, cursor: 'pointer', border: 'none', background: 'transparent', padding: 0,
      width: t.variant === 'pop' ? 30 : 28, height: t.variant === 'pop' ? 30 : 28,
    }}>
      <span style={{
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        width: '100%', height: '100%',
        borderRadius: t.variant === 'pop' ? 10 : (t.variant === 'den' ? 6 : '50%'),
        border: '2px solid ' + (task.done ? t.rust : hexA(t.ink, 0.28)),
        background: task.done ? t.rust : 'transparent',
        transition: 'background .18s, border-color .18s, transform .18s',
      }}>
        {task.done && <Check s={t.variant === 'pop' ? 18 : 16} />}
      </span>
      {/* pop confetti */}
      {burst && t.variant === 'pop' && (
        <span aria-hidden="true">
          {[0,1,2,3,4,5].map(i => (
            <span key={i} className="ft-confetti" style={{
              position: 'absolute', top: '50%', left: '50%', width: 6, height: 6, borderRadius: 2,
              background: [t.rust, t.amber, '#E86F3E'][i % 3],
              '--dx': (Math.cos((i/6)*6.28)*26) + 'px',
              '--dy': (Math.sin((i/6)*6.28)*26) + 'px',
            }} />
          ))}
        </span>
      )}
    </button>
  );

  const titleEl = editing ? (
    <input ref={editRef} value={editDraft} onChange={e => setEditDraft(e.target.value)}
      onBlur={onCommit} onKeyDown={e => { if (e.key === 'Enter') onCommit(); if (e.key === 'Escape') onCancel(); }}
      style={{
        flex: 1, minWidth: 0, border: 'none', borderBottom: '2px solid ' + t.rust,
        background: 'transparent', fontSize: 16.5, fontFamily: t.body, color: t.ink,
        padding: '2px 0', outline: 'none',
      }} />
  ) : (
    <span onClick={onStartEdit} style={{
      flex: 1, minWidth: 0, fontSize: 16.5, lineHeight: 1.35, cursor: 'text',
      color: task.done ? t.muted : t.ink,
      textDecoration: task.done ? 'line-through' : 'none',
      textDecorationColor: hexA(t.rust, 0.6),
      transition: 'color .2s',
    }}>{task.title}</span>
  );

  // confirm / cancel for inline edit — pointerDown preventDefault keeps the
  // input from blur-committing before the button's click fires
  const editActions = (
    <div style={{ display: 'flex', gap: 6, flexShrink: 0 }}>
      <button onPointerDown={e => e.preventDefault()} onClick={onCancel} className="ft-press" title="Cancel" style={{
        width: 33, height: 33, borderRadius: 9, border: '1px solid ' + t.line,
        background: 'transparent', cursor: 'pointer',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
      }}><XMark s={15} c={t.muted} /></button>
      <button onPointerDown={e => e.preventDefault()} onClick={onCommit} className="ft-press" title="Save" style={{
        width: 33, height: 33, borderRadius: 9, border: 'none',
        background: t.rust, cursor: 'pointer',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
      }}><Check s={16} c="#fff" w={3} /></button>
    </div>
  );

  // DEN — divider rows, stamp
  if (t.variant === 'den') {
    return (
      <div className={task.fresh ? 'ft-pop-in' : ''} style={{
        display: 'flex', alignItems: 'center', gap: 14,
        padding: '16px 4px', borderBottom: '1px solid ' + t.line, position: 'relative',
        opacity: task.done ? 0.62 : 1,
      }}>
        {checkbox}
        {titleEl}
        {editing ? editActions : (
          <React.Fragment>
            {task.done && (
              <span style={{
                fontFamily: t.mono, fontSize: 9, fontWeight: 700, letterSpacing: 1,
                color: t.rust, border: '1.5px solid ' + t.rust, borderRadius: 3,
                padding: '2px 5px', transform: 'rotate(-7deg)', flexShrink: 0,
              }}>DONE</span>
            )}
            <RowActions t={t} onDelete={onDelete} />
          </React.Fragment>
        )}
      </div>
    );
  }

  // PAPER + POP — cards
  const radius = t.variant === 'pop' ? 20 : 16;
  return (
    <div className={task.fresh ? 'ft-pop-in' : ''} style={{
      display: 'flex', alignItems: 'center', gap: 14,
      background: task.done ? t.surfaceAlt : t.surface,
      border: '1px solid ' + (t.variant === 'pop' ? 'transparent' : t.line),
      borderRadius: radius, padding: t.variant === 'pop' ? '16px 16px' : '15px 16px',
      marginBottom: 10, position: 'relative',
      boxShadow: task.done ? 'none' : (t.variant === 'pop'
        ? '0 4px 14px rgba(120,60,20,0.08)'
        : '0 3px 10px rgba(120,60,20,0.06)'),
      transition: 'background .2s',
    }}>
      {checkbox}
      {titleEl}
      {editing ? editActions : <RowActions t={t} onDelete={onDelete} />}
    </div>
  );
}

function RowActions({ t, onDelete }) {
  return (
    <button onClick={onDelete} className="ft-rowdel ft-press" title="Delete" style={{
      flexShrink: 0, width: 30, height: 30, borderRadius: 9, border: 'none',
      background: 'transparent', cursor: 'pointer',
      display: 'flex', alignItems: 'center', justifyContent: 'center',
    }}><Trash s={17} c={hexA(t.ink, 0.32)} /></button>
  );
}

// ─────────────────────────────────────────────────────────────
// Empty state
// ─────────────────────────────────────────────────────────────
function EmptyState({ t, onAdd }) {
  return (
    <div style={{
      display: 'flex', flexDirection: 'column', alignItems: 'center',
      textAlign: 'center', padding: '40px 30px',
    }}>
      <div style={{ fontSize: 50, transform: 'rotate(-6deg)' }}>🦊</div>
      <div style={{ fontFamily: t.serif, fontSize: 22, fontWeight: t.variant === 'pop' ? 800 : 600, marginTop: 10 }}>
        Inbox zero, fox style.
      </div>
      <p style={{ color: t.muted, fontSize: 15, lineHeight: 1.5, margin: '6px 0 18px', maxWidth: 230 }}>
        Nothing left to do. Add the next thing before you forget it.
      </p>
      <button onClick={onAdd} className="ft-press" style={{
        height: 46, padding: '0 22px', borderRadius: 13, border: 'none',
        background: t.rust, color: '#fff', fontSize: 15.5, fontWeight: 700,
        fontFamily: t.body, cursor: 'pointer',
      }}>+ New task</button>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// utils
// ─────────────────────────────────────────────────────────────
function ghostBtn(c) {
  return {
    width: 40, height: 40, borderRadius: 12, border: 'none', background: 'transparent',
    display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer',
  };
}
function hexA(hex, a) {
  const h = hex.replace('#', '');
  const r = parseInt(h.substring(0, 2), 16), g = parseInt(h.substring(2, 4), 16), b = parseInt(h.substring(4, 6), 16);
  return `rgba(${r},${g},${b},${a})`;
}

Object.assign(window, { TaskApp, THEMES });
