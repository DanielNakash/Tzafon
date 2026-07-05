// tz-core.jsx — Tzafon design tokens, compass motif, icon set, persona seed.
// The Den palette (carried from TaskManager v1.1) + v2 semantic tokens.

const TZ = {
  // ── Den base (warm kraft) ─────────────────────────────
  bg: '#F1E5CF', surface: '#FBF4E6', surfaceAlt: '#EFE3CB', card: '#FFFDF7',
  ink: '#241A12', muted: '#7C6A52', faint: '#A8967C',
  rust: '#A6421E', rustDeep: '#8A3416', amber: '#D9913A',
  due: '#B23A2E', green: '#5E7A4B',
  line: 'rgba(36,26,18,0.14)', line2: 'rgba(36,26,18,0.08)',
  cream: '#FBF4E6',

  // ── v2 state tokens (task state machine) ──────────────
  frozen: '#5E7488',   // cool slate — suspended
  backlog: '#7E7086',  // quiet mauve — parked / someday
  closed: '#9C8C74',   // faded taupe — skipped, no longer relevant

  // ── theme accents (3 active themes, harmonized) ───────
  tHealth: '#5E7A4B',  // green   — move & feel strong
  tWrite: '#8C4A63',   // mulberry— finish the novel
  tLearn: '#3F6E7D',   // teal    — speak Japanese
  tHealthWash: 'rgba(94,122,75,0.12)',
  tWriteWash: 'rgba(140,74,99,0.12)',
  tLearnWash: 'rgba(63,110,125,0.12)',

  serif: '"Newsreader", Georgia, serif',
  body: '"Hanken Grotesk", system-ui, sans-serif',
  mono: '"Spline Sans Mono", ui-monospace, monospace',
};

function tzA(hex, a) {
  const h = hex.replace('#', '');
  return `rgba(${parseInt(h.slice(0,2),16)},${parseInt(h.slice(2,4),16)},${parseInt(h.slice(4,6),16)},${a})`;
}

// ── Compass rose — the "north" motif (8-point, simple geometry) ──
function Compass({ s = 44, ring = TZ.rust, needleN = TZ.rust, needleS = TZ.faint, bg = 'transparent', stroke = 1.6, ticks = true }) {
  const star = 'M24 3.5 L26.8 21.2 L44.5 24 L26.8 26.8 L24 44.5 L21.2 26.8 L3.5 24 L21.2 21.2 Z';
  const diag = 'M24 11 L25.7 22.3 L37 24 L25.7 25.7 L24 37 L22.3 25.7 L11 24 L22.3 22.3 Z';
  return (
    <svg width={s} height={s} viewBox="0 0 48 48" fill="none">
      <circle cx="24" cy="24" r="22" fill={bg} stroke={ring} strokeWidth={stroke} />
      <circle cx="24" cy="24" r="17.5" fill="none" stroke={ring} strokeWidth={stroke * 0.6} opacity="0.4" />
      {/* diagonal 4-point star (behind) */}
      <path d={diag} transform="rotate(45 24 24)" fill={needleS} opacity="0.3" />
      {/* main 4-point rose */}
      <path d={star} fill={needleS} opacity="0.5" />
      {/* north point, accented */}
      <path d="M24 3.5 L26.8 21.2 L24 24 Z" fill={needleN} />
      <path d="M24 3.5 L21.2 21.2 L24 24 Z" fill={needleN} opacity="0.72" />
      {ticks && [45, 135, 225, 315].map(a => (
        <circle key={a} cx="24" cy="8.5" r={stroke * 0.9} fill={ring} opacity="0.5" transform={`rotate(${a} 24 24)`} />
      ))}
      <circle cx="24" cy="24" r="2.3" fill={ring} />
    </svg>
  );
}

// ── Icon set (line, 24px grid) ──────────────────────────────
const TI = {};
TI.Check = ({ s = 16, c = '#fff', w = 3 }) => (<svg width={s} height={s} viewBox="0 0 24 24" fill="none"><path d="M5 12.5l4.5 4.5L19 7" stroke={c} strokeWidth={w} strokeLinecap="round" strokeLinejoin="round"/></svg>);
TI.Plus = ({ s = 22, c = '#fff', w = 2.6 }) => (<svg width={s} height={s} viewBox="0 0 24 24" fill="none"><path d="M12 5v14M5 12h14" stroke={c} strokeWidth={w} strokeLinecap="round"/></svg>);
TI.X = ({ s = 16, c, w = 2.4 }) => (<svg width={s} height={s} viewBox="0 0 24 24" fill="none"><path d="M6 6l12 12M18 6L6 18" stroke={c} strokeWidth={w} strokeLinecap="round"/></svg>);
TI.Trash = ({ s = 18, c, w = 1.7 }) => (<svg width={s} height={s} viewBox="0 0 24 24" fill="none"><path d="M4 7h16M9 7V5a1 1 0 011-1h4a1 1 0 011 1v2m-9 0l1 13a1 1 0 001 1h6a1 1 0 001-1l1-13" stroke={c} strokeWidth={w} strokeLinecap="round" strokeLinejoin="round"/></svg>);
TI.Back = ({ s = 24, c, w = 2 }) => (<svg width={s} height={s} viewBox="0 0 24 24" fill="none"><path d="M15 5l-7 7 7 7" stroke={c} strokeWidth={w} strokeLinecap="round" strokeLinejoin="round"/></svg>);
TI.Chevron = ({ s = 20, c, w = 2, dir = 'right' }) => (<svg width={s} height={s} viewBox="0 0 24 24" fill="none" style={{ transform: dir === 'left' ? 'rotate(180deg)' : (dir === 'down' ? 'rotate(90deg)' : (dir === 'up' ? 'rotate(-90deg)' : 'none')) }}><path d="M9 5l7 7-7 7" stroke={c} strokeWidth={w} strokeLinecap="round" strokeLinejoin="round"/></svg>);
TI.Calendar = ({ s = 19, c, w = 1.7 }) => (<svg width={s} height={s} viewBox="0 0 24 24" fill="none"><rect x="3.5" y="5" width="17" height="15.5" rx="2.5" stroke={c} strokeWidth={w}/><path d="M3.5 9.5h17M8 3v4M16 3v4" stroke={c} strokeWidth={w} strokeLinecap="round"/></svg>);
TI.Repeat = ({ s = 16, c, w = 1.9 }) => (<svg width={s} height={s} viewBox="0 0 24 24" fill="none"><path d="M4 9a5 5 0 015-5h7l-2.2-2.2M20 15a5 5 0 01-5 5H8l2.2 2.2" stroke={c} strokeWidth={w} strokeLinecap="round" strokeLinejoin="round"/><path d="M16 1.8L18.4 4 16 6.2M8 22.2L5.6 20 8 17.8" stroke={c} strokeWidth={w} strokeLinecap="round" strokeLinejoin="round"/></svg>);
TI.Flag = ({ s = 14, c, w = 1.8 }) => (<svg width={s} height={s} viewBox="0 0 24 24" fill="none"><path d="M6 21V4m0 1h11l-2.5 4L17 13H6" stroke={c} strokeWidth={w} strokeLinecap="round" strokeLinejoin="round"/></svg>);
TI.Alert = ({ s = 14, c, w = 1.9 }) => (<svg width={s} height={s} viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="9" stroke={c} strokeWidth={w}/><path d="M12 7.5v5M12 16h.01" stroke={c} strokeWidth={w} strokeLinecap="round"/></svg>);
TI.Search = ({ s = 18, c, w = 1.9 }) => (<svg width={s} height={s} viewBox="0 0 24 24" fill="none"><circle cx="11" cy="11" r="7" stroke={c} strokeWidth={w}/><path d="M20 20l-4-4" stroke={c} strokeWidth={w} strokeLinecap="round"/></svg>);
TI.Eye = ({ s = 18, c, w = 1.8, off }) => (<svg width={s} height={s} viewBox="0 0 24 24" fill="none"><path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7-10-7-10-7z" stroke={c} strokeWidth={w}/><circle cx="12" cy="12" r="2.6" stroke={c} strokeWidth={w}/>{off && <path d="M3 3l18 18" stroke={c} strokeWidth={w} strokeLinecap="round"/>}</svg>);

// nav + object icons
TI.Today = ({ s = 24, c, w = 1.9 }) => (<svg width={s} height={s} viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="4.7" stroke={c} strokeWidth={w}/><path d="M12 2.2v2.7M12 19.1v2.7M2.2 12h2.7M19.1 12h2.7M5.05 5.05l1.9 1.9M17.05 17.05l1.9 1.9M18.95 5.05l-1.9 1.9M6.95 17.05l-1.9 1.9" stroke={c} strokeWidth={w} strokeLinecap="round"/></svg>);
TI.Plan = ({ s = 24, c, w = 1.8 }) => (<svg width={s} height={s} viewBox="0 0 24 24" fill="none"><rect x="3.5" y="5" width="17" height="15.5" rx="2.5" stroke={c} strokeWidth={w}/><path d="M3.5 9.5h17M8 3v4M16 3v4" stroke={c} strokeWidth={w} strokeLinecap="round"/><path d="M7.5 13.5h5M7.5 16.5h8" stroke={c} strokeWidth={w} strokeLinecap="round"/></svg>);
TI.Habit = ({ s = 24, c, w = 1.9 }) => (<svg width={s} height={s} viewBox="0 0 24 24" fill="none"><path d="M4 12a8 8 0 018-8c3.2 0 5.9 1.9 7.2 4.6" stroke={c} strokeWidth={w} strokeLinecap="round"/><path d="M20 12a8 8 0 01-8 8c-3.2 0-5.9-1.9-7.2-4.6" stroke={c} strokeWidth={w} strokeLinecap="round"/><path d="M18.2 3.5l1.3 5-5-1M5.8 20.5l-1.3-5 5 1" stroke={c} strokeWidth={w} strokeLinecap="round" strokeLinejoin="round"/></svg>);
TI.Compass = ({ s = 24, c, w = 1.9 }) => (<svg width={s} height={s} viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="8.5" stroke={c} strokeWidth={w}/><path d="M15.5 8.5L13 13l-4.5 2.5L11 11z" fill={c} stroke={c} strokeWidth="0.5" strokeLinejoin="round"/></svg>);
TI.Journey = ({ s = 24, c, w = 1.9 }) => (<svg width={s} height={s} viewBox="0 0 24 24" fill="none"><path d="M2.5 20L8.5 8l3.5 5.5L16 5l5.5 15z" stroke={c} strokeWidth={w} strokeLinejoin="round" strokeLinecap="round"/><path d="M6.4 15l2.1-1.5 1.7 1.2" stroke={c} strokeWidth={w} strokeLinecap="round" strokeLinejoin="round" opacity="0.55"/></svg>);
TI.Target = ({ s = 18, c, w = 1.8 }) => (<svg width={s} height={s} viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="8.5" stroke={c} strokeWidth={w}/><circle cx="12" cy="12" r="4.5" stroke={c} strokeWidth={w}/><circle cx="12" cy="12" r="1" fill={c}/></svg>);
TI.Cue = ({ s = 16, c, w = 1.8 }) => (<svg width={s} height={s} viewBox="0 0 24 24" fill="none"><path d="M13 2L4.5 13H11l-1 9 8.5-11H12l1-9z" stroke={c} strokeWidth={w} strokeLinejoin="round" strokeLinecap="round"/></svg>);
TI.Frozen = ({ s = 16, c, w = 1.7 }) => (<svg width={s} height={s} viewBox="0 0 24 24" fill="none"><path d="M12 2v20M4 7l16 10M20 7L4 17" stroke={c} strokeWidth={w} strokeLinecap="round"/><path d="M12 2l-2.2 2.2M12 2l2.2 2.2M12 22l-2.2-2.2M12 22l2.2-2.2" stroke={c} strokeWidth={w} strokeLinecap="round"/></svg>);
TI.Moon = ({ s = 16, c, w = 1.7 }) => (<svg width={s} height={s} viewBox="0 0 24 24" fill="none"><path d="M20 14.5A8.5 8.5 0 019.5 4a8.5 8.5 0 1010.5 10.5z" stroke={c} strokeWidth={w} strokeLinejoin="round"/></svg>);
TI.Skip = ({ s = 16, c, w = 1.8 }) => (<svg width={s} height={s} viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="9" stroke={c} strokeWidth={w}/><path d="M8.5 8.5l7 7" stroke={c} strokeWidth={w} strokeLinecap="round"/></svg>);
TI.Sprout = ({ s = 18, c, w = 1.8 }) => (<svg width={s} height={s} viewBox="0 0 24 24" fill="none"><path d="M12 21v-8" stroke={c} strokeWidth={w} strokeLinecap="round"/><path d="M12 13c0-3-2.5-5-6-5 0 3 2.5 5 6 5z" stroke={c} strokeWidth={w} strokeLinejoin="round"/><path d="M12 11c0-3 2.5-5.5 6-5.5 0 3-2.5 5.5-6 5.5z" stroke={c} strokeWidth={w} strokeLinejoin="round"/></svg>);
TI.Sparkle = ({ s = 16, c, w = 1.6 }) => (<svg width={s} height={s} viewBox="0 0 24 24" fill="none"><path d="M12 3l1.8 5.4L19 10l-5.2 1.6L12 17l-1.8-5.4L5 10l5.2-1.6z" fill={c} opacity="0.9"/><path d="M19 15l.7 2 2 .7-2 .7-.7 2-.7-2-2-.7 2-.7z" fill={c} opacity="0.7"/></svg>);
TI.Pencil = ({ s = 16, c, w = 1.8 }) => (<svg width={s} height={s} viewBox="0 0 24 24" fill="none"><path d="M4 20l4-1L19 8l-3-3L5 16l-1 4z" stroke={c} strokeWidth={w} strokeLinejoin="round"/><path d="M14.5 6.5l3 3" stroke={c} strokeWidth={w}/></svg>);
TI.Bell = ({ s = 18, c, w = 1.8 }) => (<svg width={s} height={s} viewBox="0 0 24 24" fill="none"><path d="M6 9a6 6 0 1112 0c0 5 2 6 2 6H4s2-1 2-6z" stroke={c} strokeWidth={w} strokeLinejoin="round"/><path d="M10 19a2 2 0 004 0" stroke={c} strokeWidth={w} strokeLinecap="round"/></svg>);
TI.Menu = ({ s = 20, c, w = 2 }) => (<svg width={s} height={s} viewBox="0 0 24 24" fill="none"><path d="M4 7h16M4 12h16M4 17h16" stroke={c} strokeWidth={w} strokeLinecap="round"/></svg>);
TI.Link = ({ s = 15, c, w = 1.8 }) => (<svg width={s} height={s} viewBox="0 0 24 24" fill="none"><path d="M10 14a4 4 0 005.6 0l3-3a4 4 0 00-5.6-5.6L11.5 7" stroke={c} strokeWidth={w} strokeLinecap="round" strokeLinejoin="round"/><path d="M14 10a4 4 0 00-5.6 0l-3 3a4 4 0 105.6 5.6L12.5 17" stroke={c} strokeWidth={w} strokeLinecap="round" strokeLinejoin="round"/></svg>);
TI.Clock = ({ s = 15, c, w = 1.8 }) => (<svg width={s} height={s} viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="8.5" stroke={c} strokeWidth={w}/><path d="M12 7.5V12l3 2" stroke={c} strokeWidth={w} strokeLinecap="round" strokeLinejoin="round"/></svg>);
TI.Coffee = ({ s = 15, c, w = 1.7 }) => (<svg width={s} height={s} viewBox="0 0 24 24" fill="none"><path d="M4 8h13v5a5 5 0 01-5 5H9a5 5 0 01-5-5V8z" stroke={c} strokeWidth={w} strokeLinejoin="round"/><path d="M17 9h2.5a2.5 2.5 0 010 5H17" stroke={c} strokeWidth={w}/><path d="M8 3.5c-.5.8-.5 1.7 0 2.5M12 3.5c-.5.8-.5 1.7 0 2.5" stroke={c} strokeWidth={w} strokeLinecap="round" opacity="0.7"/></svg>);
TI.Google = ({ s = 20 }) => (<svg width={s} height={s} viewBox="0 0 48 48" aria-hidden="true"><path fill="#EA4335" d="M24 9.5c3.5 0 6.6 1.2 9 3.6l6.7-6.7C35.6 2.4 30.2 0 24 0 14.6 0 6.4 5.4 2.5 13.3l7.8 6.1C12.2 13.2 17.6 9.5 24 9.5z"/><path fill="#4285F4" d="M46.5 24.5c0-1.6-.1-3.1-.4-4.5H24v9h12.7c-.5 3-2.2 5.5-4.7 7.2l7.3 5.7c4.3-3.9 6.8-9.8 6.8-17.4z"/><path fill="#FBBC05" d="M10.3 28.6c-.5-1.5-.8-3-.8-4.6s.3-3.1.8-4.6l-7.8-6.1C.9 16.5 0 20.1 0 24s.9 7.5 2.5 10.7l7.8-6.1z"/><path fill="#34A853" d="M24 48c6.2 0 11.4-2 15.2-5.5l-7.3-5.7c-2 1.4-4.7 2.3-7.9 2.3-6.4 0-11.8-3.7-13.7-9.1l-7.8 6.1C6.4 42.6 14.6 48 24 48z"/></svg>);

// ── Persona seed: a coherent Theme → Goal | Habit → Task world ──
// Three active themes (the hard cap), each with why + window + mirror.
const THEMES = {
  health: { key: 'health', name: 'Move more, feel strong', accent: TZ.tHealth, wash: TZ.tHealthWash,
    why: 'So I can keep up with the kids on the trail.', window: 'Quarter · week 7',
    mirror: '23 aligned tasks done · 2 habits held · 1 goal advancing' },
  novel: { key: 'novel', name: 'Finish the novel', accent: TZ.tWrite, wash: TZ.tWriteWash,
    why: 'Because the draft has waited three years.', window: 'Quarter · week 7',
    mirror: '31 aligned tasks done · 1 habit held · 1 goal near done' },
  japanese: { key: 'japanese', name: 'Speak Japanese by spring', accent: TZ.tLearn, wash: TZ.tLearnWash,
    why: 'For the Kyoto trip in April.', window: 'Quarter · week 3',
    mirror: '9 aligned tasks done · 2 habits forming' },
};

const GOALS = [
  { id: 'g1', theme: 'health', title: 'Run a half-marathon in October', type: 'Accumulative',
    unit: 'km longest run', cur: 8, target: 21, serves: ['health'],
    habits: ['Run', 'Strength work'], note: 'Longest run so far: 8 km' },
  { id: 'g2', theme: 'novel', title: 'Complete the first draft', type: 'Accumulative',
    unit: 'words', cur: 62000, target: 80000, serves: ['novel'],
    habits: ['Write in the morning'], note: '18,000 words to go — you can see the end' },
  { id: 'g3', theme: 'japanese', title: 'Reach JLPT N5', type: 'Stepped',
    steps: [ ['Defined this goal', true], ['Hiragana', true], ['Katakana', true], ['N5 grammar', false], ['800 core words', false], ['Mock test', false] ],
    serves: ['japanese'], habits: ['Anki reviews', 'Tutor call'] },
  { id: 'g4', theme: null, title: 'Redo the studio website', type: 'Generic',
    serves: [], habits: [], note: 'No theme yet — lives in the orphan shelf' },
];

const HABITS = [
  { id: 'h1', name: 'Run', theme: 'health', kind: 'frequency', target: 3, period: 'week', doneThis: 2,
    cue: 'After I drop the kids at school', arc: 'Running 9 weeks', week: [1,0,1,0,0,0,0], serves: ['Move more, feel strong', 'Run a half-marathon'] },
  { id: 'h2', name: 'Strength work', theme: 'health', kind: 'frequency', target: 2, period: 'week', doneThis: 1,
    cue: 'After the evening dishes', arc: 'Holding 5 weeks', week: [0,1,0,0,0,0,0], serves: ['Move more, feel strong'] },
  { id: 'h3', name: 'Write in the morning', theme: 'novel', kind: 'quantitative', target: 500, unit: 'words', period: 'day', doneThis: 4, targetDays: 5,
    cue: 'After the first coffee', arc: 'Writing 12 weeks', week: [500,650,0,500,420,0,0], serves: ['Finish the novel'] },
  { id: 'h4', name: 'Anki reviews', theme: 'japanese', kind: 'frequency', target: 7, period: 'week', doneThis: 5,
    cue: 'After breakfast', arc: 'Forming — 3 weeks', week: [1,1,1,0,1,1,0], serves: ['Speak Japanese', 'Reach JLPT N5'] },
  { id: 'h5', name: 'Tutor call', theme: 'japanese', kind: 'frequency', target: 1, period: 'week', doneThis: 1,
    cue: 'Sunday evenings', arc: 'Forming — 3 weeks', week: [0,0,0,0,0,0,1], serves: ['Speak Japanese'] },
];

// Tasks for Today / Planning / All Tasks — carry state + alignment + cue.
const TASKS = {
  focus: [
    { id: 't1', title: 'Write 500 words — chapter 14', serves: 'novel', servesLabel: 'Finish the novel', habit: 'Write in the morning', cue: 'After the first coffee', state: 'open' },
    { id: 't2', title: 'Easy 5K along the river', serves: 'health', servesLabel: 'Move more, feel strong', habit: 'Run', cue: 'After school drop-off', state: 'open' },
    { id: 't3', title: 'Anki — 20 new cards', serves: 'japanese', servesLabel: 'Speak Japanese', habit: 'Anki reviews', cue: 'After breakfast', state: 'open' },
  ],
  today: [
    { id: 't4', title: 'Reply to the editor about the deadline', due: 'today', serves: 'novel', servesLabel: 'Finish the novel', state: 'open' },
    { id: 't5', title: 'Book the physio appointment', serves: 'health', servesLabel: 'Move more', state: 'open' },
    { id: 't6', title: 'Water the studio plants 🪴', recur: 'Every 3 days', state: 'open' },
  ],
  done: [
    { id: 't7', title: 'Morning pages', serves: 'novel', servesLabel: 'Finish the novel', state: 'done' },
    { id: 't8', title: 'Stretch + foam roll', serves: 'health', habit: 'Strength work', state: 'done' },
  ],
  overdue: [
    { id: 't9', title: 'Send the newsletter draft', slipped: 2, serves: 'novel', servesLabel: 'Finish the novel', state: 'open' },
    { id: 't10', title: 'Renew the race registration', slipped: 4, due: true, serves: 'health', state: 'open' },
  ],
  inbox: [
    { id: 't11', title: 'Call the accountant', note: 'Quarterly numbers — no rush', state: 'open' },
    { id: 't12', title: 'Read "Project Hail Mary"', state: 'open' },
    { id: 't13', title: 'Look into a standing desk', state: 'open' },
  ],
  backlog: [
    { id: 't14', title: 'Learn to make sourdough', state: 'backlog' },
    { id: 't15', title: 'Plan a long weekend in Portugal', state: 'backlog' },
    { id: 't16', title: 'Digitize the old family photos', state: 'backlog' },
  ],
};

Object.assign(window, { TZ, tzA, Compass, TI, THEMES, GOALS, HABITS, TASKS });
