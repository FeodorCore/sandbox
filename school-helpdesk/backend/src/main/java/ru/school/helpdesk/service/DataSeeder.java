package ru.school.helpdesk.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.school.helpdesk.config.AppProperties;
import ru.school.helpdesk.domain.Category;
import ru.school.helpdesk.domain.Comment;
import ru.school.helpdesk.domain.Priority;
import ru.school.helpdesk.domain.Role;
import ru.school.helpdesk.domain.Ticket;
import ru.school.helpdesk.domain.TicketEvent;
import ru.school.helpdesk.domain.TicketEventType;
import ru.school.helpdesk.domain.TicketStatus;
import ru.school.helpdesk.domain.User;
import ru.school.helpdesk.repository.CategoryRepository;
import ru.school.helpdesk.repository.CommentRepository;
import ru.school.helpdesk.repository.TicketEventRepository;
import ru.school.helpdesk.repository.TicketRepository;
import ru.school.helpdesk.repository.UserRepository;

/**
 * Первичное наполнение базы данных:
 * <ul>
 *   <li>учётная запись администратора (логин/пароль — из {@code app.seed.*});</li>
 *   <li>справочник категорий проблем;</li>
 *   <li>демонстрационные данные (если {@code app.seed.demo-data=true}).</li>
 * </ul>
 * Скрипт идемпотентен: повторный запуск не создаёт дубликаты.
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    /** Название, описание, цвет справочника категорий по умолчанию. */
    private static final String[][] DEFAULT_CATEGORIES = {
            {"Компьютеры и ноутбуки", "ПК, моноблоки, ноутбуки учителей и администрации", "#2563eb"},
            {"Принтеры и МФУ", "Печать, сканирование, замятие бумаги, картриджи", "#7c3aed"},
            {"Проекторы и панели", "Проекторы, интерактивные панели, экраны", "#0891b2"},
            {"Сеть и Интернет", "Wi-Fi, кабельная сеть, доступ в Интернет", "#059669"},
            {"Программное обеспечение", "Установка и обновление программ, лицензии, ошибки ПО", "#d97706"},
            {"Электронный журнал и 1С", "Сетевой город, Дневник.ру, 1С:Образование", "#dc2626"},
            {"Телефония и связь", "Телефоны, интерком, связь с охраной", "#db2777"},
            {"Электрика и освещение", "Розетки, освещение, ИБП", "#65a30d"},
            {"Аудио и видео", "Микрофоны, колонки, камеры, оборудование актового зала", "#9333ea"},
            {"Другое", "Прочие технические вопросы", "#64748b"},
    };

    private final AppProperties properties;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TicketRepository ticketRepository;
    private final CommentRepository commentRepository;
    private final TicketEventRepository eventRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public DataSeeder(AppProperties properties,
                      UserRepository userRepository,
                      CategoryRepository categoryRepository,
                      TicketRepository ticketRepository,
                      CommentRepository commentRepository,
                      TicketEventRepository eventRepository,
                      PasswordEncoder passwordEncoder,
                      JdbcTemplate jdbcTemplate) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.ticketRepository = ticketRepository;
        this.commentRepository = commentRepository;
        this.eventRepository = eventRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        AppProperties.Seed seed = properties.seed();
        if (seed == null || !seed.enabled()) {
            return;
        }
        seedCategories();
        User admin = seedAdmin(seed);
        if (seed.demoData()) {
            seedDemoData(admin);
        }
    }

    private void seedCategories() {
        int order = 0;
        for (String[] item : DEFAULT_CATEGORIES) {
            order += 10;
            if (categoryRepository.findByNameIgnoreCase(item[0]).isPresent()) {
                continue;
            }
            categoryRepository.save(new Category(item[0], item[1], item[2], order));
        }
        log.info("Справочник категорий: {} записей", categoryRepository.count());
    }

    private User seedAdmin(AppProperties.Seed seed) {
        String username = seed.adminUsername() == null ? "admin" : seed.adminUsername().trim().toLowerCase();
        Optional<User> existing = userRepository.findByUsernameIgnoreCase(username);
        if (existing.isPresent()) {
            return existing.get();
        }
        String password = seed.adminPassword() == null || seed.adminPassword().isBlank()
                ? "admin123" : seed.adminPassword();
        User admin = new User(username, passwordEncoder.encode(password),
                seed.adminFullName() == null ? "Администратор системы" : seed.adminFullName(), Role.ADMIN);
        admin.setPosition("Системный администратор");
        admin.setEmail("admin@school.local");
        admin = userRepository.save(admin);
        log.warn("Создана учётная запись администратора: {} / {} — смените пароль после первого входа!",
                admin.getUsername(), password);
        return admin;
    }

    // ------------------------------------------------------------- демо-данные

    private void seedDemoData(User admin) {
        if (ticketRepository.count() > 0) {
            log.info("Демо-данные пропущены: в базе уже есть заявки");
            return;
        }
        User tech1 = createUser("ivanov", "tech123", "Иванов Пётр Сергеевич", Role.TECHNICIAN,
                "Инженер службы поддержки", "ivanov@school.local", "+7 900 111-22-33");
        User tech2 = createUser("petrova", "tech123", "Петрова Анна Ивановна", Role.TECHNICIAN,
                "Специалист по информатизации", "petrova@school.local", "+7 900 222-33-44");
        User teacher1 = createUser("smirnova", "user123", "Смирнова Ольга Николаевна", Role.USER,
                "Учитель математики", "smirnova@school.local", null);
        User teacher2 = createUser("kuznetsov", "user123", "Кузнецов Дмитрий Андреевич", Role.USER,
                "Учитель информатики", "kuznetsov@school.local", null);
        User teacher3 = createUser("fedorova", "user123", "Фёдорова Мария Петровна", Role.USER,
                "Заместитель директора по УВР", "fedorova@school.local", null);
        User teacher4 = createUser("sidorov", "user123", "Сидоров Алексей Викторович", Role.USER,
                "Учитель физики", "sidorov@school.local", null);
        User librarian = createUser("orlova", "user123", "Орлова Екатерина Дмитриевна", Role.USER,
                "Библиотекарь", "orlova@school.local", null);

        List<Category> categories = categoryRepository.findByActiveTrueOrderBySortOrderAscNameAsc();
        if (categories.isEmpty()) {
            return;
        }
        Category computers = cat(categories, "Компьютеры и ноутбуки");
        Category printers = cat(categories, "Принтеры и МФУ");
        Category projectors = cat(categories, "Проекторы и панели");
        Category network = cat(categories, "Сеть и Интернет");
        Category software = cat(categories, "Программное обеспечение");
        Category journal = cat(categories, "Электронный журнал и 1С");
        Category electric = cat(categories, "Электрика и освещение");
        Category audio = cat(categories, "Аудио и видео");
        Category other = cat(categories, "Другое");

        List<Ticket> tickets = new ArrayList<>();
        tickets.add(demo("Не включается компьютер учителя",
                "При нажатии кнопки питания загораются индикаторы, но изображение не появляется, кулеры шумят."
                        + " ПК не работает второй день, уроки срываются.",
                computers, TicketStatus.NEW, Priority.HIGH, teacher1, null, "204", "Корпус А", 0, null));
        tickets.add(demo("Принтер не печатает из кабинета 112",
                "Принтер HP LaserJet показывает ошибку «замятие бумаги». Бумагу извлекли, ошибка не сбрасывается,"
                        + " очередь печати зависла.",
                printers, TicketStatus.IN_PROGRESS, Priority.MEDIUM, teacher2, tech1, "112", "Корпус А", 2, null));
        tickets.add(demo("Проектор не показывает изображение",
                "Проектор включается, лампа горит, но на экране чёрный квадрат. Кабель HDMI проверяли"
                        + " на другом ноутбуке — работает.",
                projectors, TicketStatus.IN_PROGRESS, Priority.HIGH, teacher4, tech1, "301", "Корпус Б", 3, null));
        tickets.add(demo("Нет Интернета в компьютерном классе",
                "Все 15 компьютеров показывают «без доступа к Интернету», локальная сеть работает."
                        + " Проблема началась после обновления коммутатора.",
                network, TicketStatus.RESOLVED, Priority.CRITICAL, teacher2, tech2, "210", "Корпус А", 8,
                "Перезагружен коммутатор, обновлён профиль VLAN на портах 1–16. Доступ восстановлен."));
        tickets.add(demo("Не открывается электронный журнал",
                "Браузер выдаёт ошибку 502 при входе в «Сетевой город», проблема у всех учителей."
                        + " Нужно срочно выставить оценки за четверть.",
                journal, TicketStatus.RESOLVED, Priority.CRITICAL, teacher3, tech2, "—", "Администрация", 12,
                "Сбой на стороне провайдера услуги, направлена заявка в поддержку. Доступ восстановлен через 4 часа."));
        tickets.add(demo("Требуется установить LibreOffice",
                "На трёх компьютерах в кабинете информатики нужны текстовый редактор и таблицы для уроков.",
                software, TicketStatus.CLOSED, Priority.LOW, teacher2, tech1, "210", "Корпус А", 18,
                "Установлен LibreOffice, созданы ярлыки, настроены ассоциации файлов."));
        tickets.add(demo("Мигает свет в кабинете 305",
                "Лампы дневного света мигают и гудят, детям некомфортно заниматься.",
                electric, TicketStatus.ON_HOLD, Priority.MEDIUM, teacher1, tech2, "305", "Корпус Б", 5, null));
        tickets.add(demo("Не работает микрофон в актовом зале",
                "Радиомикрофон фонит и периодически отключается во время репетиции линейки.",
                audio, TicketStatus.NEW, Priority.HIGH, teacher3, null, "Актовый зал", "Корпус А", 1, null));
        tickets.add(demo("Зависает ноутбук заместителя директора",
                "Ноутбук Lenovo тормозит при открытии нескольких вкладок, иногда зависает на 1–2 минуты.",
                computers, TicketStatus.IN_PROGRESS, Priority.MEDIUM, teacher3, tech1, "101", "Администрация", 4, null));
        tickets.add(demo("Замена картриджа в МФУ библиотеки",
                "МФУ печатает бледно и полосит. Вероятно, закончился тонер.",
                printers, TicketStatus.NEW, Priority.LOW, librarian, null, "Библиотека", "Корпус А", 0, null));
        tickets.add(demo("Не открывается сайт школы",
                "С нескольких компьютеров сайт школы не открывается, ошибка DNS. У провайдера всё в порядке.",
                network, TicketStatus.CLOSED, Priority.MEDIUM, teacher3, tech2, "101", "Администрация", 21,
                "Заменён DNS-сервер в настройках DHCP, сайт открывается."));
        tickets.add(demo("Синий экран на ПК в кабинете физики",
                "Компьютер уходит в синий экран через 10–15 минут работы, код остановки MEMORY_MANAGEMENT.",
                computers, TicketStatus.IN_PROGRESS, Priority.HIGH, teacher4, tech2, "308", "Корпус Б", 6, null));
        tickets.add(demo("Настроить Wi-Fi в учительской",
                "Слабый сигнал Wi-Fi в учительской, планшеты отваливаются от сети во время перемены.",
                network, TicketStatus.NEW, Priority.MEDIUM, teacher1, null, "Учительская", "Корпус А", 1, null));
        tickets.add(demo("Ошибка при печати из 1С",
                "1С:Образование не отправляет документы на печать, выдаёт ошибку «принтер недоступен».",
                software, TicketStatus.REJECTED, Priority.LOW, teacher3, tech1, "102", "Администрация", 15,
                "Дубликат существующей заявки по принтеру, работа выполнена в её рамках."));
        tickets.add(demo("Интерактивная панель не реагирует на касания",
                "Панель показывает изображение, но сенсор не работает после обновления драйвера.",
                projectors, TicketStatus.RESOLVED, Priority.MEDIUM, teacher2, tech1, "207", "Корпус А", 9,
                "Переустановлен драйвер сенсорной рамки, выполнена калибровка."));
        tickets.add(demo("Нужен дополнительный удлинитель в кабинет 212",
                "После перестановки мебели не хватает розеток для трёх компьютеров.",
                other, TicketStatus.CLOSED, Priority.LOW, teacher1, tech2, "212", "Корпус А", 25,
                "Установлен сетевой фильтр на 8 розеток, кабели уложены в короб."));
        tickets.add(demo("Не открывается презентация на панели",
                "Файл презентации не открывается на интерактивной панели, сообщение о повреждённом файле.",
                software, TicketStatus.NEW, Priority.LOW, teacher4, null, "207", "Корпус А", 0, null));
        tickets.add(demo("Кабинет 401 — нет сети на четырёх ПК",
                "Четыре рабочих места в ряду у окна потеряли сеть, индикаторы на сетевых картах не горят.",
                network, TicketStatus.IN_PROGRESS, Priority.HIGH, teacher2, tech2, "401", "Корпус Б", 7, null));

        log.info("Демо-данные созданы: пользователей {}, заявок {}", userRepository.count(), tickets.size());
        if (admin != null) {
            log.info("Учётные записи для входа: {} / admin123, ivanov и petrova / tech123,"
                    + " smirnova, kuznetsov, fedorova, sidorov, orlova / user123", admin.getUsername());
        }
    }

    private Ticket demo(String title, String description, Category category, TicketStatus status, Priority priority,
                        User author, User assignee, String room, String building, int daysAgo, String resolution) {
        Instant createdAt = Instant.now().minus(daysAgo, ChronoUnit.DAYS);
        User admin = admin();

        Ticket ticket = new Ticket(title, description, category, author);
        ticket.setPriority(priority);
        ticket.setStatus(status);
        ticket.setAssignee(assignee);
        ticket.setRoom(room);
        ticket.setBuilding(building);
        ticket.setDueDate(LocalDate.now().minusDays(daysAgo).plusDays(switch (priority) {
            case CRITICAL -> 1;
            case HIGH -> 3;
            case MEDIUM -> 7;
            case LOW -> 14;
        }));
        if (resolution != null) {
            ticket.setResolution(resolution);
            ticket.setResolvedAt(createdAt.plus(1, ChronoUnit.DAYS));
        }
        if (status == TicketStatus.CLOSED || status == TicketStatus.REJECTED) {
            ticket.setClosedAt(createdAt.plus(2, ChronoUnit.DAYS));
        }
        ticket = ticketRepository.save(ticket);
        ticket.setNumber(String.format("HD-%05d", ticket.getId()));
        ticket = ticketRepository.saveAndFlush(ticket);
        setCreatedAt("tickets", ticket.getId(), createdAt);

        saveEvent(new TicketEvent(ticket, author, TicketEventType.CREATED,
                "Заявка " + ticket.getNumber() + " создана: " + title), createdAt);
        if (assignee != null) {
            saveEvent(new TicketEvent(ticket, admin, TicketEventType.ASSIGNED,
                            "Назначен исполнитель: " + assignee.getFullName())
                            .withValues(null, assignee.getFullName()),
                    createdAt.plus(2, ChronoUnit.HOURS));
        }
        if (status != TicketStatus.NEW) {
            saveEvent(new TicketEvent(ticket, assignee != null ? assignee : admin, TicketEventType.STATUS_CHANGED,
                            "Статус изменён: Новая → " + status.getTitle())
                            .withValues(TicketStatus.NEW.name(), status.name()),
                    createdAt.plus(4, ChronoUnit.HOURS));
        }
        if (resolution != null) {
            saveEvent(new TicketEvent(ticket, assignee != null ? assignee : admin, TicketEventType.RESOLVED,
                    "Работа выполнена: " + resolution), createdAt.plus(1, ChronoUnit.DAYS));
        }
        if (status == TicketStatus.CLOSED) {
            saveEvent(new TicketEvent(ticket, author, TicketEventType.CLOSED,
                    "Автор подтвердил выполнение заявки"), createdAt.plus(2, ChronoUnit.DAYS));
        }

        if (status != TicketStatus.NEW) {
            saveComment(new Comment(ticket, author,
                    "Подскажите, пожалуйста, когда сможете посмотреть? Кабинет нужен к третьему уроку.", false),
                    createdAt.plus(3, ChronoUnit.HOURS));
        }
        if (assignee != null) {
            saveComment(new Comment(ticket, assignee,
                    resolution != null ? resolution : "Взял в работу, приду сегодня после 14:00.", false),
                    createdAt.plus(5, ChronoUnit.HOURS));
            saveComment(new Comment(ticket, assignee,
                    "Внутренняя заметка: проверить наличие запасных картриджей и кабелей на складе.", true),
                    createdAt.plus(6, ChronoUnit.HOURS));
        }
        return ticket;
    }

    private void saveEvent(TicketEvent event, Instant createdAt) {
        event = eventRepository.saveAndFlush(event);
        setCreatedAt("ticket_events", event.getId(), createdAt);
    }

    private void saveComment(Comment comment, Instant createdAt) {
        comment = commentRepository.saveAndFlush(comment);
        setCreatedAt("comments", comment.getId(), createdAt);
    }

    /** Проставляет дату создания задним числом (для наглядных графиков в демо-режиме). */
    private void setCreatedAt(String table, Long id, Instant createdAt) {
        jdbcTemplate.update("update " + table + " set created_at = ? where id = ?",
                Timestamp.from(createdAt), id);
    }

    private User createUser(String username, String password, String fullName, Role role,
                            String position, String email, String phone) {
        Optional<User> existing = userRepository.findByUsernameIgnoreCase(username);
        if (existing.isPresent()) {
            return existing.get();
        }
        User user = new User(username, passwordEncoder.encode(password), fullName, role);
        user.setPosition(position);
        user.setEmail(email);
        user.setPhone(phone);
        return userRepository.save(user);
    }

    private User admin() {
        String username = properties.seed() == null || properties.seed().adminUsername() == null
                ? "admin" : properties.seed().adminUsername();
        return userRepository.findByUsernameIgnoreCase(username).orElse(null);
    }

    private static Category cat(List<Category> categories, String name) {
        return categories.stream()
                .filter(c -> c.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(categories.get(categories.size() - 1));
    }
}


