package com.callibrity.cowork.connector.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jwcarman.jpa.entity.BaseEntity;

@Entity
@Table(name = "team")
@Getter
@Setter
@NoArgsConstructor
public class Team extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String displayName;

    private String onCallRotation;

    private String slackChannel;

    public Team(String name, String displayName, String onCallRotation, String slackChannel) {
        this.name = name;
        this.displayName = displayName;
        this.onCallRotation = onCallRotation;
        this.slackChannel = slackChannel;
    }
}
