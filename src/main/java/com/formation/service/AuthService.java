package com.formation.service;

import com.formation.model.Utilisateur;
import com.formation.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class AuthService {

    @Autowired private UtilisateurRepository utilisateurRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    public Optional<Utilisateur> getUtilisateurParEmail(String email) {
        return utilisateurRepository.findByEmail(email);
    }

    public boolean changerMotDePasse(String email, String ancien, String nouveau) {
        return utilisateurRepository.findByEmail(email).map(u -> {
            if (passwordEncoder.matches(ancien, u.getMotDePasse())) {
                u.setMotDePasse(passwordEncoder.encode(nouveau));
                utilisateurRepository.save(u);
                return true;
            }
            return false;
        }).orElse(false);
    }
}
