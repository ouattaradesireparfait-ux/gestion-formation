package com.formation.service;

import com.formation.model.Employe;
import com.formation.repository.EmployeRepository;
import com.formation.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class EmployeService {

    @Autowired private EmployeRepository employeRepository;
    @Autowired private UtilisateurRepository utilisateurRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    public Employe creerEmploye(Employe employe) {
        if (utilisateurRepository.existsByEmail(employe.getEmail())) {
            throw new RuntimeException("Cet email est déjà utilisé");
        }
        employe.setMotDePasse(passwordEncoder.encode(employe.getMotDePasse()));
        employe.setActif(true);
        employe.setDateCreation(LocalDateTime.now());
        if (employe.getMatricule() == null || employe.getMatricule().isBlank()) {
            long count = employeRepository.count() + 1;
            employe.setMatricule("EMP" + String.format("%04d", count));
        }
        return employeRepository.save(employe);
    }

    public Employe modifierEmploye(Long id, Employe details) {
        Employe emp = trouverParId(id).orElseThrow(() -> new RuntimeException("Employé non trouvé"));
        emp.setNom(details.getNom());
        emp.setPrenom(details.getPrenom());
        emp.setEmail(details.getEmail());
        emp.setTelephone(details.getTelephone());
        emp.setAdresse(details.getAdresse());
        emp.setGrade(details.getGrade());
        emp.setSalaire(details.getSalaire());
        emp.setDepartement(details.getDepartement());
        emp.setPoste(details.getPoste());
        if (details.getMotDePasse() != null && !details.getMotDePasse().isBlank()) {
            emp.setMotDePasse(passwordEncoder.encode(details.getMotDePasse()));
        }
        return employeRepository.save(emp);
    }

    public void supprimerEmploye(Long id) { employeRepository.deleteById(id); }

    public Optional<Employe> trouverParId(Long id) { return employeRepository.findById(id); }
    public Optional<Employe> trouverParEmail(String email) { return employeRepository.findByEmail(email); }
    public List<Employe> trouverTous() { return employeRepository.findAll(); }
    public List<Employe> trouverParDepartement(String dep) { return employeRepository.findByDepartement(dep); }

    public boolean peutFaireDemande(Long id) {
        return trouverParId(id)
            .map(e -> e.getNombreFormationsSuivies() < 3)
            .orElseThrow(() -> new RuntimeException("Employé non trouvé"));
    }

    public void changerMotDePasse(Long id, String nouveau) {
        Employe emp = trouverParId(id).orElseThrow(() -> new RuntimeException("Employé non trouvé"));
        emp.setMotDePasse(passwordEncoder.encode(nouveau));
        employeRepository.save(emp);
    }
}
