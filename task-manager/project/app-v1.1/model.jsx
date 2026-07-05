// model.jsx — theme tokens, date utils, recurrence helpers, seed data

const T = {
  bg: '#F1E5CF', surface: '#FBF4E6', surfaceAlt: '#F1E5CF', card: '#FFFDF7',
  ink: '#241A12', muted: '#7C6A52', faint: '#A8967C',
  rust: '#A6421E', amber: '#D9913A', due: '#B23A2E', green: '#5E7A4B',
  line: 'rgba(36,26,18,0.14)', line2: 'rgba(36,26,18,0.08)',
  serif: '"Newsreader", Georgia, serif',
  body: '"Hanken Grotesk", system-ui, sans-serif',
  mono: '"Spline Sans Mono", ui-monospace, monospace',
};

function hexA(hex, a) {
  const h = hex.replace('#', '');
  return `rgba(${parseInt(h.slice(0,2),16)},${parseInt(h.slice(2,4),16)},${parseInt(h.slice(4,6),16)},${a})`;
}

// ── dates (ISO 'YYYY-MM-DD' strings) ──────────────────────────
const MS_DAY = 86400000;
const WD = ['Sun','Mon','Tue','Wed','Thu','Fri','Sat'];
const WD_FULL = ['Sunday','Monday','Tuesday','Wednesday','Thursday','Friday','Saturday'];
const MO = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
const MO_FULL = ['January','February','March','April','May','June','July','August','September','October','November','December'];

function iso(d) { return d.getFullYear() + '-' + String(d.getMonth()+1).padStart(2,'0') + '-' + String(d.getDate()).padStart(2,'0'); }
function parseISO(s) { const [y,m,dd] = s.split('-').map(Number); return new Date(y, m-1, dd); }
function todayISO() { return iso(new Date()); }
function addDays(s, n) { const d = parseISO(s); d.setDate(d.getDate()+n); return iso(d); }
function dayDiff(a, b) { return Math.round((parseISO(a) - parseISO(b)) / MS_DAY); }
function ordinal(n) { const s=['th','st','nd','rd'], v=n%100; return n + (s[(v-20)%10] || s[v] || s[0]); }

function fmtDate(s) { const d = parseISO(s); return WD[d.getDay()] + ', ' + MO[d.getMonth()] + ' ' + d.getDate(); }
function fmtLong(s) { const d = parseISO(s); return WD_FULL[d.getDay()] + ', ' + MO_FULL[d.getMonth()] + ' ' + d.getDate(); }

// relative group label for a To Do date
function groupFor(s, today) {
  const diff = dayDiff(s, today);
  if (diff < 0) return { key: 'overdue', label: 'Overdue', order: 0 };
  if (diff === 0) return { key: 'today', label: 'Today', order: 1 };
  if (diff === 1) return { key: 'tomorrow', label: 'Tomorrow', order: 2 };
  return { key: s, label: fmtDate(s), order: 3 + diff };
}

// human-readable recurrence summary
function recurSummary(r) {
  if (!r) return '';
  if (r.pattern === 'interval') {
    return r.interval === 1 ? ('Every ' + r.unit) : ('Every ' + r.interval + ' ' + r.unit + 's');
  }
  if (r.pattern === 'weekday') {
    const days = r.weekdays.slice().sort((a,b)=>a-b).map(d => WD_FULL[d]);
    let s = days.length === 7 ? 'Every day'
      : days.length === 0 ? 'Weekly'
      : 'Every ' + (days.length > 2 ? days.map(d=>d.slice(0,3)).join(', ') : days.join(' & '));
    if (r.weekInterval > 1) s += ', every ' + r.weekInterval + ' weeks';
    return s;
  }
  if (r.pattern === 'monthday') {
    const ord = ['', 'first', 'second', 'third', 'fourth', 'fifth'];
    let base = r.monthMode === 'date'
      ? 'The ' + ordinal(r.monthDate)
      : 'The ' + (r.monthWeekPos === -1 ? 'last' : ord[r.monthWeekPos]) + ' ' + WD_FULL[r.monthWeekday];
    base += r.monthInterval === 1 ? ' of every month' : ' of every ' + r.monthInterval + ' months';
    return base;
  }
  return '';
}

function defaultRecurrence() {
  return { pattern: 'weekday', interval: 1, unit: 'week', weekdays: [new Date().getDay()],
    weekInterval: 1, monthMode: 'date', monthDate: new Date().getDate(), monthWeekPos: 1,
    monthWeekday: new Date().getDay(), monthInterval: 1, endDate: null,
    dueMode: 'none', dueRule: null };
}

// ── seed (relative to today so groups always populate) ────────
function makeSeed() {
  const t = todayISO();
  return [
    { id: 'a1', title: 'Renew the studio domain name', desc: 'thefoxworks.net — auto-renew failed last cycle, do it manually this time.',
      toDo: addDays(t, -3), due: addDays(t, -3), done: false, recurrence: null },
    { id: 'a2', title: "Edit the SciFi short — “The Long Burrow”", desc: 'Second pass on the middle act; tighten the cave sequence.',
      toDo: addDays(t, -1), due: null, done: false, recurrence: null },
    { id: 'a3', title: 'Submit the conference talk proposal', desc: '',
      toDo: addDays(t, -1), due: t, done: false, recurrence: null },

    { id: 'b1', title: 'Water the office plant 🪴', desc: '',
      toDo: t, due: null, done: false,
      recurrence: { ...defaultRecurrence(), pattern: 'interval', interval: 3, unit: 'day' } },
    { id: 'b2', title: 'Make the weekly studio post', desc: 'Write Monday, publish by Friday.',
      toDo: t, due: addDays(t, 4), done: false,
      recurrence: { ...defaultRecurrence(), pattern: 'weekday', weekdays: [1], weekInterval: 1,
        dueMode: 'recurring', dueRule: { pattern: 'weekday', weekdays: [5], weekInterval: 1 } } },
    { id: 'b3', title: 'Reply to Roey about the podcast', desc: 'Confirm the recording date and the topic list.',
      toDo: t, due: t, done: false, recurrence: null },

    { id: 'c1', title: 'Back up the manuscript drafts', desc: '',
      toDo: addDays(t, 1), due: null, done: false, recurrence: null },
    { id: 'd1', title: 'Plan the Japan trip itinerary', desc: 'Rough day-by-day for the Kyoto + Tokyo legs.',
      toDo: addDays(t, 3), due: addDays(t, 10), done: false, recurrence: null },

    { id: 'u1', title: 'Call the accountant', desc: 'Quarterly numbers question — no rush.',
      toDo: null, due: null, done: false, recurrence: null },
    { id: 'u2', title: "Read “Project Hail Mary”", desc: '',
      toDo: null, due: null, done: false, recurrence: null },

    { id: 'x1', title: 'Send the newsletter draft', desc: '',
      toDo: addDays(t, -1), due: null, done: true, recurrence: null },
    { id: 'x2', title: 'Pay the studio rent', desc: '',
      toDo: addDays(t, -2), due: addDays(t, -2), done: true, recurrence: null },
  ];
}

Object.assign(window, {
  T, hexA, iso, parseISO, todayISO, addDays, dayDiff, ordinal,
  fmtDate, fmtLong, groupFor, recurSummary, defaultRecurrence, makeSeed,
  WD, WD_FULL, MO, MO_FULL,
});
