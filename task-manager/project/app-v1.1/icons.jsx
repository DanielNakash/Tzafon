// icons.jsx — line icons for Task Manager v1.1.0
const I = {};

I.Check = ({ s = 16, c = '#fff', w = 3 }) => (
  <svg width={s} height={s} viewBox="0 0 24 24" fill="none"><path d="M5 12.5l4.5 4.5L19 7" stroke={c} strokeWidth={w} strokeLinecap="round" strokeLinejoin="round"/></svg>
);
I.Plus = ({ s = 22, c = '#fff', w = 2.6 }) => (
  <svg width={s} height={s} viewBox="0 0 24 24" fill="none"><path d="M12 5v14M5 12h14" stroke={c} strokeWidth={w} strokeLinecap="round"/></svg>
);
I.X = ({ s = 16, c, w = 2.4 }) => (
  <svg width={s} height={s} viewBox="0 0 24 24" fill="none"><path d="M6 6l12 12M18 6L6 18" stroke={c} strokeWidth={w} strokeLinecap="round"/></svg>
);
I.Trash = ({ s = 18, c, w = 1.7 }) => (
  <svg width={s} height={s} viewBox="0 0 24 24" fill="none"><path d="M4 7h16M9 7V5a1 1 0 011-1h4a1 1 0 011 1v2m-9 0l1 13a1 1 0 001 1h6a1 1 0 001-1l1-13" stroke={c} strokeWidth={w} strokeLinecap="round" strokeLinejoin="round"/></svg>
);
I.Back = ({ s = 24, c, w = 2 }) => (
  <svg width={s} height={s} viewBox="0 0 24 24" fill="none"><path d="M15 5l-7 7 7 7" stroke={c} strokeWidth={w} strokeLinecap="round" strokeLinejoin="round"/></svg>
);
I.Chevron = ({ s = 20, c, w = 2, dir = 'right' }) => (
  <svg width={s} height={s} viewBox="0 0 24 24" fill="none" style={{ transform: dir === 'left' ? 'rotate(180deg)' : (dir === 'down' ? 'rotate(90deg)' : 'none') }}><path d="M9 5l7 7-7 7" stroke={c} strokeWidth={w} strokeLinecap="round" strokeLinejoin="round"/></svg>
);
I.Calendar = ({ s = 19, c, w = 1.7 }) => (
  <svg width={s} height={s} viewBox="0 0 24 24" fill="none"><rect x="3.5" y="5" width="17" height="15.5" rx="2.5" stroke={c} strokeWidth={w}/><path d="M3.5 9.5h17M8 3v4M16 3v4" stroke={c} strokeWidth={w} strokeLinecap="round"/></svg>
);
I.Repeat = ({ s = 16, c, w = 1.9 }) => (
  <svg width={s} height={s} viewBox="0 0 24 24" fill="none"><path d="M4 9a5 5 0 015-5h7l-2.2-2.2M20 15a5 5 0 01-5 5H8l2.2 2.2" stroke={c} strokeWidth={w} strokeLinecap="round" strokeLinejoin="round"/><path d="M16 1.8L18.4 4 16 6.2M8 22.2L5.6 20 8 17.8" stroke={c} strokeWidth={w} strokeLinecap="round" strokeLinejoin="round"/></svg>
);
I.Flag = ({ s = 14, c, w = 1.8 }) => (
  <svg width={s} height={s} viewBox="0 0 24 24" fill="none"><path d="M6 21V4m0 1h11l-2.5 4L17 13H6" stroke={c} strokeWidth={w} strokeLinecap="round" strokeLinejoin="round"/></svg>
);
I.Alert = ({ s = 14, c, w = 1.9 }) => (
  <svg width={s} height={s} viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="9" stroke={c} strokeWidth={w}/><path d="M12 7.5v5M12 16h.01" stroke={c} strokeWidth={w} strokeLinecap="round"/></svg>
);
I.Desc = ({ s = 18, c, w = 1.8 }) => (
  <svg width={s} height={s} viewBox="0 0 24 24" fill="none"><path d="M4 6h16M4 11h16M4 16h10" stroke={c} strokeWidth={w} strokeLinecap="round"/></svg>
);
I.Dots = ({ s = 20, c }) => (
  <svg width={s} height={s} viewBox="0 0 24 24" fill="none"><circle cx="5" cy="12" r="1.7" fill={c}/><circle cx="12" cy="12" r="1.7" fill={c}/><circle cx="19" cy="12" r="1.7" fill={c}/></svg>
);
I.Eye = ({ s = 18, c, w = 1.8, off }) => (
  <svg width={s} height={s} viewBox="0 0 24 24" fill="none"><path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7-10-7-10-7z" stroke={c} strokeWidth={w}/><circle cx="12" cy="12" r="2.6" stroke={c} strokeWidth={w}/>{off && <path d="M3 3l18 18" stroke={c} strokeWidth={w} strokeLinecap="round"/>}</svg>
);
I.Google = ({ s = 20 }) => (
  <svg width={s} height={s} viewBox="0 0 48 48" aria-hidden="true"><path fill="#EA4335" d="M24 9.5c3.5 0 6.6 1.2 9 3.6l6.7-6.7C35.6 2.4 30.2 0 24 0 14.6 0 6.4 5.4 2.5 13.3l7.8 6.1C12.2 13.2 17.6 9.5 24 9.5z"/><path fill="#4285F4" d="M46.5 24.5c0-1.6-.1-3.1-.4-4.5H24v9h12.7c-.5 3-2.2 5.5-4.7 7.2l7.3 5.7c4.3-3.9 6.8-9.8 6.8-17.4z"/><path fill="#FBBC05" d="M10.3 28.6c-.5-1.5-.8-3-.8-4.6s.3-3.1.8-4.6l-7.8-6.1C.9 16.5 0 20.1 0 24s.9 7.5 2.5 10.7l7.8-6.1z"/><path fill="#34A853" d="M24 48c6.2 0 11.4-2 15.2-5.5l-7.3-5.7c-2 1.4-4.7 2.3-7.9 2.3-6.4 0-11.8-3.7-13.7-9.1l-7.8 6.1C6.4 42.6 14.6 48 24 48z"/></svg>
);

window.I = I;
