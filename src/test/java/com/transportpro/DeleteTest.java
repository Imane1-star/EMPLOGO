package com.transportpro;

import com.transportpro.bus.Bus;
import com.transportpro.bus.BusDAO;
import com.transportpro.chauffeur.ChauffeurDAO;
import com.transportpro.db.DatabaseConnection;
import com.transportpro.employee.Employee;
import com.transportpro.employee.EmployeeDAO;
import com.transportpro.inscription.InscriptionDAO;
import com.transportpro.trajet.TrajetDAO;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Vérifie les suppressions / désactivations de chaque module.
 * MODIFIE LA BASE : à lancer uniquement sur une copie, par exemple
 *   java -Demplogo.db.url=jdbc:mysql://localhost:3306/emplogo_test -cp ... com.transportpro.DeleteTest
 */
public class DeleteTest {

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
        if (!System.getProperty("emplogo.db.url", "").contains("_test")) {
            System.out.println("Refusé : lancez ce test sur une base *_test (-Demplogo.db.url=...).");
            return;
        }
        EmployeeDAO emp = new EmployeeDAO();
        BusDAO bus = new BusDAO();
        ChauffeurDAO drv = new ChauffeurDAO();
        TrajetDAO trip = new TrajetDAO();
        InscriptionDAO ins = new InscriptionDAO();

        // --- employé sans lien : suppression simple
        Employee e = new Employee();
        e.setMatricule("TST-1"); e.setNom("Test"); e.setPrenom("Libre"); e.setEmail("tst1@exemple.ma");
        emp.insert(e);
        check("employé neuf : aucune donnée liée", !emp.usage(e.getIdEmploye()).any());
        emp.delete(e.getIdEmploye());
        check("employé neuf : supprimé", q("SELECT COUNT(*) FROM employe WHERE matricule='TST-1'") == 0);

        // --- employé avec inscriptions (passager) : désactiver puis supprimer tout
        int passager = q("SELECT i.id_employe FROM inscription i WHERE i.id_employe NOT IN "
                + "(SELECT id_employe FROM chauffeur) GROUP BY i.id_employe ORDER BY COUNT(*) DESC LIMIT 1");
        EmployeeDAO.Usage u = emp.usage(passager);
        check("passager : inscriptions détectées (" + u.inscriptions() + ")", u.inscriptions() > 0);
        emp.deactivate(passager);
        check("passager : désactivé, historique conservé",
                q("SELECT actif FROM employe WHERE id_employe=" + passager) == 0
                        && q("SELECT COUNT(*) FROM inscription WHERE id_employe=" + passager) == u.inscriptions());
        emp.deleteWithHistory(passager);
        check("passager : supprimé avec ses inscriptions et présences",
                q("SELECT COUNT(*) FROM employe WHERE id_employe=" + passager) == 0
                        && q("SELECT COUNT(*) FROM inscription WHERE id_employe=" + passager) == 0);

        // --- employé chauffeur avec trajets : suppression complète refusée
        int chauffeurEmp = q("SELECT ch.id_employe FROM chauffeur ch JOIN affectation a "
                + "ON a.id_chauffeur = ch.id_chauffeur LIMIT 1");
        check("chauffeur avec trajets : suppression complète interdite",
                !emp.usage(chauffeurEmp).canDeleteWithHistory());
        boolean refused = false;
        try { emp.deleteWithHistory(chauffeurEmp); } catch (EmployeeDAO.EmployeeException ex) { refused = true; }
        check("chauffeur avec trajets : deleteWithHistory refusé, rien supprimé", refused
                && q("SELECT COUNT(*) FROM employe WHERE id_employe=" + chauffeurEmp) == 1);

        // --- chauffeur : désactivation
        int idCh = q("SELECT id_chauffeur FROM chauffeur WHERE id_employe=" + chauffeurEmp);
        check("chauffeur : trajets comptés", drv.countTrajets(idCh) > 0);
        String st = drv.deactivate(idCh);
        check("chauffeur : désactivé (" + st + ")",
                q("SELECT COUNT(*) FROM chauffeur WHERE id_chauffeur=" + idCh + " AND statut='" + st + "'") == 1);

        // --- bus sans trajet mais avec maintenance : supprimer tout
        Bus b = new Bus(0, "TST-BUS-1", "Test", "Test", 30, "DISPONIBLE");
        bus.insert(b);
        try (Connection c = DatabaseConnection.getConnection(); Statement s = c.createStatement()) {
            s.executeUpdate("INSERT INTO maintenance (id_bus, " + maintenanceTypeColumn(c)
                    + ", date_debut, statut) VALUES (" + b.getIdBus() + ", 'PREVENTIVE', '2026-12-01', 'PLANIFIEE')");
        }
        BusDAO.Usage bu = bus.usage(b.getIdBus());
        check("bus : maintenance détectée, suppression complète permise", bu.maintenances() == 1 && bu.canDeleteWithHistory());
        bus.deleteWithHistory(b.getIdBus());
        check("bus : supprimé avec sa maintenance",
                q("SELECT COUNT(*) FROM bus WHERE id_bus=" + b.getIdBus()) == 0
                        && q("SELECT COUNT(*) FROM maintenance WHERE id_bus=" + b.getIdBus()) == 0);

        // --- bus avec trajets : hors service uniquement
        int busAvecTrajets = q("SELECT id_bus FROM affectation LIMIT 1");
        check("bus avec trajets : suppression complète interdite", !bus.usage(busAvecTrajets).canDeleteWithHistory());
        bus.retire(busAvecTrajets);
        check("bus avec trajets : mis hors service",
                q("SELECT COUNT(*) FROM bus WHERE id_bus=" + busAvecTrajets + " AND statut='HORS_SERVICE'") == 1);

        // --- trajet : annulation puis suppression complète
        int aff = q("SELECT a.id_affectation FROM affectation a JOIN inscription i ON i.id_affectation=a.id_affectation "
                + "GROUP BY a.id_affectation ORDER BY COUNT(*) DESC LIMIT 1");
        TrajetDAO.Usage tu = trip.usage(aff);
        check("trajet : inscriptions détectées (" + tu.inscriptions() + ")", tu.inscriptions() > 0);
        trip.cancel(aff);
        check("trajet : annulé", q("SELECT COUNT(*) FROM affectation WHERE id_affectation=" + aff + " AND statut='ANNULEE'") == 1);
        trip.deleteWithHistory(aff);
        check("trajet : supprimé avec inscriptions / présences / incidents",
                q("SELECT COUNT(*) FROM affectation WHERE id_affectation=" + aff) == 0
                        && q("SELECT COUNT(*) FROM inscription WHERE id_affectation=" + aff) == 0
                        && q("SELECT COUNT(*) FROM incident WHERE id_affectation=" + aff) == 0);

        // --- inscription avec présence
        int insc = q("SELECT id_inscription FROM presence LIMIT 1");
        if (insc > 0) {
            check("inscription : présence détectée", ins.countPresences(insc) == 1);
            ins.cancel(insc);
            check("inscription : annulée", q("SELECT COUNT(*) FROM inscription WHERE id_inscription=" + insc + " AND statut='ANNULEE'") == 1);
            ins.deleteWithHistory(insc);
            check("inscription : supprimée avec sa présence",
                    q("SELECT COUNT(*) FROM inscription WHERE id_inscription=" + insc) == 0
                            && q("SELECT COUNT(*) FROM presence WHERE id_inscription=" + insc) == 0);
        }

        System.out.println("\nPASS=" + pass + " FAIL=" + fail);
        if (fail > 0) System.exit(1);
    }

    /** La colonne s'appelle « type_maintenance » (schéma de référence) ou « type » (ancienne base). */
    private static String maintenanceTypeColumn(Connection c) throws Exception {
        try (ResultSet rs = c.getMetaData().getColumns(c.getCatalog(), null, "maintenance", "type_maintenance")) {
            return rs.next() ? "type_maintenance" : "type";
        }
    }
}
