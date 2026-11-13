import { useMemo, useState } from 'react';
import type { DayCount, NameCount } from '../types';
import { formatShortDate } from '../utils/format';

/* ------------------------------------------------------------------ столбцы */

interface BarChartProps {
  data: NameCount[];
  /** максимум для масштаба; по умолчанию — максимальное значение */
  max?: number;
  emptyText?: string;
  onSelect?: (item: NameCount) => void;
}

/** Горизонтальная столбчатая диаграмма (категории, кабинеты). */
export function BarChart({ data, max, emptyText = 'Нет данных', onSelect }: BarChartProps) {
  const items = data.filter((item) => item.count > 0);
  if (items.length === 0) {
    return <p className="chart-empty">{emptyText}</p>;
  }
  const maxValue = max ?? Math.max(...items.map((item) => item.count), 1);

  return (
    <div className="bar-chart">
      {items.map((item) => (
        <button
          type="button"
          key={item.code}
          className={`bar-chart__row${onSelect ? ' bar-chart__row--clickable' : ''}`}
          onClick={() => onSelect?.(item)}
          disabled={!onSelect}
        >
          <span className="bar-chart__label" title={item.name}>
            {item.name}
          </span>
          <span className="bar-chart__track">
            <span
              className="bar-chart__fill"
              style={{
                width: `${Math.max((item.count / maxValue) * 100, 4)}%`,
                background: item.color ?? '#2563eb',
              }}
            />
          </span>
          <span className="bar-chart__value">{item.count}</span>
        </button>
      ))}
    </div>
  );
}

/* -------------------------------------------------------------- кольцевая */

interface DonutProps {
  data: NameCount[];
  colors?: Record<string, string>;
  centerLabel?: string;
  onSelect?: (item: NameCount) => void;
}

/** Кольцевая диаграмма с легендой (распределение по статусам). */
export function DonutChart({ data, colors, centerLabel, onSelect }: DonutProps) {
  const total = data.reduce((sum, item) => sum + item.count, 0);
  const radius = 54;
  const circumference = 2 * Math.PI * radius;
  let offset = 0;

  if (total === 0) {
    return <p className="chart-empty">Нет данных для отображения</p>;
  }

  return (
    <div className="donut">
      <svg viewBox="0 0 140 140" className="donut__svg" role="img" aria-label="Распределение заявок">
        <circle cx="70" cy="70" r={radius} fill="none" stroke="#e2e8f0" strokeWidth="18" />
        {data.map((item) => {
          if (item.count === 0) return null;
          const length = (item.count / total) * circumference;
          const dash = `${length} ${circumference - length}`;
          const element = (
            <circle
              key={item.code}
              cx="70"
              cy="70"
              r={radius}
              fill="none"
              stroke={colors?.[item.code] ?? item.color ?? '#2563eb'}
              strokeWidth="18"
              strokeDasharray={dash}
              strokeDashoffset={-offset}
              transform="rotate(-90 70 70)"
              className={onSelect ? 'donut__segment donut__segment--clickable' : 'donut__segment'}
              onClick={() => onSelect?.(item)}
            >
              <title>{`${item.name}: ${item.count}`}</title>
            </circle>
          );
          offset += length;
          return element;
        })}
        <text x="70" y="66" textAnchor="middle" className="donut__value">
          {total}
        </text>
        <text x="70" y="84" textAnchor="middle" className="donut__caption">
          {centerLabel ?? 'всего'}
        </text>
      </svg>

      <ul className="donut__legend">
        {data.map((item) => (
          <li key={item.code}>
            <button
              type="button"
              className="donut__legend-item"
              onClick={() => onSelect?.(item)}
              disabled={!onSelect}
            >
              <span className="dot" style={{ background: colors?.[item.code] ?? item.color ?? '#94a3b8' }} />
              <span className="donut__legend-name">{item.name}</span>
              <span className="donut__legend-value">{item.count}</span>
            </button>
          </li>
        ))}
      </ul>
    </div>
  );
}

/* ---------------------------------------------------------------- динамика */

interface TrendProps {
  data: DayCount[];
}

/** График «создано / выполнено» по дням. */
export function TrendChart({ data }: TrendProps) {
  const [hover, setHover] = useState<number | null>(null);

  const { width, height, padding, max, createdPath, resolvedPath, createdArea, points } = useMemo(() => {
    const w = 720;
    const h = 220;
    const pad = { top: 16, right: 12, bottom: 26, left: 30 };
    const maxValue = Math.max(1, ...data.map((d) => Math.max(d.created, d.resolved)));
    const innerW = w - pad.left - pad.right;
    const innerH = h - pad.top - pad.bottom;
    const stepX = data.length > 1 ? innerW / (data.length - 1) : innerW;

    const x = (index: number) => pad.left + index * stepX;
    const y = (value: number) => pad.top + innerH - (value / maxValue) * innerH;

    const line = (pick: (d: DayCount) => number) =>
      data.map((d, i) => `${i === 0 ? 'M' : 'L'}${x(i).toFixed(1)},${y(pick(d)).toFixed(1)}`).join(' ');

    const area = `${line((d) => d.created)} L${x(data.length - 1).toFixed(1)},${(pad.top + innerH).toFixed(1)} L${x(0).toFixed(1)},${(pad.top + innerH).toFixed(1)} Z`;

    return {
      width: w,
      height: h,
      padding: pad,
      max: maxValue,
      createdPath: line((d) => d.created),
      resolvedPath: line((d) => d.resolved),
      createdArea: area,
      points: data.map((d, i) => ({ ...d, cx: x(i), cy: y(d.created) })),
    };
  }, [data]);

  if (data.length === 0) return <p className="chart-empty">Нет данных</p>;

  const gridLines = 4;
  const labelEvery = Math.max(1, Math.ceil(data.length / 8));

  return (
    <div className="trend">
      <svg
        viewBox={`0 0 ${width} ${height}`}
        className="trend__svg"
        onMouseLeave={() => setHover(null)}
        role="img"
        aria-label="Динамика заявок по дням"
      >
        {Array.from({ length: gridLines + 1 }).map((_, i) => {
          const value = (max / gridLines) * i;
          const y = padding.top + (height - padding.top - padding.bottom) * (1 - i / gridLines);
          return (
            <g key={i}>
              <line x1={padding.left} x2={width - padding.right} y1={y} y2={y} className="trend__grid" />
              <text x={padding.left - 6} y={y + 3} textAnchor="end" className="trend__tick">
                {Math.round(value)}
              </text>
            </g>
          );
        })}

        <path d={createdArea} className="trend__area" />
        <path d={resolvedPath} className="trend__line trend__line--resolved" />
        <path d={createdPath} className="trend__line trend__line--created" />

        {points.map((point, index) => (
          <g key={point.date}>
            <rect
              x={point.cx - (width - padding.left - padding.right) / (data.length * 2)}
              y={padding.top}
              width={(width - padding.left - padding.right) / data.length}
              height={height - padding.top - padding.bottom}
              fill="transparent"
              onMouseEnter={() => setHover(index)}
            />
            {index % labelEvery === 0 && (
              <text x={point.cx} y={height - 8} textAnchor="middle" className="trend__tick">
                {formatShortDate(point.date)}
              </text>
            )}
            {hover === index && (
              <>
                <line x1={point.cx} x2={point.cx} y1={padding.top} y2={height - padding.bottom} className="trend__cursor" />
                <circle cx={point.cx} cy={point.cy} r="4" className="trend__dot" />
                <circle
                  cx={point.cx}
                  cy={padding.top + (height - padding.top - padding.bottom) * (1 - point.resolved / max)}
                  r="4"
                  className="trend__dot trend__dot--resolved"
                />
              </>
            )}
          </g>
        ))}
      </svg>

      <div className="trend__footer">
        <span className="trend__legend">
          <i className="trend__swatch trend__swatch--created" /> создано
        </span>
        <span className="trend__legend">
          <i className="trend__swatch trend__swatch--resolved" /> выполнено
        </span>
        {hover !== null && (
          <span className="trend__tooltip">
            {formatShortDate(data[hover].date)}: создано {data[hover].created}, выполнено {data[hover].resolved}
          </span>
        )}
      </div>
    </div>
  );
}


