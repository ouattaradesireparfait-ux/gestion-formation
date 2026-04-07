package com.formation.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
public class Facture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String numeroFacture;

    private LocalDate dateFacture;
    private Double montant;
    private boolean payee = false;
    private LocalDate datePaiement;

    // Liée à une inscription (et donc à un employé + une session)
    @OneToOne
    @JoinColumn(name = "inscription_id")
    private Inscription inscription;
}
