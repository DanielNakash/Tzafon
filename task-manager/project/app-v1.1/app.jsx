// app.jsx — TaskAppV2: sign-in → All Tasks View → editor, with persistence + undo

function SignIn({ onDone }) {
  const [busy, setBusy] = React.useState(false);
  const go = () => { setBusy(true); setTimeout(() => { setBusy(false); onDone(); }, 950); };
  return (
    <div style={{ height: '100%', background: T.bg, color: T.ink, fontFamily: T.body, display: 'flex', flexDirection: 'column', position: 'relative', overflow: 'hidden' }}>
      <div style={{ position: 'absolute', top: -120, right: -90, width: 320, height: 320, borderRadius: '50%', background: T.amber, opacity: 0.16, filter: 'blur(8px)' }} />
      <div style={{ position: 'absolute', bottom: -140, left: -100, width: 300, height: 300, borderRadius: '50%', background: T.rust, opacity: 0.12, filter: 'blur(6px)' }} />
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: '0 40px', textAlign: 'center', position: 'relative', zIndex: 2 }}>
        <div style={{ width: 104, height: 104, borderRadius: '50%', background: T.surface, border: '2px solid ' + T.rust, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 54, boxShadow: '0 16px 30px rgba(120,60,20,0.18)', transform: 'rotate(-4deg)' }}>🦊</div>
        <div style={{ marginTop: 16, fontFamily: T.mono, fontSize: 12, letterSpacing: 4, color: T.rust, fontWeight: 600, whiteSpace: 'nowrap' }}>DON'T PANIC</div>
        <h1 style={{ margin: '12px 0 0', fontFamily: T.serif, fontSize: 38, fontWeight: 600, lineHeight: 1.1, letterSpacing: -0.2, whiteSpace: 'nowrap' }}>The Fox Works</h1>
        <p style={{ margin: '16px 0 0', fontSize: 16, lineHeight: 1.5, color: T.muted, maxWidth: 264 }}>Your quiet little burrow for everything you mean to get done.</p>
        <button onClick={go} disabled={busy} className="ft-press" style={{ marginTop: 34, width: '100%', maxWidth: 300, height: 56, borderRadius: 14, border: '1px solid ' + T.line, background: T.surface, color: T.ink, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 12, fontFamily: T.body, fontSize: 16.5, fontWeight: 600, boxShadow: '0 8px 20px rgba(120,60,20,0.10)' }}>
          {busy ? <span className="ft-spin" style={{ width: 20, height: 20, borderRadius: '50%', border: '2.5px solid ' + T.line, borderTopColor: T.rust }} /> : <I.Google s={20} />}
          <span style={{ whiteSpace: 'nowrap' }}>{busy ? 'Signing in…' : 'Continue with Google'}</span>
        </button>
        <p style={{ margin: '20px 0 0', fontSize: 12.5, color: T.muted, maxWidth: 250, lineHeight: 1.5 }}>Sign in to see your tasks. They're yours alone — fenced off behind your account.</p>
      </div>
      <div style={{ padding: '0 0 30px', textAlign: 'center', fontFamily: T.mono, fontSize: 11, letterSpacing: 1, color: T.muted, opacity: 0.7 }}>🔒 SECURED OVER HTTPS</div>
    </div>
  );
}

function TaskAppV2({ instanceId = 'v11' }) {
  const KEY = 'taskmgr:' + instanceId;
  const today = todayISO();
  const boot = React.useMemo(() => { try { return JSON.parse(localStorage.getItem(KEY)); } catch (e) { return null; } }, [KEY]);

  const [signedIn, setSignedIn] = React.useState(boot ? !!boot.signedIn : false);
  const [tasks, setTasks] = React.useState(boot && boot.tasks ? boot.tasks : makeSeed());
  const [showDone, setShowDone] = React.useState(boot ? !!boot.showDone : false);
  const [editing, setEditing] = React.useState(null); // task id | 'new' | null
  const [undo, setUndo] = React.useState(null);
  const [toast, setToast] = React.useState(null);
  const undoTimer = React.useRef(null);
  const toastTimer = React.useRef(null);

  React.useEffect(() => { localStorage.setItem(KEY, JSON.stringify({ tasks, signedIn, showDone })); }, [tasks, signedIn, showDone, KEY]);

  const flash = (msg) => { setToast(msg); clearTimeout(toastTimer.current); toastTimer.current = setTimeout(() => setToast(null), 2600); };

  const toggle = (id) => setTasks(prev => prev.map(t => t.id === id ? { ...t, done: !t.done } : t));

  const save = (task, scope) => {
    setTasks(prev => prev.some(t => t.id === task.id) ? prev.map(t => t.id === task.id ? task : t) : [task, ...prev]);
    setEditing(null);
    if (task.recurrence && scope === 'one') flash('Rescheduled this occurrence only');
    else if (task.recurrence && scope === 'forward') flash('Updated this & future occurrences');
    else flash('Saved');
  };

  const del = (id, scope) => {
    const idx = tasks.findIndex(t => t.id === id);
    const task = tasks[idx];
    setTasks(prev => prev.filter(t => t.id !== id));
    setEditing(null);
    setUndo({ task, idx });
    clearTimeout(undoTimer.current);
    undoTimer.current = setTimeout(() => setUndo(null), 4500);
    flash(task.recurrence && scope === 'all' ? 'Deleted all occurrences' : 'Task deleted');
  };
  const doUndo = () => { if (!undo) return; setTasks(prev => { const n = [...prev]; n.splice(Math.min(undo.idx, n.length), 0, undo.task); return n; }); setUndo(null); };

  if (!signedIn) return <SignIn onDone={() => setSignedIn(true)} />;

  if (editing) {
    const task = editing === 'new' ? null : tasks.find(t => t.id === editing);
    return <TaskEditor task={task} today={today} onSave={save} onDelete={del} onClose={() => setEditing(null)} />;
  }

  return (
    <React.Fragment>
      <AllTasksView tasks={tasks} today={today} showDone={showDone} setShowDone={setShowDone}
        onOpen={(id) => setEditing(id)} onToggle={toggle} onAdd={() => setEditing('new')} onSignOut={() => setSignedIn(false)} />

      {toast && !undo && (
        <div className="ft-toast" style={{ position: 'absolute', left: '50%', transform: 'translateX(-50%)', bottom: 100, zIndex: 45, background: T.ink, color: T.surface, borderRadius: 999, padding: '10px 18px', fontSize: 13.5, fontFamily: T.body, whiteSpace: 'nowrap', boxShadow: '0 10px 26px rgba(0,0,0,0.25)' }}>{toast}</div>
      )}
      {undo && (
        <div className="ft-toast" style={{ position: 'absolute', left: 18, right: 18, bottom: 30, zIndex: 46, height: 52, borderRadius: 14, background: T.ink, color: T.surface, display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0 8px 0 18px', fontSize: 14.5, fontFamily: T.body, boxShadow: '0 14px 30px rgba(0,0,0,0.25)' }}>
          <span>{toast || 'Task deleted'}</span>
          <button onClick={doUndo} style={{ border: 'none', background: 'transparent', color: T.amber, fontWeight: 700, fontSize: 14.5, padding: '10px 14px', cursor: 'pointer', fontFamily: T.body, letterSpacing: 0.3 }}>UNDO</button>
        </div>
      )}
    </React.Fragment>
  );
}

window.TaskAppV2 = TaskAppV2;
window.SignIn = SignIn;
