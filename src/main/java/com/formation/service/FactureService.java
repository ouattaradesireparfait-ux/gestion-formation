package com.formation.service;

import com.formation.model.Facture;
import com.formation.model.Inscription;
import com.formation.repository.FactureRepository;
import com.formation.repository.InscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class FactureService {

    @Autowired private FactureRepository factureRepo;
    @Autowired private InscriptionRepository inscriptionRepo;

    /**
     * Génère automatiquement une facture pour une inscription.
     * Appelé dès que le responsable accepte une demande avec un montant.
     */
    public Facture genererFacture(Long inscriptionId, Double montant) {
        Inscription inscription = inscriptionRepo.findById(inscriptionId)
                .orElseThrow(() -> new RuntimeException("Inscription introuvable"));

        // Vérifier si une facture existe déjà pour cette inscription
        Optional<Facture> existante = factureRepo.findByInscriptionId(inscriptionId);
        if (existante.isPresent()) {
            return existante.get();
        }

        Facture f = new Facture();
        f.setInscription(inscription);
        f.setMontant(montant);
        f.setDateFacture(LocalDate.now());
        f.setNumeroFacture(genererNumero());
        return factureRepo.save(f);
    }

    public List<Facture> getToutesFactures() {
        return factureRepo.findAll();
    }

    public Optional<Facture> findByInscriptionId(Long inscriptionId) {
        return factureRepo.findByInscriptionId(inscriptionId);
    }

    public Optional<Facture> findById(Long id) {
        return factureRepo.findById(id);
    }

    public double getTotalNonPayees() {
        Double t = factureRepo.totalNonPayees();
        return t != null ? t : 0.0;
    }

    public double getTotalPayees() {
        Double t = factureRepo.totalPayees();
        return t != null ? t : 0.0;
    }

    public void marquerPayee(Long id) {
        Facture f = factureRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Facture introuvable"));
        f.setPayee(true);
        f.setDatePaiement(LocalDate.now());
        factureRepo.save(f);
    }

    public void marquerNonPayee(Long id) {
        Facture f = factureRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Facture introuvable"));
        f.setPayee(false);
        f.setDatePaiement(null);
        factureRepo.save(f);
    }

    public void supprimer(Long id) {
        factureRepo.deleteById(id);
    }

    private String genererNumero() {
        String mois = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        long count = factureRepo.count() + 1;
        return "FACT-" + mois + "-" + String.format("%04d", count);
    }
}
