package com.formation.service;

import com.formation.model.OrganismeFormation;
import com.formation.repository.OrganismeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class OrganismeService {

    @Autowired private OrganismeRepository organismeRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    public Optional<OrganismeFormation> trouverParEmail(String email) {
        return organismeRepository.findByEmail(email);
    }

    public Optional<OrganismeFormation> trouverParId(Long id) {
        return organismeRepository.findById(id);
    }

    public boolean changerMotDePasse(String email, String ancien, String nouveau) {
        return organismeRepository.findByEmail(email).map(o -> {
            if (passwordEncoder.matches(ancien, o.getMotDePasse())) {
                o.setMotDePasse(passwordEncoder.encode(nouveau));
                organismeRepository.save(o);
                return true;
            }
            return false;
        }).orElse(false);
    }
}
