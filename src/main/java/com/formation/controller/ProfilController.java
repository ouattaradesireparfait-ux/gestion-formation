package com.formation.controller;

import com.formation.dto.PasswordChangeRequest;
import com.formation.model.Utilisateur;
import com.formation.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProfilController {

    @Autowired private AuthService authService;

    @GetMapping("/profil")
    public String profil(Authentication auth, Model model) {
        Utilisateur u = authService.getUtilisateurParEmail(auth.getName())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        model.addAttribute("utilisateur", u);
        model.addAttribute("passwordRequest", new PasswordChangeRequest());
        return switch (u.getRole()) {
            case "EMPLOYE"     -> "employe/profil";
            case "RESPONSABLE" -> "responsable/profil";
            default            -> "profil";
        };
    }

    @PostMapping("/changer-mot-de-passe")
    public String changerMotDePasse(Authentication auth,
            @ModelAttribute PasswordChangeRequest request,
            RedirectAttributes ra) {
        if (!request.isMatching()) {
            ra.addFlashAttribute("error", "Les mots de passe ne correspondent pas.");
            return "redirect:/profil";
        }
        boolean ok = authService.changerMotDePasse(auth.getName(),
                request.getAncienMotDePasse(), request.getNouveauMotDePasse());
        if (ok) ra.addFlashAttribute("success", "Mot de passe modifié avec succès !");
        else    ra.addFlashAttribute("error", "Ancien mot de passe incorrect.");
        return "redirect:/profil";
    }
}
