package ru.school.helpdesk.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** Техническая заявка. */
@Entity
@Table(
        name = "tickets",
        indexes = {
            @Index(name = "idx_tickets_status", columnList = "status"),
            @Index(name = "idx_tickets_priority", columnList = "priority"),
            @Index(name = "idx_tickets_created_at", columnList = "created_at"),
            @Index(name = "idx_tickets_room", columnList = "room"),
            @Index(name = "idx_tickets_number", columnList = "number")
        })
@EntityListeners(AuditingEntityListener.class)
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Человекочитаемый номер, например HD-00042. Формируется после сохранения. */
    @Column(length = 20, unique = true)
    private String number;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus status = TicketStatus.NEW;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Priority priority = Priority.MEDIUM;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "category_id", nullable = false, foreignKey = @jakarta.persistence.ForeignKey(name = "fk_tickets_category"))
    private Category category;

    /** Автор заявки (сотрудник школы). */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "author_id", nullable = false, foreignKey = @jakarta.persistence.ForeignKey(name = "fk_tickets_author"))
    private User author;

    /** Назначенный исполнитель. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assignee_id", foreignKey = @jakarta.persistence.ForeignKey(name = "fk_tickets_assignee"))
    private User assignee;

    /** Кабинет, где возникла проблема. */
    @Column(length = 40)
    private String room;

    /** Корпус / место (например «Корпус А», «Столовая»). */
    @Column(length = 80)
    private String building;

    /** Наименование или инвентарный номер оборудования. */
    @Column(length = 120)
    private String equipment;

    /** Плановый срок выполнения. */
    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    /** Текст решения проблемы (заполняется при переводе в RESOLVED). */
    @Column(name = "resolution", length = 2000)
    private String resolution;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Ticket() {
        // для JPA
    }

    public Ticket(String title, String description, Category category, User author) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.author = author;
    }

    public Long getId() {
        return id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public User getAssignee() {
        return assignee;
    }

    public void setAssignee(User assignee) {
        this.assignee = assignee;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public String getEquipment() {
        return equipment;
    }

    public void setEquipment(String equipment) {
        this.equipment = equipment;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /** Заявка просрочена: плановая дата прошла, а работа не завершена. */
    public boolean isOverdue() {
        return dueDate != null && dueDate.isBefore(LocalDate.now()) && status.isActive();
    }

    public boolean isAssigned() {
        return assignee != null;
    }

    @Override
    public String toString() {
        return "Ticket{id=" + id + ", number='" + number + "', status=" + status + '}';
    }
}


