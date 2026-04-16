package com.callibrity.cowork.connector.catalog.repository;

import com.callibrity.cowork.connector.catalog.domain.Dependency;
import com.callibrity.cowork.connector.catalog.domain.Service;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DependencyRepository extends JpaRepository<Dependency, UUID> {

    List<Dependency> findAllByFromService(Service fromService);

    List<Dependency> findAllByToService(Service toService);
}
