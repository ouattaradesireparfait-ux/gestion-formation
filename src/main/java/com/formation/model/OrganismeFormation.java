package com.formation.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

@Entity
@DiscriminatorValue("ORGANISME")
@Data
@EqualsAndHashCode(callSuper = true)
public class OrganismeFormation extends Utilisateur {

    private String nomOrganisme;
    private String registreCommerce;
    private String contactPrincipal;

    @OneToMany(mappedBy = "organisme", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Session> sessions;

    @Override
    public String getRole() { return "ORGANISME"; }
}
