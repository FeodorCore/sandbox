import { useEffect, useState } from 'react';
import type { FormEvent, ReactNode } from 'react';
import { Eye, EyeOff, KeyRound, Save, ShieldCheck } from 'lucide-react';
import { authApi } from '../api/endpoints';
import { ApiError } from '../api/client';
import { useAuth } from '../store/AuthContext';
import { useToast } from '../store/ToastContext';
import { ROLE_META } from '../utils/dict';
import { avatarColor, formatDateTime, initials } from '../utils/format';
import { RoleBadge } from '../components/Badges';
import { Spinner } from '../components/Spinner';

/** Профиль текущего пользователя и смена собственного пароля. */
export default function ProfilePage() {
  const { user, refresh } = useAuth();
  const toast = useToast();

  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [repeatPassword, setRepeatPassword] = useState('');
  const [showPasswords, setShowPasswords] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    document.title = 'Профиль — School HelpDesk';
    // подтягиваем свежие данные (например, время последнего входа)
    void refresh().catch(() => undefined);
  }, [refresh]);

  if (!user) return null;

  const roleMeta = ROLE_META[user.role];

  async function submitPassword(event: FormEvent) {
    event.preventDefault();
    if (busy) return;

    const errors: Record<string, string> = {};
    if (!currentPassword) errors.currentPassword = 'Укажите текущий пароль';
    if (!newPassword) errors.newPassword = 'Укажите новый пароль';
    else if (newPassword.length < 6) errors.newPassword = 'Новый пароль: от 6 до 72 символов';
    if (newPassword !== repeatPassword) errors.repeatPassword = 'Пароли не совпадают';
    if (newPassword && newPassword === currentPassword) {
      errors.newPassword = 'Новый пароль должен отличаться от текущего';
    }
    setFieldErrors(errors);
    setFormError(null);
    if (Object.keys(errors).length > 0) return;

    setBusy(true);
    try {
      await authApi.changePassword(currentPassword, newPassword);
      toast.success('Пароль изменён');
      setCurrentPassword('');
      setNewPassword('');
      setRepeatPassword('');
    } catch (e) {
      if (e instanceof ApiError) {
        if (e.violations.length > 0) {
          const mapped: Record<string, string> = {};
          e.violations.forEach((v) => {
            mapped[v.field] = v.message;
          });
          setFieldErrors(mapped);
        }
        setFormError(e.message);
      } else {
        setFormError('Не удалось изменить пароль');
      }
    } finally {
      setBusy(false);
    }
  }

  const rows: { label: string; value: ReactNode }[] = [
    { label: 'Логин', value: `@${user.username}` },
    { label: 'Роль', value: <RoleBadge role={user.role} /> },
    { label: 'Должность', value: user.position || '—' },
    { label: 'E-mail', value: user.email || '—' },
    { label: 'Телефон', value: user.phone || '—' },
    { label: 'Последний вход', value: user.lastLoginAt ? formatDateTime(user.lastLoginAt) : '—' },
    { label: 'В системе с', value: user.createdAt ? formatDateTime(user.createdAt) : '—' },
  ];

  return (
    <div className="profile-page">
      <div className="profile-layout">
        <section className="card profile-card">
          <div className="card__body">
            <div className="profile-head">
              <span className="avatar avatar--xl" style={{ background: avatarColor(user.id) }}>
                {initials(user.fullName)}
              </span>
              <div className="profile-head__info">
                <h2 className="profile-head__name">{user.fullName}</h2>
                <span className={`badge badge--${roleMeta?.tone ?? 'blue'}`}>{roleMeta?.label ?? user.roleTitle}</span>
                {user.position && <p className="profile-head__position">{user.position}</p>}
              </div>
            </div>

            <dl className="meta-list">
              {rows.map((row) => (
                <div className="meta-list__row" key={row.label}>
                  <dt>{row.label}</dt>
                  <dd>{row.value}</dd>
                </div>
              ))}
            </dl>

            {(user.ticketsCreated !== null && user.ticketsCreated !== undefined) ||
            (user.ticketsAssigned !== null && user.ticketsAssigned !== undefined) ? (
              <div className="profile-stats">
                <div className="profile-stats__item">
                  <strong>{user.ticketsCreated ?? 0}</strong>
                  <span>создано заявок</span>
                </div>
                <div className="profile-stats__item">
                  <strong>{user.ticketsAssigned ?? 0}</strong>
                  <span>назначено на меня</span>
                </div>
              </div>
            ) : null}
          </div>
        </section>

        <section className="card">
          <header className="card__header">
            <h3 className="card__title">
              <KeyRound size={16} /> Смена пароля
            </h3>
            <span className="card__note">рекомендуется менять пароль раз в несколько месяцев</span>
          </header>
          <div className="card__body">
            <form className="password-form" onSubmit={submitPassword} noValidate>
              {formError && (
                <div className="alert alert--error" role="alert">
                  {formError}
                </div>
              )}

              <label className="field">
                <span className="field__label">Текущий пароль</span>
                <span className="input-group">
                  <input
                    className="input"
                    type={showPasswords ? 'text' : 'password'}
                    value={currentPassword}
                    autoComplete="current-password"
                    onChange={(event) => setCurrentPassword(event.target.value)}
                  />
                  <button
                    type="button"
                    className="input-group__btn"
                    onClick={() => setShowPasswords((v) => !v)}
                    aria-label={showPasswords ? 'Скрыть пароли' : 'Показать пароли'}
                  >
                    {showPasswords ? <EyeOff size={16} /> : <Eye size={16} />}
                  </button>
                </span>
                {fieldErrors.currentPassword && <span className="field__error">{fieldErrors.currentPassword}</span>}
              </label>

              <label className="field">
                <span className="field__label">
                  Новый пароль <span className="req">*</span>
                </span>
                <input
                  className="input"
                  type={showPasswords ? 'text' : 'password'}
                  value={newPassword}
                  maxLength={72}
                  autoComplete="new-password"
                  placeholder="минимум 6 символов"
                  onChange={(event) => setNewPassword(event.target.value)}
                />
                {fieldErrors.newPassword && <span className="field__error">{fieldErrors.newPassword}</span>}
              </label>

              <label className="field">
                <span className="field__label">Повторите новый пароль</span>
                <input
                  className="input"
                  type={showPasswords ? 'text' : 'password'}
                  value={repeatPassword}
                  maxLength={72}
                  autoComplete="new-password"
                  onChange={(event) => setRepeatPassword(event.target.value)}
                />
                {fieldErrors.repeatPassword && <span className="field__error">{fieldErrors.repeatPassword}</span>}
              </label>

              <button type="submit" className="btn btn--primary" disabled={busy}>
                {busy ? <Spinner size={16} /> : <Save size={16} />}
                {busy ? 'Сохранение…' : 'Изменить пароль'}
              </button>

              <p className="password-form__note">
                <ShieldCheck size={14} />
                После смены пароля повторно войдите в систему на остальных устройствах
              </p>
            </form>
          </div>
        </section>
      </div>
    </div>
  );
}
