package com.formation.repository;

import com.formation.model.Formation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FormationRepository extends JpaRepository<Formation, Long> {
    Optional<Formation> findByCodeFormation(String codeFormation);
    List<Formation> findByNomFormationContainingIgnoreCase(String nom);
}
