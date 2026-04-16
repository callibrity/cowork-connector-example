package com.callibrity.cowork.connector.catalog.repository;

import com.callibrity.cowork.connector.catalog.domain.Team;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, UUID> {

    Optional<Team> findByName(String name);
}
