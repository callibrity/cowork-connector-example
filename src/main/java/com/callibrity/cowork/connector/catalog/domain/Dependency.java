package com.callibrity.cowork.connector.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jwcarman.jpa.entity.BaseEntity;

@Entity
@Table(name = "dependency")
@Getter
@Setter
@NoArgsConstructor
public class Dependency extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "from_service_id", nullable = false)
    private Service fromService;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "to_service_id", nullable = false)
    private Service toService;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DependencyType type;

    public Dependency(Service fromService, Service toService, DependencyType type) {
        this.fromService = fromService;
        this.toService = toService;
        this.type = type;
    }
}
