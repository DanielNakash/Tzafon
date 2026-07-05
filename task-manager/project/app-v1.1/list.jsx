// list.jsx — All Tasks View (default landing): grouped by To Do Date,
// undated group at bottom, Done hidden behind a toggle, indicators on rows.

function dueIndicator(task, today) {
  if (!task.due) return null;
  if (task.toDo && task.due === task.toDo) return 'deadline';      // persistent: scheduled date is the deadline
  if (task.due === today && task.toDo !== task.due) return 'today'; // due-today note
  return null;
}

function TaskRow({ task, today, onOpen, onToggle }) {
  const ind = dueIndicator(task, today);
  const sched = task.toDo === task.due && !!task.due;
  return (
    <div className="ft-press" onClick={onOpen} style={{
      display: 'flex', alignItems: 'center', gap: 13, padding: '15px 4px',
      borderBottom: '1px solid ' + T.line, cursor: 'pointer', opacity: task.done ? 0.6 : 1,
    }}>
      <button onClick={(e) => { e.stopPropagation(); onToggle(); }} className="ft-press" title="Toggle done" style={{
        flexShrink: 0, width: 26, height: 26, padding: 0, cursor: 'pointer',
        border: '2px solid ' + (task.done ? T.rust : hexA(T.ink, 0.3)),
        borderRadius: 7, background: task.done ? T.rust : 'transparent',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
      }}>{task.done && <I.Check s={15} />}</button>

      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{
          fontSize: 16.5, lineHeight: 1.3, color: task.done ? T.muted : T.ink,
          textDecoration: task.done ? 'line-through' : 'none', textDecorationColor: hexA(T.rust, 0.5),
          overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
        }}>{task.title}</div>

        {(task.recurrence || ind) && (
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginTop: 5, flexWrap: 'wrap' }}>
            {task.recurrence && (
              <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4, fontSize: 11.5, color: T.muted, fontFamily: T.mono, whiteSpace: 'nowrap' }}>
                <I.Repeat s={13} c={T.muted} /> {recurSummary(task.recurrence)}
              </span>
            )}
            {ind === 'deadline' && (
              <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4, fontSize: 11, fontFamily: T.mono, whiteSpace: 'nowrap',
                color: T.due, border: '1px solid ' + hexA(T.due, 0.4), borderRadius: 4, padding: '1px 6px', letterSpacing: 0.3 }}>
                <I.Flag s={11} c={T.due} /> DEADLINE
              </span>
            )}
            {ind === 'today' && (
              <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4, fontSize: 11, fontFamily: T.mono, whiteSpace: 'nowrap',
                color: '#fff', background: T.due, borderRadius: 4, padding: '2px 6px', letterSpacing: 0.3 }}>
                <I.Alert s={11} c="#fff" /> DUE TODAY
              </span>
            )}
          </div>
        )}
      </div>
      <I.Chevron s={18} c={hexA(T.ink, 0.28)} />
    </div>
  );
}

function GroupHeader({ label, count, accent }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 9, padding: '22px 4px 8px' }}>
      <span style={{ fontFamily: T.mono, fontSize: 11.5, letterSpacing: 1.5, textTransform: 'uppercase',
        color: accent || T.muted, fontWeight: 600 }}>{label}</span>
      <span style={{ fontFamily: T.mono, fontSize: 11.5, color: T.faint }}>{count}</span>
      <div style={{ flex: 1, height: 1, background: T.line }} />
    </div>
  );
}

function AllTasksView({ tasks, today, showDone, setShowDone, onOpen, onToggle, onAdd, onSignOut }) {
  const active = tasks.filter(t => !t.done);
  const doneTasks = tasks.filter(t => t.done);
  const total = tasks.length, doneCount = doneTasks.length;
  const pct = total ? Math.round((doneCount / total) * 100) : 0;

  // dated active tasks sorted oldest -> newest, then grouped
  const dated = active.filter(t => t.toDo).sort((a, b) => a.toDo.localeCompare(b.toDo));
  const undated = active.filter(t => !t.toDo);

  const groups = [];
  const byKey = {};
  dated.forEach(t => {
    const g = groupFor(t.toDo, today);
    if (!byKey[g.key]) { byKey[g.key] = { ...g, items: [] }; groups.push(byKey[g.key]); }
    byKey[g.key].items.push(t);
  });
  groups.sort((a, b) => a.order - b.order);

  return (
    <div style={{ height: '100%', background: T.bg, color: T.ink, fontFamily: T.body, display: 'flex', flexDirection: 'column' }}>
      {/* header */}
      <div style={{ flexShrink: 0, background: T.rust, color: '#FBF4E6', padding: '54px 18px 16px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span style={{ fontFamily: T.mono, fontSize: 11, letterSpacing: 3, opacity: 0.85, whiteSpace: 'nowrap' }}>DON'T PANIC</span>
          <button onClick={onSignOut} title="Sign out" style={{ width: 38, height: 38, border: 'none', background: 'transparent', cursor: 'pointer', fontSize: 20 }}>🦊</button>
        </div>
        <h1 style={{ margin: '6px 0 0', fontFamily: T.serif, fontSize: 38, fontWeight: 600, letterSpacing: -0.4, lineHeight: 1 }}>All Tasks</h1>
        <div style={{ marginTop: 14 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', fontFamily: T.mono, fontSize: 11.5, letterSpacing: 0.5, opacity: 0.92 }}>
            <span style={{ whiteSpace: 'nowrap' }}>{fmtLong(today).toUpperCase()}</span>
            <span style={{ whiteSpace: 'nowrap' }}>{doneCount}/{total} DONE</span>
          </div>
          <div style={{ marginTop: 8, height: 10, borderRadius: 6, background: 'rgba(255,255,255,0.22)', overflow: 'hidden' }}>
            <div style={{ width: pct + '%', height: '100%', background: T.amber, borderRadius: 6, transition: 'width .45s cubic-bezier(.2,.8,.2,1)' }} />
          </div>
        </div>
      </div>

      {/* list */}
      <div className="ft-scroll" style={{ flex: 1, overflowY: 'auto', padding: '0 18px 120px', position: 'relative' }}>
        {/* done toggle bar */}
        <div style={{ display: 'flex', justifyContent: 'flex-end', paddingTop: 12 }}>
          <button onClick={() => setShowDone(v => !v)} className="ft-press" style={{
            display: 'inline-flex', alignItems: 'center', gap: 7, cursor: 'pointer',
            background: showDone ? T.surface : 'transparent', border: '1px solid ' + T.line,
            borderRadius: 999, padding: '7px 13px', color: T.muted, fontFamily: T.mono,
            fontSize: 11.5, letterSpacing: 0.5,
          }}>
            <I.Eye s={15} c={T.muted} off={!showDone} />
            {showDone ? 'HIDE DONE' : 'SHOW DONE'}
          </button>
        </div>

        {groups.map(g => (
          <div key={g.key}>
            <GroupHeader label={g.label} count={g.items.length} accent={g.key === 'overdue' ? T.due : (g.key === 'today' ? T.rust : null)} />
            {g.items.map(t => <TaskRow key={t.id} task={t} today={today} onOpen={() => onOpen(t.id)} onToggle={() => onToggle(t.id)} />)}
          </div>
        ))}

        {undated.length > 0 && (
          <div>
            <GroupHeader label="No date" count={undated.length} />
            {undated.map(t => <TaskRow key={t.id} task={t} today={today} onOpen={() => onOpen(t.id)} onToggle={() => onToggle(t.id)} />)}
          </div>
        )}

        {active.length === 0 && (
          <div style={{ textAlign: 'center', padding: '54px 30px 20px' }}>
            <div style={{ fontSize: 46, transform: 'rotate(-6deg)' }}>🦊</div>
            <div style={{ fontFamily: T.serif, fontSize: 22, fontWeight: 600, marginTop: 8 }}>All clear.</div>
            <p style={{ color: T.muted, fontSize: 15, maxWidth: 220, margin: '6px auto 0', lineHeight: 1.5 }}>Nothing left on the list. Go read a book.</p>
          </div>
        )}

        {showDone && doneTasks.length > 0 && (
          <div>
            <GroupHeader label="Done" count={doneTasks.length} />
            {doneTasks.map(t => <TaskRow key={t.id} task={t} today={today} onOpen={() => onOpen(t.id)} onToggle={() => onToggle(t.id)} />)}
          </div>
        )}
      </div>

      {/* FAB */}
      <button onClick={onAdd} className="ft-press" title="New task" style={{
        position: 'absolute', right: 20, bottom: 30, zIndex: 30, width: 60, height: 60, borderRadius: '50%',
        border: 'none', background: T.rust, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center',
        boxShadow: '0 12px 26px ' + hexA(T.rust, 0.45),
      }}><I.Plus s={26} /></button>
    </div>
  );
}

window.AllTasksView = AllTasksView;
window.dueIndicator = dueIndicator;
