package com.formation.controller;

import com.formation.model.*;
import com.formation.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/employe")
public class EmployeController {

    @Autowired private EmployeService employeService;
    @Autowired private DemandeService demandeService;
    @Autowired private FormationService formationService;
    @Autowired private SessionService sessionService;
    @Autowired private InscriptionService inscriptionService;
    @Autowired private FactureService factureService;

    private Employe getEmployeConnecte(Authentication auth) {
        return employeService.trouverParEmail(auth.getName())
            .orElseThrow(() -> new RuntimeException("Employé non trouvé"));
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        Employe emp = getEmployeConnecte(auth);
        List<DemandeFormation> demandes = demandeService.trouverParEmploye(emp.getId());
        long enCours = demandes.stream()
            .filter(d -> d.getStatut().name().equals("EN_ATTENTE") || d.getStatut().name().equals("EN_INSTRUCTION"))
            .count();
        long enAttente = demandes.stream().filter(d -> d.getStatut().name().equals("EN_ATTENTE")).count();

        model.addAttribute("username", emp.getPrenom() + " " + emp.getNom());
        model.addAttribute("nbDemandes", enCours);
        model.addAttribute("nbFormationsSuivies", emp.getNombreFormationsSuivies());
        model.addAttribute("nbEnAttente", enAttente);
        model.addAttribute("peutDemander", employeService.peutFaireDemande(emp.getId()));
        return "employe/dashboard";
    }

    @GetMapping("/demandes")
    public String demandes(Authentication auth, Model model) {
        Employe emp = getEmployeConnecte(auth);
        model.addAttribute("demandes", demandeService.trouverParEmploye(emp.getId()));
        model.addAttribute("username", emp.getPrenom() + " " + emp.getNom());
        return "employe/demandes";
    }

    @GetMapping("/demande/details/{id}")
    public String detailsDemande(@PathVariable Long id, Authentication auth, Model model) {
        Employe emp = getEmployeConnecte(auth);
        DemandeFormation demande = demandeService.trouverParId(id)
            .orElseThrow(() -> new RuntimeException("Demande non trouvée"));
        if (!demande.getEmploye().getId().equals(emp.getId()))
            throw new RuntimeException("Accès non autorisé");
        model.addAttribute("demande", demande);
        model.addAttribute("username", emp.getPrenom() + " " + emp.getNom());
        return "employe/demande-details";
    }

    @PostMapping("/demande/annuler/{id}")
    public String annulerDemande(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        Employe emp = getEmployeConnecte(auth);
        DemandeFormation d = demandeService.trouverParId(id)
            .orElseThrow(() -> new RuntimeException("Demande non trouvée"));
        if (!d.getEmploye().getId().equals(emp.getId()))
            throw new RuntimeException("Accès non autorisé");
        try {
            demandeService.annulerDemande(id);
            ra.addFlashAttribute("success", "Demande annulée.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/employe/demandes";
    }

    @PostMapping("/demande/supprimer/{id}")
    public String supprimerDemande(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        Employe emp = getEmployeConnecte(auth);
        com.formation.model.DemandeFormation d = demandeService.trouverParId(id)
            .orElseThrow(() -> new RuntimeException("Demande non trouvée"));
        if (!d.getEmploye().getId().equals(emp.getId()))
            throw new RuntimeException("Accès non autorisé");
        // On ne peut supprimer que les demandes refusées ou annulées
        if (d.getStatut() != com.formation.enums.StatutDemande.REFUSEE
            && d.getStatut() != com.formation.enums.StatutDemande.ANNULEE) {
            ra.addFlashAttribute("error", "Seules les demandes refusées ou annulées peuvent être supprimées.");
            return "redirect:/employe/demandes";
        }
        try {
            demandeService.supprimerDemande(id);
            ra.addFlashAttribute("success", "Demande supprimée de votre historique.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/employe/demandes";
    }

    @GetMapping("/catalogue")
    public String catalogue(Authentication auth, Model model) {
        Employe emp = getEmployeConnecte(auth);
        model.addAttribute("formations", formationService.trouverToutes());
        model.addAttribute("username", emp.getPrenom() + " " + emp.getNom());
        return "employe/catalogue";
    }

    @GetMapping("/nouvelle-demande")
    public String nouvelleDemande(@RequestParam(required = false) Long formationId,
                                  Authentication auth, Model model) {
        Employe emp = getEmployeConnecte(auth);
        if (!employeService.peutFaireDemande(emp.getId())) {
            model.addAttribute("error", "Vous avez atteint la limite de 3 formations par an.");
        }
        model.addAttribute("formations", formationService.trouverToutes());
        model.addAttribute("formationId", formationId);
        model.addAttribute("username", emp.getPrenom() + " " + emp.getNom());
        return "employe/nouvelle-demande";
    }

    @PostMapping("/nouvelle-demande")
    public String creerDemande(Authentication auth,
            @RequestParam Long formationId,
            @RequestParam String motivation,
            RedirectAttributes ra) {
        Employe emp = getEmployeConnecte(auth);
        try {
            demandeService.creerDemande(emp.getId(), formationId, motivation);
            ra.addFlashAttribute("success", "Votre demande a été envoyée avec succès !");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/employe/demandes";
    }

    // L'employé choisit sa session après acceptation de sa demande
    @GetMapping("/choisir-session")
    public String choisirSession(@RequestParam Long demandeId, Authentication auth, Model model) {
        Employe emp = getEmployeConnecte(auth);
        DemandeFormation demande = demandeService.trouverParId(demandeId)
            .orElseThrow(() -> new RuntimeException("Demande non trouvée"));
        if (!demande.getEmploye().getId().equals(emp.getId()))
            throw new RuntimeException("Accès non autorisé");
        List<Session> sessions = sessionService.trouverParFormation(demande.getFormation().getId())
            .stream()
            .filter(Session::estDisponible)
            .filter(s -> s.getDateDebut() != null && s.getDateDebut().isAfter(java.time.LocalDate.now()))
            .toList();
        model.addAttribute("demande", demande);
        model.addAttribute("sessions", sessions);
        model.addAttribute("username", emp.getPrenom() + " " + emp.getNom());
        return "employe/choisir-session";
    }

    @PostMapping("/choisir-session")
    public String validerSession(@RequestParam Long demandeId, @RequestParam Long sessionId,
                                 RedirectAttributes ra) {
        try {
            // Récupérer le montant stocké dans la demande
            DemandeFormation demande = demandeService.trouverParId(demandeId)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));
            Double montant = demande.getMontantFormation() != null ? demande.getMontantFormation() : 0.0;
            inscriptionService.inscrireDepuisDemande(demandeId, sessionId, montant);
            ra.addFlashAttribute("success", "Session choisie et inscription confirmée !");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/employe/demandes";
    }

    @GetMapping("/mes-inscriptions")
public String mesInscriptions(Authentication auth, Model model) {
    Employe emp = getEmployeConnecte(auth);
    List<Inscription> inscriptions = inscriptionService.trouverParEmploye(emp.getId());
    model.addAttribute("inscriptions", inscriptions);
    model.addAttribute("username", emp.getPrenom() + " " + emp.getNom());

    // Map inscriptionId -> facture pour afficher le bouton "Ma facture"
    Map<Long, Facture> facturesMap = new HashMap<>();
    for (Inscription insc : inscriptions) {
        factureService.findByInscriptionId(insc.getId())
            .ifPresent(f -> facturesMap.put(insc.getId(), f));
    }
    model.addAttribute("facturesMap", facturesMap);

    return "employe/mes-inscriptions";
}

    @PostMapping("/inscription/empechement/{id}")
    public String signalerEmpechement(@PathVariable Long id,
            @RequestParam String motif, RedirectAttributes ra) {
        try {
            inscriptionService.signalerEmpechement(id, motif);
            ra.addFlashAttribute("success", "Empêchement signalé. La place a été libérée. Vous pouvez maintenant choisir une autre session.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/employe/mes-inscriptions";
    }

    // Transmission de document de fin de formation
    @GetMapping("/transmettre-document")
    public String afficherTransmission(Authentication auth, Model model) {
        Employe emp = getEmployeConnecte(auth);
        // Récupérer les inscriptions confirmées ou terminées (sans document déjà transmis)
        List<Inscription> inscriptionsEligibles = inscriptionService.trouverParEmploye(emp.getId())
            .stream()
            .filter(i -> (i.getStatut().name().equals("CONFIRMEE") || i.getStatut().name().equals("TERMINEE"))
                      && i.getDocumentFinFormation() == null)
            .toList();
        List<Inscription> documentsTransmis = inscriptionService.trouverParEmploye(emp.getId())
            .stream()
            .filter(i -> i.getDocumentFinFormation() != null)
            .toList();
        model.addAttribute("inscriptionsEligibles", inscriptionsEligibles);
        model.addAttribute("documentsTransmis", documentsTransmis);
        model.addAttribute("username", emp.getPrenom() + " " + emp.getNom());
        return "employe/transmettre-document";
    }

    @PostMapping("/transmettre-document")
    public String transmettreDocument(@RequestParam Long inscriptionId,
            @RequestParam("fichier") MultipartFile fichier,
            RedirectAttributes ra) {
        if (fichier.isEmpty()) {
            ra.addFlashAttribute("error", "Veuillez sélectionner un fichier.");
            return "redirect:/employe/transmettre-document";
        }
        try {
            inscriptionService.transmettreDocument(inscriptionId, fichier);
            ra.addFlashAttribute("success", "Document transmis avec succès au responsable !");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur lors de la transmission : " + e.getMessage());
        }
        return "redirect:/employe/transmettre-document";
    }
}
