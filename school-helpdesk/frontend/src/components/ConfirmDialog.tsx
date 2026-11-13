import { AlertTriangle } from 'lucide-react';
import Modal from './Modal';

interface Props {
  open: boolean;
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  danger?: boolean;
  busy?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}

/** Диалог подтверждения действия. */
export default function ConfirmDialog({
  open,
  title,
  message,
  confirmText = 'Подтвердить',
  cancelText = 'Отмена',
  danger = true,
  busy = false,
  onConfirm,
  onCancel,
}: Props) {
  return (
    <Modal
      open={open}
      title={title}
      onClose={onCancel}
      busy={busy}
      width="sm"
      footer={
        <>
          <button type="button" className="btn btn--secondary" onClick={onCancel} disabled={busy}>
            {cancelText}
          </button>
          <button
            type="button"
            className={`btn ${danger ? 'btn--danger' : 'btn--primary'}`}
            onClick={onConfirm}
            disabled={busy}
          >
            {confirmText}
          </button>
        </>
      }
    >
      <div className="confirm">
        <span className={`confirm__icon${danger ? ' confirm__icon--danger' : ''}`}>
          <AlertTriangle size={20} />
        </span>
        <p>{message}</p>
      </div>
    </Modal>
  );
}


