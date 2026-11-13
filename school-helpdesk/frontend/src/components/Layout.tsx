import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { useEffect, useState } from 'react';
import {
  FolderTree,
  LayoutDashboard,
  ListChecks,
  LogOut,
  Menu,
  PlusCircle,
  User as UserIcon,
  Users,
  X,
} from 'lucide-react';
import { useAuth } from '../store/AuthContext';
import { initials, avatarColor } from '../utils/format';
import { ROLE_META } from '../utils/dict';

interface NavItem {
  to: string;
  label: string;
  icon: typeof LayoutDashboard;
  roles?: string[];
  end?: boolean;
}

const NAV_ITEMS: NavItem[] = [
  { to: '/', label: 'Мониторинг', icon: LayoutDashboard, end: true },
  { to: '/tickets', label: 'Заявки', icon: ListChecks },
  { to: '/tickets/new', label: 'Новая заявка', icon: PlusCircle },
  { to: '/users', label: 'Пользователи', icon: Users, roles: ['ADMIN'] },
  { to: '/categories', label: 'Категории', icon: FolderTree, roles: ['ADMIN'] },
  { to: '/profile', label: 'Профиль', icon: UserIcon },
];

const TITLES: Record<string, string> = {
  '/': 'Панель мониторинга',
  '/tickets': 'Заявки',
  '/tickets/new': 'Новая заявка',
  '/users': 'Пользователи',
  '/categories': 'Категории заявок',
  '/profile': 'Мой профиль',
};

function resolveTitle(pathname: string): string {
  if (pathname.startsWith('/tickets/new')) return 'Новая заявка';
  if (/^\/tickets\/\d+\/edit$/.test(pathname)) return 'Редактирование заявки';
  if (/^\/tickets\/\d+$/.test(pathname)) return 'Карточка заявки';
  if (pathname.startsWith('/tickets')) return 'Заявки';
  return TITLES[pathname] ?? 'School HelpDesk';
}

export default function Layout() {
  const { user, logout } = useAuth();
  const location = useLocation();
  const [menuOpen, setMenuOpen] = useState(false);

  useEffect(() => {
    setMenuOpen(false);
  }, [location.pathname]);

  const items = NAV_ITEMS.filter((item) => !item.roles || (user && item.roles.includes(user.role)));
  const roleMeta = user ? ROLE_META[user.role] : null;

  return (
    <div className="app-shell">
      {menuOpen && <div className="sidebar-backdrop" onClick={() => setMenuOpen(false)} />}

      <aside className={`sidebar${menuOpen ? ' sidebar--open' : ''}`}>
        <div className="sidebar__brand">
          <div className="sidebar__logo" aria-hidden="true">
            <svg viewBox="0 0 32 32" width="30" height="30">
              <rect width="32" height="32" rx="8" fill="#2563eb" />
              <path
                d="M9 12.5A3.5 3.5 0 0 1 12.5 9h7A3.5 3.5 0 0 1 23 12.5v3a3.5 3.5 0 0 1-3.5 3.5H17l-4 3v-3h-.5A3.5 3.5 0 0 1 9 15.5z"
                fill="#fff"
              />
              <circle cx="13" cy="14" r="1.2" fill="#2563eb" />
              <circle cx="16" cy="14" r="1.2" fill="#2563eb" />
              <circle cx="19" cy="14" r="1.2" fill="#2563eb" />
            </svg>
          </div>
          <div className="sidebar__title">
            <strong>School HelpDesk</strong>
            <span>служба технической поддержки</span>
          </div>
          <button
            type="button"
            className="sidebar__close"
            onClick={() => setMenuOpen(false)}
            aria-label="Закрыть меню"
          >
            <X size={18} />
          </button>
        </div>

        <nav className="sidebar__nav">
          {items.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) => `nav-link${isActive ? ' nav-link--active' : ''}`}
            >
              <item.icon size={18} />
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>

        {user && (
          <div className="sidebar__footer">
            <div className="user-chip">
              <span className="avatar" style={{ background: avatarColor(user.id) }}>
                {initials(user.fullName)}
              </span>
              <span className="user-chip__info">
                <strong>{user.fullName}</strong>
                <small>{roleMeta?.label ?? user.roleTitle}</small>
              </span>
            </div>
            <button type="button" className="btn btn--ghost btn--block" onClick={logout}>
              <LogOut size={16} />
              Выйти
            </button>
          </div>
        )}
      </aside>

      <div className="main">
        <header className="topbar">
          <button
            type="button"
            className="topbar__menu"
            onClick={() => setMenuOpen(true)}
            aria-label="Открыть меню"
          >
            <Menu size={20} />
          </button>
          <h1 className="topbar__title">{resolveTitle(location.pathname)}</h1>
          <div className="topbar__right">
            {user && (
              <span className={`badge badge--${roleMeta?.tone ?? 'blue'} topbar__role`}>
                {roleMeta?.label ?? user.roleTitle}
              </span>
            )}
          </div>
        </header>

        <main className="content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}


