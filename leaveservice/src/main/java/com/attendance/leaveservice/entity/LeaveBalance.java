package com.attendance.leaveservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "leave_balance",
        uniqueConstraints = @UniqueConstraint(name = "uk_balance_user_year", columnNames = {"user_id", "year"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(nullable = false)
    private int year;

    @Column(name = "pl_total", nullable = false)
    private int plTotal;

    @Column(name = "pl_used", nullable = false)
    private int plUsed;

    @Column(name = "sl_total", nullable = false)
    private int slTotal;

    @Column(name = "sl_used", nullable = false)
    private int slUsed;

    @Column(name = "unpaid_used", nullable = false)
    private int unpaidUsed;

    @Builder.Default
    @Column(name = "monthly_salary", nullable = false)
    private Double monthlySalary = 30000.0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public int plRemaining() {
        return plTotal - plUsed;
    }

    public int slRemaining() {
        return slTotal - slUsed;
    }
}
