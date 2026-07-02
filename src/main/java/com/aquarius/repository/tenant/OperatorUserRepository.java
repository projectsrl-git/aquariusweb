package com.aquarius.repository.tenant;

import com.aquarius.entity.tenant.OperatorUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OperatorUserRepository extends JpaRepository<OperatorUser, String> {

    /** Lookup case-insensitive sul CODICE legacy (come fa PASS.SCX). */
    Optional<OperatorUser> findByCodeIgnoreCase(String code);
}
