// calendar.jsx — bottom-sheet shell + month calendar date picker

function Sheet({ title, onClose, children, maxH }) {
  return (
    <div style={{ position: 'absolute', inset: 0, zIndex: 60 }}>
      <div onClick={onClose} className="ft-fade" style={{ position: 'absolute', inset: 0, background: 'rgba(30,18,8,0.4)' }} />
      <div className="ft-sheet" style={{
        position: 'absolute', left: 0, right: 0, bottom: 0, maxHeight: maxH || '86%',
        display: 'flex', flexDirection: 'column', background: T.surface,
        borderTopLeftRadius: 26, borderTopRightRadius: 26, boxShadow: '0 -10px 40px rgba(0,0,0,0.2)',
      }}>
        <div style={{ flexShrink: 0, padding: '14px 18px 6px' }}>
          <div style={{ width: 40, height: 5, borderRadius: 3, background: T.line, margin: '0 auto 12px' }} />
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div style={{ fontFamily: T.serif, fontSize: 23, fontWeight: 600 }}>{title}</div>
            <button onClick={onClose} className="ft-press" style={{ width: 34, height: 34, borderRadius: 9, border: 'none', background: T.surfaceAlt, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}><I.X s={16} c={T.muted} /></button>
          </div>
        </div>
        <div className="ft-scroll" style={{ overflowY: 'auto', padding: '8px 18px 26px' }}>{children}</div>
      </div>
    </div>
  );
}

function CalendarPicker({ value, today, onPick, onClear, allowClear = true }) {
  const init = value ? parseISO(value) : parseISO(today);
  const [view, setView] = React.useState({ y: init.getFullYear(), m: init.getMonth() });
  const first = new Date(view.y, view.m, 1);
  const startPad = first.getDay();
  const days = new Date(view.y, view.m + 1, 0).getDate();
  const cells = [];
  for (let i = 0; i < startPad; i++) cells.push(null);
  for (let d = 1; d <= days; d++) cells.push(iso(new Date(view.y, view.m, d)));

  const shift = (n) => { const d = new Date(view.y, view.m + n, 1); setView({ y: d.getFullYear(), m: d.getMonth() }); };

  const quick = [
    ['Today', today], ['Tomorrow', addDays(today, 1)],
    ['In a week', addDays(today, 7)], ['Next month', addDays(today, 30)],
  ];

  return (
    <div>
      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 14 }}>
        {quick.map(([lbl, d]) => (
          <button key={lbl} onClick={() => onPick(d)} className="ft-press" style={{
            border: '1px solid ' + T.line, background: T.card, color: T.ink, borderRadius: 999,
            padding: '8px 13px', fontSize: 13.5, cursor: 'pointer', fontFamily: T.body,
          }}>{lbl}</button>
        ))}
      </div>

      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 8 }}>
        <button onClick={() => shift(-1)} className="ft-press" style={navBtn()}><I.Chevron s={18} c={T.ink} dir="left" /></button>
        <div style={{ fontFamily: T.serif, fontSize: 19, fontWeight: 600 }}>{MO_FULL[view.m]} {view.y}</div>
        <button onClick={() => shift(1)} className="ft-press" style={navBtn()}><I.Chevron s={18} c={T.ink} /></button>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7,1fr)', gap: 3, textAlign: 'center' }}>
        {WD.map(d => <div key={d} style={{ fontFamily: T.mono, fontSize: 10.5, color: T.faint, padding: '4px 0', letterSpacing: 0.5 }}>{d[0]}</div>)}
        {cells.map((c, i) => {
          if (!c) return <div key={i} />;
          const isToday = c === today, isSel = c === value, dd = parseISO(c).getDate();
          return (
            <button key={i} onClick={() => onPick(c)} className="ft-press" style={{
              aspectRatio: '1', border: 'none', cursor: 'pointer', borderRadius: 10, fontFamily: T.body, fontSize: 14.5,
              background: isSel ? T.rust : 'transparent', color: isSel ? '#fff' : (isToday ? T.rust : T.ink),
              fontWeight: isToday || isSel ? 700 : 400,
              boxShadow: isToday && !isSel ? 'inset 0 0 0 1.5px ' + hexA(T.rust, 0.4) : 'none',
            }}>{dd}</button>
          );
        })}
      </div>

      {allowClear && (
        <button onClick={onClear} className="ft-press" style={{
          marginTop: 16, width: '100%', height: 46, borderRadius: 12, border: '1px solid ' + T.line,
          background: 'transparent', color: T.muted, fontSize: 15, fontWeight: 600, cursor: 'pointer', fontFamily: T.body,
        }}>Clear date</button>
      )}
    </div>
  );
}

function navBtn() {
  return { width: 36, height: 36, borderRadius: 10, border: '1px solid ' + T.line, background: T.card,
    cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' };
}

Object.assign(window, { Sheet, CalendarPicker });
