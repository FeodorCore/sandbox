import type { ReactNode } from 'react';
import { Inbox } from 'lucide-react';

interface Props {
  title?: string;
  description?: string;
  icon?: ReactNode;
  action?: ReactNode;
}

/** Заглушка для пустых списков и состояний «ничего не найдено». */
export default function EmptyState({
  title = 'Ничего не найдено',
  description,
  icon,
  action,
}: Props) {
  return (
    <div className="empty-state">
      <span className="empty-state__icon">{icon ?? <Inbox size={28} strokeWidth={1.5} />}</span>
      <h3>{title}</h3>
      {description && <p>{description}</p>}
      {action}
    </div>
  );
}


