package com.formation.controller;

import com.formation.model.Facture;
import com.formation.model.ResponsableFormation;
import com.formation.service.FactureService;
import com.formation.service.ResponsableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.Optional;

@Controller
public class FactureController {

    @Autowired private FactureService factureService;
    @Autowired private ResponsableService responsableService;

    private String getUsername(Authentication auth) {
        if (auth == null) return "";
        return responsableService.trouverParEmail(auth.getName())
            .map(r -> r.getPrenom() + " " + r.getNom())
            .orElse(auth.getName());
    }

    // ─── Responsable : liste toutes les factures ──────────────────────────────
    @GetMapping("/responsable/factures")
    public String listeFactures(Authentication auth, Model model) {
        model.addAttribute("factures", factureService.getToutesFactures());
        model.addAttribute("totalNonPayees", factureService.getTotalNonPayees());
        model.addAttribute("totalPayees", factureService.getTotalPayees());
        model.addAttribute("username", getUsername(auth));
        return "responsable/factures";
    }

    // ─── Responsable : marquer payée ─────────────────────────────────────────
    @PostMapping("/responsable/factures/{id}/payer")
    public String payer(@PathVariable Long id, RedirectAttributes ra) {
        try {
            factureService.marquerPayee(id);
            ra.addFlashAttribute("success", "Facture marquée comme payée.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/responsable/factures";
    }

    // ─── Responsable : annuler paiement ──────────────────────────────────────
    @PostMapping("/responsable/factures/{id}/annuler-paiement")
    public String annulerPaiement(@PathVariable Long id, RedirectAttributes ra) {
        try {
            factureService.marquerNonPayee(id);
            ra.addFlashAttribute("success", "Paiement annulé.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/responsable/factures";
    }

    // ─── Responsable : supprimer facture ─────────────────────────────────────
    @PostMapping("/responsable/factures/{id}/supprimer")
    public String supprimer(@PathVariable Long id, RedirectAttributes ra) {
        try {
            factureService.supprimer(id);
            ra.addFlashAttribute("success", "Facture supprimée.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/responsable/factures";
    }

    // ─── Responsable : voir une facture ──────────────────────────────────────────
    @GetMapping("/responsable/factures/{id}/voir")
    public String voirFactureResponsable(@PathVariable Long id, Authentication auth, Model model) {
        Facture facture = factureService.findById(id)
                .orElseThrow(() -> new RuntimeException("Facture introuvable"));
        model.addAttribute("facture", facture);
        model.addAttribute("username", getUsername(auth));
        model.addAttribute("estResponsable", true);
        return "responsable/facture-detail";
    }

    // ─── Employé : voir sa facture ────────────────────────────────────────────
    @GetMapping("/employe/inscription/{inscriptionId}/facture")
    public String voirFacture(@PathVariable Long inscriptionId, Authentication auth, Model model) {
        Optional<Facture> factureOpt = factureService.findByInscriptionId(inscriptionId);
        if (factureOpt.isEmpty()) {
            model.addAttribute("error", "La facture associée à cette inscription n'existe plus. Elle a peut-être été supprimée par le responsable.");
            model.addAttribute("username", auth != null ? auth.getName() : "");
            return "employe/mes-inscriptions";
        }
        model.addAttribute("facture", factureOpt.get());
        model.addAttribute("username", auth != null ? auth.getName() : "");
        return "employe/facture";
    }
}
