package com.formation.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Data
public class Formation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String codeFormation;

    private String nomFormation;

    @Column(length = 2000)
    private String description;

    private Integer dureeHeures;
    private String objectifs;
    private String prerequisites;

    @OneToMany(mappedBy = "formation", cascade = CascadeType.ALL)
    private List<Session> sessions;

    @OneToMany(mappedBy = "formation")
    private List<DemandeFormation> demandes;
}
