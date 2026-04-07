package com.formation.controller;

import com.formation.model.*;
import com.formation.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/responsable")
public class ResponsableController {

    @Autowired private ResponsableService responsableService;
    @Autowired private DemandeService demandeService;
    @Autowired private FormationService formationService;
    @Autowired private SessionService sessionService;
    @Autowired private InscriptionService inscriptionService;
    @Autowired private EmployeService employeService;
    @Autowired private FactureService factureService;
    private ResponsableFormation getResponsableConnecte(Authentication auth) {
        return responsableService.trouverParEmail(auth.getName())
            .orElseThrow(() -> new RuntimeException("Responsable non trouvé"));
    }

    // ========== DASHBOARD ==========
    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        ResponsableFormation r = getResponsableConnecte(auth);
        List<DemandeFormation> enAttente = demandeService.trouverDemandesEnAttente();
        long traitees = demandeService.trouverToutes().stream()
            .filter(d -> d.getStatut().name().equals("ACCEPTEE") || d.getStatut().name().equals("REFUSEE"))
            .count();
        long sessionsAVenir = sessionService.trouverSessionsAVenir().size();
        long employesFormes = employeService.trouverTous().stream()
            .mapToInt(Employe::getNombreFormationsSuivies).sum();
        long documentsNonLus = inscriptionService.trouverDocumentsNonLus().size();

        model.addAttribute("username", r.getPrenom() + " " + r.getNom());
        model.addAttribute("demandesEnAttente", enAttente.size());
        model.addAttribute("demandesTraitees", traitees);
        model.addAttribute("sessionsAVenir", sessionsAVenir);
        model.addAttribute("employesFormes", employesFormes);
        model.addAttribute("documentsNonLus", documentsNonLus);
        model.addAttribute("demandesRecentes", enAttente.stream().limit(5).toList());
        return "responsable/dashboard";
    }

    // ========== DEMANDES ==========
    @GetMapping("/demandes")
    public String demandes(Authentication auth, Model model) {
        ResponsableFormation r = getResponsableConnecte(auth);
        model.addAttribute("demandes", demandeService.trouverDemandesEnAttente());
        model.addAttribute("username", r.getPrenom() + " " + r.getNom());
        return "responsable/demandes-recues";
    }

    @GetMapping("/instruire-demande")
    public String instruireDemande(@RequestParam Long demandeId, Authentication auth, Model model) {
        ResponsableFormation r = getResponsableConnecte(auth);
        DemandeFormation demande = demandeService.trouverParId(demandeId)
            .orElseThrow(() -> new RuntimeException("Demande non trouvée"));
        model.addAttribute("demande", demande);
        model.addAttribute("username", r.getPrenom() + " " + r.getNom());
        return "responsable/instruire-demande";
    }

    @PostMapping("/accepter")
    public String accepterDemande(@RequestParam Long demandeId,
            @RequestParam String commentaire,
            @RequestParam(required = false) Double montantFacture,
            RedirectAttributes ra) {
        try {
            demandeService.accepterDemande(demandeId, commentaire, montantFacture);
            ra.addFlashAttribute("success", "Demande acceptée. L'employé peut maintenant choisir sa session.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/responsable/demandes";
    }

    @PostMapping("/refuser")
    public String refuserDemande(@RequestParam Long demandeId,
            @RequestParam String commentaire, RedirectAttributes ra) {
        try {
            demandeService.refuserDemande(demandeId, commentaire);
            ra.addFlashAttribute("success", "Demande refusée.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/responsable/demandes";
    }

    // ========== GESTION EMPLOYES ==========
    @GetMapping("/employes")
    public String listeEmployes(Authentication auth, Model model) {
        ResponsableFormation r = getResponsableConnecte(auth);
        model.addAttribute("employes", employeService.trouverTous());
        model.addAttribute("username", r.getPrenom() + " " + r.getNom());
        return "responsable/employes";
    }

    @GetMapping("/employes/nouveau")
    public String nouvelEmploye(Authentication auth, Model model) {
        ResponsableFormation r = getResponsableConnecte(auth);
        model.addAttribute("username", r.getPrenom() + " " + r.getNom());
        return "responsable/nouvel-employe";
    }

    @PostMapping("/employes/creer")
    public String creerEmploye(@RequestParam String nom, @RequestParam String prenom,
            @RequestParam String email, @RequestParam String motDePasse,
            @RequestParam(required = false) String telephone,
            @RequestParam(required = false) String adresse,
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) String departement,
            @RequestParam(required = false) String poste,
            @RequestParam(required = false) Double salaire,
            @RequestParam(required = false) String matricule,
            RedirectAttributes ra) {
        try {
            Employe emp = new Employe();
            emp.setNom(nom); emp.setPrenom(prenom); emp.setEmail(email);
            emp.setMotDePasse(motDePasse); emp.setTelephone(telephone);
            emp.setAdresse(adresse); emp.setGrade(grade);
            emp.setDepartement(departement); emp.setPoste(poste);
            emp.setSalaire(salaire); emp.setMatricule(matricule);
            employeService.creerEmploye(emp);
            ra.addFlashAttribute("success", "Employé créé avec succès !");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/responsable/employes/nouveau";
        }
        return "redirect:/responsable/employes";
    }

    @PostMapping("/employes/supprimer/{id}")
    public String supprimerEmploye(@PathVariable Long id, RedirectAttributes ra) {
        try {
            employeService.supprimerEmploye(id);
            ra.addFlashAttribute("success", "Employé supprimé.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "\"Cet employé ne peut pas être supprimé car il a des inscriptions ou des factures enregistrées à son nom. Veuillez d'abord supprimer ses inscriptions et factures associées.");
        }
        return "redirect:/responsable/employes";
    }

    // ========== INSCRIPTION DIRECTE ==========
    @GetMapping("/inscription")
    public String inscriptionDirecte(@RequestParam(required = false) Long employeId,
                                     Authentication auth, Model model) {
        ResponsableFormation r = getResponsableConnecte(auth);
        model.addAttribute("employes", employeService.trouverTous());
        model.addAttribute("formations", formationService.trouverToutes());
        model.addAttribute("employeIdSelectionne", employeId);
        model.addAttribute("username", r.getPrenom() + " " + r.getNom());
        return "responsable/inscription";
    }

    @PostMapping("/inscription")
    public String inscrireEmploye(@RequestParam Long employeId,
            @RequestParam Long sessionId,
            @RequestParam(required = false) String appreciation,
            RedirectAttributes ra) {
        try {
            inscriptionService.inscrireDirectement(employeId, sessionId, appreciation);
            ra.addFlashAttribute("success", "Employé inscrit avec succès !");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/responsable/inscription";
    }

    // ========== SESSIONS (ancien rôle organisme) ==========
    @GetMapping("/sessions")
    public String sessions(Authentication auth, Model model) {
        ResponsableFormation r = getResponsableConnecte(auth);
        model.addAttribute("sessions", sessionService.trouverToutes());
        model.addAttribute("username", r.getPrenom() + " " + r.getNom());
        return "responsable/sessions";
    }

    @GetMapping("/sessions/nouvelle")
    public String nouvelleSession(Authentication auth, Model model) {
        ResponsableFormation r = getResponsableConnecte(auth);
        model.addAttribute("formations", formationService.trouverToutes());
        model.addAttribute("username", r.getPrenom() + " " + r.getNom());
        return "responsable/nouvelle-session";
    }

    @PostMapping("/sessions/nouvelle")
    public String creerSession(Authentication auth,
            @RequestParam Long formationId,
            @RequestParam java.time.LocalDate dateDebut,
            @RequestParam java.time.LocalDate dateFin,
            @RequestParam String lieu,
            @RequestParam Integer placesMax,
            RedirectAttributes ra) {
        Session session = new Session();
        session.setDateDebut(dateDebut);
        session.setDateFin(dateFin);
        session.setLieu(lieu);
        session.setPlacesMax(placesMax);
        try {
            sessionService.creerSession(session, formationId);
            ra.addFlashAttribute("success", "Session créée avec succès !");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/responsable/sessions";
    }

    @GetMapping("/sessions/modifier/{id}")
    public String afficherModifierSession(@PathVariable Long id, Authentication auth, Model model) {
        ResponsableFormation r = getResponsableConnecte(auth);
        Session sess = sessionService.trouverParId(id)
            .orElseThrow(() -> new RuntimeException("Session non trouvée"));
        model.addAttribute("sess", sess);
        model.addAttribute("username", r.getPrenom() + " " + r.getNom());
        return "responsable/modifier-session";
    }

    @PostMapping("/sessions/modifier/{id}")
    public String modifierSession(@PathVariable Long id,
            @RequestParam java.time.LocalDate dateDebut,
            @RequestParam java.time.LocalDate dateFin,
            @RequestParam String lieu,
            @RequestParam Integer placesMax,
            RedirectAttributes ra) {
        try {
            Session details = new Session();
            details.setDateDebut(dateDebut); details.setDateFin(dateFin);
            details.setLieu(lieu); details.setPlacesMax(placesMax);
            sessionService.modifierSession(id, details);
            ra.addFlashAttribute("success", "Session modifiée !");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/responsable/sessions";
    }

    @PostMapping("/sessions/annuler/{id}")
    public String annulerSession(@PathVariable Long id, RedirectAttributes ra) {
        try {
            sessionService.annulerEtSupprimerSession(id);
            ra.addFlashAttribute("success", "Session supprimée. Les employés inscrits ont été notifiés et peuvent rechoisir une session.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/responsable/sessions";
    }

    @GetMapping("/sessions/{id}/inscrits")
    public String inscrits(@PathVariable Long id, Authentication auth, Model model) {
        ResponsableFormation r = getResponsableConnecte(auth);
        Session sess = sessionService.trouverParId(id)
            .orElseThrow(() -> new RuntimeException("Session non trouvée"));
        model.addAttribute("sess", sess);
        model.addAttribute("inscriptions", inscriptionService.trouverParSession(id));
        model.addAttribute("username", r.getPrenom() + " " + r.getNom());
        return "responsable/inscrits";
    }

    // ========== SESSIONS PAR FORMATION (AJAX) ==========
    @GetMapping("/sessions-par-formation")
    @ResponseBody
    public List<Map<String, Object>> sessionsParFormation(@RequestParam Long formationId) {
        List<Map<String, Object>> result = new ArrayList<>();
        sessionService.trouverParFormation(formationId).stream()
            .filter(Session::estDisponible)
            .filter(s -> s.getDateDebut() != null && s.getDateDebut().isAfter(java.time.LocalDate.now()))
            .forEach(s -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", s.getId());
                m.put("dateDebut", s.getDateDebut().toString());
                m.put("dateFin", s.getDateFin().toString());
                m.put("lieu", s.getLieu());
                m.put("placesDisponibles", s.getPlacesDisponibles());
                result.add(m);
            });
        return result;
    }

    // ========== CATALOGUE ==========
    @GetMapping("/catalogue")
    public String catalogue(Authentication auth, Model model) {
        ResponsableFormation r = getResponsableConnecte(auth);
        model.addAttribute("formations", formationService.trouverToutes());
        model.addAttribute("username", r.getPrenom() + " " + r.getNom());
        return "responsable/catalogue";
    }

    @PostMapping("/catalogue/ajouter")
    public String ajouterFormation(@ModelAttribute Formation formation, RedirectAttributes ra) {
        try {
            formationService.creerFormation(formation);
            ra.addFlashAttribute("success", "Formation ajoutée !");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/responsable/catalogue";
    }

    @PostMapping("/catalogue/modifier/{id}")
    public String modifierFormation(@PathVariable Long id,
            @ModelAttribute Formation formation, RedirectAttributes ra) {
        try {
            formationService.modifierFormation(id, formation);
            ra.addFlashAttribute("success", "Formation modifiée.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/responsable/catalogue";
    }

    @PostMapping("/catalogue/supprimer/{id}")
    public String supprimerFormation(@PathVariable Long id, RedirectAttributes ra) {
        try {
            formationService.supprimerFormation(id);
            ra.addFlashAttribute("success", "Formation supprimée.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/responsable/catalogue";
    }

    // ========== DOCUMENTS FIN DE FORMATION ==========
    @GetMapping("/documents")
    public String documents(Authentication auth, Model model) {
        ResponsableFormation r = getResponsableConnecte(auth);
        model.addAttribute("documents", inscriptionService.trouverTousDocuments());
        model.addAttribute("nbNonLus", inscriptionService.trouverDocumentsNonLus().size());
        model.addAttribute("username", r.getPrenom() + " " + r.getNom());
        return "responsable/documents";
    }

    @PostMapping("/documents/marquer-lu/{id}")
    public String marquerDocumentLu(@PathVariable Long id, RedirectAttributes ra) {
        try {
            inscriptionService.marquerDocumentLu(id);
            ra.addFlashAttribute("success", "Document marqué comme lu.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/responsable/documents";
    }

    @GetMapping("/documents/telecharger/{id}")
    public ResponseEntity<Resource> telechargerDocument(@PathVariable Long id) {
        try {
            Inscription insc = inscriptionService.trouverParId(id)
                .orElseThrow(() -> new RuntimeException("Inscription non trouvée"));
            inscriptionService.marquerDocumentLu(id);
            Path filePath = Paths.get(insc.getDocumentFinFormation());
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) throw new RuntimeException("Fichier introuvable");
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + insc.getNomDocumentFinFormation() + "\"")
                .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
