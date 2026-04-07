package com.formation.service;

import com.formation.enums.StatutDemande;
import com.formation.enums.StatutInscription;
import com.formation.model.Formation;
import com.formation.model.Inscription;
import com.formation.model.Session;
import com.formation.repository.DemandeRepository;
import com.formation.repository.EmployeRepository;
import com.formation.repository.FormationRepository;
import com.formation.repository.InscriptionRepository;
import com.formation.repository.SessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class SessionService {

    @Autowired private SessionRepository sessionRepository;
    @Autowired private FormationRepository formationRepository;
    @Autowired private InscriptionRepository inscriptionRepository;
    @Autowired private DemandeRepository demandeRepository;
    @Autowired private EmployeRepository employeRepository;

    public Session creerSession(Session session, Long formationId) {
        Formation formation = formationRepository.findById(formationId)
            .orElseThrow(() -> new RuntimeException("Formation non trouvée"));

        if (session.getDateFin() != null && session.getDateDebut() != null
            && !session.getDateFin().isAfter(session.getDateDebut())) {
            throw new RuntimeException("La date de fin doit être postérieure à la date de début.");
        }

        session.setFormation(formation);
        session.setPlacesDisponibles(session.getPlacesMax());
        session.setStatut(StatutInscription.CONFIRMEE);
        return sessionRepository.save(session);
    }

    public Session modifierSession(Long id, Session details) {
        Session s = trouverParId(id).orElseThrow(() -> new RuntimeException("Session non trouvée"));

        if (s.getDateDebut() != null && !s.getDateDebut().isAfter(LocalDate.now())) {
            throw new RuntimeException("Impossible de modifier une session qui a déjà débuté le "
                + s.getDateDebut() + ".");
        }

        if (details.getDateFin() != null && details.getDateDebut() != null
            && !details.getDateFin().isAfter(details.getDateDebut())) {
            throw new RuntimeException("La date de fin doit être postérieure à la date de début.");
        }

        s.setDateDebut(details.getDateDebut());
        s.setDateFin(details.getDateFin());
        s.setLieu(details.getLieu());
        int diff = details.getPlacesMax() - s.getPlacesMax();
        s.setPlacesMax(details.getPlacesMax());
        s.setPlacesDisponibles(Math.max(0, s.getPlacesDisponibles() + diff));
        return sessionRepository.save(s);
    }

    /**
     * Annule une session et SUPPRIME toutes les inscriptions associées.
     * Remet aussi les demandes liées en statut ACCEPTEE (l'employé peut rechoisir une session).
     * Décrémente le compteur de formations suivies de chaque employé concerné.
     */
    @Transactional
    public void annulerEtSupprimerSession(Long id) {
        Session s = trouverParId(id).orElseThrow(() -> new RuntimeException("Session non trouvée"));

        // Impossible d'annuler une session qui a déjà débuté
        if (s.getDateDebut() != null && !s.getDateDebut().isAfter(LocalDate.now())) {
            throw new RuntimeException("Impossible d'annuler une session qui a déjà débuté le "
                + s.getDateDebut() + ".");
        }

        // Récupérer toutes les inscriptions liées à cette session
        List<Inscription> inscriptions = inscriptionRepository.findBySessionId(id);

        for (Inscription insc : inscriptions) {
            // Décrémenter le compteur de l'employé si l'inscription était confirmée
            if (insc.getStatut() == StatutInscription.CONFIRMEE
                    || insc.getStatut() == StatutInscription.EMPECHEMENT) {
                if (insc.getDemande() != null) {
                    var emp = insc.getDemande().getEmploye();
                    if (emp.getNombreFormationsSuivies() > 0) {
                        emp.setNombreFormationsSuivies(emp.getNombreFormationsSuivies() - 1);
                        employeRepository.save(emp);
                    }
                    // Remettre la demande en ACCEPTEE pour que l'employé puisse rechoisir
                    var demande = insc.getDemande();
                    demande.setStatut(StatutDemande.ACCEPTEE);
                    demande.setSessionChoisie(null);
                    demandeRepository.save(demande);
                }
            }
        }

        // Supprimer toutes les inscriptions liées
        inscriptionRepository.deleteAll(inscriptions);

        // Supprimer la session elle-même
        sessionRepository.delete(s);
    }

    public Optional<Session> trouverParId(Long id) { return sessionRepository.findById(id); }

    public List<Session> trouverParFormation(Long formationId) {
        Formation f = formationRepository.findById(formationId)
            .orElseThrow(() -> new RuntimeException("Formation non trouvée"));
        return sessionRepository.findByFormation(f);
    }

    public List<Session> trouverSessionsAVenir() {
        return sessionRepository.findByDateDebutAfter(LocalDate.now());
    }

    public List<Session> trouverToutes() { return sessionRepository.findAll(); }
}
