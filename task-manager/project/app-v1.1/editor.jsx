// editor.jsx — full-screen task editor (create + edit), recurrence config,
// due-date modes, end date, and per-occurrence edit scope.

function DateRow({ icon, label, value, placeholder, onClick, onClear, note, noteColor }) {
  return (
    <div>
      <div className="ft-press" onClick={onClick} style={{
        display: 'flex', alignItems: 'center', gap: 13, background: T.card, border: '1px solid ' + T.line,
        borderRadius: 13, padding: '14px 14px', cursor: 'pointer',
      }}>
        <I.Calendar s={19} c={T.rust} />
        <div style={{ flex: 1 }}>
          <div style={{ fontSize: 11, fontFamily: T.mono, letterSpacing: 1, textTransform: 'uppercase', color: T.faint }}>{label}</div>
          <div style={{ fontSize: 16, color: value ? T.ink : T.faint, marginTop: 2 }}>{value ? fmtLong(value) : placeholder}</div>
        </div>
        {value ? (
          <button onClick={(e) => { e.stopPropagation(); onClear(); }} className="ft-press" style={{ width: 30, height: 30, borderRadius: 8, border: 'none', background: T.surfaceAlt, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}><I.X s={14} c={T.muted} /></button>
        ) : <I.Chevron s={18} c={hexA(T.ink, 0.3)} />}
      </div>
      {note && (
        <div style={{ display: 'inline-flex', alignItems: 'center', gap: 5, marginTop: 8, marginLeft: 4, fontSize: 12.5, fontFamily: T.mono, color: noteColor || T.due }}>
          {noteColor === T.due ? <I.Alert s={13} c={T.due} /> : <I.Flag s={13} c={noteColor || T.due} />} {note}
        </div>
      )}
    </div>
  );
}

function SectionLabel({ children }) {
  return <div style={{ fontFamily: T.mono, fontSize: 10.5, letterSpacing: 1.4, textTransform: 'uppercase', color: T.faint, margin: '22px 4px 10px' }}>{children}</div>;
}

function TaskEditor({ task, today, onSave, onDelete, onClose }) {
  const isNew = !task;
  const [d, setD] = React.useState(() => task ? { ...task } : {
    id: 'n' + Date.now(), title: '', desc: '', toDo: today, due: null, done: false, recurrence: null,
  });
  const [sheet, setSheet] = React.useState(null);     // 'todo' | 'due' | 'end' | null
  const [scope, setScope] = React.useState('all');    // recurring edit scope
  const [delSheet, setDelSheet] = React.useState(false);
  const up = (patch) => setD(p => ({ ...p, ...patch }));

  const repeat = !!d.recurrence;
  const r = d.recurrence;

  const toggleRepeat = () => {
    if (repeat) up({ recurrence: null });
    else up({ recurrence: { ...defaultRecurrence(), dueMode: d.due ? 'singular' : 'none' } });
  };
  const upRule = (patch) => up({ recurrence: { ...r, ...patch } });

  // due indicator note in editor
  let note = null, noteColor = null;
  if (d.due) {
    if (d.toDo && d.due === d.toDo) { note = 'Scheduled date is also the deadline'; noteColor = T.due; }
    else if (d.due === today) { note = 'Due today'; noteColor = T.due; }
  }

  const canSave = d.title.trim().length > 0;
  const singular = repeat && r.dueMode === 'singular';

  return (
    <div style={{ height: '100%', background: T.bg, color: T.ink, fontFamily: T.body, display: 'flex', flexDirection: 'column', position: 'relative' }}>
      {/* header */}
      <div style={{ flexShrink: 0, background: T.rust, color: '#FBF4E6', padding: '50px 14px 14px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <button onClick={onClose} className="ft-press" style={{ display: 'flex', alignItems: 'center', gap: 2, border: 'none', background: 'transparent', color: '#FBF4E6', cursor: 'pointer', fontSize: 16, fontFamily: T.body, padding: 6 }}>
          <I.Back s={22} c="#FBF4E6" /> Cancel
        </button>
        <div style={{ fontFamily: T.mono, fontSize: 11, letterSpacing: 2, opacity: 0.9 }}>{isNew ? 'NEW TASK' : 'EDIT TASK'}</div>
        <button onClick={() => canSave && onSave(d, scope)} className="ft-press" disabled={!canSave} style={{
          border: 'none', background: '#FBF4E6', color: T.rust, cursor: canSave ? 'pointer' : 'default', opacity: canSave ? 1 : 0.45,
          borderRadius: 10, padding: '9px 16px', fontSize: 15, fontWeight: 700, fontFamily: T.body,
        }}>Save</button>
      </div>

      <div className="ft-scroll" style={{ flex: 1, overflowY: 'auto', padding: '6px 18px 40px' }}>
        {/* recurring edit scope */}
        {!isNew && repeat && (
          <div style={{ background: hexA(T.amber, 0.14), border: '1px solid ' + hexA(T.amber, 0.4), borderRadius: 13, padding: '13px 14px', marginTop: 14 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 7, fontSize: 13.5, color: T.ink, marginBottom: 10 }}>
              <I.Repeat s={15} c={T.rust} /> <b>Repeating task.</b> Apply changes to:
            </div>
            <Segmented small value={scope} set={setScope} options={[
              { v: 'one', label: 'This one' }, { v: 'forward', label: 'This & future' }, { v: 'all', label: 'All' },
            ]} />
            <div style={{ fontSize: 12, color: T.muted, marginTop: 8, lineHeight: 1.4 }}>
              {scope === 'one' && 'Reschedules or edits only this occurrence — the series is untouched.'}
              {scope === 'forward' && 'Edits this and every future occurrence in place. The series is not split.'}
              {scope === 'all' && 'Edits the whole series, past and future occurrences alike.'}
            </div>
          </div>
        )}

        {/* title */}
        <SectionLabel>Title</SectionLabel>
        <input value={d.title} onChange={e => up({ title: e.target.value })} placeholder="What needs doing?" autoFocus={isNew} style={{
          width: '100%', boxSizing: 'border-box', border: '1px solid ' + T.line, background: T.card, borderRadius: 13,
          padding: '14px 14px', fontSize: 18, fontFamily: T.serif, fontWeight: 600, color: T.ink, outline: 'none',
        }} />

        {/* description */}
        <SectionLabel>Description <span style={{ textTransform: 'none', letterSpacing: 0, color: T.faint }}>· optional</span></SectionLabel>
        <textarea value={d.desc} onChange={e => up({ desc: e.target.value })} placeholder="Add detail, links, context…" rows={3} style={{
          width: '100%', boxSizing: 'border-box', border: '1px solid ' + T.line, background: T.card, borderRadius: 13,
          padding: '13px 14px', fontSize: 15.5, lineHeight: 1.5, fontFamily: T.body, color: T.ink, outline: 'none', resize: 'none',
        }} />

        {/* dates */}
        <SectionLabel>Dates</SectionLabel>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 11 }}>
          <DateRow icon="cal" label="To Do Date" value={d.toDo} placeholder="None — undated" onClick={() => setSheet('todo')} onClear={() => up({ toDo: null })} />
          <DateRow icon="cal" label={singular ? 'Due Date · singular' : 'Due Date'} value={d.due} placeholder="None" onClick={() => setSheet('due')}
            onClear={() => up({ due: null, recurrence: repeat ? { ...r, dueMode: 'none' } : null })} note={note} noteColor={noteColor} />
        </div>

        {/* repeat */}
        <SectionLabel>Recurrence</SectionLabel>
        <div className="ft-press" onClick={toggleRepeat} style={{
          display: 'flex', alignItems: 'center', gap: 12, background: T.card, border: '1px solid ' + T.line,
          borderRadius: 13, padding: '14px 14px', cursor: 'pointer',
        }}>
          <I.Repeat s={18} c={repeat ? T.rust : T.muted} />
          <div style={{ flex: 1, fontSize: 16, color: T.ink }}>Repeat this task</div>
          <div style={{ width: 48, height: 28, borderRadius: 999, background: repeat ? T.rust : hexA(T.ink, 0.18), position: 'relative', transition: 'background .2s' }}>
            <div style={{ position: 'absolute', top: 3, left: repeat ? 23 : 3, width: 22, height: 22, borderRadius: '50%', background: '#fff', transition: 'left .2s', boxShadow: '0 1px 3px rgba(0,0,0,0.25)' }} />
          </div>
        </div>

        {repeat && (
          <div style={{ background: T.surface, border: '1px solid ' + T.line, borderRadius: 16, padding: '16px 15px', marginTop: 11 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 7, fontFamily: T.serif, fontSize: 17, fontWeight: 600, marginBottom: 14 }}>
              <I.Calendar s={16} c={T.rust} /> To Do repeats…
            </div>
            <RecurrenceFields rule={r} onChange={(nr) => up({ recurrence: { ...r, ...nr } })} />

            <div style={{ fontFamily: T.mono, fontSize: 12.5, color: T.rust, background: hexA(T.rust, 0.08), borderRadius: 9, padding: '9px 12px', marginTop: 4 }}>
              ↻ {recurSummary(r)}
            </div>

            {/* due-date mode (only when a due date is set) */}
            {d.due && (
              <React.Fragment>
                <div style={{ height: 1, background: T.line, margin: '18px 0' }} />
                <Field label="Due date on this series">
                  <Segmented small value={r.dueMode === 'none' ? 'singular' : r.dueMode} set={(v) => upRule({ dueMode: v, dueRule: v === 'recurring' ? (r.dueRule || defaultRecurrence()) : r.dueRule })} options={[
                    { v: 'singular', label: 'Single deadline' }, { v: 'recurring', label: 'Recurring deadline' },
                  ]} />
                  <div style={{ fontSize: 12.5, color: T.muted, marginTop: 9, lineHeight: 1.45 }}>
                    {r.dueMode === 'recurring'
                      ? 'Each occurrence gets its own deadline from a separate rule. The two need not align.'
                      : 'One fixed deadline for the whole series. It also ends the recurrence — no occurrences are generated after it.'}
                  </div>
                </Field>
                {r.dueMode === 'recurring' && (
                  <div style={{ background: T.card, border: '1px solid ' + T.line, borderRadius: 13, padding: '14px 13px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 7, fontFamily: T.serif, fontSize: 16, fontWeight: 600, marginBottom: 12 }}>
                      <I.Flag s={14} c={T.due} /> Deadline repeats…
                    </div>
                    <RecurrenceFields rule={r.dueRule || defaultRecurrence()} onChange={(nr) => upRule({ dueRule: { ...(r.dueRule || defaultRecurrence()), ...nr } })} />
                    <div style={{ fontFamily: T.mono, fontSize: 12.5, color: T.due, background: hexA(T.due, 0.08), borderRadius: 9, padding: '9px 12px' }}>
                      ⚑ {recurSummary(r.dueRule || defaultRecurrence())}
                    </div>
                  </div>
                )}
              </React.Fragment>
            )}

            {/* end date — unavailable in singular mode */}
            <div style={{ height: 1, background: T.line, margin: '18px 0' }} />
            {singular ? (
              <div style={{ display: 'flex', alignItems: 'center', gap: 9, fontSize: 13, color: T.muted, lineHeight: 1.4 }}>
                <I.Flag s={15} c={T.faint} />
                <span>End date is set by the single deadline — <b style={{ color: T.ink }}>{fmtLong(d.due)}</b>.</span>
              </div>
            ) : (
              <div className="ft-press" onClick={() => setSheet('end')} style={{ display: 'flex', alignItems: 'center', gap: 12, cursor: 'pointer' }}>
                <I.Calendar s={18} c={T.muted} />
                <div style={{ flex: 1 }}>
                  <div style={{ fontSize: 11, fontFamily: T.mono, letterSpacing: 1, textTransform: 'uppercase', color: T.faint }}>Ends</div>
                  <div style={{ fontSize: 15.5, color: r.endDate ? T.ink : T.faint, marginTop: 2 }}>{r.endDate ? fmtLong(r.endDate) : 'Never'}</div>
                </div>
                {r.endDate
                  ? <button onClick={(e) => { e.stopPropagation(); upRule({ endDate: null }); }} className="ft-press" style={{ width: 30, height: 30, borderRadius: 8, border: 'none', background: T.surfaceAlt, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}><I.X s={14} c={T.muted} /></button>
                  : <I.Chevron s={18} c={hexA(T.ink, 0.3)} />}
              </div>
            )}
          </div>
        )}

        {/* delete */}
        {!isNew && (
          <button onClick={() => repeat ? setDelSheet(true) : onDelete(d.id, 'one')} className="ft-press" style={{
            marginTop: 26, width: '100%', height: 50, borderRadius: 13, border: '1px solid ' + hexA(T.due, 0.45),
            background: 'transparent', color: T.due, fontSize: 15.5, fontWeight: 600, fontFamily: T.body, cursor: 'pointer',
            display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
          }}><I.Trash s={18} c={T.due} /> Delete task</button>
        )}
      </div>

      {/* date sheets */}
      {sheet === 'todo' && (
        <Sheet title="To Do Date" onClose={() => setSheet(null)}>
          <CalendarPicker value={d.toDo} today={today} onPick={(v) => { up({ toDo: v }); setSheet(null); }} onClear={() => { up({ toDo: null }); setSheet(null); }} />
        </Sheet>
      )}
      {sheet === 'due' && (
        <Sheet title="Due Date" onClose={() => setSheet(null)}>
          <CalendarPicker value={d.due} today={today} onPick={(v) => { up({ due: v, recurrence: repeat ? { ...r, dueMode: r.dueMode === 'none' ? 'singular' : r.dueMode } : null }); setSheet(null); }} onClear={() => { up({ due: null }); setSheet(null); }} />
        </Sheet>
      )}
      {sheet === 'end' && (
        <Sheet title="Recurrence ends" onClose={() => setSheet(null)}>
          <CalendarPicker value={r.endDate} today={today} onPick={(v) => { upRule({ endDate: v }); setSheet(null); }} onClear={() => { upRule({ endDate: null }); setSheet(null); }} />
        </Sheet>
      )}

      {/* delete scope sheet for recurring */}
      {delSheet && (
        <Sheet title="Delete repeating task" onClose={() => setDelSheet(false)} maxH="auto">
          <div style={{ fontSize: 14.5, color: T.muted, lineHeight: 1.5, marginBottom: 16 }}>This task repeats — choose what to remove.</div>
          <button onClick={() => { setDelSheet(false); onDelete(d.id, 'one'); }} className="ft-press" style={delBtn(false)}>Delete this occurrence</button>
          <button onClick={() => { setDelSheet(false); onDelete(d.id, 'all'); }} className="ft-press" style={delBtn(true)}>Delete all occurrences</button>
        </Sheet>
      )}
    </div>
  );
}

function delBtn(strong) {
  return { width: '100%', height: 52, borderRadius: 13, marginBottom: 10, cursor: 'pointer', fontFamily: T.body, fontSize: 15.5, fontWeight: 600,
    border: strong ? 'none' : '1px solid ' + hexA(T.due, 0.45), background: strong ? T.due : 'transparent', color: strong ? '#fff' : T.due };
}

window.TaskEditor = TaskEditor;
