package com.aquarius.repository.tenant;

import com.aquarius.entity.tenant.WebUserCredentials;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WebUserCredentialsRepository extends JpaRepository<WebUserCredentials, Long> {

    Optional<WebUserCredentials> findByOperatorCodeIgnoreCase(String operatorCode);

    Optional<WebUserCredentials> findByResetToken(String resetToken);
}
