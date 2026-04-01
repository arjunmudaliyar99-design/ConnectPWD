package org.connectpwd.consent;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ConsentRepository extends JpaRepository<Consent, UUID> {
}
