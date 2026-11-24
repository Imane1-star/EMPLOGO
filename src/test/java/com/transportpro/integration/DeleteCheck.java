package com.transportpro.integration;

import com.transportpro.dao.AffectationDAO;
import com.transportpro.dao.BusDAO;
import com.transportpro.dao.ChauffeurDAO;
import com.transportpro.dao.DatabaseConnection;
import com.transportpro.dao.EmployeDAO;
import com.transportpro.dao.InscriptionDAO;
import com.transportpro.model.Bus;
import com.transportpro.model.Employe;
import com.transportpro.model.enums.StatutBus;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;

/**
 * Vérification manuelle des suppressions / désactivations / annulations de chaque module.
 * MODIFIE LA BASE : à lancer sur une copie (-Demplogo.db.url=jdbc:mysql://localhost:3306/employgo_test).
 */
public class DeleteCheck {

    static int pass = 0, fail = 0;

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS " : "FAIL ") + name);
        if (ok) pass++; else fail++;
    }

    static int q(String sql) throws Exception {
        try (Connection c = DatabaseConnection.getConnection();
             Statement s = c.createStatement(); ResultSet r = s.executeQuery(sql)) {
            return r.next() ? r.getInt(1) : -1;
        }
    }

    public static void main(String[] args) throws Exception {
        Guard.requireTestDatabase();
        EmployeDAO emp = new EmployeDAO();
        BusDAO bus = new BusDAO();
        ChauffeurDAO drv = new ChauffeurDAO();
        AffectationDAO aff = new AffectationDAO();
        InscriptionDAO ins = new InscriptionDAO();

        // --- employé sans lien : suppression simple
        Employe e = new Employe(0, "DEL-1", "Test", "Libre", "del1@exemple.ma", null, null, null, LocalDate.now(), true);
        emp.insert(e);
        check("employé neuf : aucune donnée liée", !emp.usage(e.getIdEmploye()).any());
        emp.delete(e.getIdEmploye());
        check("employé neuf : supprimé (findById -> null)", emp.findById(e.getIdEmploye()) == null);

        // --- employé avec inscriptions (passager) : désactiver puis supprimer tout
        int passager = q("SELECT i.id_employe FROM inscription i WHERE i.id_employe NOT IN "
                + "(SELECT id_employe FROM chauffeur) GROUP BY i.id_employe ORDER BY COUNT(*) DESC LIMIT 1");
        EmployeDAO.Usage u = emp.usage(passager);
        check("passager : inscriptions détectées (" + u.inscriptions() + ")", u.inscriptions() > 0);
        boolean refused = false;
        try { emp.delete(passager); } catch (Exception ex) { refused = true; }
        check("passager : suppression simple refusée par la base (clé étrangère)", refused);
        emp.desactiver(passager);
        check("passager : désactivé, historique conservé",
                q("SELECT actif FROM employe WHERE id_employe=" + passager) == 0
                        && q("SELECT COUNT(*) FROM inscription WHERE id_employe=" + passager) == u.inscriptions());
        emp.deleteWithHistory(passager);
        check("passager : supprimé avec ses inscriptions et présences",
                q("SELECT COUNT(*) FROM employe WHERE id_employe=" + passager) == 0
                        && q("SELECT COUNT(*) FROM inscription WHERE id_employe=" + passager) == 0);

        // --- employé chauffeur avec trajets : suppression complète refusée
        int chauffeurEmp = q("SELECT ch.id_employe FROM chauffeur ch JOIN affectation a ON a.id_chauffeur = ch.id_chauffeur LIMIT 1");
        check("chauffeur avec trajets : suppression complète interdite", !emp.usage(chauffeurEmp).canDeleteWithHistory());
        refused = false;
        try { emp.deleteWithHistory(chauffeurEmp); } catch (Exception ex) { refused = true; }
        check("chauffeur avec trajets : deleteWithHistory refusé, rien supprimé",
                refused && q("SELECT COUNT(*) FROM employe WHERE id_employe=" + chauffeurEmp) == 1);

        // --- chauffeur : suspension
        int idCh = q("SELECT id_chauffeur FROM chauffeur WHERE id_employe=" + chauffeurEmp);
        check("chauffeur : trajets comptés", drv.countTrajets(idCh) > 0);
        drv.desactiver(idCh);
        check("chauffeur : suspendu", q("SELECT COUNT(*) FROM chauffeur WHERE id_chauffeur=" + idCh + " AND statut='SUSPENDU'") == 1);

        // --- bus sans trajet mais avec maintenance : supprimer tout
        Bus b = new Bus(0, "DEL-BUS-1", "Test", "Test", 30, StatutBus.DISPONIBLE);
        bus.insert(b);
        try (Connection c = DatabaseConnection.getConnection(); Statement s = c.createStatement()) {
            s.executeUpdate("INSERT INTO maintenance (id_bus, type, date_debut, statut) VALUES ("
                    + b.getIdBus() + ", 'PREVENTIVE', '2026-12-01', 'PLANIFIEE')");
        }
        BusDAO.Usage bu = bus.usage(b.getIdBus());
        check("bus : maintenance détectée, suppression complète permise", bu.maintenances() == 1 && bu.canDeleteWithHistory());
        bus.deleteWithHistory(b.getIdBus());
        check("bus : supprimé avec sa maintenance", q("SELECT COUNT(*) FROM bus WHERE id_bus=" + b.getIdBus()) == 0
                && q("SELECT COUNT(*) FROM maintenance WHERE id_bus=" + b.getIdBus()) == 0);

        // --- bus avec trajets : hors service uniquement
        int busAvecTrajets = q("SELECT id_bus FROM affectation LIMIT 1");
        check("bus avec trajets : suppression complète interdite", !bus.usage(busAvecTrajets).canDeleteWithHistory());
        bus.mettreHorsService(busAvecTrajets);
        check("bus avec trajets : mis hors service",
                q("SELECT COUNT(*) FROM bus WHERE id_bus=" + busAvecTrajets + " AND statut='HORS_SERVICE'") == 1);

        // --- trajet : annulation puis suppression complète
        int a = q("SELECT a.id_affectation FROM affectation a JOIN inscription i ON i.id_affectation=a.id_affectation "
                + "GROUP BY a.id_affectation ORDER BY COUNT(*) DESC LIMIT 1");
        AffectationDAO.Usage tu = aff.usage(a);
        check("trajet : inscriptions détectées (" + tu.inscriptions() + ")", tu.inscriptions() > 0);
        aff.annuler(a);
        check("trajet : annulé", q("SELECT COUNT(*) FROM affectation WHERE id_affectation=" + a + " AND statut='ANNULEE'") == 1);
        aff.deleteWithHistory(a);
        check("trajet : supprimé avec inscriptions / présences / incidents",
                q("SELECT COUNT(*) FROM affectation WHERE id_affectation=" + a) == 0
                        && q("SELECT COUNT(*) FROM inscription WHERE id_affectation=" + a) == 0
                        && q("SELECT COUNT(*) FROM incident WHERE id_affectation=" + a) == 0);

        // --- inscription avec présence
        int insc = q("SELECT id_inscription FROM presence LIMIT 1");
        check("inscription : présence détectée", ins.countPresences(insc) == 1);
        ins.annuler(insc);
        check("inscription : annulée", q("SELECT COUNT(*) FROM inscription WHERE id_inscription=" + insc + " AND statut='ANNULEE'") == 1);
        ins.deleteWithHistory(insc);
        check("inscription : supprimée avec sa présence", q("SELECT COUNT(*) FROM inscription WHERE id_inscription=" + insc) == 0
                && q("SELECT COUNT(*) FROM presence WHERE id_inscription=" + insc) == 0);

        System.out.println("\nPASS=" + pass + " FAIL=" + fail);
        if (fail > 0) System.exit(1);
    }
}
