import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Eye, EyeOff, KeyRound, LogIn, ShieldCheck, User as UserIcon } from 'lucide-react';
import { useAuth } from '../store/AuthContext';
import { ApiError } from '../api/client';
import { Spinner } from '../components/Spinner';

interface LocationState {
  from?: string;
}

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const state = (location.state ?? {}) as LocationState;

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    document.title = 'Вход — School HelpDesk';
  }, []);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (busy) return;
    setError(null);
    setBusy(true);
    try {
      const user = await login(username.trim(), password);
      navigate(state.from && state.from !== '/login' ? state.from : '/', { replace: true });
      void user;
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Не удалось войти в систему');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="login">
      <div className="login__bg" aria-hidden="true" />
      <form className="login__card" onSubmit={handleSubmit} noValidate>
        <div className="login__brand">
          <svg viewBox="0 0 32 32" width="42" height="42" aria-hidden="true">
            <rect width="32" height="32" rx="8" fill="#2563eb" />
            <path
              d="M9 12.5A3.5 3.5 0 0 1 12.5 9h7A3.5 3.5 0 0 1 23 12.5v3a3.5 3.5 0 0 1-3.5 3.5H17l-4 3v-3h-.5A3.5 3.5 0 0 1 9 15.5z"
              fill="#fff"
            />
            <circle cx="13" cy="14" r="1.2" fill="#2563eb" />
            <circle cx="16" cy="14" r="1.2" fill="#2563eb" />
            <circle cx="19" cy="14" r="1.2" fill="#2563eb" />
          </svg>
          <div>
            <h1>School HelpDesk</h1>
            <p>Система технических заявок школы</p>
          </div>
        </div>

        {error && (
          <div className="alert alert--error" role="alert">
            {error}
          </div>
        )}

        <label className="field">
          <span className="field__label">
            <UserIcon size={15} /> Логин
          </span>
          <input
            className="input"
            type="text"
            value={username}
            autoComplete="username"
            autoFocus
            placeholder="например, admin"
            onChange={(event) => setUsername(event.target.value)}
            required
          />
        </label>

        <label className="field">
          <span className="field__label">
            <KeyRound size={15} /> Пароль
          </span>
          <span className="input-group">
            <input
              className="input"
              type={showPassword ? 'text' : 'password'}
              value={password}
              autoComplete="current-password"
              placeholder="Введите пароль"
              onChange={(event) => setPassword(event.target.value)}
              required
            />
            <button
              type="button"
              className="input-group__btn"
              onClick={() => setShowPassword((v) => !v)}
              aria-label={showPassword ? 'Скрыть пароль' : 'Показать пароль'}
            >
              {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
            </button>
          </span>
        </label>

        <button type="submit" className="btn btn--primary btn--block btn--lg" disabled={busy}>
          {busy ? <Spinner size={18} /> : <LogIn size={18} />}
          {busy ? 'Проверка…' : 'Войти'}
        </button>

        <p className="login__hint">
          <ShieldCheck size={14} />
          Доступ к системе выдают администратор школы или IT-специалист
        </p>
      </form>
    </div>
  );
}


