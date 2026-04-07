package com.formation.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

@Entity
@DiscriminatorValue("EMPLOYE")
@Data
@EqualsAndHashCode(callSuper = true)
public class Employe extends Utilisateur {

    private String matricule;
    private String grade;
    private Double salaire;
    private String departement;
    private String poste;
    private int nombreFormationsSuivies = 0;

    @OneToMany(mappedBy = "employe", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DemandeFormation> demandes;

    @Override
    public String getRole() { return "EMPLOYE"; }

    public boolean peutDemander() { return nombreFormationsSuivies < 3; }
}
