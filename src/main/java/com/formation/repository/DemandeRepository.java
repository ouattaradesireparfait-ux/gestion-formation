package com.formation.repository;

import com.formation.model.DemandeFormation;
import com.formation.model.Employe;
import com.formation.model.Formation;
import com.formation.enums.StatutDemande;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DemandeRepository extends JpaRepository<DemandeFormation, Long> {
    List<DemandeFormation> findByEmploye(Employe employe);
    List<DemandeFormation> findByStatut(StatutDemande statut);
    List<DemandeFormation> findByStatutOrderByDateDemandeAsc(StatutDemande statut);
    List<DemandeFormation> findByEmployeOrderByDateDemandeDesc(Employe employe);
    List<DemandeFormation> findByEmployeAndFormation(Employe employe, Formation formation);
}
