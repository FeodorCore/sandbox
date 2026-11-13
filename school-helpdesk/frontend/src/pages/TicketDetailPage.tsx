import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import {
  ArrowLeft,
  CalendarClock,
  CheckCircle2,
  History,
  Lock,
  MessageSquare,
  Pencil,
  Play,
  RotateCcw,
  Send,
  Trash2,
  UserCog,
  XCircle,
} from 'lucide-react';
import { commentsApi, ticketsApi, usersApi } from '../api/endpoints';
import { ApiError } from '../api/client';
import { useAuth } from '../store/AuthContext';
import { useToast } from '../store/ToastContext';
import type { Comment, Ticket, TicketEvent, TicketStatus, Priority, UserRef } from '../types';
import { PRIORITY_META, PRIORITY_ORDER, STATUS_META, STATUS_ORDER } from '../utils/dict';
import { avatarColor, daysUntil, formatDate, formatDateTime, formatRelative, initials, plural } from '../utils/format';
import { CategoryTag, PriorityBadge, RoleBadge, StatusBadge, UserLink } from '../components/Badges';
import ConfirmDialog from '../components/ConfirmDialog';
import Modal from '../components/Modal';
import { PageLoader } from '../components/Spinner';
import EmptyState from '../components/EmptyState';

type ModalKind = 'status' | 'assign' | 'priority' | 'delete' | null;

export default function TicketDetailPage() {
  const { id } = useParams<{ id: string }>();
  const ticketId = Number(id);
  const navigate = useNavigate();
  const toast = useToast();
  const { user, isStaff } = useAuth();

  const [ticket, setTicket] = useState<Ticket | null>(null);
  const [comments, setComments] = useState<Comment[]>([]);
  const [history, setHistory] = useState<TicketEvent[]>([]);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);

  const [modal, setModal] = useState<ModalKind>(null);
  const [statusTarget, setStatusTarget] = useState<TicketStatus>('IN_PROGRESS');
  const [resolution, setResolution] = useState('');
  const [statusComment, setStatusComment] = useState('');
  const [assigneeId, setAssigneeId] = useState<number | ''>('');
  const [priorityTarget, setPriorityTarget] = useState<Priority>('MEDIUM');
  const [assignees, setAssignees] = useState<UserRef[]>([]);
  const [busy, setBusy] = useState(false);

  const [commentText, setCommentText] = useState('');
  const [internal, setInternal] = useState(false);
  const [sendingComment, setSendingComment] = useState(false);
  const [tab, setTab] = useState<'comments' | 'history'>('comments');

  const load = useCallback(async () => {
    try {
      const [ticketData, commentsData, historyData] = await Promise.all([
        ticketsApi.get(ticketId),
        commentsApi.list(ticketId),
        ticketsApi.history(ticketId),
      ]);
      setTicket(ticketData);
      setComments(commentsData);
      setHistory(historyData);
      setNotFound(false);
    } catch (e) {
      if (e instanceof ApiError && e.status === 404) {
        setNotFound(true);
      } else {
        toast.error(e instanceof ApiError ? e.message : 'Не удалось загрузить заявку');
      }
    } finally {
      setLoading(false);
    }
  }, [ticketId, toast]);

  useEffect(() => {
    if (!Number.isFinite(ticketId)) {
      setNotFound(true);
      setLoading(false);
      return;
    }
    setLoading(true);
    void load();
  }, [ticketId, load]);

  useEffect(() => {
    if (isStaff) {
      usersApi
        .assignees()
        .then(setAssignees)
        .catch(() => undefined);
    }
  }, [isStaff]);

  useEffect(() => {
    document.title = ticket ? `${ticket.number} — ${ticket.title}` : 'Заявка — School HelpDesk';
  }, [ticket]);

  const actions = useMemo(() => new Set(ticket?.actions ?? []), [ticket]);

  const allowedStatuses = useMemo<TicketStatus[]>(() => {
    if (!ticket) return [];
    const all = STATUS_ORDER.filter((s) => s !== ticket.status);
    if (isStaff) return all;
    // автор может только отменить новую заявку, подтвердить выполнение или вернуть в работу
    if (ticket.status === 'NEW') return ['CLOSED'];
    if (ticket.status === 'RESOLVED') return ['CLOSED', 'IN_PROGRESS'];
    return [];
  }, [ticket, isStaff]);

  if (loading) return <PageLoader text="Открываем заявку…" />;

  if (notFound || !ticket) {
    return (
      <EmptyState
        title="Заявка не найдена"
        description="Возможно, она была удалена или у вас нет прав на её просмотр"
        action={
          <Link className="btn btn--primary" to="/tickets">
            К списку заявок
          </Link>
        }
      />
    );
  }

  const due = daysUntil(ticket.dueDate);

  async function run(action: () => Promise<unknown>, successMessage: string) {
    setBusy(true);
    try {
      await action();
      await load();
      toast.success(successMessage);
      setModal(null);
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : 'Операция не выполнена');
    } finally {
      setBusy(false);
    }
  }

  const openStatusModal = (preset?: TicketStatus) => {
    const target = preset ?? allowedStatuses[0] ?? ticket!.status;
    setStatusTarget(target);
    setResolution(ticket!.resolution ?? '');
    setStatusComment('');
    setModal('status');
  };

  const openAssignModal = () => {
    setAssigneeId(ticket!.assignee?.id ?? '');
    setModal('assign');
  };

  const submitStatus = () =>
    run(
      () => ticketsApi.changeStatus(ticket!.id, statusTarget, resolution.trim() || undefined, statusComment.trim() || undefined),
      `Статус изменён на «${STATUS_META[statusTarget].label}»`,
    );

  const submitAssign = () =>
    run(
      () => ticketsApi.assign(ticket!.id, assigneeId === '' ? null : Number(assigneeId)),
      assigneeId === '' ? 'Исполнитель снят' : 'Исполнитель назначен',
    );

  const submitPriority = () =>
    run(() => ticketsApi.changePriority(ticket!.id, priorityTarget), 'Приоритет обновлён');

  const submitDelete = () =>
    run(async () => {
      await ticketsApi.remove(ticket!.id);
      toast.success('Заявка удалена');
      navigate('/tickets');
    }, 'Заявка удалена');

  const submitComment = async () => {
    if (!commentText.trim()) return;
    setSendingComment(true);
    try {
      const created = await commentsApi.create(ticket!.id, commentText.trim(), internal);
      setComments((prev) => [...prev, created]);
      setCommentText('');
      setInternal(false);
      await load();
      toast.success('Комментарий добавлен');
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : 'Не удалось добавить комментарий');
    } finally {
      setSendingComment(false);
    }
  };

  const removeComment = async (commentId: number) => {
    try {
      await commentsApi.remove(commentId);
      setComments((prev) => prev.filter((c) => c.id !== commentId));
      toast.success('Комментарий удалён');
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : 'Не удалось удалить комментарий');
    }
  };

  return (
    <div className="ticket-page">
      <div className="page-header page-header--stacked">
        <Link className="back-link" to="/tickets">
          <ArrowLeft size={15} />
          К списку заявок
        </Link>

        <div className="ticket-header">
          <div className="ticket-header__main">
            <span className="ticket-header__number">{ticket.number}</span>
            <h2 className="ticket-header__title">{ticket.title}</h2>
            <div className="ticket-header__badges">
              <StatusBadge status={ticket.status} />
              <PriorityBadge priority={ticket.priority} />
              <CategoryTag name={ticket.category.name} color={ticket.category.color} />
              {ticket.overdue && <span className="badge badge--red">просрочена</span>}
            </div>
          </div>

          <div className="ticket-header__actions">
            {actions.has('RESOLVE') && (
              <button type="button" className="btn btn--success" onClick={() => openStatusModal('RESOLVED')}>
                <CheckCircle2 size={16} />
                Выполнено
              </button>
            )}
            {isStaff && ticket.status === 'NEW' && (
              <button
                type="button"
                className="btn btn--primary"
                onClick={() => run(() => ticketsApi.assign(ticket.id, user!.id), 'Заявка взята в работу')}
              >
                <Play size={16} />
                Взять в работу
              </button>
            )}
            {actions.has('CLOSE') && (
              <button type="button" className="btn btn--secondary" onClick={() => openStatusModal('CLOSED')}>
                <XCircle size={16} />
                Закрыть
              </button>
            )}
            {actions.has('REOPEN') && (
              <button type="button" className="btn btn--secondary" onClick={() => openStatusModal('IN_PROGRESS')}>
                <RotateCcw size={16} />
                Открыть заново
              </button>
            )}
            {actions.has('CHANGE_STATUS') && (
              <button type="button" className="btn btn--secondary" onClick={() => openStatusModal()}>
                <History size={16} />
                Сменить статус
              </button>
            )}
            {actions.has('ASSIGN') && (
              <button type="button" className="btn btn--secondary" onClick={openAssignModal}>
                <UserCog size={16} />
                Исполнитель
              </button>
            )}
            {actions.has('CHANGE_PRIORITY') && (
              <button
                type="button"
                className="btn btn--secondary"
                onClick={() => {
                  setPriorityTarget(ticket.priority);
                  setModal('priority');
                }}
              >
                Приоритет
              </button>
            )}
            {actions.has('EDIT') && (
              <Link className="btn btn--secondary" to={`/tickets/${ticket.id}/edit`}>
                <Pencil size={16} />
                Редактировать
              </Link>
            )}
            {actions.has('DELETE') && (
              <button type="button" className="btn btn--danger" onClick={() => setModal('delete')}>
                <Trash2 size={16} />
                Удалить
              </button>
            )}
          </div>
        </div>
      </div>

      <div className="ticket-layout">
        <div className="ticket-main">
          <section className="card">
            <header className="card__header">
              <h3 className="card__title">Описание проблемы</h3>
              <span className="card__note">создана {formatRelative(ticket.createdAt)}</span>
            </header>
            <div className="card__body">
              <p className="ticket-description">{ticket.description}</p>

              {ticket.equipment && (
                <p className="ticket-equipment">
                  <strong>Оборудование:</strong> {ticket.equipment}
                </p>
              )}

              {ticket.resolution && (
                <div className="resolution">
                  <span className="resolution__icon">
                    <CheckCircle2 size={16} />
                  </span>
                  <div>
                    <strong>Решение</strong>
                    <p>{ticket.resolution}</p>
                  </div>
                </div>
              )}
            </div>
          </section>

          <section className="card">
            <div className="tabs">
              <button
                type="button"
                className={`tab${tab === 'comments' ? ' tab--active' : ''}`}
                onClick={() => setTab('comments')}
              >
                <MessageSquare size={15} />
                Комментарии
                <span className="tab__counter">{comments.length}</span>
              </button>
              <button
                type="button"
                className={`tab${tab === 'history' ? ' tab--active' : ''}`}
                onClick={() => setTab('history')}
              >
                <History size={15} />
                История
                <span className="tab__counter">{history.length}</span>
              </button>
            </div>

            <div className="card__body">
              {tab === 'comments' ? (
                <>
                  {comments.length === 0 ? (
                    <p className="chart-empty">Комментариев пока нет — задайте вопрос или уточните детали</p>
                  ) : (
                    <ul className="comments">
                      {comments.map((comment) => (
                        <li key={comment.id} className={`comment${comment.internal ? ' comment--internal' : ''}`}>
                          <span className="avatar avatar--sm" style={{ background: avatarColor(comment.author.id) }}>
                            {initials(comment.author.fullName)}
                          </span>
                          <div className="comment__body">
                            <div className="comment__head">
                              <strong>{comment.author.fullName}</strong>
                              <RoleBadge role={comment.author.role} />
                              {comment.internal && (
                                <span className="badge badge--slate">
                                  <Lock size={11} /> внутренний
                                </span>
                              )}
                              <span className="comment__date" title={formatDateTime(comment.createdAt)}>
                                {formatRelative(comment.createdAt)}
                              </span>
                              {comment.editable && (
                                <button
                                  type="button"
                                  className="btn btn--icon btn--ghost comment__delete"
                                  onClick={() => void removeComment(comment.id)}
                                  aria-label="Удалить комментарий"
                                >
                                  <Trash2 size={14} />
                                </button>
                              )}
                            </div>
                            <p className="comment__text">{comment.text}</p>
                          </div>
                        </li>
                      ))}
                    </ul>
                  )}

                  {actions.has('COMMENT') && (
                    <form
                      className="comment-form"
                      onSubmit={(event) => {
                        event.preventDefault();
                        void submitComment();
                      }}
                    >
                      {user && (
                        <span className="avatar avatar--sm" style={{ background: avatarColor(user.id) }}>
                          {initials(user.fullName)}
                        </span>
                      )}
                      <div className="comment-form__field">
                        <textarea
                          className="textarea"
                          rows={3}
                          maxLength={2000}
                          placeholder="Напишите комментарий…"
                          value={commentText}
                          onChange={(event) => setCommentText(event.target.value)}
                        />
                        <div className="comment-form__actions">
                          {isStaff && (
                            <label className="checkbox">
                              <input
                                type="checkbox"
                                checked={internal}
                                onChange={(event) => setInternal(event.target.checked)}
                              />
                              <span>
                                <Lock size={13} /> Внутренний комментарий (не виден автору заявки)
                              </span>
                            </label>
                          )}
                          <button
                            type="submit"
                            className="btn btn--primary"
                            disabled={sendingComment || !commentText.trim()}
                          >
                            <Send size={15} />
                            Отправить
                          </button>
                        </div>
                      </div>
                    </form>
                  )}
                </>
              ) : (
                <ul className="timeline">
                  {history.map((event) => (
                    <li key={event.id} className="timeline__item">
                      <span className="timeline__dot" />
                      <div className="timeline__body">
                        <div className="timeline__head">
                          <strong>{event.typeTitle}</strong>
                          <span className="timeline__date" title={formatDateTime(event.createdAt)}>
                            {formatDateTime(event.createdAt)}
                          </span>
                        </div>
                        {event.message && <p className="timeline__message">{event.message}</p>}
                        {event.actor && (
                          <span className="timeline__actor">
                            <UserLink name={event.actor.fullName} id={event.actor.id} sub={event.actor.position} />
                          </span>
                        )}
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </section>
        </div>

        <aside className="ticket-side">
          <section className="card">
            <header className="card__header">
              <h3 className="card__title">Сведения</h3>
            </header>
            <div className="card__body">
              <dl className="meta-list">
                <div className="meta-list__row">
                  <dt>Статус</dt>
                  <dd>
                    <StatusBadge status={ticket.status} />
                  </dd>
                </div>
                <div className="meta-list__row">
                  <dt>Приоритет</dt>
                  <dd>
                    <PriorityBadge priority={ticket.priority} />
                  </dd>
                </div>
                <div className="meta-list__row">
                  <dt>Категория</dt>
                  <dd>
                    <CategoryTag name={ticket.category.name} color={ticket.category.color} />
                  </dd>
                </div>
                <div className="meta-list__row">
                  <dt>Автор</dt>
                  <dd>
                    <UserLink
                      name={ticket.author.fullName}
                      id={ticket.author.id}
                      sub={ticket.author.position ?? ticket.author.username}
                    />
                  </dd>
                </div>
                <div className="meta-list__row">
                  <dt>Исполнитель</dt>
                  <dd>
                    <UserLink name={ticket.assignee?.fullName} id={ticket.assignee?.id} sub={ticket.assignee?.position} />
                  </dd>
                </div>
                <div className="meta-list__row">
                  <dt>Кабинет</dt>
                  <dd>{ticket.room || '—'}</dd>
                </div>
                {ticket.building && (
                  <div className="meta-list__row">
                    <dt>Место</dt>
                    <dd>{ticket.building}</dd>
                  </div>
                )}
                <div className="meta-list__row">
                  <dt>
                    <CalendarClock size={14} /> Плановый срок
                  </dt>
                  <dd>
                    {ticket.dueDate ? (
                      <span className={ticket.overdue ? 'due due--overdue' : 'due'}>
                        {formatDate(ticket.dueDate)}
                        {due !== null && ticket.status !== 'CLOSED' && ticket.status !== 'REJECTED' && (
                          <small>
                            {due >= 0
                              ? `осталось ${due} ${plural(due, 'день', 'дня', 'дней')}`
                              : `просрочено на ${Math.abs(due)} ${plural(Math.abs(due), 'день', 'дня', 'дней')}`}
                          </small>
                        )}
                      </span>
                    ) : (
                      '—'
                    )}
                  </dd>
                </div>
                <div className="meta-list__row">
                  <dt>Создана</dt>
                  <dd>{formatDateTime(ticket.createdAt)}</dd>
                </div>
                <div className="meta-list__row">
                  <dt>Обновлена</dt>
                  <dd>{formatDateTime(ticket.updatedAt)}</dd>
                </div>
                {ticket.resolvedAt && (
                  <div className="meta-list__row">
                    <dt>Выполнена</dt>
                    <dd>{formatDateTime(ticket.resolvedAt)}</dd>
                  </div>
                )}
                {ticket.closedAt && (
                  <div className="meta-list__row">
                    <dt>Закрыта</dt>
                    <dd>{formatDateTime(ticket.closedAt)}</dd>
                  </div>
                )}
              </dl>
            </div>
          </section>
        </aside>
      </div>

      {/* ------------------------------------------------------------ модалки */}

      <Modal
        open={modal === 'status'}
        title="Изменение статуса"
        subtitle={`Заявка ${ticket.number} · текущий статус «${STATUS_META[ticket.status].label}»`}
        onClose={() => setModal(null)}
        busy={busy}
        footer={
          <>
            <button type="button" className="btn btn--secondary" onClick={() => setModal(null)} disabled={busy}>
              Отмена
            </button>
            <button type="button" className="btn btn--primary" onClick={() => void submitStatus()} disabled={busy}>
              Сохранить
            </button>
          </>
        }
      >
        <label className="field">
          <span className="field__label">Новый статус</span>
          <select
            className="select"
            value={statusTarget}
            onChange={(event) => setStatusTarget(event.target.value as TicketStatus)}
          >
            {allowedStatuses.length === 0 && <option value={ticket.status}>{STATUS_META[ticket.status].label}</option>}
            {allowedStatuses.map((status) => (
              <option key={status} value={status}>
                {STATUS_META[status].label} — {STATUS_META[status].hint}
              </option>
            ))}
          </select>
        </label>

        {statusTarget === 'RESOLVED' && (
          <label className="field">
            <span className="field__label">
              Что было сделано <span className="req">*</span>
            </span>
            <textarea
              className="textarea"
              rows={4}
              maxLength={2000}
              value={resolution}
              placeholder="Например: заменён картридж, настроен сетевой доступ, переустановлен драйвер…"
              onChange={(event) => setResolution(event.target.value)}
              required
            />
            <span className="field__hint">Текст попадёт в карточку заявки и в историю</span>
          </label>
        )}

        {statusTarget === 'REJECTED' && (
          <label className="field">
            <span className="field__label">Причина отклонения</span>
            <textarea
              className="textarea"
              rows={3}
              maxLength={2000}
              value={resolution}
              onChange={(event) => setResolution(event.target.value)}
              placeholder="Например: дубликат заявки HD-00012"
            />
          </label>
        )}

        <label className="field">
          <span className="field__label">Комментарий (необязательно)</span>
          <textarea
            className="textarea"
            rows={2}
            maxLength={2000}
            value={statusComment}
            onChange={(event) => setStatusComment(event.target.value)}
            placeholder="Пояснение для автора заявки"
          />
        </label>
      </Modal>

      <Modal
        open={modal === 'assign'}
        title="Назначение исполнителя"
        subtitle={`Заявка ${ticket.number}`}
        onClose={() => setModal(null)}
        busy={busy}
        width="sm"
        footer={
          <>
            <button type="button" className="btn btn--secondary" onClick={() => setModal(null)} disabled={busy}>
              Отмена
            </button>
            <button type="button" className="btn btn--primary" onClick={() => void submitAssign()} disabled={busy}>
              Назначить
            </button>
          </>
        }
      >
        <label className="field">
          <span className="field__label">IT-специалист</span>
          <select
            className="select"
            value={assigneeId}
            onChange={(event) => setAssigneeId(event.target.value === '' ? '' : Number(event.target.value))}
          >
            <option value="">— не назначен (в общей очереди) —</option>
            {assignees.map((person) => (
              <option key={person.id} value={person.id}>
                {person.fullName}
                {person.position ? ` · ${person.position}` : ''}
              </option>
            ))}
          </select>
          <span className="field__hint">
            При назначении новая заявка автоматически переходит в статус «В работе»
          </span>
        </label>
      </Modal>

      <Modal
        open={modal === 'priority'}
        title="Изменение приоритета"
        subtitle={`Заявка ${ticket.number}`}
        onClose={() => setModal(null)}
        busy={busy}
        width="sm"
        footer={
          <>
            <button type="button" className="btn btn--secondary" onClick={() => setModal(null)} disabled={busy}>
              Отмена
            </button>
            <button type="button" className="btn btn--primary" onClick={() => void submitPriority()} disabled={busy}>
              Сохранить
            </button>
          </>
        }
      >
        <div className="field">
          <span className="field__label">Приоритет</span>
          <div className="radio-cards">
            {PRIORITY_ORDER.map((priority) => (
              <label
                key={priority}
                className={`radio-card${priorityTarget === priority ? ' radio-card--active' : ''}`}
              >
                <input
                  type="radio"
                  name="priority"
                  value={priority}
                  checked={priorityTarget === priority}
                  onChange={() => setPriorityTarget(priority)}
                />
                <span className="radio-card__title">{PRIORITY_META[priority].label}</span>
              </label>
            ))}
          </div>
        </div>
      </Modal>

      <ConfirmDialog
        open={modal === 'delete'}
        title="Удалить заявку?"
        message={`Заявка ${ticket.number} «${ticket.title}» и вся её история будут удалены безвозвратно.`}
        confirmText="Удалить"
        busy={busy}
        onConfirm={() => void submitDelete()}
        onCancel={() => setModal(null)}
      />
    </div>
  );
}


