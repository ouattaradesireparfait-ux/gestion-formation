package com.formation.repository;

import com.formation.model.OrganismeFormation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OrganismeRepository extends JpaRepository<OrganismeFormation, Long> {
    Optional<OrganismeFormation> findByEmail(String email);
    Optional<OrganismeFormation> findByNomOrganisme(String nomOrganisme);
}
