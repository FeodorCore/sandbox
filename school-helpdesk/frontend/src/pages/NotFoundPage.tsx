import { Link } from 'react-router-dom';
import { Compass, ShieldAlert } from 'lucide-react';
import { useAuth } from '../store/AuthContext';

/** 404 — страница не найдена. */
export default function NotFoundPage() {
  return (
    <div className="state-page">
      <Compass size={44} strokeWidth={1.5} />
      <h1>Страница не найдена</h1>
      <p>Возможно, она была удалена или указан неверный адрес.</p>
      <Link className="btn btn--primary" to="/">
        На главную
      </Link>
    </div>
  );
}

/** 403 — недостаточно прав. */
export function ForbiddenPage() {
  const { user } = useAuth();
  return (
    <div className="state-page">
      <ShieldAlert size={44} strokeWidth={1.5} />
      <h1>Доступ ограничен</h1>
      <p>
        Раздел доступен только администратору системы.
        {user ? ` Ваша роль: ${user.roleTitle}.` : ''}
      </p>
      <Link className="btn btn--primary" to="/">
        Вернуться на главную
      </Link>
    </div>
  );
}


