package com.formation.service;

import com.formation.model.Formation;
import com.formation.repository.FormationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class FormationService {

    @Autowired private FormationRepository formationRepository;

    public Formation creerFormation(Formation f) {
        if (f.getCodeFormation() == null || f.getCodeFormation().isBlank()) {
            long count = formationRepository.count() + 1;
            f.setCodeFormation("F-" + String.format("%03d", count));
        }
        return formationRepository.save(f);
    }

    public Formation modifierFormation(Long id, Formation details) {
        Formation f = trouverParId(id).orElseThrow(() -> new RuntimeException("Formation non trouvée"));
        f.setNomFormation(details.getNomFormation());
        f.setDescription(details.getDescription());
        f.setDureeHeures(details.getDureeHeures());
        f.setObjectifs(details.getObjectifs());
        f.setPrerequisites(details.getPrerequisites());
        return formationRepository.save(f);
    }

    public void supprimerFormation(Long id) { formationRepository.deleteById(id); }

    public Optional<Formation> trouverParId(Long id) { return formationRepository.findById(id); }
    public List<Formation> trouverToutes() { return formationRepository.findAll(); }
    public List<Formation> rechercherParNom(String nom) {
        return formationRepository.findByNomFormationContainingIgnoreCase(nom);
    }
}
