package com.formation.repository;

import com.formation.model.Employe;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EmployeRepository extends JpaRepository<Employe, Long> {
    Optional<Employe> findByEmail(String email);
    Optional<Employe> findByMatricule(String matricule);
    List<Employe> findByDepartement(String departement);
}
