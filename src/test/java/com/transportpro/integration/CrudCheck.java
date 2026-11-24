package com.transportpro.integration;

import com.transportpro.dao.AffectationDAO;
import com.transportpro.dao.BusDAO;
import com.transportpro.dao.ChauffeurDAO;
import com.transportpro.dao.EmployeDAO;
import com.transportpro.dao.InscriptionDAO;
import com.transportpro.model.Affectation;
import com.transportpro.model.Bus;
import com.transportpro.model.Chauffeur;
import com.transportpro.model.Employe;
import com.transportpro.model.Inscription;
import com.transportpro.model.Trajet;
import com.transportpro.model.enums.StatutBus;
import com.transportpro.model.enums.StatutPresence;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Vérification manuelle : ajout / lecture / modification / recherche de chaque module et règles métier
 * appliquées par les déclencheurs (RG1 à RG7, BF14). MODIFIE LA BASE : à lancer sur une copie, par exemple
 *   java -Demplogo.db.url=jdbc:mysql://localhost:3306/employgo_test -cp ... com.transportpro.integration.CrudCheck
 */
public class CrudCheck {

    static int pass = 0, fail = 0;

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS " : "FAIL ") + name);
        if (ok) pass++; else fail++;
    }

    interface Step { void run() throws Exception; }

    static void step(String name, Step s) {
        try { s.run(); check(name, true); }
        catch (Exception e) { check(name + " -> " + e.getMessage(), false); }
    }

    static void refused(String name, Step s) {
        try { s.run(); check(name + " (aurait dû être refusé)", false); }
        catch (Exception e) { check(name + " : refusé « " + e.getMessage() + " »", true); }
    }

    static Employe employe(String mat, String prenom) {
        return new Employe(0, mat, "Crud", prenom, mat.toLowerCase() + "@exemple.ma", "0600000099",
                "Agadir", "Test", LocalDate.of(2024, 1, 1), true);
    }

    static Inscription inscription(int idEmploye, int idAffectation) {
        Employe e = new Employe();
        e.setIdEmploye(idEmploye);
        Trajet t = new Trajet();
        t.setIdTrajet(idAffectation);
        return new Inscription(0, null, StatutPresence.NON_RENSEIGNE, e, t);
    }

    public static void main(String[] args) throws Exception {
        Guard.requireTestDatabase();
        EmployeDAO emp = new EmployeDAO();
        BusDAO bus = new BusDAO();
        ChauffeurDAO drv = new ChauffeurDAO();
        AffectationDAO aff = new AffectationDAO();
        InscriptionDAO ins = new InscriptionDAO();

        Employe driverEmp = employe("CRUD-D", "Chauffeur");
        Employe p1 = employe("CRUD-P1", "Passager");
        Employe p2 = employe("CRUD-P2", "Second");
        step("employé : ajout x3", () -> { emp.insert(driverEmp); emp.insert(p1); emp.insert(p2); });
        step("employé : modification", () -> { p1.setDepartement("Qualité"); emp.update(p1); });
        check("employé : relu après modification", "Qualité".equals(emp.findById(p1.getIdEmploye()).getDepartement()));
        check("employé : recherche « CRUD-P »", emp.search("CRUD-P").size() == 2);
        refused("employé : matricule en double", () -> emp.insert(employe("CRUD-P1", "Double")));
        refused("employé : e-mail invalide", () -> { Employe x = employe("CRUD-X", "X"); x.setEmail("pas-un-mail"); emp.insert(x); });

        Bus small = new Bus(0, "CRUD-BUS-1", "Test", "Mini", 1, StatutBus.DISPONIBLE);
        Bus big = new Bus(0, "CRUD-BUS-2", "Test", "Grand", 50, StatutBus.DISPONIBLE);
        Bus broken = new Bus(0, "CRUD-BUS-3", "Test", "Panne", 50, StatutBus.HORS_SERVICE);
        step("bus : ajout x3", () -> { bus.insert(small); bus.insert(big); bus.insert(broken); });
        step("bus : modification", () -> { big.setModele("Grand 2"); bus.update(big); });
        check("bus : relu", "Grand 2".equals(bus.findById(big.getIdBus()).getModele()));
        refused("bus : immatriculation en double",
                () -> bus.insert(new Bus(0, "CRUD-BUS-1", null, null, 10, StatutBus.DISPONIBLE)));

        Chauffeur ch = new Chauffeur();
        ch.setIdEmploye(driverEmp.getIdEmploye());
        ch.setNumeroPermis("CRUD-PERMIS");
        ch.setValiditePermis(LocalDate.of(2030, 1, 1));
        ch.setStatut("ACTIF");
        step("chauffeur : ajout", () -> drv.insert(ch));
        step("chauffeur : modification", () -> { ch.setValiditePermis(LocalDate.of(2031, 1, 1)); drv.update(ch); });
        Chauffeur relu = drv.findById(ch.getIdChauffeur());
        check("chauffeur : relu (nom depuis employe, permis 2031)",
                relu.getValiditePermis().getYear() == 2031 && "Crud".equals(relu.getNom()));
        check("chauffeur : recherche par permis", drv.search("CRUD-PERMIS").size() == 1);
        check("chauffeur : héritage Utilisateur (rôle CHAUFFEUR)", relu.getRole() == com.transportpro.model.enums.Role.CHAUFFEUR);

        int ligne = aff.findLignes().get(0).id();
        LocalDate day = LocalDate.of(2027, 3, 15);
        Affectation t = new Affectation(ligne, small.getIdBus(), ch.getIdChauffeur(), day,
                LocalTime.of(7, 0), LocalTime.of(8, 0), "PLANIFIEE");
        step("trajet : ajout (bus 1 place)", () -> aff.insert(t));
        check("trajet : relu", aff.findById(t.getIdAffectation()).getIdBus() == small.getIdBus());
        check("trajet : chauffeurDisponible / busDisponibleEtSuffisant",
                aff.chauffeurDisponible(ch.getIdChauffeur()) && aff.busDisponibleEtSuffisant(big.getIdBus(), 10));
        check("trajet : hasConflit détecte le chevauchement",
                aff.hasConflit(small.getIdBus(), -1, day, LocalTime.of(7, 30), LocalTime.of(8, 30)));

        refused("RG1/RG2 : trajet qui chevauche", () -> aff.insert(new Affectation(ligne, small.getIdBus(),
                ch.getIdChauffeur(), day, LocalTime.of(7, 30), LocalTime.of(8, 30), "PLANIFIEE")));
        refused("RG6 : bus hors service", () -> aff.insert(new Affectation(ligne, broken.getIdBus(),
                ch.getIdChauffeur(), day, LocalTime.of(10, 0), LocalTime.of(11, 0), "PLANIFIEE")));
        refused("heures : arrivée avant départ", () -> aff.insert(new Affectation(ligne, big.getIdBus(),
                ch.getIdChauffeur(), day, LocalTime.of(12, 0), LocalTime.of(11, 0), "PLANIFIEE")));

        Inscription i1 = inscription(p1.getIdEmploye(), t.getIdAffectation());
        step("inscription : ajout", () -> ins.insert(i1));
        refused("RG3 : capacité du bus atteinte", () -> ins.insert(inscription(p2.getIdEmploye(), t.getIdAffectation())));
        step("RG3 : passage au grand bus accepté", () -> { t.setIdBus(big.getIdBus()); aff.update(t); });
        step("inscription : 2e employé après changement de bus",
                () -> ins.insert(inscription(p2.getIdEmploye(), t.getIdAffectation())));
        refused("RG5 : double inscription", () -> ins.insert(inscription(p1.getIdEmploye(), t.getIdAffectation())));
        refused("BF14 : retour au bus 1 place avec 2 inscrits", () -> { t.setIdBus(small.getIdBus()); aff.update(t); });
        t.setIdBus(big.getIdBus());
        check("inscription : recherche « Crud »", ins.search("Crud").size() >= 2);

        step("BF18 : présence PRESENT", () -> ins.enregistrerPresence(i1.getIdInscription(), StatutPresence.PRESENT));
        check("BF18 : présence relue", ins.findById(i1.getIdInscription()).getStatutPresence() == StatutPresence.PRESENT);
        step("BF18 : présence remise à « non renseigné »",
                () -> ins.enregistrerPresence(i1.getIdInscription(), StatutPresence.NON_RENSEIGNE));
        check("BF18 : plus de ligne presence", ins.countPresences(i1.getIdInscription()) == 0);

        step("RG7 : désactiver le passager", () -> emp.desactiver(p2.getIdEmploye()));
        Affectation t2 = new Affectation(ligne, big.getIdBus(), ch.getIdChauffeur(), day.plusDays(1),
                LocalTime.of(7, 0), LocalTime.of(8, 0), "PLANIFIEE");
        step("trajet : 2e trajet le lendemain", () -> aff.insert(t2));
        refused("RG7 : employé désactivé ne peut plus s'inscrire",
                () -> ins.insert(inscription(p2.getIdEmploye(), t2.getIdAffectation())));

        System.out.println("\nPASS=" + pass + " FAIL=" + fail);
        if (fail > 0) System.exit(1);
    }
}
