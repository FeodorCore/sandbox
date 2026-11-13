import { useCallback, useEffect, useMemo, useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import {
  ArrowLeft,
  CalendarClock,
  Info,
  MapPin,
  MonitorSmartphone,
  Save,
  Send,
  UserCog,
  X,
} from 'lucide-react';
import { categoriesApi, ticketsApi, usersApi } from '../api/endpoints';
import { ApiError } from '../api/client';
import { useAuth } from '../store/AuthContext';
import { useToast } from '../store/ToastContext';
import type { Category, Priority, Ticket, UserRef } from '../types';
import { PRIORITY_META, PRIORITY_ORDER } from '../utils/dict';
import { toDateInput } from '../utils/format';
import EmptyState from '../components/EmptyState';
import { PageLoader, Spinner } from '../components/Spinner';

/** Форма создания (/tickets/new) и редактирования (/tickets/:id/edit) заявки. */
export default function TicketFormPage() {
  const { id } = useParams<{ id: string }>();
  const editId = id ? Number(id) : null;
  const isEdit = editId !== null && Number.isFinite(editId);

  const navigate = useNavigate();
  const toast = useToast();
  const { isStaff } = useAuth();

  const [loading, setLoading] = useState(isEdit);
  const [notFound, setNotFound] = useState(false);
  const [forbidden, setForbidden] = useState(false);
  const [ticket, setTicket] = useState<Ticket | null>(null);

  const [categories, setCategories] = useState<Category[]>([]);
  const [assignees, setAssignees] = useState<UserRef[]>([]);

  // ------------------------------------------------------------- поля формы
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [categoryId, setCategoryId] = useState<number | ''>('');
  const [priority, setPriority] = useState<Priority>('MEDIUM');
  const [room, setRoom] = useState('');
  const [building, setBuilding] = useState('');
  const [equipment, setEquipment] = useState('');
  const [dueDate, setDueDate] = useState('');
  const [assigneeId, setAssigneeId] = useState<number | ''>('');
  const [comment, setComment] = useState('');

  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  /** Приоритет можно менять только сотрудникам (backend игнорирует смену роли USER). */
  const canEditPriority = !isEdit || isStaff;

  // ------------------------------------------------------- справочники/данные
  useEffect(() => {
    categoriesApi
      .list(true)
      .then(setCategories)
      .catch(() => toast.error('Не удалось загрузить категории'));
    if (isStaff && !isEdit) {
      usersApi
        .assignees()
        .then(setAssignees)
        .catch(() => undefined);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isStaff, isEdit]);

  useEffect(() => {
    if (!isEdit) return;
    let cancelled = false;
    setLoading(true);
    ticketsApi
      .get(editId!)
      .then((data) => {
        if (cancelled) return;
        if (!data.actions.includes('EDIT')) {
          setForbidden(true);
          return;
        }
        setTicket(data);
        setTitle(data.title);
        setDescription(data.description);
        setCategoryId(data.category.id);
        setPriority(data.priority);
        setRoom(data.room ?? '');
        setBuilding(data.building ?? '');
        setEquipment(data.equipment ?? '');
        setDueDate(toDateInput(data.dueDate));
      })
      .catch((e) => {
        if (cancelled) return;
        if (e instanceof ApiError && (e.status === 404 || e.status === 403)) {
          setNotFound(true);
        } else {
          toast.error(e instanceof ApiError ? e.message : 'Не удалось загрузить заявку');
          setNotFound(true);
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [isEdit, editId, toast]);

  useEffect(() => {
    document.title = isEdit ? 'Редактирование заявки — School HelpDesk' : 'Новая заявка — School HelpDesk';
  }, [isEdit]);

  /** Категории, доступные для выбора: активные + текущая категория заявки при редактировании. */
  const visibleCategories = useMemo(
    () => categories.filter((c) => c.active || c.id === categoryId),
    [categories, categoryId],
  );

  // ------------------------------------------------------------- валидация
  function validate(): boolean {
    const errors: Record<string, string> = {};
    const t = title.trim();
    const d = description.trim();
    if (t.length < 5) errors.title = 'Заголовок: от 5 до 200 символов';
    else if (t.length > 200) errors.title = 'Заголовок: не более 200 символов';
    if (d.length < 10) errors.description = 'Описание: от 10 до 4000 символов';
    else if (d.length > 4000) errors.description = 'Описание: не более 4000 символов';
    if (categoryId === '') errors.categoryId = 'Выберите категорию';
    if (room.length > 40) errors.room = 'Кабинет: не более 40 символов';
    if (building.length > 80) errors.building = 'Корпус/место: не более 80 символов';
    if (equipment.length > 120) errors.equipment = 'Оборудование: не более 120 символов';
    if (comment.length > 2000) errors.comment = 'Комментарий: не более 2000 символов';
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  }

  const submit = useCallback(
    async (event: FormEvent) => {
      event.preventDefault();
      if (busy) return;
      setFormError(null);
      if (!validate()) return;

      setBusy(true);
      try {
        const payload = {
          title: title.trim(),
          description: description.trim(),
          categoryId: categoryId === '' ? null : Number(categoryId),
          room: room.trim() || undefined,
          building: building.trim() || undefined,
          equipment: equipment.trim() || undefined,
          dueDate: dueDate || null,
        };
        let saved: Ticket;
        if (isEdit) {
          saved = await ticketsApi.update(editId!, { ...payload, priority });
          toast.success('Изменения сохранены');
        } else {
          saved = await ticketsApi.create({
            ...payload,
            priority,
            assigneeId: isStaff && assigneeId !== '' ? Number(assigneeId) : undefined,
            comment: comment.trim() || undefined,
          });
          toast.success(`Заявка ${saved.number} создана`);
        }
        navigate(`/tickets/${saved.id}`, { replace: true });
      } catch (e) {
        if (e instanceof ApiError) {
          if (e.violations.length > 0) {
            const errors: Record<string, string> = {};
            e.violations.forEach((v) => {
              errors[v.field] = v.message;
            });
            setFieldErrors(errors);
          }
          setFormError(e.message);
        } else {
          setFormError('Не удалось сохранить заявку. Повторите попытку.');
        }
      } finally {
        setBusy(false);
      }
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [busy, isEdit, editId, title, description, categoryId, priority, room, building, equipment, dueDate, assigneeId, comment, isStaff, navigate, toast],
  );

  // -------------------------------------------------------------- рендер
  if (loading) return <PageLoader text="Открываем форму…" />;

  if (notFound) {
    return (
      <EmptyState
        title="Заявка не найдена"
        description="Возможно, она была удалена или указан неверный адрес"
        action={
          <Link className="btn btn--primary" to="/tickets">
            К списку заявок
          </Link>
        }
      />
    );
  }

  if (forbidden) {
    return (
      <EmptyState
        title="Редактирование недоступно"
        description="У вас нет прав на изменение этой заявки или её текущий статус не позволяет вносить правки"
        action={
          <Link className="btn btn--primary" to={`/tickets/${editId}`}>
            Вернуться к заявке
          </Link>
        }
      />
    );
  }

  const backTo = isEdit ? `/tickets/${editId}` : '/tickets';

  return (
    <div className="ticket-form-page">
      <div className="page-header page-header--stacked">
        <Link className="back-link" to={backTo}>
          <ArrowLeft size={15} />
          {isEdit ? 'К заявке' : 'К списку заявок'}
        </Link>
        <div>
          <h2 className="page-header__title">{isEdit ? `Редактирование ${ticket?.number ?? 'заявки'}` : 'Новая заявка'}</h2>
          <p className="page-header__subtitle">
            {isEdit
              ? 'Измените данные заявки и сохраните — изменения попадут в историю'
              : 'Опишите проблему: чем подробнее, тем быстрее специалист сможет помочь'}
          </p>
        </div>
      </div>

      <form className="card ticket-form" onSubmit={submit} noValidate>
        {formError && (
          <div className="alert alert--error" role="alert">
            <Info size={16} />
            {formError}
          </div>
        )}

        <div className="card__body">
          <label className="field">
            <span className="field__label">
              Кратко: что случилось <span className="req">*</span>
            </span>
            <input
              className="input"
              type="text"
              value={title}
              maxLength={200}
              placeholder="Например: не включается компьютер у учителя"
              onChange={(event) => setTitle(event.target.value)}
              autoFocus={!isEdit}
            />
            {fieldErrors.title ? (
              <span className="field__error">{fieldErrors.title}</span>
            ) : (
              <span className="field__hint">Один короткий заголовок — 5…200 символов</span>
            )}
          </label>

          <label className="field">
            <span className="field__label">
              Описание проблемы <span className="req">*</span>
            </span>
            <textarea
              className="textarea"
              rows={6}
              maxLength={4000}
              value={description}
              placeholder={
                'Опишите подробно: что произошло, когда началось, что уже пробовали сделать, '
                + 'как проблема мешает работе. Минимум 10 символов.'
              }
              onChange={(event) => setDescription(event.target.value)}
            />
            {fieldErrors.description ? (
              <span className="field__error">{fieldErrors.description}</span>
            ) : (
              <span className="field__hint">{description.trim().length} / 4000 символов</span>
            )}
          </label>

          <div className="form-grid">
            <label className="field">
              <span className="field__label">
                Категория <span className="req">*</span>
              </span>
              <select
                className="select"
                value={categoryId}
                onChange={(event) => setCategoryId(event.target.value === '' ? '' : Number(event.target.value))}
              >
                <option value="">— выберите категорию —</option>
                {visibleCategories.map((category) => (
                  <option key={category.id} value={category.id}>
                    {category.name}
                    {!category.active ? ' (архив)' : ''}
                  </option>
                ))}
              </select>
              {fieldErrors.categoryId && <span className="field__error">{fieldErrors.categoryId}</span>}
            </label>

            <label className="field">
              <span className="field__label">
                <MapPin size={14} /> Кабинет / место
              </span>
              <input
                className="input"
                type="text"
                value={room}
                maxLength={40}
                placeholder="например, 204"
                onChange={(event) => setRoom(event.target.value)}
              />
              {fieldErrors.room && <span className="field__error">{fieldErrors.room}</span>}
            </label>

            <label className="field">
              <span className="field__label">Корпус / расположение</span>
              <input
                className="input"
                type="text"
                value={building}
                maxLength={80}
                placeholder="например, главный корпус, 2 этаж"
                onChange={(event) => setBuilding(event.target.value)}
              />
              {fieldErrors.building && <span className="field__error">{fieldErrors.building}</span>}
            </label>

            <label className="field">
              <span className="field__label">
                <MonitorSmartphone size={14} /> Оборудование
              </span>
              <input
                className="input"
                type="text"
                value={equipment}
                maxLength={120}
                placeholder="марка, модель, инвентарный номер"
                onChange={(event) => setEquipment(event.target.value)}
              />
              {fieldErrors.equipment && <span className="field__error">{fieldErrors.equipment}</span>}
            </label>

            <label className="field">
              <span className="field__label">
                <CalendarClock size={14} /> Плановый срок
              </span>
              <input
                className="input"
                type="date"
                value={dueDate}
                min={!isEdit ? toDateInput(new Date().toISOString()) : undefined}
                onChange={(event) => setDueDate(event.target.value)}
              />
              <span className="field__hint">Необязательно — когда проблема должна быть решена</span>
            </label>

            {isStaff && !isEdit && (
              <label className="field">
                <span className="field__label">
                  <UserCog size={14} /> Исполнитель
                </span>
                <select
                  className="select"
                  value={assigneeId}
                  onChange={(event) => setAssigneeId(event.target.value === '' ? '' : Number(event.target.value))}
                >
                  <option value="">— не назначать (общая очередь) —</option>
                  {assignees.map((person) => (
                    <option key={person.id} value={person.id}>
                      {person.fullName}
                      {person.position ? ` · ${person.position}` : ''}
                    </option>
                  ))}
                </select>
                <span className="field__hint">При назначении заявка сразу перейдёт в статус «В работе»</span>
              </label>
            )}
          </div>

          {canEditPriority && (
            <div className="field">
              <span className="field__label">Приоритет</span>
              <div className="radio-cards">
                {PRIORITY_ORDER.map((option) => (
                  <label
                    key={option}
                    className={`radio-card radio-card--${option.toLowerCase()}${
                      priority === option ? ' radio-card--active' : ''
                    }`}
                  >
                    <input
                      type="radio"
                      name="priority"
                      value={option}
                      checked={priority === option}
                      onChange={() => setPriority(option)}
                    />
                    <span className="radio-card__title">{PRIORITY_META[option].label}</span>
                    <span className="radio-card__hint">{priorityHint(option)}</span>
                  </label>
                ))}
              </div>
            </div>
          )}

          {!isEdit && (
            <label className="field">
              <span className="field__label">Комментарий (необязательно)</span>
              <textarea
                className="textarea"
                rows={3}
                maxLength={2000}
                value={comment}
                placeholder="Дополнительная информация для специалиста: контакты, когда удобнее принять мастера…"
                onChange={(event) => setComment(event.target.value)}
              />
              {fieldErrors.comment && <span className="field__error">{fieldErrors.comment}</span>}
            </label>
          )}
        </div>

        <footer className="form-actions">
          <button type="button" className="btn btn--secondary" onClick={() => navigate(backTo)} disabled={busy}>
            <X size={16} />
            Отмена
          </button>
          <button type="submit" className="btn btn--primary btn--lg" disabled={busy}>
            {busy ? <Spinner size={17} /> : isEdit ? <Save size={17} /> : <Send size={17} />}
            {isEdit ? 'Сохранить изменения' : 'Отправить заявку'}
          </button>
        </footer>
      </form>
    </div>
  );
}

function priorityHint(priority: Priority): string {
  switch (priority) {
    case 'LOW':
      return 'не мешает работе';
    case 'MEDIUM':
      return 'обычная задача';
    case 'HIGH':
      return 'мешает учебному процессу';
    case 'CRITICAL':
      return 'срочно! процесс остановлен';
    default:
      return '';
  }
}
