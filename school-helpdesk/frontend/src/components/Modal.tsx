import { useEffect } from 'react';
import type { ReactNode } from 'react';
import { X } from 'lucide-react';
import { Spinner } from './Spinner';

interface ModalProps {
  open: boolean;
  title: string;
  subtitle?: string;
  onClose: () => void;
  children: ReactNode;
  footer?: ReactNode;
  width?: 'sm' | 'md' | 'lg';
  busy?: boolean;
}

/** Модальное окно с блокировкой прокрутки фона и закрытием по Escape. */
export default function Modal({
  open,
  title,
  subtitle,
  onClose,
  children,
  footer,
  width = 'md',
  busy = false,
}: ModalProps) {
  useEffect(() => {
    if (!open) return undefined;
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && !busy) onClose();
    };
    document.addEventListener('keydown', onKey);
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', onKey);
      document.body.style.overflow = '';
    };
  }, [open, onClose, busy]);

  if (!open) return null;

  return (
    <div className="modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && !busy && onClose()}>
      <div className={`modal modal--${width}`} role="dialog" aria-modal="true" aria-label={title}>
        <header className="modal__header">
          <div>
            <h2>{title}</h2>
            {subtitle && <p>{subtitle}</p>}
          </div>
          <button type="button" className="btn btn--icon btn--ghost" onClick={onClose} aria-label="Закрыть" disabled={busy}>
            <X size={18} />
          </button>
        </header>
        <div className="modal__body">{children}</div>
        {footer && <footer className="modal__footer">{footer}</footer>}
        {busy && (
          <div className="modal__busy">
            <Spinner size={26} />
          </div>
        )}
      </div>
    </div>
  );
}


