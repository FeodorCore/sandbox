import type { ReactNode } from 'react';

interface Props {
  label: string;
  value: ReactNode;
  hint?: string;
  icon?: ReactNode;
  tone?: 'default' | 'blue' | 'amber' | 'green' | 'red' | 'slate';
  onClick?: () => void;
  active?: boolean;
}

/** Карточка показателя для панели мониторинга. */
export default function StatCard({ label, value, hint, icon, tone = 'default', onClick, active }: Props) {
  const Tag = onClick ? 'button' : 'div';
  return (
    <Tag
      type={onClick ? 'button' : undefined}
      className={`stat-card stat-card--${tone}${onClick ? ' stat-card--clickable' : ''}${active ? ' stat-card--active' : ''}`}
      onClick={onClick}
      title={hint}
    >
      {icon && <span className={`stat-card__icon stat-card__icon--${tone}`}>{icon}</span>}
      <span className="stat-card__body">
        <span className="stat-card__value">{value}</span>
        <span className="stat-card__label">{label}</span>
        {hint && <span className="stat-card__hint">{hint}</span>}
      </span>
    </Tag>
  );
}


