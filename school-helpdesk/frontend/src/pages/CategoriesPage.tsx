import { useCallback, useEffect, useState } from 'react';
import { Check, FolderTree, GripVertical, Pencil, PlusCircle, Trash2, X } from 'lucide-react';
import { categoriesApi } from '../api/endpoints';
import { ApiError } from '../api/client';
import { useToast } from '../store/ToastContext';
import type { Category, CategoryPayload } from '../types';
import { plural, ticketsWord } from '../utils/format';
import ConfirmDialog from '../components/ConfirmDialog';
import EmptyState from '../components/EmptyState';
import Modal from '../components/Modal';
import { PageLoader } from '../components/Spinner';

/** Палитра цветов категории для быстрого выбора. */
const COLOR_PRESETS = [
  '#2563eb', '#7c3aed', '#db2777', '#dc2626', '#f97316',
  '#d97706', '#16a34a', '#0891b2', '#4f46e5', '#64748b',
];

const DEFAULT_COLOR = '#2563eb';

const emptyForm = (): CategoryPayload => ({
  name: '',
  description: '',
  color: DEFAULT_COLOR,
  sortOrder: 0,
  active: true,
});

/** Управление категориями заявок — раздел администратора. */
export default function CategoriesPage() {
  const toast = useToast();

  const [categories, setCategories] = useState<Category[] | null>(null);
  const [loading, setLoading] = useState(true);

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<Category | null>(null);
  const [form, setForm] = useState<CategoryPayload>(emptyForm());
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<Category | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const list = await categoriesApi.list(true, true);
      setCategories([...list].sort((a, b) => a.sortOrder - b.sortOrder || a.name.localeCompare(b.name)));
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : 'Не удалось загрузить категории');
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => {
    document.title = 'Категории — School HelpDesk';
    void load();
  }, [load]);

  const set = (patch: Partial<CategoryPayload>) => setForm((prev) => ({ ...prev, ...patch }));

  const openCreate = () => {
    const maxOrder = categories?.reduce((max, c) => Math.max(max, c.sortOrder), 0) ?? 0;
    setEditing(null);
    setForm({ ...emptyForm(), sortOrder: maxOrder + 10 });
    setFieldErrors({});
    setModalOpen(true);
  };

  const openEdit = (category: Category) => {
    setEditing(category);
    setForm({
      name: category.name,
      description: category.description ?? '',
      color: category.color ?? DEFAULT_COLOR,
      sortOrder: category.sortOrder,
      active: category.active,
    });
    setFieldErrors({});
    setModalOpen(true);
  };

  function validateForm(): boolean {
    const errors: Record<string, string> = {};
    const name = form.name?.trim() ?? '';
    if (!name) errors.name = 'Укажите название категории';
    else if (name.length > 80) errors.name = 'Название: не более 80 символов';
    else if (
      categories?.some(
        (c) => c.id !== editing?.id && c.name.trim().toLowerCase() === name.toLowerCase(),
      )
    ) {
      errors.name = 'Категория с таким названием уже есть';
    }
    if ((form.description ?? '').length > 255) errors.description = 'Описание: не более 255 символов';
    if (form.color && !/^#[0-9a-fA-F]{6}$/.test(form.color)) errors.color = 'Цвет в формате #RRGGBB';
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  }

  const submitForm = async () => {
    if (busy || !validateForm()) return;
    setBusy(true);
    try {
      const payload: CategoryPayload = {
        name: form.name!.trim(),
        description: form.description?.trim() || undefined,
        color: form.color || undefined,
        sortOrder: form.sortOrder ?? 0,
        active: form.active ?? true,
      };
      if (editing) {
        await categoriesApi.update(editing.id, payload);
        toast.success('Категория обновлена');
      } else {
        await categoriesApi.create(payload);
        toast.success(`Категория «${payload.name}» создана`);
      }
      setModalOpen(false);
      await load();
    } catch (e) {
      if (e instanceof ApiError) {
        if (e.violations.length > 0) {
          const errors: Record<string, string> = {};
          e.violations.forEach((v) => {
            errors[v.field] = v.message;
          });
          setFieldErrors(errors);
        }
        toast.error(e.message);
      } else {
        toast.error('Не удалось сохранить категорию');
      }
    } finally {
      setBusy(false);
    }
  };

  const confirmDelete = async () => {
    if (!deleteTarget) return;
    setBusy(true);
    try {
      await categoriesApi.remove(deleteTarget.id);
      const list = await categoriesApi.list(true, true);
      // если категория осталась в списке — она архивирована (к ней есть заявки)
      const stillThere = list.some((c) => c.id === deleteTarget.id);
      if (stillThere) {
        setCategories([...list].sort((a, b) => a.sortOrder - b.sortOrder || a.name.localeCompare(b.name)));
        toast.info(`Категория «${deleteTarget.name}» архивирована: к ней есть заявки`);
      } else {
        toast.success(`Категория «${deleteTarget.name}» удалена`);
      }
      setDeleteTarget(null);
      await load();
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : 'Не удалось удалить категорию');
    } finally {
      setBusy(false);
    }
  };

  const toggleActive = async (category: Category) => {
    try {
      await categoriesApi.update(category.id, {
        name: category.name,
        description: category.description ?? undefined,
        color: category.color ?? undefined,
        sortOrder: category.sortOrder,
        active: !category.active,
      });
      toast.success(category.active ? `Категория «${category.name}» скрыта из выбора` : `Категория «${category.name}» снова активна`);
      await load();
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : 'Не удалось изменить категорию');
    }
  };

  const activeCount = categories?.filter((c) => c.active).length ?? 0;
  const totalTickets = categories?.reduce((sum, c) => sum + (c.ticketCount ?? 0), 0) ?? 0;

  if (loading && categories === null) {
    return <PageLoader text="Загружаем категории…" />;
  }

  return (
    <div className="categories-page">
      <div className="page-header">
        <div>
          <h2 className="page-header__title">Категории заявок</h2>
          <p className="page-header__subtitle">
            Справочник видов технических проблем · активных {activeCount} · {ticketsWord(totalTickets)}
          </p>
        </div>
        <div className="page-header__actions">
          <button type="button" className="btn btn--primary" onClick={openCreate}>
            <PlusCircle size={16} />
            Новая категория
          </button>
        </div>
      </div>

      <div className={`card${loading ? ' card--loading' : ''}`}>
        {!categories || categories.length === 0 ? (
          <EmptyState
            icon={<FolderTree size={28} strokeWidth={1.5} />}
            title="Категорий пока нет"
            description="Создайте категории — они упростят классификацию заявок и статистику"
            action={
              <button type="button" className="btn btn--primary" onClick={openCreate}>
                <PlusCircle size={16} />
                Новая категория
              </button>
            }
          />
        ) : (
          <ul className="category-list">
            {categories.map((category) => (
              <li key={category.id} className={`category-item${category.active ? '' : ' category-item--inactive'}`}>
                <span className="category-item__order" title="Порядок сортировки">
                  <GripVertical size={14} />
                  {category.sortOrder}
                </span>
                <span className="category-item__color" style={{ background: category.color ?? '#94a3b8' }} />
                <div className="category-item__body">
                  <div className="category-item__head">
                    <strong>{category.name}</strong>
                    {!category.active && <span className="badge badge--grey">архив</span>}
                    <span className="category-item__count" title="Заявок в категории">
                      {category.ticketCount ?? 0} {plural(category.ticketCount ?? 0, 'заявка', 'заявки', 'заявок')}
                    </span>
                  </div>
                  {category.description && <p className="category-item__description">{category.description}</p>}
                </div>
                <div className="row-actions">
                  <button
                    type="button"
                    className={`btn btn--icon btn--ghost${category.active ? '' : ' btn--icon-success'}`}
                    onClick={() => void toggleActive(category)}
                    title={category.active ? 'Скрыть из выбора (архивировать)' : 'Вернуть в активные'}
                    aria-label={category.active ? 'Архивировать' : 'Активировать'}
                  >
                    {category.active ? <X size={15} /> : <Check size={15} />}
                  </button>
                  <button
                    type="button"
                    className="btn btn--icon btn--ghost"
                    onClick={() => openEdit(category)}
                    title="Редактировать"
                    aria-label={`Редактировать ${category.name}`}
                  >
                    <Pencil size={15} />
                  </button>
                  <button
                    type="button"
                    className="btn btn--icon btn--ghost btn--icon-danger"
                    onClick={() => setDeleteTarget(category)}
                    title="Удалить"
                    aria-label={`Удалить ${category.name}`}
                  >
                    <Trash2 size={15} />
                  </button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>

      <p className="hint">
        Совет: категорию с заявками нельзя удалить полностью — она архивируется и перестаёт появляться в форме новой
        заявки, но история сохраняется.
      </p>

      {/* -------------------------------------------------- создание/правка */}
      <Modal
        open={modalOpen}
        title={editing ? 'Редактирование категории' : 'Новая категория'}
        subtitle={editing ? editing.name : 'Вид технической проблемы для классификации заявок'}
        onClose={() => setModalOpen(false)}
        busy={busy}
        width="sm"
        footer={
          <>
            <button type="button" className="btn btn--secondary" onClick={() => setModalOpen(false)} disabled={busy}>
              Отмена
            </button>
            <button type="button" className="btn btn--primary" onClick={() => void submitForm()} disabled={busy}>
              {editing ? 'Сохранить' : 'Создать'}
            </button>
          </>
        }
      >
        <label className="field">
          <span className="field__label">
            Название <span className="req">*</span>
          </span>
          <input
            className="input"
            type="text"
            value={form.name ?? ''}
            maxLength={80}
            placeholder="например, Принтеры"
            onChange={(event) => set({ name: event.target.value })}
            autoFocus
          />
          {fieldErrors.name && <span className="field__error">{fieldErrors.name}</span>}
        </label>

        <label className="field">
          <span className="field__label">Описание</span>
          <textarea
            className="textarea"
            rows={2}
            maxLength={255}
            value={form.description ?? ''}
            placeholder="Короткая подсказка: что относится к этой категории"
            onChange={(event) => set({ description: event.target.value })}
          />
          {fieldErrors.description && <span className="field__error">{fieldErrors.description}</span>}
        </label>

        <div className="field">
          <span className="field__label">Цвет метки</span>
          <div className="color-picker">
            <input
              className="color-picker__input"
              type="color"
              value={form.color ?? DEFAULT_COLOR}
              onChange={(event) => set({ color: event.target.value })}
              aria-label="Цвет категории"
            />
            <input
              className="input color-picker__hex"
              type="text"
              value={form.color ?? ''}
              maxLength={7}
              placeholder="#2563eb"
              onChange={(event) => set({ color: event.target.value })}
            />
          </div>
          <div className="color-presets">
            {COLOR_PRESETS.map((color) => (
              <button
                key={color}
                type="button"
                className={`color-preset${(form.color ?? '').toLowerCase() === color ? ' color-preset--active' : ''}`}
                style={{ background: color }}
                onClick={() => set({ color })}
                aria-label={`Выбрать цвет ${color}`}
              />
            ))}
          </div>
          {fieldErrors.color && <span className="field__error">{fieldErrors.color}</span>}
        </div>

        <div className="form-grid form-grid--2">
          <label className="field">
            <span className="field__label">Порядок</span>
            <input
              className="input"
              type="number"
              value={form.sortOrder ?? 0}
              min={0}
              max={9999}
              onChange={(event) => set({ sortOrder: Number(event.target.value) || 0 })}
            />
            <span className="field__hint">Меньшее значение — выше в списке</span>
          </label>

          <label className="field">
            <span className="field__label">Состояние</span>
            <label className="checkbox">
              <input
                type="checkbox"
                checked={form.active ?? true}
                onChange={(event) => set({ active: event.target.checked })}
              />
              <span>Активна (доступна для выбора)</span>
            </label>
          </label>
        </div>
      </Modal>

      <ConfirmDialog
        open={deleteTarget !== null}
        title="Удалить категорию?"
        message={
          deleteTarget
            ? `«${deleteTarget.name}». Если к категории есть заявки, она будет архивирована вместо удаления.`
            : ''
        }
        confirmText="Удалить"
        busy={busy}
        onConfirm={() => void confirmDelete()}
        onCancel={() => setDeleteTarget(null)}
      />
    </div>
  );
}
