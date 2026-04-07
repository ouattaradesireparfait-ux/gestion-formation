package com.formation.repository;

import com.formation.model.Inscription;
import com.formation.model.Employe;
import com.formation.model.Session;
import com.formation.enums.StatutInscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface InscriptionRepository extends JpaRepository<Inscription, Long> {
    List<Inscription> findByDemande_Employe(Employe employe);
    List<Inscription> findByEmploye(Employe employe);
    List<Inscription> findByStatut(StatutInscription statut);
    List<Inscription> findByDemandeId(Long demandeId);
    List<Inscription> findBySessionId(Long sessionId);

    // Documents non lus reçus par le responsable
    List<Inscription> findByDocumentFinFormationIsNotNullAndDocumentLuFalse();

    // Toutes les inscriptions avec documents
    List<Inscription> findByDocumentFinFormationIsNotNull();

    // Toutes les inscriptions d'un employé (via demande OU inscription directe)
    @Query("SELECT i FROM Inscription i WHERE " +
            "((i.demande IS NOT NULL AND i.demande.employe = :employe) OR " +
            "(i.demande IS NULL AND i.employe = :employe))")
    List<Inscription> findToutesParEmploye(@Param("employe") Employe employe);
    // Vérifie si un employé est déjà inscrit à une session (via demande ou directe)
    @Query("SELECT i FROM Inscription i WHERE " +
            "((i.demande IS NOT NULL AND i.demande.employe = :employe) OR " +
            "(i.demande IS NULL AND i.employe = :employe)) " +
            "AND i.session = :session " +
            "AND i.statut NOT IN ('ANNULEE', 'EMPECHEMENT')")
    List<Inscription> findInscriptionsActivesParEmployeEtSession(
            @Param("employe") Employe employe,
            @Param("session") Session session);

    // Vérifie si un employé est déjà inscrit à une session active pour une formation donnée
    @Query("SELECT i FROM Inscription i " +
           "WHERE i.demande.employe = :employe " +
           "AND i.demande.formation.id = :formationId " +
           "AND i.statut IN (com.formation.enums.StatutInscription.CONFIRMEE, com.formation.enums.StatutInscription.EN_ATTENTE) " +
           "AND i.session.dateFin >= CURRENT_DATE")
    List<Inscription> findInscriptionsActivesParFormation(
        @Param("employe") Employe employe,
        @Param("formationId") Long formationId);
}
