package ru.school.helpdesk.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Запись истории заявки (append-only журнал).
 * Хранит, кто, когда и что изменил: {@code fromValue} → {@code toValue}.
 */
@Entity
@Table(
        name = "ticket_events",
        indexes = {
            @Index(name = "idx_events_ticket", columnList = "ticket_id"),
            @Index(name = "idx_events_created_at", columnList = "created_at")
        })
@EntityListeners(AuditingEntityListener.class)
public class TicketEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false, foreignKey = @ForeignKey(name = "fk_events_ticket"))
    private Ticket ticket;

    /** Инициатор изменения (null — системное событие). */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "actor_id", foreignKey = @ForeignKey(name = "fk_events_actor"))
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TicketEventType type;

    /** Человекочитаемое описание события. */
    @Column(length = 500)
    private String message;

    @Column(name = "from_value", length = 120)
    private String fromValue;

    @Column(name = "to_value", length = 120)
    private String toValue;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TicketEvent() {
        // для JPA
    }

    public TicketEvent(Ticket ticket, User actor, TicketEventType type, String message) {
        this.ticket = ticket;
        this.actor = actor;
        this.type = type;
        this.message = message;
    }

    public TicketEvent withValues(String from, String to) {
        this.fromValue = from;
        this.toValue = to;
        return this;
    }

    public Long getId() {
        return id;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public User getActor() {
        return actor;
    }

    public TicketEventType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public String getFromValue() {
        return fromValue;
    }

    public String getToValue() {
        return toValue;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}


