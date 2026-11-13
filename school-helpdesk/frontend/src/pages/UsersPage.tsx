import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  ArrowDownWideNarrow,
  Mail,
  Pencil,
  Phone,
  PlusCircle,
  Search,
  ShieldCheck,
  Trash2,
  UserPlus,
  X,
} from 'lucide-react';
import { usersApi } from '../api/endpoints';
import { ApiError } from '../api/client';
import { useAuth } from '../store/AuthContext';
import { useToast } from '../store/ToastContext';
import type { Page, Role, User, UserPayload } from '../types';
import { ROLE_META, ROLE_ORDER, roleLabel } from '../utils/dict';
import { avatarColor, formatDate, formatRelative, initials } from '../utils/format';
import { RoleBadge } from '../components/Badges';
import ConfirmDialog from '../components/ConfirmDialog';
import EmptyState from '../components/EmptyState';
import Modal from '../components/Modal';
import Pagination from '../components/Pagination';
import { PageLoader } from '../components/Spinner';

const SORT_OPTIONS: { value: string; label: string }[] = [
  { value: 'fullName', label: 'ФИО' },
  { value: 'username', label: 'Логин' },
  { value: 'role', label: 'Роль' },
  { value: 'createdAt', label: 'Дата создания' },
  { value: 'lastLoginAt', label: 'Последний вход' },
];

const PAGE_SIZE = 20;

const emptyForm = (): UserPayload & { passwordRepeat: string } => ({
  username: '',
  password: '',
  fullName: '',
  email: '',
  position: '',
  phone: '',
  role: 'USER',
  active: true,
  passwordRepeat: '',
});

/** Управление пользователями — раздел администратора. */
export default function UsersPage() {
  const toast = useToast();
  const { user: me } = useAuth();

  const [data, setData] = useState<Page<User> | null>(null);
  const [loading, setLoading] = useState(true);

  const [searchText, setSearchText] = useState('');
  const [role, setRole] = useState<Role | ''>('');
  const [activeOnly, setActiveOnly] = useState(false);
  const [page, setPage] = useState(0);
  const [sort, setSort] = useState('fullName');
  const [direction, setDirection] = useState<'asc' | 'desc'>('asc');
  const debounceRef = useRef<number | null>(null);

  // ------------------------------------------------------------- модальное окно
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<User | null>(null);
  const [form, setForm] = useState(emptyForm());
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<User | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const result = await usersApi.search({
        search: searchText.trim() || undefined,
        role: role || undefined,
        activeOnly: activeOnly || undefined,
        page,
        size: PAGE_SIZE,
        sort,
        direction,
      });
      setData(result);
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : 'Не удалось загрузить пользователей');
    } finally {
      setLoading(false);
    }
  }, [searchText, role, activeOnly, page, sort, direction, toast]);

  useEffect(() => {
    void load();
  }, [load]);

  // поиск с задержкой
  useEffect(() => {
    if (debounceRef.current) window.clearTimeout(debounceRef.current);
    debounceRef.current = window.setTimeout(() => {
      setPage(0);
      void load();
    }, 400);
    return () => {
      if (debounceRef.current) window.clearTimeout(debounceRef.current);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchText]);

  useEffect(() => {
    document.title = 'Пользователи — School HelpDesk';
  }, []);

  const set = (patch: Partial<typeof form>) => setForm((prev) => ({ ...prev, ...patch }));

  const openCreate = () => {
    setEditing(null);
    setForm(emptyForm());
    setFieldErrors({});
    setModalOpen(true);
  };

  const openEdit = (person: User) => {
    setEditing(person);
    setForm({
      username: person.username,
      password: '',
      fullName: person.fullName,
      email: person.email ?? '',
      position: person.position ?? '',
      phone: person.phone ?? '',
      role: person.role,
      active: person.active,
      passwordRepeat: '',
    });
    setFieldErrors({});
    setModalOpen(true);
  };

  function validateForm(): boolean {
    const errors: Record<string, string> = {};
    if (!form.fullName.trim()) errors.fullName = 'Укажите ФИО';
    if (!editing) {
      if (!form.username?.trim()) errors.username = 'Укажите логин';
      else if (!/^[a-zA-Z0-9._-]{3,64}$/.test(form.username.trim())) {
        errors.username = 'Логин: 3–64 символа, только латиница, цифры, точка, дефис, подчёркивание';
      }
      if (!form.password) errors.password = 'Укажите пароль';
      else if (form.password.length < 6) errors.password = 'Пароль: от 6 до 72 символов';
    } else if (form.password && form.password.length < 6) {
      errors.password = 'Пароль: от 6 до 72 символов';
    }
    if (form.password !== form.passwordRepeat) errors.passwordRepeat = 'Пароли не совпадают';
    if (form.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) errors.email = 'Некорректный e-mail';
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  }

  const submitForm = async () => {
    if (busy || !validateForm()) return;
    setBusy(true);
    try {
      const payload: UserPayload = {
        fullName: form.fullName.trim(),
        email: form.email?.trim() || undefined,
        position: form.position?.trim() || undefined,
        phone: form.phone?.trim() || undefined,
        role: form.role,
        active: form.active,
      };
      if (editing) {
        if (form.password) payload.password = form.password;
        await usersApi.update(editing.id, payload);
        toast.success('Данные пользователя обновлены');
      } else {
        await usersApi.create({ ...payload, username: form.username!.trim(), password: form.password });
        toast.success(`Пользователь ${form.username!.trim()} создан`);
      }
      setModalOpen(false);
      await load();
    } catch (e) {
      if (e instanceof ApiError) {
        if (e.violations.length > 0) {
          const errors: Record<string, string> = {};
          e.violations.forEach((v) => {
            errors[v.field] = v.message;
          });
          setFieldErrors(errors);
        }
        toast.error(e.message);
      } else {
        toast.error('Не удалось сохранить пользователя');
      }
    } finally {
      setBusy(false);
    }
  };

  const confirmDelete = async () => {
    if (!deleteTarget) return;
    setBusy(true);
    try {
      const result = await usersApi.remove(deleteTarget.id);
      // 200 + dto — пользователь заблокирован (на него есть заявки/комментарии), 204 — удалён
      if (result && typeof result === 'object') {
        toast.info(`Пользователь заблокирован: на него есть заявки или комментарии`);
      } else {
        toast.success('Пользователь удалён');
      }
      setDeleteTarget(null);
      await load();
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : 'Не удалось удалить пользователя');
    } finally {
      setBusy(false);
    }
  };

  const totals = useMemo(() => {
    if (!data) return null;
    return data.totalElements;
  }, [data]);

  return (
    <div className="users-page">
      <div className="page-header">
        <div>
          <h2 className="page-header__title">Пользователи</h2>
          <p className="page-header__subtitle">
            Учётные записи сотрудников школы и IT-специалистов
            {totals !== null ? ` · всего ${totals}` : ''}
          </p>
        </div>
        <div className="page-header__actions">
          <button type="button" className="btn btn--primary" onClick={openCreate}>
            <UserPlus size={16} />
            Добавить пользователя
          </button>
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
            placeholder="Поиск: ФИО, логин, должность, e-mail…"
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
          <div className="chip-group">
            <button
              type="button"
              className={`chip chip--sm${role === '' ? ' chip--active' : ''}`}
              onClick={() => {
                setRole('');
                setPage(0);
              }}
            >
              Все роли
            </button>
            {ROLE_ORDER.map((option) => (
              <button
                key={option}
                type="button"
                className={`chip chip--sm${role === option ? ' chip--active' : ''}`}
                onClick={() => {
                  setRole(role === option ? '' : option);
                  setPage(0);
                }}
              >
                {ROLE_META[option].label}
              </button>
            ))}
          </div>

          <div className="select-inline">
            <ArrowDownWideNarrow size={15} />
            <select
              className="select select--inline"
              value={sort}
              onChange={(event) => {
                setSort(event.target.value);
                setPage(0);
              }}
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
              onClick={() => setDirection(direction === 'asc' ? 'desc' : 'asc')}
              title="Изменить направление сортировки"
            >
              {direction === 'asc' ? 'по возрастанию' : 'по убыванию'}
            </button>
          </div>

          <button
            type="button"
            className={`chip chip--sm${activeOnly ? ' chip--active' : ''}`}
            onClick={() => {
              setActiveOnly(!activeOnly);
              setPage(0);
            }}
          >
            Только активные
          </button>
        </div>
      </div>

      <div className="card">
        {loading && !data ? (
          <PageLoader text="Загружаем пользователей…" />
        ) : !data || data.content.length === 0 ? (
          <EmptyState
            icon={<ShieldCheck size={28} strokeWidth={1.5} />}
            title="Пользователи не найдены"
            description="Измените условия поиска или добавьте нового пользователя"
            action={
              <button type="button" className="btn btn--primary" onClick={openCreate}>
                <PlusCircle size={16} />
                Добавить пользователя
              </button>
            }
          />
        ) : (
          <>
            <div className={`table-wrap${loading ? ' table-wrap--loading' : ''}`}>
              <table className="table">
                <thead>
                  <tr>
                    <th>Пользователь</th>
                    <th>Роль</th>
                    <th className="th--hide-md">Должность</th>
                    <th className="th--hide-lg">Контакты</th>
                    <th className="th--hide-md">Заявки</th>
                    <th className="th--hide-sm">Последний вход</th>
                    <th>Статус</th>
                    <th aria-label="Действия" />
                  </tr>
                </thead>
                <tbody>
                  {data.content.map((person) => (
                    <tr key={person.id} className="table-row">
                      <td data-label="Пользователь">
                        <span className="user-inline">
                          <span className="avatar avatar--sm" style={{ background: avatarColor(person.id) }}>
                            {initials(person.fullName)}
                          </span>
                          <span className="user-inline__text">
                            <span>
                              {person.fullName}
                              {me?.id === person.id && <em className="you-mark">это вы</em>}
                            </span>
                            <small>@{person.username}</small>
                          </span>
                        </span>
                      </td>
                      <td data-label="Роль">
                        <RoleBadge role={person.role} />
                      </td>
                      <td data-label="Должность" className="td--hide-md">
                        {person.position || <span className="muted">—</span>}
                      </td>
                      <td data-label="Контакты" className="td--hide-lg">
                        <span className="contacts-cell">
                          {person.email && (
                            <span className="contacts-cell__item" title={person.email}>
                              <Mail size={13} /> {person.email}
                            </span>
                          )}
                          {person.phone && (
                            <span className="contacts-cell__item" title={person.phone}>
                              <Phone size={13} /> {person.phone}
                            </span>
                          )}
                          {!person.email && !person.phone && <span className="muted">—</span>}
                        </span>
                      </td>
                      <td data-label="Заявки" className="td--hide-md">
                        <span className="counts-cell" title="Создал / назначено на него">
                          {person.ticketsCreated ?? 0} / {person.ticketsAssigned ?? 0}
                        </span>
                      </td>
                      <td data-label="Последний вход" className="td--hide-sm">
                        <span className="date-cell" title={formatDate(person.lastLoginAt)}>
                          {person.lastLoginAt ? formatRelative(person.lastLoginAt) : <span className="muted">не входил</span>}
                        </span>
                      </td>
                      <td data-label="Статус">
                        {person.active ? (
                          <span className="badge badge--green">
                            <span className="badge__dot" /> активен
                          </span>
                        ) : (
                          <span className="badge badge--grey">заблокирован</span>
                        )}
                      </td>
                      <td data-label="Действия">
                        <span className="row-actions">
                          <button
                            type="button"
                            className="btn btn--icon btn--ghost"
                            onClick={() => openEdit(person)}
                            aria-label={`Редактировать ${person.fullName}`}
                            title="Редактировать"
                          >
                            <Pencil size={15} />
                          </button>
                          <button
                            type="button"
                            className="btn btn--icon btn--ghost btn--icon-danger"
                            onClick={() => setDeleteTarget(person)}
                            disabled={me?.id === person.id}
                            aria-label={`Удалить ${person.fullName}`}
                            title={me?.id === person.id ? 'Нельзя удалить собственную учётную запись' : 'Удалить'}
                          >
                            <Trash2 size={15} />
                          </button>
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination
              page={data}
              itemWord={['пользователь', 'пользователя', 'пользователей']}
              onChange={setPage}
            />
          </>
        )}
      </div>

      <p className="hint">
        Совет: удаление пользователя, на которого есть заявки или комментарии, заменяется блокировкой — история работ
        сохраняется.
      </p>

      {/* -------------------------------------------------- создание/правка */}
      <Modal
        open={modalOpen}
        title={editing ? 'Редактирование пользователя' : 'Новый пользователь'}
        subtitle={editing ? `@${editing.username} · ${roleLabel(editing.role)}` : 'Учётная запись для входа в систему'}
        onClose={() => setModalOpen(false)}
        busy={busy}
        footer={
          <>
            <button type="button" className="btn btn--secondary" onClick={() => setModalOpen(false)} disabled={busy}>
              Отмена
            </button>
            <button type="button" className="btn btn--primary" onClick={() => void submitForm()} disabled={busy}>
              {editing ? 'Сохранить' : 'Создать'}
            </button>
          </>
        }
      >
        <div className="form-grid">
          <label className="field">
            <span className="field__label">
              ФИО <span className="req">*</span>
            </span>
            <input
              className="input"
              type="text"
              value={form.fullName}
              maxLength={160}
              placeholder="Иванов Пётр Сергеевич"
              onChange={(event) => set({ fullName: event.target.value })}
            />
            {fieldErrors.fullName && <span className="field__error">{fieldErrors.fullName}</span>}
          </label>

          <label className="field">
            <span className="field__label">
              Логин {editing ? '' : <span className="req">*</span>}
            </span>
            <input
              className="input"
              type="text"
              value={form.username ?? ''}
              maxLength={64}
              placeholder="например, petrov"
              disabled={Boolean(editing)}
              onChange={(event) => set({ username: event.target.value })}
            />
            {fieldErrors.username ? (
              <span className="field__error">{fieldErrors.username}</span>
            ) : editing ? (
              <span className="field__hint">Логин изменить нельзя</span>
            ) : (
              <span className="field__hint">Латиница, цифры, точка, дефис, подчёркивание</span>
            )}
          </label>

          <label className="field">
            <span className="field__label">
              {editing ? 'Новый пароль' : 'Пароль'} {!editing && <span className="req">*</span>}
            </span>
            <input
              className="input"
              type="password"
              value={form.password ?? ''}
              maxLength={72}
              autoComplete="new-password"
              placeholder={editing ? 'оставьте пустым, чтобы не менять' : 'минимум 6 символов'}
              onChange={(event) => set({ password: event.target.value })}
            />
            {fieldErrors.password && <span className="field__error">{fieldErrors.password}</span>}
          </label>

          <label className="field">
            <span className="field__label">Повторите пароль</span>
            <input
              className="input"
              type="password"
              value={form.passwordRepeat}
              maxLength={72}
              autoComplete="new-password"
              placeholder="ещё раз"
              onChange={(event) => set({ passwordRepeat: event.target.value })}
            />
            {fieldErrors.passwordRepeat && <span className="field__error">{fieldErrors.passwordRepeat}</span>}
          </label>

          <label className="field">
            <span className="field__label">
              Роль <span className="req">*</span>
            </span>
            <select
              className="select"
              value={form.role}
              onChange={(event) => set({ role: event.target.value as Role })}
            >
              {ROLE_ORDER.map((option) => (
                <option key={option} value={option}>
                  {ROLE_META[option].label}
                </option>
              ))}
            </select>
            <span className="field__hint">
              Администратор — полный доступ; IT-специалист — выполнение заявок; сотрудник — создание заявок
            </span>
          </label>

          <label className="field">
            <span className="field__label">Должность</span>
            <input
              className="input"
              type="text"
              value={form.position ?? ''}
              maxLength={120}
              placeholder="например, учитель информатики"
              onChange={(event) => set({ position: event.target.value })}
            />
          </label>

          <label className="field">
            <span className="field__label">E-mail</span>
            <input
              className="input"
              type="email"
              value={form.email ?? ''}
              maxLength={120}
              placeholder="name@school.ru"
              onChange={(event) => set({ email: event.target.value })}
            />
            {fieldErrors.email && <span className="field__error">{fieldErrors.email}</span>}
          </label>

          <label className="field">
            <span className="field__label">Телефон</span>
            <input
              className="input"
              type="tel"
              value={form.phone ?? ''}
              maxLength={40}
              placeholder="+7 900 000-00-00"
              onChange={(event) => set({ phone: event.target.value })}
            />
          </label>
        </div>

        <label className="checkbox">
          <input
            type="checkbox"
            checked={form.active ?? true}
            onChange={(event) => set({ active: event.target.checked })}
          />
          <span>Учётная запись активна (разрешён вход в систему)</span>
        </label>
      </Modal>

      <ConfirmDialog
        open={deleteTarget !== null}
        title="Удалить пользователя?"
        message={
          deleteTarget
            ? `«${deleteTarget.fullName}» (@${deleteTarget.username}) будет удалён. Если на него есть заявки или комментарии — учётная запись будет заблокирована вместо удаления.`
            : ''
        }
        confirmText="Удалить"
        busy={busy}
        onConfirm={() => void confirmDelete()}
        onCancel={() => setDeleteTarget(null)}
      />
    </div>
  );
}
