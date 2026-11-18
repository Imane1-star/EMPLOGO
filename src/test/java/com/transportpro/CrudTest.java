package com.transportpro;

import com.transportpro.bus.Bus;
import com.transportpro.bus.BusDAO;
import com.transportpro.chauffeur.Chauffeur;
import com.transportpro.chauffeur.ChauffeurDAO;
import com.transportpro.employee.Employee;
import com.transportpro.employee.EmployeeDAO;
import com.transportpro.inscription.Inscription;
import com.transportpro.inscription.InscriptionDAO;
import com.transportpro.presence.Presence;
import com.transportpro.presence.PresenceDAO;
import com.transportpro.trajet.Trajet;
import com.transportpro.trajet.TrajetDAO;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Ajout / lecture / modification / recherche de chaque module, y compris les règles métier
 * des déclencheurs (bus indisponible, capacité, double inscription).
 * MODIFIE LA BASE : à lancer uniquement sur une copie (-Demplogo.db.url=...emplogo_test).
 */
public class CrudTest {

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

    public static void main(String[] args) throws Exception {
        if (!System.getProperty("emplogo.db.url", "").contains("_test")) {
            System.out.println("Refusé : lancez ce test sur une base *_test (-Demplogo.db.url=...).");
            return;
        }
        EmployeeDAO emp = new EmployeeDAO();
        BusDAO bus = new BusDAO();
        ChauffeurDAO drv = new ChauffeurDAO();
        TrajetDAO trip = new TrajetDAO();
        InscriptionDAO ins = new InscriptionDAO();
        PresenceDAO pres = new PresenceDAO();

        Employee driverEmp = new Employee();
        driverEmp.setMatricule("CRUD-D"); driverEmp.setNom("Crud"); driverEmp.setPrenom("Chauffeur");
        driverEmp.setEmail("crud.d@exemple.ma"); driverEmp.setTelephone("0600000099");
        Employee p1 = new Employee();
        p1.setMatricule("CRUD-P1"); p1.setNom("Crud"); p1.setPrenom("Passager"); p1.setEmail("crud.p1@exemple.ma");
        Employee p2 = new Employee();
        p2.setMatricule("CRUD-P2"); p2.setNom("Crud"); p2.setPrenom("Second"); p2.setEmail("crud.p2@exemple.ma");

        step("employé : ajout x3", () -> { emp.insert(driverEmp); emp.insert(p1); emp.insert(p2); });
        step("employé : modification", () -> { p1.setDepartement("Qualité"); emp.update(p1); });
        check("employé : relu après modification", "Qualité".equals(emp.findById(p1.getIdEmploye()).getDepartement()));
        check("employé : recherche « CRUD-P »", emp.search("CRUD-P").size() == 2);
        refused("employé : matricule en double", () -> emp.insert(p1));

        Bus small = new Bus(0, "CRUD-BUS-1", "Test", "Mini", 1, "DISPONIBLE");
        Bus big = new Bus(0, "CRUD-BUS-2", "Test", "Grand", 50, "DISPONIBLE");
        Bus broken = new Bus(0, "CRUD-BUS-3", "Test", "Panne", 50, "HORS_SERVICE");
        step("bus : ajout x3", () -> { bus.insert(small); bus.insert(big); bus.insert(broken); });
        step("bus : modification", () -> { big.setModele("Grand 2"); bus.update(big); });
        refused("bus : immatriculation en double", () -> bus.insert(new Bus(0, "CRUD-BUS-1", null, null, 10, "DISPONIBLE")));

        Chauffeur ch = new Chauffeur();
        ch.setIdEmploye(driverEmp.getIdEmploye());
        ch.setNumeroPermis("CRUD-PERMIS");
        ch.setDateValiditePermis(LocalDate.of(2030, 1, 1));
        ch.setStatut("ACTIF");
        step("chauffeur : ajout", () -> drv.insert(ch));
        step("chauffeur : modification", () -> { ch.setDateValiditePermis(LocalDate.of(2031, 1, 1)); drv.update(ch); });
        check("chauffeur : relu", drv.findById(ch.getIdChauffeur()).getDateValiditePermis().getYear() == 2031);
        check("chauffeur : recherche par permis", drv.search("CRUD-PERMIS").size() == 1);

        int ligne = trip.findLignes().get(0).id();
        LocalDate day = LocalDate.of(2027, 3, 15);
        Trajet t = new Trajet();
        t.setIdTrajet(ligne); t.setIdBus(small.getIdBus()); t.setIdChauffeur(ch.getIdChauffeur());
        t.setDateTrajet(day); t.setHeureDepart(LocalTime.of(7, 0)); t.setHeureArrivee(LocalTime.of(8, 0));
        t.setStatut("PLANIFIEE");
        step("trajet : ajout (bus 1 place)", () -> trip.insert(t));
        check("trajet : relu", trip.findById(t.getIdAffectation()).getIdBus() == small.getIdBus());

        Trajet overlap = new Trajet();
        overlap.setIdTrajet(ligne); overlap.setIdBus(small.getIdBus()); overlap.setIdChauffeur(ch.getIdChauffeur());
        overlap.setDateTrajet(day); overlap.setHeureDepart(LocalTime.of(7, 30)); overlap.setHeureArrivee(LocalTime.of(8, 30));
        overlap.setStatut("PLANIFIEE");
        refused("RG1/RG2 : trajet qui chevauche", () -> trip.insert(overlap));
        refused("RG6 : bus hors service", () -> { overlap.setIdBus(broken.getIdBus()); overlap.setHeureDepart(LocalTime.of(10, 0));
            overlap.setHeureArrivee(LocalTime.of(11, 0)); trip.insert(overlap); });

        Inscription i1 = new Inscription();
        i1.setIdEmploye(p1.getIdEmploye()); i1.setIdAffectation(t.getIdAffectation()); i1.setStatut("CONFIRMEE");
        step("inscription : ajout", () -> ins.insert(i1));
        refused("RG5 : double inscription", () -> {
            Inscription d = new Inscription();
            d.setIdEmploye(p1.getIdEmploye()); d.setIdAffectation(t.getIdAffectation()); d.setStatut("CONFIRMEE");
            ins.insert(d);
        });
        Inscription i2 = new Inscription();
        i2.setIdEmploye(p2.getIdEmploye()); i2.setIdAffectation(t.getIdAffectation()); i2.setStatut("CONFIRMEE");
        refused("RG3 : capacité du bus atteinte", () -> ins.insert(i2));
        step("RG3 : changement vers le grand bus accepté", () -> { t.setIdBus(big.getIdBus()); trip.update(t); });
        step("inscription : 2e employé après changement de bus", () -> ins.insert(i2));
        refused("BF14 : retour au bus 1 place avec 2 inscrits", () -> { t.setIdBus(small.getIdBus()); trip.update(t); });
        check("inscription : recherche « CRUD »", ins.search("Crud").size() >= 2);

        step("RG7 : désactiver le passager", () -> emp.deactivate(p2.getIdEmploye()));
        refused("RG7 : employé désactivé ne peut plus s'inscrire", () -> {
            Trajet t2 = new Trajet();
            t2.setIdTrajet(ligne); t2.setIdBus(big.getIdBus()); t2.setIdChauffeur(ch.getIdChauffeur());
            t2.setDateTrajet(day.plusDays(1)); t2.setHeureDepart(LocalTime.of(7, 0)); t2.setHeureArrivee(LocalTime.of(8, 0));
            t2.setStatut("PLANIFIEE");
            trip.insert(t2);
            Inscription d = new Inscription();
            d.setIdEmploye(p2.getIdEmploye()); d.setIdAffectation(t2.getIdAffectation()); d.setStatut("CONFIRMEE");
            ins.insert(d);
        });

        Presence p = new Presence();
        p.setIdInscription(i1.getIdInscription());
        p.setStatut("PRESENT");
        p.setHeureMontee(LocalTime.of(6, 58));
        step("présence : inscriptions disponibles", () -> pres.findAvailableInscriptions());
        step("présence : ajout", () -> pres.insert(p));
        step("présence : liste / recherche", () -> { pres.findAll(); pres.search("Crud"); });

        System.out.println("\nPASS=" + pass + " FAIL=" + fail);
        if (fail > 0) System.exit(1);
    }
}
