package com.formation.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

@Entity
@DiscriminatorValue("RESPONSABLE")
@Data
@EqualsAndHashCode(callSuper = true)
public class ResponsableFormation extends Utilisateur {

    private String matricule;
    private LocalDate dateNomination;
    private String decisionNomination;

    @Override
    public String getRole() { return "RESPONSABLE"; }
}
