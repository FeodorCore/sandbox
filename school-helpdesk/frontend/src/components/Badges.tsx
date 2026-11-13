import { Link } from 'react-router-dom';
import { MessageSquare } from 'lucide-react';
import type { Priority, Role, TicketStatus } from '../types';
import { PRIORITY_META, ROLE_META, STATUS_META } from '../utils/dict';
import { avatarColor, initials } from '../utils/format';

export function StatusBadge({ status, withDot = true }: { status: TicketStatus; withDot?: boolean }) {
  const meta = STATUS_META[status];
  if (!meta) return <span className="badge">{status}</span>;
  return (
    <span className={`badge badge--${meta.tone}`} title={meta.hint}>
      {withDot && <span className="badge__dot" />}
      {meta.label}
    </span>
  );
}

export function PriorityBadge({ priority }: { priority: Priority }) {
  const meta = PRIORITY_META[priority];
  if (!meta) return <span className="badge">{priority}</span>;
  return (
    <span className={`prio prio--${priority.toLowerCase()}`}>
      <span className="prio__bars" aria-hidden="true">
        <i />
        <i />
        <i />
      </span>
      {meta.label}
    </span>
  );
}

export function RoleBadge({ role }: { role: Role }) {
  const meta = ROLE_META[role];
  return <span className={`badge badge--${meta?.tone ?? 'blue'}`}>{meta?.label ?? role}</span>;
}

export function CategoryTag({ name, color }: { name: string; color?: string | null }) {
  return (
    <span className="cat-tag">
      <span className="cat-tag__dot" style={{ background: color ?? '#94a3b8' }} />
      {name}
    </span>
  );
}

export function UserLink({
  name,
  id,
  sub,
  muted,
}: {
  name?: string | null;
  id?: number | null;
  sub?: string | null;
  muted?: string;
}) {
  if (!name) {
    return <span className="user-inline user-inline--empty">{muted ?? 'не назначен'}</span>;
  }
  return (
    <span className="user-inline" title={sub ?? undefined}>
      <span className="avatar avatar--sm" style={{ background: avatarColor(id ?? name) }}>
        {initials(name)}
      </span>
      <span className="user-inline__text">
        <span>{name}</span>
        {sub && <small>{sub}</small>}
      </span>
    </span>
  );
}

export function CommentsCount({ count }: { count: number }) {
  return (
    <span className="comments-count" title={`Комментариев: ${count}`}>
      <MessageSquare size={14} />
      {count}
    </span>
  );
}

export function TicketNumberLink({ id, number: num }: { id: number; number: string }) {
  return (
    <Link to={`/tickets/${id}`} className="ticket-number">
      {num}
    </Link>
  );
}


