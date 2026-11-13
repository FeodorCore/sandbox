import { api, buildQuery } from './client';
import type {
  Category,
  CategoryPayload,
  Comment,
  DashboardStats,
  LoginResponse,
  Page,
  Priority,
  Ticket,
  TicketEvent,
  TicketFilters,
  TicketPayload,
  TicketStatus,
  User,
  UserPayload,
  UserRef,
} from '../types';

// --------------------------------------------------------------- аутентификация

export const authApi = {
  login: (username: string, password: string) =>
    api<LoginResponse>('/api/auth/login', { method: 'POST', body: { username, password } }),
  me: () => api<User>('/api/auth/me'),
  changePassword: (currentPassword: string, newPassword: string) =>
    api<void>('/api/auth/change-password', { method: 'POST', body: { currentPassword, newPassword } }),
  logout: () => api<void>('/api/auth/logout', { method: 'POST', silent: true }),
};

// ------------------------------------------------------------------- заявки

export const ticketsApi = {
  search: (filters: TicketFilters) =>
    api<Page<Ticket>>(`/api/tickets${buildQuery(filters as Record<string, unknown>)}`),
  get: (id: number) => api<Ticket>(`/api/tickets/${id}`),
  create: (payload: TicketPayload) => api<Ticket>('/api/tickets', { method: 'POST', body: payload }),
  update: (id: number, payload: TicketPayload & { priority: Priority }) =>
    api<Ticket>(`/api/tickets/${id}`, { method: 'PUT', body: payload }),
  changeStatus: (id: number, status: TicketStatus, resolution?: string, comment?: string) =>
    api<Ticket>(`/api/tickets/${id}/status`, { method: 'PATCH', body: { status, resolution, comment } }),
  changePriority: (id: number, priority: Priority) =>
    api<Ticket>(`/api/tickets/${id}/priority`, { method: 'PATCH', body: { priority } }),
  assign: (id: number, assigneeId: number | null, comment?: string) =>
    api<Ticket>(`/api/tickets/${id}/assign`, { method: 'PATCH', body: { assigneeId, comment } }),
  remove: (id: number) => api<void>(`/api/tickets/${id}`, { method: 'DELETE' }),
  history: (id: number) => api<TicketEvent[]>(`/api/tickets/${id}/history`),
};

// --------------------------------------------------------------- комментарии

export const commentsApi = {
  list: (ticketId: number) => api<Comment[]>(`/api/tickets/${ticketId}/comments`),
  create: (ticketId: number, text: string, internal: boolean) =>
    api<Comment>(`/api/tickets/${ticketId}/comments`, { method: 'POST', body: { text, internal } }),
  remove: (id: number) => api<void>(`/api/comments/${id}`, { method: 'DELETE' }),
};

// ---------------------------------------------------------------- категории

export const categoriesApi = {
  list: (includeInactive?: boolean, withCounts = false) =>
    api<Category[]>(`/api/categories${buildQuery({ includeInactive, withCounts })}`),
  create: (payload: CategoryPayload) => api<Category>('/api/categories', { method: 'POST', body: payload }),
  update: (id: number, payload: CategoryPayload) =>
    api<Category>(`/api/categories/${id}`, { method: 'PUT', body: payload }),
  remove: (id: number) => api<void>(`/api/categories/${id}`, { method: 'DELETE' }),
};

// ------------------------------------------------------------- пользователи

export const usersApi = {
  search: (params: {
    search?: string;
    role?: string;
    activeOnly?: boolean;
    page?: number;
    size?: number;
    sort?: string;
    direction?: 'asc' | 'desc';
  }) => api<Page<User>>(`/api/users${buildQuery(params as Record<string, unknown>)}`),
  get: (id: number) => api<User>(`/api/users/${id}`),
  create: (payload: UserPayload) => api<User>('/api/users', { method: 'POST', body: payload }),
  update: (id: number, payload: UserPayload) =>
    api<User>(`/api/users/${id}`, { method: 'PUT', body: payload }),
  remove: (id: number) => api<User | void>(`/api/users/${id}`, { method: 'DELETE' }),
  assignees: () => api<UserRef[]>('/api/users/assignees'),
  options: () => api<UserRef[]>('/api/users/options'),
};

// ---------------------------------------------------------------- мониторинг

export const dashboardApi = {
  stats: (trendDays = 14) => api<DashboardStats>(`/api/dashboard/stats${buildQuery({ trendDays })}`),
};


