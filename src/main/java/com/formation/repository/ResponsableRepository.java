package com.formation.repository;

import com.formation.model.ResponsableFormation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ResponsableRepository extends JpaRepository<ResponsableFormation, Long> {
    Optional<ResponsableFormation> findByEmail(String email);
}
