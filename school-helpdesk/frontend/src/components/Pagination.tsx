import { ChevronLeft, ChevronRight } from 'lucide-react';
import type { Page } from '../types';
import { plural } from '../utils/format';

interface Props<T> {
  page: Page<T>;
  onChange: (page: number) => void;
  itemWord?: [string, string, string];
}

/** Пагинация с подписью общего количества записей. */
export default function Pagination<T>({ page, onChange, itemWord = ['запись', 'записи', 'записей'] }: Props<T>) {
  const { page: current, totalPages, totalElements } = page;
  if (totalElements === 0) return null;

  const windowSize = 2;
  const pages: number[] = [];
  for (let i = Math.max(0, current - windowSize); i <= Math.min(totalPages - 1, current + windowSize); i += 1) {
    pages.push(i);
  }

  const from = current * page.size + 1;
  const to = Math.min((current + 1) * page.size, totalElements);

  return (
    <div className="pagination">
      <span className="pagination__info">
        {from}–{to} из {totalElements} ({totalElements} {plural(totalElements, ...itemWord)})
      </span>
      <div className="pagination__controls">
        <button
          type="button"
          className="btn btn--sm btn--ghost"
          disabled={current === 0}
          onClick={() => onChange(current - 1)}
          aria-label="Предыдущая страница"
        >
          <ChevronLeft size={16} />
        </button>
        {pages[0] > 0 && <span className="pagination__gap">…</span>}
        {pages.map((p) => (
          <button
            key={p}
            type="button"
            className={`btn btn--sm${p === current ? ' btn--primary' : ' btn--ghost'}`}
            onClick={() => onChange(p)}
          >
            {p + 1}
          </button>
        ))}
        {pages[pages.length - 1] < totalPages - 1 && <span className="pagination__gap">…</span>}
        <button
          type="button"
          className="btn btn--sm btn--ghost"
          disabled={current >= totalPages - 1}
          onClick={() => onChange(current + 1)}
          aria-label="Следующая страница"
        >
          <ChevronRight size={16} />
        </button>
      </div>
    </div>
  );
}


