package com.formation.repository;

import com.formation.model.Facture;
import com.formation.model.Inscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface FactureRepository extends JpaRepository<Facture, Long> {

    Optional<Facture> findByInscription(Inscription inscription);

    Optional<Facture> findByInscriptionId(Long inscriptionId);

    List<Facture> findByPayee(boolean payee);

    @Query("SELECT COALESCE(SUM(f.montant), 0) FROM Facture f WHERE f.payee = false")
    Double totalNonPayees();

    @Query("SELECT COALESCE(SUM(f.montant), 0) FROM Facture f WHERE f.payee = true")
    Double totalPayees();
}
