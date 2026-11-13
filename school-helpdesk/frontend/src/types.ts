/** Типы данных, повторяющие DTO backend-а (ru.school.helpdesk.dto). */

export type Role = 'ADMIN' | 'TECHNICIAN' | 'USER';

export type TicketStatus =
  | 'NEW'
  | 'IN_PROGRESS'
  | 'ON_HOLD'
  | 'RESOLVED'
  | 'CLOSED'
  | 'REJECTED';

export type Priority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export type TicketEventType =
  | 'CREATED'
  | 'UPDATED'
  | 'STATUS_CHANGED'
  | 'PRIORITY_CHANGED'
  | 'CATEGORY_CHANGED'
  | 'ASSIGNED'
  | 'UNASSIGNED'
  | 'COMMENT_ADDED'
  | 'RESOLVED'
  | 'REOPENED'
  | 'CLOSED';

export type TicketAction =
  | 'VIEW'
  | 'EDIT'
  | 'ASSIGN'
  | 'CHANGE_STATUS'
  | 'CHANGE_PRIORITY'
  | 'COMMENT'
  | 'INTERNAL_COMMENT'
  | 'RESOLVE'
  | 'CLOSE'
  | 'REOPEN'
  | 'CANCEL'
  | 'DELETE';

export interface UserRef {
  id: number;
  username: string;
  fullName: string;
  role: Role;
  position?: string | null;
}

export interface User extends UserRef {
  email?: string | null;
  phone?: string | null;
  roleTitle: string;
  active: boolean;
  lastLoginAt?: string | null;
  createdAt?: string | null;
  ticketsCreated?: number | null;
  ticketsAssigned?: number | null;
}

export interface Category {
  id: number;
  name: string;
  description?: string | null;
  color?: string | null;
  sortOrder: number;
  active: boolean;
  ticketCount?: number | null;
}

export interface Ticket {
  id: number;
  number: string;
  title: string;
  description: string;
  status: TicketStatus;
  statusTitle: string;
  priority: Priority;
  priorityTitle: string;
  category: Category;
  author: UserRef;
  assignee?: UserRef | null;
  room?: string | null;
  building?: string | null;
  equipment?: string | null;
  dueDate?: string | null;
  overdue: boolean;
  resolution?: string | null;
  createdAt: string;
  updatedAt: string;
  resolvedAt?: string | null;
  closedAt?: string | null;
  commentCount: number;
  actions: TicketAction[];
}

export interface Comment {
  id: number;
  ticketId: number;
  author: UserRef;
  text: string;
  internal: boolean;
  editable: boolean;
  createdAt: string;
}

export interface TicketEvent {
  id: number;
  type: TicketEventType;
  typeTitle: string;
  message?: string | null;
  fromValue?: string | null;
  toValue?: string | null;
  actor?: UserRef | null;
  createdAt: string;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface NameCount {
  code: string;
  name: string;
  count: number;
  color?: string | null;
}

export interface Workload {
  userId: number;
  fullName: string;
  active: number;
  done: number;
}

export interface DayCount {
  date: string;
  created: number;
  resolved: number;
}

export interface DashboardStats {
  scopedToUser: boolean;
  total: number;
  active: number;
  newCount: number;
  inProgress: number;
  onHold: number;
  resolved: number;
  closed: number;
  rejected: number;
  unassigned: number;
  overdue: number;
  critical: number;
  createdToday: number;
  resolvedToday: number;
  avgResolutionHours?: number | null;
  byStatus: NameCount[];
  byPriority: NameCount[];
  byCategory: NameCount[];
  workload: Workload[];
  topRooms: NameCount[];
  trend: DayCount[];
  recent: Ticket[];
}

export interface LoginResponse {
  token: string;
  tokenType: string;
  expiresInSeconds: number;
  user: User;
}

export interface ApiErrorBody {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  violations?: { field: string; message: string }[] | null;
}

/** Параметры списка заявок. */
export interface TicketFilters {
  search?: string;
  status?: TicketStatus[];
  priority?: Priority[];
  categoryId?: number | null;
  assigneeId?: number | null;
  unassigned?: boolean | null;
  authorId?: number | null;
  onlyMine?: boolean | null;
  room?: string;
  building?: string;
  dateFrom?: string | null;
  dateTo?: string | null;
  activeOnly?: boolean | null;
  overdue?: boolean | null;
  page?: number;
  size?: number;
  sort?: string;
  direction?: 'asc' | 'desc';
}

export interface TicketPayload {
  title: string;
  description: string;
  categoryId: number | null;
  priority?: Priority;
  room?: string;
  building?: string;
  equipment?: string;
  dueDate?: string | null;
  assigneeId?: number | null;
  comment?: string;
}

export interface UserPayload {
  username?: string;
  password?: string;
  fullName: string;
  email?: string;
  position?: string;
  phone?: string;
  role: Role;
  active?: boolean;
}

export interface CategoryPayload {
  name: string;
  description?: string;
  color?: string;
  sortOrder?: number;
  active?: boolean;
}


