import { Loader2 } from 'lucide-react';

interface SpinnerProps {
  size?: number;
  label?: string;
}

export function Spinner({ size = 20, label }: SpinnerProps) {
  return (
    <span className="spinner" role="status" aria-label={label ?? 'Загрузка'}>
      <Loader2 size={size} className="spin" />
    </span>
  );
}

export function FullScreenLoader({ text = 'Загрузка…' }: { text?: string }) {
  return (
    <div className="fullscreen-loader">
      <Loader2 size={34} className="spin" />
      <p>{text}</p>
    </div>
  );
}

export function PageLoader({ text = 'Загрузка данных…' }: { text?: string }) {
  return (
    <div className="page-loader">
      <Loader2 size={26} className="spin" />
      <span>{text}</span>
    </div>
  );
}


