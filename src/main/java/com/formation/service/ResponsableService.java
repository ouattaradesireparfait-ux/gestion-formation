package com.formation.service;

import com.formation.model.ResponsableFormation;
import com.formation.repository.ResponsableRepository;
import com.formation.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class ResponsableService {

    @Autowired private ResponsableRepository responsableRepository;
    @Autowired private UtilisateurRepository utilisateurRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    public Optional<ResponsableFormation> trouverParEmail(String email) {
        return responsableRepository.findByEmail(email);
    }

    public Optional<ResponsableFormation> trouverParId(Long id) {
        return responsableRepository.findById(id);
    }

    public boolean changerMotDePasse(String email, String ancien, String nouveau) {
        return responsableRepository.findByEmail(email).map(r -> {
            if (passwordEncoder.matches(ancien, r.getMotDePasse())) {
                r.setMotDePasse(passwordEncoder.encode(nouveau));
                responsableRepository.save(r);
                return true;
            }
            return false;
        }).orElse(false);
    }
}
