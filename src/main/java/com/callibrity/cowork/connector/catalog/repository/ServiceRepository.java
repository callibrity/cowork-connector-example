package com.callibrity.cowork.connector.catalog.repository;

import com.callibrity.cowork.connector.catalog.domain.LifecycleStage;
import com.callibrity.cowork.connector.catalog.domain.Service;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServiceRepository extends JpaRepository<Service, UUID> {

    Optional<Service> findByName(String name);

    Page<Service> findAllByOwnerIsNull(Pageable pageable);

    @Query("""
            select s from Service s
            where (:domain is null or s.domain = :domain)
              and (:lifecycle is null or s.lifecycleStage = :lifecycle)
              and (:tag is null or :tag member of s.tags)
            """)
    Page<Service> search(
            @Param("domain") String domain,
            @Param("lifecycle") LifecycleStage lifecycle,
            @Param("tag") String tag,
            Pageable pageable);

    @Query("""
            select s from Service s
            where s.lifecycleStage = com.callibrity.cowork.connector.catalog.domain.LifecycleStage.DEPRECATED
              and exists (select 1 from Dependency d where d.toService = s)
            """)
    Page<Service> findDeprecatedInUse(Pageable pageable);

    List<Service> findAllByOwnerName(String teamName);
}
