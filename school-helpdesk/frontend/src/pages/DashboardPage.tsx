import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  AlarmClock,
  AlertTriangle,
  Archive,
  CheckCircle2,
  Clock,
  Inbox,
  PauseCircle,
  PlusCircle,
  RefreshCw,
  UserX,
  Wrench,
} from 'lucide-react';
import { dashboardApi } from '../api/endpoints';
import { ApiError } from '../api/client';
import { useAuth } from '../store/AuthContext';
import { useToast } from '../store/ToastContext';
import type { DashboardStats, Ticket } from '../types';
import StatCard from '../components/StatCard';
import { BarChart, DonutChart, TrendChart } from '../components/Charts';
import { PageLoader } from '../components/Spinner';
import EmptyState from '../components/EmptyState';
import { CommentsCount, PriorityBadge, StatusBadge } from '../components/Badges';
import { PRIORITY_CHART_COLOR, STATUS_CHART_COLOR } from '../utils/dict';
import { formatHours, formatRelative, plural } from '../utils/format';

export default function DashboardPage() {
  const { user, isStaff } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();

  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setStats(await dashboardApi.stats(14));
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : 'Не удалось загрузить статистику');
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => {
    document.title = 'Мониторинг — School HelpDesk';
    void load();
  }, [load]);

  if (loading || !stats) {
    return <PageLoader text="Собираем статистику…" />;
  }

  const firstName = user?.fullName?.split(' ')[0] ?? '';
  const hour = new Date().getHours();
  const greeting = hour < 6 ? 'Доброй ночи' : hour < 12 ? 'Доброе утро' : hour < 18 ? 'Добрый день' : 'Добрый вечер';

  const openTickets = (extra: Record<string, string | number | boolean>) => {
    const params = new URLSearchParams();
    Object.entries(extra).forEach(([key, value]) => params.set(key, String(value)));
    navigate(`/tickets?${params.toString()}`);
  };

  return (
    <div className="dashboard">
      <div className="page-header">
        <div>
          <h2 className="page-header__title">
            {greeting}
            {firstName ? `, ${firstName}` : ''}!
          </h2>
          <p className="page-header__subtitle">
            {stats.scopedToUser
              ? 'Статистика по вашим заявкам в службу технической поддержки'
              : 'Сводка по всем заявкам службы технической поддержки школы'}
          </p>
        </div>
        <div className="page-header__actions">
          <button type="button" className="btn btn--secondary" onClick={() => void load()}>
            <RefreshCw size={16} />
            Обновить
          </button>
          <Link className="btn btn--primary" to="/tickets/new">
            <PlusCircle size={16} />
            Создать заявку
          </Link>
        </div>
      </div>

      <section className="stat-grid">
        <StatCard
          label="Новые"
          value={stats.newCount}
          tone="blue"
          icon={<Inbox size={18} />}
          hint="Ожидают назначения исполнителя"
          onClick={() => openTickets({ status: 'NEW' })}
        />
        <StatCard
          label="В работе"
          value={stats.inProgress}
          tone="amber"
          icon={<Wrench size={18} />}
          hint="Выполняются специалистами"
          onClick={() => openTickets({ status: 'IN_PROGRESS' })}
        />
        <StatCard
          label="Отложено"
          value={stats.onHold}
          tone="slate"
          icon={<PauseCircle size={18} />}
          hint="Ожидание запчастей или ответа"
          onClick={() => openTickets({ status: 'ON_HOLD' })}
        />
        <StatCard
          label="Выполнено"
          value={stats.resolved}
          tone="green"
          icon={<CheckCircle2 size={18} />}
          hint="Ожидают подтверждения автора"
          onClick={() => openTickets({ status: 'RESOLVED' })}
        />
        <StatCard
          label="Закрыто"
          value={stats.closed}
          tone="default"
          icon={<Archive size={18} />}
          hint="Завершённые заявки"
          onClick={() => openTickets({ status: 'CLOSED' })}
        />
        {isStaff && (
          <StatCard
            label="Без исполнителя"
            value={stats.unassigned}
            tone="red"
            icon={<UserX size={18} />}
            hint="Активные заявки, которым нужен исполнитель"
            onClick={() => openTickets({ unassigned: true })}
          />
        )}
        <StatCard
          label="Просрочено"
          value={stats.overdue}
          tone="red"
          icon={<AlarmClock size={18} />}
          hint="Плановый срок выполнения прошёл"
          onClick={() => openTickets({ overdue: true })}
        />
        <StatCard
          label="Критических"
          value={stats.critical}
          tone="amber"
          icon={<AlertTriangle size={18} />}
          hint="Активные заявки с критическим приоритетом"
          onClick={() => openTickets({ priority: 'CRITICAL', activeOnly: true })}
        />
      </section>

      <section className="dash-grid">
        <div className="card">
          <header className="card__header">
            <h3 className="card__title">Динамика за 14 дней</h3>
            <span className="card__note">
              сегодня: {stats.createdToday} {plural(stats.createdToday, 'новая', 'новые', 'новых')}, выполнено{' '}
              {stats.resolvedToday}
            </span>
          </header>
          <div className="card__body">
            <TrendChart data={stats.trend} />
          </div>
        </div>

        <div className="card">
          <header className="card__header">
            <h3 className="card__title">Статусы заявок</h3>
            <span className="card__note">
              всего {stats.total} · активных {stats.active}
            </span>
          </header>
          <div className="card__body">
            <DonutChart
              data={stats.byStatus}
              colors={STATUS_CHART_COLOR}
              centerLabel="заявок"
              onSelect={(item) => openTickets({ status: item.code })}
            />
          </div>
        </div>
      </section>

      <section className="dash-grid dash-grid--3">
        <div className="card">
          <header className="card__header">
            <h3 className="card__title">По категориям</h3>
          </header>
          <div className="card__body">
            <BarChart data={stats.byCategory} emptyText="Заявок пока нет" />
          </div>
        </div>

        <div className="card">
          <header className="card__header">
            <h3 className="card__title">Активные по приоритету</h3>
          </header>
          <div className="card__body">
            <BarChart
              data={stats.byPriority.map((item) => ({ ...item, color: PRIORITY_CHART_COLOR[item.code as keyof typeof PRIORITY_CHART_COLOR] }))}
              emptyText="Активных заявок нет"
            />
            <div className="metric-row">
              <span className="metric">
                <Clock size={15} />
                Среднее время решения
                <strong>{formatHours(stats.avgResolutionHours)}</strong>
              </span>
            </div>
          </div>
        </div>

        {isStaff ? (
          <div className="card">
            <header className="card__header">
              <h3 className="card__title">Загрузка специалистов</h3>
            </header>
            <div className="card__body">
              {stats.workload.length === 0 ? (
                <p className="chart-empty">Исполнители ещё не назначались</p>
              ) : (
                <ul className="workload">
                  {stats.workload.map((item) => (
                    <li key={item.userId} className="workload__item">
                      <button type="button" className="workload__name" onClick={() => openTickets({ assigneeId: item.userId, activeOnly: true })}>
                        {item.fullName}
                      </button>
                      <span className="workload__counts">
                        <span className="badge badge--amber">{item.active} в работе</span>
                        <span className="badge badge--green">{item.done} выполнено</span>
                      </span>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>
        ) : (
          <div className="card">
            <header className="card__header">
              <h3 className="card__title">Чаще всего проблемы в</h3>
            </header>
            <div className="card__body">
              <BarChart data={stats.topRooms} emptyText="Нет данных по кабинетам" />
            </div>
          </div>
        )}
      </section>

      <section className="card">
        <header className="card__header">
          <h3 className="card__title">Последние заявки</h3>
          <Link className="card__link" to="/tickets">
            Все заявки →
          </Link>
        </header>
        <div className="card__body card__body--flush">
          {stats.recent.length === 0 ? (
            <EmptyState
              title="Заявок пока нет"
              description="Создайте первую заявку — и она появится здесь"
              action={
                <Link className="btn btn--primary" to="/tickets/new">
                  <PlusCircle size={16} />
                  Создать заявку
                </Link>
              }
            />
          ) : (
            <ul className="recent">
              {stats.recent.map((ticket: Ticket) => (
                <li key={ticket.id}>
                  <Link to={`/tickets/${ticket.id}`} className="recent__item">
                    <span className="recent__number">{ticket.number}</span>
                    <span className="recent__title">{ticket.title}</span>
                    <span className="recent__meta">
                      <PriorityBadge priority={ticket.priority} />
                      <StatusBadge status={ticket.status} />
                      <CommentsCount count={ticket.commentCount} />
                    </span>
                    <span className="recent__when">{formatRelative(ticket.createdAt)}</span>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </div>
      </section>
    </div>
  );
}


