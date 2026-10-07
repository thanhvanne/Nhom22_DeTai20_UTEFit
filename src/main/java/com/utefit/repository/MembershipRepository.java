package com.utefit.repository;

import com.utefit.entity.Membership;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipRepository
        extends JpaRepository<Membership, Long> {
}