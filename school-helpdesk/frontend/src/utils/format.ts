const DATE_OPTIONS: Intl.DateTimeFormatOptions = { day: '2-digit', month: '2-digit', year: 'numeric' };
const DATETIME_OPTIONS: Intl.DateTimeFormatOptions = {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
};

const LOCALE = 'ru-RU';

/** 12.11.2026 */
export function formatDate(value?: string | null): string {
  if (!value) return '—';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? '—' : date.toLocaleDateString(LOCALE, DATE_OPTIONS);
}

/** 12.11.2026, 14:35 */
export function formatDateTime(value?: string | null): string {
  if (!value) return '—';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? '—' : date.toLocaleString(LOCALE, DATETIME_OPTIONS);
}

/** Короткая дата для графиков: 12.11 */
export function formatShortDate(value: string): string {
  const date = new Date(`${value}T00:00:00`);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleDateString(LOCALE, { day: '2-digit', month: '2-digit' });
}

/** «5 минут назад», «вчера», «12.11.2026» */
export function formatRelative(value?: string | null): string {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '—';

  const diffMs = Date.now() - date.getTime();
  const future = diffMs < 0;
  const minutes = Math.round(Math.abs(diffMs) / 60000);

  if (minutes < 1) return 'только что';
  if (minutes < 60) return future ? `через ${minutes} ${plural(minutes, 'минуту', 'минуты', 'минут')}` : `${minutes} ${plural(minutes, 'минуту', 'минуты', 'минут')} назад`;

  const hours = Math.round(minutes / 60);
  if (hours < 24) return future ? `через ${hours} ${plural(hours, 'час', 'часа', 'часов')}` : `${hours} ${plural(hours, 'час', 'часа', 'часов')} назад`;

  const days = Math.round(hours / 24);
  if (days === 1) return future ? 'завтра' : 'вчера';
  if (days < 8) return future ? `через ${days} ${plural(days, 'день', 'дня', 'дней')}` : `${days} ${plural(days, 'день', 'дня', 'дней')} назад`;

  return formatDate(value);
}

/** 12.11.2026 в формате поля <input type="date"> */
export function toDateInput(value?: string | null): string {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';
  const offset = date.getTimezoneOffset();
  return new Date(date.getTime() - offset * 60000).toISOString().slice(0, 10);
}

/** Часы → «2 ч 15 мин» */
export function formatHours(hours?: number | null): string {
  if (hours === null || hours === undefined) return '—';
  if (hours < 1) return `${Math.round(hours * 60)} мин`;
  const h = Math.floor(hours);
  const m = Math.round((hours - h) * 60);
  return m === 0 ? `${h} ${plural(h, 'час', 'часа', 'часов')}` : `${h} ч ${m} мин`;
}

/** Русская форма множественного числа: plural(2, 'заявка', 'заявки', 'заявок') */
export function plural(count: number, one: string, few: string, many: string): string {
  const n = Math.abs(count) % 100;
  const n1 = n % 10;
  if (n > 10 && n < 20) return many;
  if (n1 > 1 && n1 < 5) return few;
  if (n1 === 1) return one;
  return many;
}

export function ticketsWord(count: number): string {
  return `${count} ${plural(count, 'заявка', 'заявки', 'заявок')}`;
}

/** Инициалы для аватара: «Иванов Пётр» → «ИП» */
export function initials(fullName?: string | null): string {
  if (!fullName) return '??';
  const parts = fullName.trim().split(/\s+/);
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[1][0]).toUpperCase();
}

/** Цвет аватара по идентификатору пользователя. */
export function avatarColor(seed?: number | string | null): string {
  const palette = ['#2563eb', '#7c3aed', '#0891b2', '#059669', '#d97706', '#dc2626', '#db2777', '#4f46e5'];
  const key = String(seed ?? '');
  let hash = 0;
  for (let i = 0; i < key.length; i += 1) {
    hash = (hash * 31 + key.charCodeAt(i)) % 100000;
  }
  return palette[hash % palette.length];
}

/** Дни до плановой даты: отрицательное значение — просрочка. */
export function daysUntil(value?: string | null): number | null {
  if (!value) return null;
  const target = new Date(`${value}T23:59:59`);
  if (Number.isNaN(target.getTime())) return null;
  return Math.ceil((target.getTime() - Date.now()) / 86400000);
}


