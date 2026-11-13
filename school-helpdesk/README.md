# School HelpDesk

Веб-система управления техническими заявками для школы: сотрудники создают заявки о неисправностях
(компьютеры, принтеры, проекторы, сеть, ПО…), IT-специалисты принимают их в работу, меняют статусы,
оставляют комментарии и ведут историю выполнения. Администратор управляет пользователями, категориями
и следит за сводной статистикой.

Один сервер (Spring Boot) одновременно раздаёт REST API и собранный React-интерфейс —
развёртывание сводится к запуску одного JAR и контейнера с PostgreSQL.

---

## Возможности

- **Авторизация** по логину/паролю (JWT, stateless).
- **Три роли** с разграничением прав:
  | Роль | Права |
  |---|---|
  | `ADMIN` | всё + управление пользователями и категориями, удаление заявок |
  | `TECHNICIAN` | все заявки, назначение исполнителей, статусы, приоритеты, внутренние комментарии |
  | `USER` | создание заявок, просмотр и комментарии только к **своим** заявкам, подтверждение выполнения |
- **Жизненный цикл заявки**: `Новая → В работе → Отложена → Выполнена → Закрыта` (или `Отклонена`).
  При назначении исполнителя новая заявка автоматически переходит в работу; автор может подтвердить
  выполнение, вернуть заявку в работу или отменить новую.
- **Приоритеты**: низкий, средний, высокий, критический.
- **Категории** проблем с цветными метками (управляются администратором, архивируются вместо удаления,
  если к ним есть заявки).
- **Место проблемы**: кабинет, корпус, оборудование, плановый срок выполнения, контроль просрочек.
- **Комментарии**: обычные и внутренние (видны только персоналу).
- **История** всех изменений заявки (кто, когда, что сделал).
- **Поиск, фильтры, сортировка** заявок: статус, приоритет, категория, исполнитель, автор, кабинет,
  даты, «без исполнителя», «просроченные», «мои заявки», полнотекстовый поиск.
- **Панель мониторинга**: счётчики статусов, динамика «создано/выполнено» за 14 дней, распределение
  по категориям и приоритетам, загрузка специалистов, проблемные кабинеты, среднее время решения.
- **Управление пользователями**: создание, редактирование, смена пароля, блокировка
  (удаление заменяется блокировкой, если на пользователя есть заявки — история сохраняется).
- Адаптивный интерфейс (работает на планшете/телефоне), русский язык, Swagger UI в режиме разработки.

## Технологический стек

| Слой | Технологии |
|---|---|
| Frontend | React 18, TypeScript, Vite, CSS (без UI-фреймворков), React Router 6, Lucide React |
| Backend | Java 21, Spring Boot 3.5 (Web, Data JPA, Security, Validation), JWT (jjwt), springdoc-openapi |
| База данных | PostgreSQL 15+ (в режиме разработки — встроенная H2) |
| Развёртывание | Linux, один JAR раздаёт API и статику на порту 80, Docker Compose для PostgreSQL |

## Структура проекта

```
school-helpdesk/
├── docker-compose.yml          # PostgreSQL (порт 5432 только для localhost)
├── scripts/
│   ├── build.sh                # полная сборка: frontend → static → JAR
│   └── run.sh                  # быстрый запуск для разработки (H2 + демо-данные)
├── deploy/
│   ├── school-helpdesk.service # systemd-юнит для Linux-сервера
│   └── helpdesk.env.example    # шаблон переменных окружения
├── backend/                    # Spring Boot (API + раздача фронтенда)
│   ├── pom.xml
│   └── src/main/
│       ├── java/ru/school/helpdesk/
│       │   ├── config/         # безопасность, MVC (SPA-роутинг), OpenAPI, свойства
│       │   ├── domain/         # сущности: User, Ticket, Category, Comment, TicketEvent + enum'ы
│       │   ├── repository/     # Spring Data JPA + спецификации для фильтров
│       │   ├── service/        # бизнес-логика, права доступа, история, сидер данных
│       │   ├── mapper/         # сущность → DTO
│       │   ├── dto/            # запросы/ответы REST API
│       │   ├── security/       # JWT-фильтр, principal, обработчики ошибок безопасности
│       │   ├── web/            # REST-контроллеры
│       │   └── exception/      # единый формат ошибок API
│       └── resources/
│           ├── application*.yml
│           └── static/         # сюда собирается фронтенд (vite build)
└── frontend/                   # React + TypeScript + Vite
    ├── package.json
    ├── vite.config.ts          # dev-прокси /api → :8080, сборка в backend/static
    └── src/
        ├── api/                # HTTP-клиент (JWT, ошибки валидации) и эндпоинты
        ├── components/         # Layout, таблицы, бейджи, модалки, диаграммы…
        ├── pages/              # вход, мониторинг, заявки, карточка, форма, пользователи, категории, профиль
        ├── store/              # AuthContext, ToastContext
        ├── styles/             # global.css, components.css, pages.css
        └── utils/              # словари статусов/ролей, форматирование дат
```

## Быстрый старт (без установки — для знакомства)

Нужны только Java 21+ и Node.js 18+ (PostgreSQL не требуется — используется встроенная H2):

```bash
cd school-helpdesk

# 1. собрать фронтенд в ресурсы backend-а
cd frontend && npm install && npm run build && cd ..

# 2. запустить backend в dev-режиме (H2 + демо-данные)
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev    # либо: mvn spring-boot:run ...
```

Откройте **http://localhost:8080** и войдите одной из демо-учёток:

| Логин | Пароль | Роль |
|---|---|---|
| `admin` | `admin123` | администратор |
| `ivanov` / `petrova` | `tech123` | IT-специалист |
| `smirnova`, `kuznetsov`, `fedorova`, `sidorov`, `orlova` | `user123` | сотрудники |

> Демо-данные (пользователи, категории, ~18 заявок с историей и комментариями) создаются только
> при `APP_SEED_DEMO=true` / профиле `dev`. Администратор `admin/admin123` создаётся при первом
> старте в любом режиме — **смените пароль после первого входа**.

## Production-развёртывание на Linux-сервере

### 1. Требования

- Linux-сервер в школьной сети (например, `192.168.1.100`);
- Java 21+ (например, `apt install openjdk-21-jre-headless` или Temurin);
- Docker + Docker Compose (для PostgreSQL) **или** установленный PostgreSQL 15+;
- Node.js 18+ — только на машине сборки (можно собрать JAR где угодно и скопировать на сервер).

### 2. База данных

```bash
cd school-helpdesk
docker compose up -d          # поднимет PostgreSQL 16 на 127.0.0.1:5432
```

Параметры по умолчанию: БД `helpdesk`, пользователь `helpdesk`, пароль `helpdesk`.
**Обязательно задайте свой пароль** — создайте файл `.env` рядом с `docker-compose.yml`:

```env
DB_NAME=helpdesk
DB_USER=helpdesk
DB_PASSWORD=<надёжный пароль>
```

Порт 5432 намеренно опубликован только на `127.0.0.1` — из сети школы база недоступна,
к ней подключается лишь backend на этом же сервере.

### 3. Сборка

```bash
cd school-helpdesk
bash scripts/build.sh         # npm ci + vite build + mvn package
# результат: backend/target/school-helpdesk.jar (API + интерфейс внутри)
```

### 4. Запуск (порт 80)

Вручную:

```bash
sudo APP_JWT_SECRET='<случайная строка 32+ символа>' \
     DB_PASSWORD='<пароль БД>' \
     SERVER_PORT=80 \
     java -jar backend/target/school-helpdesk.jar
```

Либо как служба systemd (рекомендуется — автозапуск и перезапуск при сбое):

```bash
sudo cp deploy/school-helpdesk.service /etc/systemd/system/
sudo cp deploy/helpdesk.env.example /etc/school-helpdesk.env   # и отредактируйте пароли!
sudo systemctl daemon-reload
sudo systemctl enable --now school-helpdesk
```

Откройте в браузере **http://192.168.1.100** (IP вашего сервера) — сотрудники заходят
под своими учётками, которые создаёт администратор в разделе «Пользователи».

### 5. HTTPS (при необходимости, порт 443)

Проще всего поставить перед приложением nginx с TLS-сертификатом (например, внутренняя CA
или Let's Encrypt, если сервер доступен извне). nginx проксирует на `127.0.0.1:8080`:

```nginx
server {
    listen 443 ssl;
    server_name helpdesk.school.local;

    ssl_certificate     /etc/ssl/certs/helpdesk.crt;
    ssl_certificate_key /etc/ssl/private/helpdesk.key;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

Приложение уже настроено на корректную работу за reverse-proxy (`forward-headers-strategy: framework`).
В этом случае запустите backend на `SERVER_PORT=8080` и закройте 80-й порт файрволом.
Альтернатива без nginx — встроенный SSL Spring Boot (`server.ssl.*` в `application.yml`).

## Переменные окружения

| Переменная | По умолчанию | Назначение |
|---|---|---|
| `SERVER_PORT` | `8080` | порт веб-интерфейса и API (в production — `80`) |
| `DB_HOST`, `DB_PORT` | `localhost`, `5432` | адрес PostgreSQL |
| `DB_NAME`, `DB_USER`, `DB_PASSWORD` | `helpdesk` | параметры БД |
| `APP_JWT_SECRET` | dev-ключ | **задайте в production**: секрет JWT, ≥32 символов |
| `APP_JWT_EXPIRATION` | `12h` | время жизни токена |
| `APP_SEED_ADMIN_USERNAME` / `APP_SEED_ADMIN_PASSWORD` | `admin` / `admin123` | первый администратор |
| `APP_SEED_DEMO` | `false` | наполнить БД демонстрационными данными |
| `APP_CORS_ORIGINS` | `http://localhost:5173` | источники CORS (нужны только для dev-сервера Vite) |
| `SPRINGDOC_ENABLED` | `false` | включить Swagger UI (`/swagger-ui.html`, только ADMIN) |

## Режим разработки (frontend отдельно)

```bash
# терминал 1 — backend на :8080
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev

# терминал 2 — Vite dev-server на :5173 с горячими обновлениями
cd frontend && npm install && npm run dev
```

Vite проксирует `/api` на `http://localhost:8080` (переопределяется `VITE_BACKEND_URL`).

## REST API (кратко)

| Метод и путь | Назначение | Доступ |
|---|---|---|
| `POST /api/auth/login` | вход, выдача JWT | все |
| `GET /api/auth/me` | текущий пользователь | авторизованные |
| `POST /api/auth/change-password` | смена своего пароля | авторизованные |
| `GET /api/tickets` | поиск заявок (фильтры, сортировка, страницы) | по ролям |
| `POST /api/tickets` | создать заявку | все |
| `GET/PUT /api/tickets/{id}` | карточка / редактирование | по правам |
| `PATCH /api/tickets/{id}/status` | смена статуса (+решение, +комментарий) | по правам |
| `PATCH /api/tickets/{id}/priority` | смена приоритета | ADMIN, TECHNICIAN |
| `PATCH /api/tickets/{id}/assign` | назначить/снять исполнителя | ADMIN, TECHNICIAN |
| `DELETE /api/tickets/{id}` | удаление | ADMIN |
| `GET /api/tickets/{id}/history` | история изменений | по правам |
| `GET/POST /api/tickets/{id}/comments`, `DELETE /api/comments/{id}` | комментарии | по правам |
| `GET /api/dashboard/stats` | сводная статистика | по ролям |
| `GET/POST/PUT/DELETE /api/categories` | категории | чтение — все, изменения — ADMIN |
| `GET/POST/PUT/DELETE /api/users` | пользователи | ADMIN |
| `GET /api/users/assignees`, `/api/users/options` | списки для фильтров/назначения | ADMIN, TECHNICIAN |
| `GET /api/meta/enums`, `/api/meta/about` | справочники | авторизованные |

Полная документация — Swagger UI (`/swagger-ui.html`) при `SPRINGDOC_ENABLED=true` или в dev-профиле.
Ошибки возвращаются единым JSON-форматом `{status, error, message, path, violations[]}`.

## Тесты

```bash
cd backend && mvn test     # интеграционные тесты API на H2 (профиль test)
```

## Резервное копирование

Все данные хранятся в PostgreSQL (том Docker `pgdata`):

```bash
docker exec helpdesk-postgres pg_dump -U helpdesk helpdesk > backup-$(date +%F).sql   # дамп
docker exec -i helpdesk-postgres psql -U helpdesk helpdesk < backup-2026-01-01.sql    # восстановление
```
