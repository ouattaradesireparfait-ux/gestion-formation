package com.formation.service;

import com.formation.enums.StatutDemande;
import com.formation.model.DemandeFormation;
import com.formation.model.Employe;
import com.formation.model.Formation;
import com.formation.repository.DemandeRepository;
import com.formation.repository.EmployeRepository;
import com.formation.repository.FormationRepository;
import com.formation.repository.InscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class DemandeService {

    @Autowired private DemandeRepository demandeRepository;
    @Autowired private EmployeRepository employeRepository;
    @Autowired private FormationRepository formationRepository;
    @Autowired private InscriptionRepository inscriptionRepository;
    @Autowired private EmployeService employeService;

    public DemandeFormation creerDemande(Long employeId, Long formationId, String motivation) {
        if (!employeService.peutFaireDemande(employeId)) {
            throw new RuntimeException("Vous avez atteint la limite de 3 formations par an.");
        }
        Employe employe = employeRepository.findById(employeId)
            .orElseThrow(() -> new RuntimeException("Employé non trouvé"));
        Formation formation = formationRepository.findById(formationId)
            .orElseThrow(() -> new RuntimeException("Formation non trouvée"));

        // Vérifier si l'employé est déjà inscrit à une session active pour cette formation
        List<com.formation.model.Inscription> inscriptionsActives =
            inscriptionRepository.findInscriptionsActivesParFormation(employe, formationId);
        if (!inscriptionsActives.isEmpty()) {
            throw new RuntimeException(
                "Vous êtes déjà inscrit à une session active pour la formation \"" +
                formation.getNomFormation() + "\".");
        }

        // Vérifier si une demande EN_ATTENTE ou ACCEPTEE existe déjà pour cette formation
        List<DemandeFormation> demandesExistantes = demandeRepository.findByEmployeAndFormation(employe, formation);
        boolean demandeActive = demandesExistantes.stream().anyMatch(d ->
            d.getStatut() == StatutDemande.EN_ATTENTE ||
            d.getStatut() == StatutDemande.ACCEPTEE ||
            d.getStatut() == StatutDemande.EN_INSTRUCTION
        );
        if (demandeActive) {
            throw new RuntimeException(
                "Vous avez déjà une demande en cours pour la formation \"" +
                formation.getNomFormation() + "\".");
        }

        DemandeFormation demande = new DemandeFormation();
        demande.setEmploye(employe);
        demande.setFormation(formation);
        demande.setMotivation(motivation);
        demande.setDateDemande(LocalDate.now());
        demande.setStatut(StatutDemande.EN_ATTENTE);
        demande.setNumeroDemande(genererNumero());
        return demandeRepository.save(demande);
    }

    // Le responsable accepte sans choisir de session — c'est à l'employé de le faire
    public DemandeFormation accepterDemande(Long id, String commentaire, Double montantFormation) {
        DemandeFormation d = trouverParId(id).orElseThrow(() -> new RuntimeException("Demande non trouvée"));
        d.setStatut(StatutDemande.ACCEPTEE);
        d.setCommentaireResponsable(commentaire);
        if (montantFormation != null && montantFormation > 0) {
            d.setMontantFormation(montantFormation);
        }
        return demandeRepository.save(d);
    }

    public DemandeFormation refuserDemande(Long id, String commentaire) {
        DemandeFormation d = trouverParId(id).orElseThrow(() -> new RuntimeException("Demande non trouvée"));
        d.setStatut(StatutDemande.REFUSEE);
        d.setCommentaireResponsable(commentaire);
        return demandeRepository.save(d);
    }

    public DemandeFormation annulerDemande(Long id) {
        DemandeFormation d = trouverParId(id).orElseThrow(() -> new RuntimeException("Demande non trouvée"));
        if (d.getStatut() != StatutDemande.EN_ATTENTE) {
            throw new RuntimeException("Seules les demandes en attente peuvent être annulées.");
        }
        d.setStatut(StatutDemande.ANNULEE);
        return demandeRepository.save(d);
    }

    public Optional<DemandeFormation> trouverParId(Long id) { return demandeRepository.findById(id); }

    public List<DemandeFormation> trouverParEmploye(Long employeId) {
        Employe emp = employeRepository.findById(employeId)
            .orElseThrow(() -> new RuntimeException("Employé non trouvé"));
        return demandeRepository.findByEmployeOrderByDateDemandeDesc(emp);
    }

    public List<DemandeFormation> trouverToutes() { return demandeRepository.findAll(); }

    public List<DemandeFormation> trouverDemandesEnAttente() {
        return demandeRepository.findByStatutOrderByDateDemandeAsc(StatutDemande.EN_ATTENTE);
    }

    public void supprimerDemande(Long id) {
        DemandeFormation d = trouverParId(id).orElseThrow(() -> new RuntimeException("Demande non trouvée"));
        if (d.getStatut() != StatutDemande.REFUSEE && d.getStatut() != StatutDemande.ANNULEE) {
            throw new RuntimeException("Seules les demandes refusées ou annulées peuvent être supprimées.");
        }
        demandeRepository.deleteById(id);
    }

    private String genererNumero() {
        long count = demandeRepository.count() + 1;
        return "DEM-" + LocalDate.now().getYear() + "-" + String.format("%04d", count);
    }
}
