import type { Priority, Role, TicketStatus, TicketEventType } from '../types';

interface StatusMeta {
  label: string;
  /** CSS-класс бейджа */
  tone: 'blue' | 'amber' | 'slate' | 'green' | 'grey' | 'red';
  hint: string;
}

export const STATUS_META: Record<TicketStatus, StatusMeta> = {
  NEW: { label: 'Новая', tone: 'blue', hint: 'Заявка создана и ждёт назначения исполнителя' },
  IN_PROGRESS: { label: 'В работе', tone: 'amber', hint: 'Специалист выполняет заявку' },
  ON_HOLD: { label: 'Отложена', tone: 'slate', hint: 'Работа приостановлена (ожидание запчастей, ответа)' },
  RESOLVED: { label: 'Выполнена', tone: 'green', hint: 'Работа выполнена, ожидается подтверждение автора' },
  CLOSED: { label: 'Закрыта', tone: 'grey', hint: 'Заявка закрыта и подтверждена' },
  REJECTED: { label: 'Отклонена', tone: 'red', hint: 'Заявка отклонена (дубликат или не техническая проблема)' },
};

export const STATUS_ORDER: TicketStatus[] = [
  'NEW',
  'IN_PROGRESS',
  'ON_HOLD',
  'RESOLVED',
  'CLOSED',
  'REJECTED',
];

export const ACTIVE_STATUSES: TicketStatus[] = ['NEW', 'IN_PROGRESS', 'ON_HOLD'];

interface PriorityMeta {
  label: string;
  tone: 'grey' | 'blue' | 'orange' | 'red';
  weight: number;
}

export const PRIORITY_META: Record<Priority, PriorityMeta> = {
  LOW: { label: 'Низкий', tone: 'grey', weight: 1 },
  MEDIUM: { label: 'Средний', tone: 'blue', weight: 2 },
  HIGH: { label: 'Высокий', tone: 'orange', weight: 3 },
  CRITICAL: { label: 'Критический', tone: 'red', weight: 4 },
};

export const PRIORITY_ORDER: Priority[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

export const ROLE_META: Record<Role, { label: string; tone: 'red' | 'amber' | 'blue' }> = {
  ADMIN: { label: 'Администратор', tone: 'red' },
  TECHNICIAN: { label: 'IT-специалист', tone: 'amber' },
  USER: { label: 'Сотрудник', tone: 'blue' },
};

export const ROLE_ORDER: Role[] = ['ADMIN', 'TECHNICIAN', 'USER'];

/** Цвета диаграмм для статусов. */
export const STATUS_CHART_COLOR: Record<TicketStatus, string> = {
  NEW: '#2563eb',
  IN_PROGRESS: '#d97706',
  ON_HOLD: '#64748b',
  RESOLVED: '#16a34a',
  CLOSED: '#94a3b8',
  REJECTED: '#dc2626',
};

export const PRIORITY_CHART_COLOR: Record<Priority, string> = {
  LOW: '#94a3b8',
  MEDIUM: '#2563eb',
  HIGH: '#f97316',
  CRITICAL: '#dc2626',
};

export const EVENT_TONE: Record<TicketEventType, string> = {
  CREATED: 'blue',
  UPDATED: 'slate',
  STATUS_CHANGED: 'amber',
  PRIORITY_CHANGED: 'orange',
  CATEGORY_CHANGED: 'slate',
  ASSIGNED: 'green',
  UNASSIGNED: 'grey',
  COMMENT_ADDED: 'slate',
  RESOLVED: 'green',
  REOPENED: 'red',
  CLOSED: 'grey',
};

/** Подписи статусов для select-элементов. */
export const statusLabel = (status: TicketStatus): string => STATUS_META[status]?.label ?? status;
export const priorityLabel = (priority: Priority): string => PRIORITY_META[priority]?.label ?? priority;
export const roleLabel = (role: Role): string => ROLE_META[role]?.label ?? role;


