package com.formation.repository;

import com.formation.model.Formation;
import com.formation.model.Session;
import com.formation.enums.StatutInscription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface SessionRepository extends JpaRepository<Session, Long> {
    List<Session> findByFormation(Formation formation);
    List<Session> findByDateDebutAfter(LocalDate date);
    List<Session> findByStatutNot(StatutInscription statut);
}
