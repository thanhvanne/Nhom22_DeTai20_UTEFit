package com.utefit.repository;

import com.utefit.entity.ManagerBranchAssignment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ManagerBranchAssignmentRepository extends JpaRepository<ManagerBranchAssignment, Long> {
    @EntityGraph(attributePaths = "branch")
    List<ManagerBranchAssignment> findByManager_IdOrderByBranch_NameAsc(Long managerId);
    Optional<ManagerBranchAssignment> findByManager_IdAndBranch_Id(Long managerId, Long branchId);
}
