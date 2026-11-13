import type { ReactElement } from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import type { Role } from '../types';
import { useAuth } from '../store/AuthContext';
import { ForbiddenPage } from '../pages/NotFoundPage';

interface Props {
  /** Если заданы — доступ разрешён только перечисленным ролям. */
  roles?: Role[];
  children?: ReactElement;
}

/**
 * Защита маршрутов: без входа перенаправляет на /login (с возвратом обратно),
 * при нехватке прав показывает страницу 403.
 */
export default function ProtectedRoute({ roles, children }: Props) {
  const { user } = useAuth();
  const location = useLocation();

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />;
  }

  if (roles && roles.length > 0 && !roles.includes(user.role)) {
    return children ? <ForbiddenPage /> : <ForbiddenPage />;
  }

  return children ?? <Outlet />;
}


