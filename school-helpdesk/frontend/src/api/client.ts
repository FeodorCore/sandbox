import type { ApiErrorBody } from '../types';

const TOKEN_KEY = 'helpdesk.token';

/** Ошибка REST API с HTTP-статусом и списком ошибок валидации. */
export class ApiError extends Error {
  readonly status: number;
  readonly violations: { field: string; message: string }[];

  constructor(status: number, message: string, violations: { field: string; message: string }[] = []) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.violations = violations;
  }

  /** Текст ошибки для конкретного поля формы. */
  fieldError(field: string): string | undefined {
    return this.violations.find((v) => v.field === field)?.message;
  }
}

export const getToken = (): string | null => localStorage.getItem(TOKEN_KEY);
export const setToken = (token: string): void => localStorage.setItem(TOKEN_KEY, token);
export const clearToken = (): void => localStorage.removeItem(TOKEN_KEY);

export const UNAUTHORIZED_EVENT = 'helpdesk:unauthorized';

interface RequestOptions extends Omit<RequestInit, 'body'> {
  body?: unknown;
  /** не показывать глобальное сообщение об ошибке (обработает вызывающий код) */
  silent?: boolean;
}

/** Базовый HTTP-клиент: JSON + JWT в заголовке Authorization. */
export async function api<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { body, headers, silent, ...rest } = options;

  const finalHeaders = new Headers(headers);
  if (body !== undefined && body !== null) {
    finalHeaders.set('Content-Type', 'application/json;charset=UTF-8');
  }
  finalHeaders.set('Accept', 'application/json');
  const token = getToken();
  if (token) {
    finalHeaders.set('Authorization', `Bearer ${token}`);
  }

  let response: Response;
  try {
    response = await fetch(path, {
      ...rest,
      headers: finalHeaders,
      body: body === undefined || body === null ? undefined : JSON.stringify(body),
    });
  } catch {
    throw new ApiError(0, 'Сервер недоступен. Проверьте подключение и повторите попытку.');
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const contentType = response.headers.get('content-type') ?? '';
  const isJson = contentType.includes('application/json');

  if (!response.ok) {
    let message = `Ошибка сервера (${response.status})`;
    let violations: { field: string; message: string }[] = [];
    if (isJson) {
      try {
        const data = (await response.json()) as ApiErrorBody;
        message = data.message || data.error || message;
        violations = data.violations ?? [];
      } catch {
        /* оставляем сообщение по умолчанию */
      }
    }
    if (response.status === 401 && !silent) {
      clearToken();
      window.dispatchEvent(new CustomEvent(UNAUTHORIZED_EVENT));
    }
    throw new ApiError(response.status, message, violations);
  }

  if (!isJson) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

/** Сборка query-строки: пустые значения пропускаются, массивы повторяются. */
export function buildQuery(params: Record<string, unknown>): string {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '' || value === false) {
      if (value === false && (key === 'active' || key === 'unassigned' || key === 'onlyMine'
        || key === 'activeOnly' || key === 'overdue' || key === 'includeInactive' || key === 'withCounts'
        || key === 'activeOnly')) {
        // явные false тоже передаём — сервер различает «не задано» и «false»
        search.append(key, 'false');
      }
      return;
    }
    if (Array.isArray(value)) {
      value.forEach((item) => {
        if (item !== undefined && item !== null && item !== '') {
          search.append(key, String(item));
        }
      });
      return;
    }
    if (value === true) {
      search.append(key, 'true');
      return;
    }
    search.append(key, String(value));
  });
  const query = search.toString();
  return query ? `?${query}` : '';
}


