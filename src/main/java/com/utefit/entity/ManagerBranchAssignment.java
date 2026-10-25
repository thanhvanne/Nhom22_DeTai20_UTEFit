package com.utefit.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "manager_branch_assignments", uniqueConstraints = @UniqueConstraint(
        name = "uk_manager_branch", columnNames = {"manager_id", "branch_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerBranchAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "manager_id", nullable = false)
    private User manager;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;
}
