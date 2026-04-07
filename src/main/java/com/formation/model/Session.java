package com.formation.model;

import com.formation.enums.StatutInscription;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Entity
@Data
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String lieu;
    private Integer placesMax;
    private Integer placesDisponibles;

    @Enumerated(EnumType.STRING)
    private StatutInscription statut = StatutInscription.CONFIRMEE;

    @ManyToOne
    @JoinColumn(name = "formation_id")
    private Formation formation;

    @ManyToOne
    @JoinColumn(name = "organisme_id")
    private OrganismeFormation organisme;

    @OneToMany(mappedBy = "session")
    private List<Inscription> inscriptions;

    public boolean estDisponible() {
        return placesDisponibles != null && placesDisponibles > 0
            && statut != StatutInscription.ANNULEE
            && statut != StatutInscription.TERMINEE;
    }

    public void reserverPlace() {
        if (estDisponible()) placesDisponibles--;
    }

    public void libererPlace() {
        if (placesMax != null && placesDisponibles < placesMax) placesDisponibles++;
    }
}
