import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import {
  ArrowDownWideNarrow,
  CalendarDays,
  ChevronDown,
  Filter,
  PlusCircle,
  Search,
  SlidersHorizontal,
  X,
} from 'lucide-react';
import { categoriesApi, ticketsApi, usersApi } from '../api/endpoints';
import { ApiError } from '../api/client';
import { useAuth } from '../store/AuthContext';
import { useToast } from '../store/ToastContext';
import type { Category, Page, Priority, Ticket, TicketFilters, TicketStatus, UserRef } from '../types';
import { ACTIVE_STATUSES, PRIORITY_META, PRIORITY_ORDER, STATUS_META, STATUS_ORDER } from '../utils/dict';
import { daysUntil, formatDate, formatRelative, plural } from '../utils/format';
import { CommentsCount, PriorityBadge, StatusBadge, TicketNumberLink, UserLink } from '../components/Badges';
import EmptyState from '../components/EmptyState';
import Pagination from '../components/Pagination';
import { PageLoader } from '../components/Spinner';

const toNumber = (value: string | null): number | null => {
  if (value === null || value === '') return null;
  const parsed = Number(value);
  return Number.isNaN(parsed) ? null : parsed;
};
const toBool = (value: string | null): boolean | null => {
  if (value === null) return null;
  return value === 'true';
};

const QUICK_FILTERS: { key: string; label: string; params: Record<string, string> }[] = [
  { key: 'all', label: 'Все', params: {} },
  { key: 'active', label: 'Активные', params: { activeOnly: 'true' } },
  { key: 'new', label: 'Новые', params: { status: 'NEW' } },
  { key: 'progress', label: 'В работе', params: { status: 'IN_PROGRESS' } },
  { key: 'resolved', label: 'Выполнены', params: { status: 'RESOLVED' } },
  { key: 'closed', label: 'Закрыты', params: { status: 'CLOSED' } },
];

const SORT_OPTIONS: { value: string; label: string }[] = [
  { value: 'createdAt', label: 'Дата создания' },
  { value: 'updatedAt', label: 'Последнее изменение' },
  { value: 'priority', label: 'Приоритет' },
  { value: 'status', label: 'Статус' },
  { value: 'number', label: 'Номер' },
  { value: 'title', label: 'Заголовок' },
  { value: 'dueDate', label: 'Плановый срок' },
  { value: 'room', label: 'Кабинет' },
  { value: 'author', label: 'Автор' },
  { value: 'assignee', label: 'Исполнитель' },
  { value: 'category', label: 'Категория' },
];

export default function TicketsPage() {
  const { user, isStaff } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const [data, setData] = useState<Page<Ticket> | null>(null);
  const [loading, setLoading] = useState(true);
  const [searchText, setSearchText] = useState(searchParams.get('search') ?? '');
  const [showFilters, setShowFilters] = useState(false);
  const [categories, setCategories] = useState<Category[]>([]);
  const [users, setUsers] = useState<UserRef[]>([]);
  const debounceRef = useRef<number | null>(null);

  const filters = useMemo<TicketFilters>(() => {
    const statuses = searchParams.getAll('status') as TicketStatus[];
    const priorities = searchParams.getAll('priority') as Priority[];
    return {
      search: searchParams.get('search') ?? undefined,
      status: statuses.length ? statuses : undefined,
      priority: priorities.length ? priorities : undefined,
      categoryId: toNumber(searchParams.get('categoryId')),
      assigneeId: toNumber(searchParams.get('assigneeId')),
      unassigned: toBool(searchParams.get('unassigned')),
      authorId: toNumber(searchParams.get('authorId')),
      onlyMine: toBool(searchParams.get('onlyMine')),
      room: searchParams.get('room') ?? undefined,
      building: searchParams.get('building') ?? undefined,
      dateFrom: searchParams.get('dateFrom') ?? undefined,
      dateTo: searchParams.get('dateTo') ?? undefined,
      activeOnly: toBool(searchParams.get('activeOnly')),
      overdue: toBool(searchParams.get('overdue')),
      page: toNumber(searchParams.get('page')) ?? 0,
      size: toNumber(searchParams.get('size')) ?? 20,
      sort: searchParams.get('sort') ?? 'createdAt',
      direction: (searchParams.get('direction') as 'asc' | 'desc') ?? 'desc',
    };
  }, [searchParams]);

  const updateParams = useCallback(
    (changes: Record<string, string | number | boolean | null | undefined>, resetPage = true) => {
      const next = new URLSearchParams(searchParams);
      Object.entries(changes).forEach(([key, value]) => {
        if (value === undefined || value === null || value === '') {
          next.delete(key);
        } else {
          next.set(key, String(value));
        }
      });
      if (resetPage) next.set('page', '0');
      setSearchParams(next, { replace: true });
    },
    [searchParams, setSearchParams],
  );

  const toggleMulti = useCallback(
    (key: string, value: string) => {
      const next = new URLSearchParams(searchParams);
      const current = next.getAll(key);
      if (current.includes(value)) {
        next.delete(key);
        current.filter((item) => item !== value).forEach((item) => next.append(key, item));
      } else {
        next.append(key, value);
      }
      next.set('page', '0');
      setSearchParams(next, { replace: true });
    },
    [searchParams, setSearchParams],
  );

  // загрузка списка заявок
  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    ticketsApi
      .search(filters)
      .then((page) => {
        if (!cancelled) setData(page);
      })
      .catch((e) => {
        if (!cancelled) toast.error(e instanceof ApiError ? e.message : 'Не удалось загрузить заявки');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [filters, toast]);

  // справочники для фильтров
  useEffect(() => {
    categoriesApi
      .list(true)
      .then(setCategories)
      .catch(() => undefined);
    if (isStaff) {
      usersApi
        .options()
        .then(setUsers)
        .catch(() => undefined);
    }
  }, [isStaff]);

  // поиск с задержкой
  useEffect(() => {
    if (debounceRef.current) window.clearTimeout(debounceRef.current);
    debounceRef.current = window.setTimeout(() => {
      const current = searchParams.get('search') ?? '';
      if (current !== searchText) {
        updateParams({ search: searchText || null });
      }
    }, 400);
    return () => {
      if (debounceRef.current) window.clearTimeout(debounceRef.current);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchText]);

  useEffect(() => {
    document.title = 'Заявки — School HelpDesk';
  }, []);

  const activeFilterCount = useMemo(() => {
    let count = 0;
    if (filters.search) count += 1;
    count += filters.status?.length ?? 0;
    count += filters.priority?.length ?? 0;
    if (filters.categoryId) count += 1;
    if (filters.assigneeId) count += 1;
    if (filters.authorId) count += 1;
    if (filters.unassigned) count += 1;
    if (filters.onlyMine) count += 1;
    if (filters.activeOnly) count += 1;
    if (filters.overdue) count += 1;
    if (filters.room) count += 1;
    if (filters.dateFrom) count += 1;
    if (filters.dateTo) count += 1;
    return count;
  }, [filters]);

  const quickKey = useMemo(() => {
    if (filters.overdue) return 'overdue';
    if (filters.unassigned) return 'unassigned';
    if (filters.onlyMine) return 'mine';
    const match = QUICK_FILTERS.find((item) =>
      Object.entries(item.params).every(([key, value]) => searchParams.get(key) === value),
    );
    if (!match) return 'custom';
    // «Все» подходит, только если нет других фильтров статуса
    return match.key;
  }, [filters, searchParams]);

  const resetFilters = () => {
    const size = searchParams.get('size');
    const next = new URLSearchParams();
    if (size) next.set('size', size);
    setSearchParams(next, { replace: true });
    setSearchText('');
  };

  const toggleSort = (field: string) => {
    if (filters.sort === field) {
      updateParams({ direction: filters.direction === 'asc' ? 'desc' : 'asc' }, false);
    } else {
      updateParams({ sort: field, direction: field === 'title' || field === 'room' ? 'asc' : 'desc' }, false);
    }
  };

  const sortIcon = (field: string) =>
    filters.sort === field ? (filters.direction === 'asc' ? '↑' : '↓') : '';

  return (
    <div className="tickets-page">
      <div className="page-header">
        <div>
          <h2 className="page-header__title">Заявки</h2>
          <p className="page-header__subtitle">
            {isStaff
              ? 'Все обращения сотрудников школы в службу технической поддержки'
              : 'Ваши обращения в службу технической поддержки'}
          </p>
        </div>
        <div className="page-header__actions">
          <Link className="btn btn--primary" to="/tickets/new">
            <PlusCircle size={16} />
            Создать заявку
          </Link>
        </div>
      </div>

      <div className="card filters-card">
        <div className="filters-card__search">
          <span className="filters-card__search-icon">
            <Search size={17} />
          </span>
          <input
            className="input input--search"
            type="search"
            placeholder="Поиск: номер, заголовок, кабинет, ФИО, категория…"
            value={searchText}
            onChange={(event) => setSearchText(event.target.value)}
          />
          {searchText && (
            <button
              type="button"
              className="btn btn--icon btn--ghost"
              onClick={() => setSearchText('')}
              aria-label="Очистить поиск"
            >
              <X size={16} />
            </button>
          )}
        </div>

        <div className="filters-card__tools">
          <button
            type="button"
            className={`btn btn--secondary${showFilters ? ' btn--active' : ''}`}
            onClick={() => setShowFilters((v) => !v)}
          >
            <SlidersHorizontal size={16} />
            Фильтры
            {activeFilterCount > 0 && <span className="btn__counter">{activeFilterCount}</span>}
            <ChevronDown size={14} className={showFilters ? 'rotated' : ''} />
          </button>

          <div className="select-inline">
            <ArrowDownWideNarrow size={15} />
            <select
              className="select select--inline"
              value={filters.sort}
              onChange={(event) => updateParams({ sort: event.target.value }, false)}
              aria-label="Сортировка"
            >
              {SORT_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
            <button
              type="button"
              className="btn btn--sm btn--ghost"
              onClick={() => updateParams({ direction: filters.direction === 'asc' ? 'desc' : 'asc' }, false)}
              title="Изменить направление сортировки"
            >
              {filters.direction === 'asc' ? 'по возрастанию' : 'по убыванию'}
            </button>
          </div>

          {activeFilterCount > 0 && (
            <button type="button" className="btn btn--ghost" onClick={resetFilters}>
              <X size={15} />
              Сбросить
            </button>
          )}
        </div>

        <div className="chips">
          {QUICK_FILTERS.map((item) => (
            <button
              key={item.key}
              type="button"
              className={`chip${quickKey === item.key ? ' chip--active' : ''}`}
              onClick={() => {
                const next = new URLSearchParams(searchParams);
                ['status', 'activeOnly', 'overdue', 'unassigned', 'onlyMine'].forEach((key) => next.delete(key));
                Object.entries(item.params).forEach(([key, value]) => next.set(key, value));
                next.set('page', '0');
                setSearchParams(next, { replace: true });
              }}
            >
              {item.label}
            </button>
          ))}
          {isStaff && (
            <>
              <button
                type="button"
                className={`chip${filters.unassigned ? ' chip--active' : ''}`}
                onClick={() => updateParams({ unassigned: filters.unassigned ? null : true, activeOnly: true })}
              >
                Без исполнителя
              </button>
              <button
                type="button"
                className={`chip${filters.overdue ? ' chip--active' : ''}`}
                onClick={() => updateParams({ overdue: filters.overdue ? null : true })}
              >
                Просроченные
              </button>
            </>
          )}
          <button
            type="button"
            className={`chip${quickKey === 'mine' ? ' chip--active' : ''}`}
            onClick={() => updateParams({ onlyMine: filters.onlyMine ? null : true })}
          >
            Мои заявки
          </button>
        </div>

        {showFilters && (
          <div className="filters-panel">
            <div className="field">
              <span className="field__label">Статус</span>
              <div className="chip-group">
                {STATUS_ORDER.map((status) => (
                  <button
                    key={status}
                    type="button"
                    className={`chip chip--sm${filters.status?.includes(status) ? ' chip--active' : ''}`}
                    onClick={() => toggleMulti('status', status)}
                  >
                    {STATUS_META[status].label}
                  </button>
                ))}
              </div>
            </div>

            <div className="field">
              <span className="field__label">Приоритет</span>
              <div className="chip-group">
                {PRIORITY_ORDER.map((priority) => (
                  <button
                    key={priority}
                    type="button"
                    className={`chip chip--sm${filters.priority?.includes(priority) ? ' chip--active' : ''}`}
                    onClick={() => toggleMulti('priority', priority)}
                  >
                    {PRIORITY_META[priority].label}
                  </button>
                ))}
              </div>
            </div>

            <div className="filters-grid">
              <label className="field">
                <span className="field__label">Категория</span>
                <select
                  className="select"
                  value={filters.categoryId ?? ''}
                  onChange={(event) => updateParams({ categoryId: event.target.value || null })}
                >
                  <option value="">Все категории</option>
                  {categories.map((category) => (
                    <option key={category.id} value={category.id}>
                      {category.name}
                      {!category.active ? ' (архив)' : ''}
                    </option>
                  ))}
                </select>
              </label>

              {isStaff && (
                <label className="field">
                  <span className="field__label">Исполнитель</span>
                  <select
                    className="select"
                    value={filters.assigneeId ?? ''}
                    onChange={(event) => updateParams({ assigneeId: event.target.value || null })}
                  >
                    <option value="">Любой</option>
                    {users
                      .filter((u) => u.role !== 'USER')
                      .map((u) => (
                        <option key={u.id} value={u.id}>
                          {u.fullName}
                        </option>
                      ))}
                  </select>
                </label>
              )}

              {isStaff && (
                <label className="field">
                  <span className="field__label">Автор</span>
                  <select
                    className="select"
                    value={filters.authorId ?? ''}
                    onChange={(event) => updateParams({ authorId: event.target.value || null })}
                  >
                    <option value="">Любой</option>
                    {users.map((u) => (
                      <option key={u.id} value={u.id}>
                        {u.fullName}
                      </option>
                    ))}
                  </select>
                </label>
              )}

              <label className="field">
                <span className="field__label">Кабинет</span>
                <input
                  className="input"
                  type="text"
                  placeholder="например, 204"
                  defaultValue={filters.room ?? ''}
                  onBlur={(event) => updateParams({ room: event.target.value.trim() || null })}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter') {
                      updateParams({ room: (event.target as HTMLInputElement).value.trim() || null });
                    }
                  }}
                />
              </label>

              <label className="field">
                <span className="field__label">
                  <CalendarDays size={14} /> Создана с
                </span>
                <input
                  className="input"
                  type="date"
                  value={filters.dateFrom ?? ''}
                  onChange={(event) => updateParams({ dateFrom: event.target.value || null })}
                />
              </label>

              <label className="field">
                <span className="field__label">Создана по</span>
                <input
                  className="input"
                  type="date"
                  value={filters.dateTo ?? ''}
                  onChange={(event) => updateParams({ dateTo: event.target.value || null })}
                />
              </label>
            </div>

            <div className="chip-group">
              <button
                type="button"
                className={`chip chip--sm${filters.activeOnly ? ' chip--active' : ''}`}
                onClick={() => updateParams({ activeOnly: filters.activeOnly ? null : true })}
              >
                Только активные
              </button>
            </div>
          </div>
        )}
      </div>

      <div className="card">
        {loading && !data ? (
          <PageLoader text="Загружаем заявки…" />
        ) : !data || data.content.length === 0 ? (
          <EmptyState
            title={activeFilterCount > 0 ? 'По заданным условиям заявок нет' : 'Заявок пока нет'}
            description={
              activeFilterCount > 0
                ? 'Попробуйте изменить условия поиска или сбросить фильтры'
                : 'Создайте заявку — она появится в этом списке'
            }
            action={
              activeFilterCount > 0 ? (
                <button type="button" className="btn btn--secondary" onClick={resetFilters}>
                  <Filter size={16} />
                  Сбросить фильтры
                </button>
              ) : (
                <Link className="btn btn--primary" to="/tickets/new">
                  <PlusCircle size={16} />
                  Создать заявку
                </Link>
              )
            }
          />
        ) : (
          <>
            <div className={`table-wrap${loading ? ' table-wrap--loading' : ''}`}>
              <table className="table">
                <thead>
                  <tr>
                    <th className="sortable" onClick={() => toggleSort('number')}>
                      Номер {sortIcon('number')}
                    </th>
                    <th className="sortable" onClick={() => toggleSort('title')}>
                      Проблема {sortIcon('title')}
                    </th>
                    <th className="sortable th--hide-md" onClick={() => toggleSort('category')}>
                      Категория {sortIcon('category')}
                    </th>
                    <th className="sortable th--hide-sm" onClick={() => toggleSort('room')}>
                      Кабинет {sortIcon('room')}
                    </th>
                    <th className="sortable" onClick={() => toggleSort('status')}>
                      Статус {sortIcon('status')}
                    </th>
                    <th className="sortable" onClick={() => toggleSort('priority')}>
                      Приоритет {sortIcon('priority')}
                    </th>
                    <th className="th--hide-md">Исполнитель</th>
                    <th className="sortable th--hide-sm" onClick={() => toggleSort('createdAt')}>
                      Создана {sortIcon('createdAt')}
                    </th>
                    <th className="th--hide-lg">Срок</th>
                  </tr>
                </thead>
                <tbody>
                  {data.content.map((ticket) => {
                    const due = daysUntil(ticket.dueDate);
                    return (
                      <tr
                        key={ticket.id}
                        className="table-row"
                        onClick={() => navigate(`/tickets/${ticket.id}`)}
                      >
                        <td data-label="Номер">
                          <TicketNumberLink id={ticket.id} number={ticket.number} />
                        </td>
                        <td data-label="Проблема">
                          <div className="cell-title">
                            <span className="cell-title__text">{ticket.title}</span>
                            <span className="cell-title__sub">
                              <UserLink name={ticket.author.fullName} id={ticket.author.id} />
                              <CommentsCount count={ticket.commentCount} />
                            </span>
                          </div>
                        </td>
                        <td data-label="Категория" className="td--hide-md">
                          <span className="cat-tag">
                            <span className="cat-tag__dot" style={{ background: ticket.category.color ?? '#94a3b8' }} />
                            {ticket.category.name}
                          </span>
                        </td>
                        <td data-label="Кабинет" className="td--hide-sm">
                          {ticket.room ? (
                            <span className="room">
                              {ticket.room}
                              {ticket.building ? <small>{ticket.building}</small> : null}
                            </span>
                          ) : (
                            <span className="muted">—</span>
                          )}
                        </td>
                        <td data-label="Статус">
                          <StatusBadge status={ticket.status} />
                        </td>
                        <td data-label="Приоритет">
                          <PriorityBadge priority={ticket.priority} />
                        </td>
                        <td data-label="Исполнитель" className="td--hide-md">
                          <UserLink name={ticket.assignee?.fullName} id={ticket.assignee?.id} />
                        </td>
                        <td data-label="Создана" className="td--hide-sm">
                          <span className="date-cell" title={formatDate(ticket.createdAt)}>
                            {formatRelative(ticket.createdAt)}
                          </span>
                        </td>
                        <td data-label="Срок" className="td--hide-lg">
                          {ticket.dueDate ? (
                            <span className={`due${ticket.overdue ? ' due--overdue' : ''}`}>
                              {formatDate(ticket.dueDate)}
                              {ticket.overdue && due !== null ? ` (${Math.abs(due)} ${plural(Math.abs(due), 'день', 'дня', 'дней')} просрочка)` : ''}
                            </span>
                          ) : (
                            <span className="muted">—</span>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
            <Pagination
              page={data}
              itemWord={['заявка', 'заявки', 'заявок']}
              onChange={(page) => updateParams({ page }, false)}
            />
          </>
        )}
      </div>

      <p className="hint">
        {ACTIVE_STATUSES.length === 3
          ? `Совет: клик по строке открывает карточку заявки. ${
              user && !isStaff ? 'Вы видите только свои обращения.' : 'Доступны все обращения школы.'
            }`
          : ''}
      </p>
    </div>
  );
}


