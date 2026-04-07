package com.formation.service;

import com.formation.enums.StatutDemande;
import com.formation.enums.StatutInscription;
import com.formation.model.*;
import com.formation.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class InscriptionService {

    @Autowired private InscriptionRepository inscriptionRepository;
    @Autowired private DemandeRepository demandeRepository;
    @Autowired private SessionRepository sessionRepository;
    @Autowired private EmployeRepository employeRepository;
    @Autowired private FactureRepository factureRepository;

    private static final String UPLOAD_DIR = "uploads/documents/";

    // ── Inscription depuis une demande acceptée ───────────────────────────────
    public Inscription inscrireDepuisDemande(Long demandeId, Long sessionId, Double montantFacture) {
        DemandeFormation demande = demandeRepository.findById(demandeId)
            .orElseThrow(() -> new RuntimeException("Demande non trouvée"));
        Session session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Session non trouvée"));

        if (demande.getStatut() != StatutDemande.ACCEPTEE) {
            throw new RuntimeException("La demande doit être acceptée pour choisir une session.");
        }
        if (!session.estDisponible()) {
            throw new RuntimeException("Plus de places disponibles pour cette session.");
        }

        // Ignorer les anciennes inscriptions EMPECHEMENT ou ANNULEE
        List<Inscription> existantes = inscriptionRepository.findByDemandeId(demandeId)
            .stream()
            .filter(i -> i.getStatut() == StatutInscription.CONFIRMEE
                      || i.getStatut() == StatutInscription.EN_ATTENTE
                      || i.getStatut() == StatutInscription.TERMINEE)
            .toList();
        if (!existantes.isEmpty()) {
            return existantes.get(0);
        }

        Inscription inscription = new Inscription();
        inscription.setDemande(demande);
        inscription.setSession(session);
        inscription.setDateInscription(LocalDate.now());
        inscription.setStatut(StatutInscription.CONFIRMEE);

        session.reserverPlace();
        sessionRepository.save(session);

        Employe employe = demande.getEmploye();
        employe.setNombreFormationsSuivies(employe.getNombreFormationsSuivies() + 1);
        employeRepository.save(employe);

        demande.setStatut(StatutDemande.VALIDEE);
        demande.setSessionChoisie(session);
        demandeRepository.save(demande);

        // IMPORTANT : sauvegarder l'inscription AVANT de créer la facture
        Inscription saved = inscriptionRepository.save(inscription);

        // Générer la facture si un montant est fourni
        if (montantFacture != null && montantFacture > 0) {
            boolean factureExistante = factureRepository.findByInscriptionId(saved.getId()).isPresent();
            if (!factureExistante) {
                Facture facture = new Facture();
                facture.setInscription(saved);
                facture.setMontant(montantFacture);
                facture.setDateFacture(LocalDate.now());
                long count = factureRepository.count() + 1;
                String mois = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
                facture.setNumeroFacture("FACT-" + mois + "-" + String.format("%04d", count));
                factureRepository.save(facture);
            }
        }

        return saved;
    }

    // ── Inscription directe par le responsable ────────────────────────────────
    public Inscription inscrireDirectement(Long employeId, Long sessionId, String appreciation) {
        Employe employe = employeRepository.findById(employeId)
            .orElseThrow(() -> new RuntimeException("Employé non trouvé"));
        Session session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Session non trouvée"));

        if (!session.estDisponible()) {
            throw new RuntimeException("Plus de places disponibles pour cette session.");
        }

        // Vérifier qu'il n'est pas déjà inscrit à cette session
        List<Inscription> dejainscrit = inscriptionRepository
            .findInscriptionsActivesParEmployeEtSession(employe, session);
        if (!dejainscrit.isEmpty()) {
            throw new RuntimeException("Cet employé est déjà inscrit à cette session.");
        }

        Inscription inscription = new Inscription();
        inscription.setEmploye(employe); // lier l'employé directement
        inscription.setSession(session);
        inscription.setDateInscription(LocalDate.now());
        inscription.setStatut(StatutInscription.CONFIRMEE);
        inscription.setAppreciation(appreciation);

        session.reserverPlace();
        sessionRepository.save(session);

        employe.setNombreFormationsSuivies(employe.getNombreFormationsSuivies() + 1);
        employeRepository.save(employe);

        return inscriptionRepository.save(inscription);
    }

    // ── Empêchement signalé par l'employé ─────────────────────────────────────
    public Inscription signalerEmpechement(Long inscriptionId, String motif) {
        Inscription inscription = inscriptionRepository.findById(inscriptionId)
            .orElseThrow(() -> new RuntimeException("Inscription non trouvée"));

        if (inscription.getStatut() != StatutInscription.CONFIRMEE) {
            throw new RuntimeException("Seule une inscription confirmée peut faire l'objet d'un empêchement.");
        }

        // Mettre à jour le statut de l'inscription
        inscription.setStatut(StatutInscription.EMPECHEMENT);
        inscription.setMotifEmpechement(motif);
        inscription.setDateEmpechement(LocalDate.now());

        // Libérer la place dans la session explicitement
        Session session = inscription.getSession();
        if (session != null) {
            session.libererPlace();
            sessionRepository.save(session);
        }

        // Traitement selon le type d'inscription
        if (inscription.getDemande() != null) {
            Employe emp = inscription.getDemande().getEmploye();
            if (emp.getNombreFormationsSuivies() > 0) {
                emp.setNombreFormationsSuivies(emp.getNombreFormationsSuivies() - 1);
                employeRepository.save(emp);
            }
            // Remettre la demande en ACCEPTEE pour que l'employé puisse rechoisir
            DemandeFormation demande = inscription.getDemande();
            demande.setStatut(StatutDemande.ACCEPTEE);
            demande.setSessionChoisie(null);
            demandeRepository.save(demande);
        } else if (inscription.getEmploye() != null) {
            Employe emp = inscription.getEmploye();
            if (emp.getNombreFormationsSuivies() > 0) {
                emp.setNombreFormationsSuivies(emp.getNombreFormationsSuivies() - 1);
                employeRepository.save(emp);
            }
        }
        // Supprimer la facture liée à cette inscription si elle existe
        factureRepository.findByInscriptionId(inscriptionId).ifPresent(factureRepository::delete);
        return inscriptionRepository.save(inscription);
    }

    // ── Transmission de document ──────────────────────────────────────────────
    public Inscription transmettreDocument(Long inscriptionId, MultipartFile fichier) throws IOException {
        Inscription inscription = inscriptionRepository.findById(inscriptionId)
            .orElseThrow(() -> new RuntimeException("Inscription non trouvée"));

        if (inscription.getStatut() != StatutInscription.CONFIRMEE
            && inscription.getStatut() != StatutInscription.TERMINEE) {
            throw new RuntimeException("Vous ne pouvez transmettre un document que pour une inscription confirmée ou terminée.");
        }

        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

        String originalName = fichier.getOriginalFilename();
        String ext = originalName != null && originalName.contains(".")
            ? originalName.substring(originalName.lastIndexOf(".")) : ".pdf";
        String nomFichier = UUID.randomUUID() + ext;
        Path filePath = uploadPath.resolve(nomFichier);
        Files.copy(fichier.getInputStream(), filePath);

        inscription.setDocumentFinFormation(filePath.toString());
        inscription.setNomDocumentFinFormation(originalName);
        inscription.setDateTransmissionDocument(LocalDate.now());
        inscription.setDocumentLu(false);

        return inscriptionRepository.save(inscription);
    }

    // ── Marquer document lu ───────────────────────────────────────────────────
    public void marquerDocumentLu(Long inscriptionId) {
        Inscription inscription = inscriptionRepository.findById(inscriptionId)
            .orElseThrow(() -> new RuntimeException("Inscription non trouvée"));
        inscription.setDocumentLu(true);
        inscriptionRepository.save(inscription);
    }

    public Inscription annulerInscription(Long id) {
        Inscription inscription = inscriptionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Inscription non trouvée"));
        inscription.annuler();
        sessionRepository.save(inscription.getSession());
        return inscriptionRepository.save(inscription);
    }

    // Retourne TOUTES les inscriptions d'un employé (demande + directes)
    public List<Inscription> trouverParEmploye(Long employeId) {
        Employe emp = employeRepository.findById(employeId)
            .orElseThrow(() -> new RuntimeException("Employé non trouvé"));
        return inscriptionRepository.findToutesParEmploye(emp);
    }

    public List<Inscription> trouverParSession(Long sessionId) {
        return inscriptionRepository.findBySessionId(sessionId);
    }

    public List<Inscription> trouverDocumentsNonLus() {
        return inscriptionRepository.findByDocumentFinFormationIsNotNullAndDocumentLuFalse();
    }

    public List<Inscription> trouverTousDocuments() {
        return inscriptionRepository.findByDocumentFinFormationIsNotNull();
    }

    public Optional<Inscription> trouverParId(Long id) { return inscriptionRepository.findById(id); }
    public List<Inscription> trouverToutes() { return inscriptionRepository.findAll(); }
}
