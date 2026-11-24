
package com.transportpro.controller;

import com.transportpro.Ctx;
import com.transportpro.dao.*;
import com.transportpro.model.Bus;
import com.transportpro.model.Chauffeur;
import com.transportpro.model.Employe;
import com.transportpro.model.Trajet;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MainControllerTest {

    @Test
    void testChargerEmployes() throws Exception {

        EmployeDAO employeDAO = new EmployeDAO() {
            @Override
            public List<Employe> findAll() {
                return List.of(new Employe());
            }
        };

        Ctx ctx = new FakeCtx();

        MainController controller = new MainController(
                ctx,
                employeDAO,
                new BusDAO(),
                new ChauffeurDAO(),
                new TrajetDAO()
        );

        List<Employe> result = controller.chargerEmployes();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testChargerEmployesOnError() throws Exception {

        FakeCtx ctx = new FakeCtx();

        EmployeDAO employeDAO = new EmployeDAO() {
            @Override
            public List<Employe> findAll() throws SQLException {
                throw new SQLException("Database error");
            }
        };

        MainController controller = new MainController(
                ctx,
                employeDAO,
                new BusDAO(),
                new ChauffeurDAO(),
                new TrajetDAO()
        );

        List<Employe> result = controller.chargerEmployes();

        assertTrue(result.isEmpty());
        assertEquals("Erreur employés : Database error", ctx.lastMessage);
    }
    // test3  

    @Test
    void testAjouterEmploye() throws Exception {

        FakeCtx ctx = new FakeCtx();

        EmployeDAO employeDAO = new EmployeDAO() {
            @Override
            public void insert(Employe e) {
                // Simulate successful insertion
            }
        };

        MainController controller = new MainController(
                ctx,
                employeDAO,
                new BusDAO(),
                new ChauffeurDAO(),
                new TrajetDAO()
        );

        Employe employe = new Employe();
        employe.setNom("Ahmed");
        employe.setPrenom("Ali");

        controller.ajouterEmploye(employe);

        assertEquals("Employé ajouté : Ahmed Ali", ctx.lastMessage);
    }

    @Test
    void testAjouterEmployeOnError() throws Exception {

        FakeCtx ctx = new FakeCtx();

        EmployeDAO employeDAO = new EmployeDAO() {
            @Override
            public void insert(Employe e) throws SQLException {
                throw new SQLException("Insert error");
            }
        };

        MainController controller = new MainController(
                ctx,
                employeDAO,
                new BusDAO(),
                new ChauffeurDAO(),
                new TrajetDAO()
        );

        controller.ajouterEmploye(new Employe());

        assertEquals("Échec ajout : Insert error", ctx.lastMessage);
    }

    @Test
    void testSupprimerEmploye() throws Exception {

        FakeCtx ctx = new FakeCtx();

        EmployeDAO employeDAO = new EmployeDAO() {
            @Override
            public void delete(int id) {
                // Simulate successful deletion
            }
        };

        MainController controller = new MainController(
                ctx,
                employeDAO,
                new BusDAO(),
                new ChauffeurDAO(),
                new TrajetDAO()
        );

        controller.supprimerEmploye(1);

        assertEquals("Employé supprimé", ctx.lastMessage);
    }

    @Test
    void testSupprimerEmployeOnError() throws Exception {

        FakeCtx ctx = new FakeCtx();

        EmployeDAO employeDAO = new EmployeDAO() {
            @Override
            public void delete(int id) throws SQLException {
                throw new SQLException("Delete error");
            }
        };

        MainController controller = new MainController(
                ctx,
                employeDAO,
                new BusDAO(),
                new ChauffeurDAO(),
                new TrajetDAO()
        );

        controller.supprimerEmploye(1);

        assertEquals("Échec suppression : Delete error", ctx.lastMessage);
    }


// test Charger Bus

    @Test
    void testChargerBus() throws Exception {

        FakeCtx ctx = new FakeCtx();

        BusDAO busDAO = new BusDAO() {
            @Override
            public List<Bus> findAll() {
                return List.of();
            }
        };

        MainController controller = new MainController(
                ctx,
                new EmployeDAO(),
                busDAO,
                new ChauffeurDAO(),
                new TrajetDAO()
        );

        List<Bus> result = controller.chargerBus();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testChargerBusOnError() throws Exception {

        FakeCtx ctx = new FakeCtx();

        BusDAO busDAO = new BusDAO() {
            @Override
            public List<Bus> findAll() throws SQLException {
                throw new SQLException("Bus error");
            }
        };

        MainController controller = new MainController(
                ctx,
                new EmployeDAO(),
                busDAO,
                new ChauffeurDAO(),
                new TrajetDAO()
        );

        List<Bus> result = controller.chargerBus();

        assertTrue(result.isEmpty());
        assertEquals("Erreur bus : Bus error", ctx.lastMessage);
    }


    // chargerChauffeurs

    @Test
    void testChargerChauffeurs() throws Exception {

        FakeCtx ctx = new FakeCtx();

        ChauffeurDAO chauffeurDAO = new ChauffeurDAO() {
            @Override
            public List<Chauffeur> findAll() {
                return List.of();
            }
        };

        MainController controller = new MainController(
                ctx,
                new EmployeDAO(),
                new BusDAO(),
                chauffeurDAO,
                new TrajetDAO()
        );

        List<Chauffeur> result = controller.chargerChauffeurs();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testChargerChauffeursOnError() throws Exception {

        FakeCtx ctx = new FakeCtx();

        ChauffeurDAO chauffeurDAO = new ChauffeurDAO() {
            @Override
            public List<Chauffeur> findAll() throws SQLException {
                throw new SQLException("Chauffeur error");
            }
        };

        MainController controller = new MainController(
                ctx,
                new EmployeDAO(),
                new BusDAO(),
                chauffeurDAO,
                new TrajetDAO()
        );

        List<Chauffeur> result = controller.chargerChauffeurs();

        assertTrue(result.isEmpty());
        assertEquals(
                "Erreur chauffeurs : Chauffeur error",
                ctx.lastMessage
        );
    }


// charger Trajets

    @Test
    void testChargerTrajets() throws Exception {

        FakeCtx ctx = new FakeCtx();

        TrajetDAO trajetDAO = new TrajetDAO() {
            @Override
            public List<Trajet> findAll() {
                return List.of();
            }
        };

        MainController controller = new MainController(
                ctx,
                new EmployeDAO(),
                new BusDAO(),
                new ChauffeurDAO(),
                trajetDAO
        );

        List<Trajet> result = controller.chargerTrajets();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testChargerTrajetsOnError() throws Exception {

        FakeCtx ctx = new FakeCtx();

        TrajetDAO trajetDAO = new TrajetDAO() {
            @Override
            public List<Trajet> findAll() throws SQLException {
                throw new SQLException("Trajet error");
            }
        };

        MainController controller = new MainController(
                ctx,
                new EmployeDAO(),
                new BusDAO(),
                new ChauffeurDAO(),
                trajetDAO
        );

        List<Trajet> result = controller.chargerTrajets();

        assertTrue(result.isEmpty());
        assertEquals("Erreur trajets : Trajet error", ctx.lastMessage);
    }


    // Fake Ctx for testing
    static class FakeCtx implements Ctx {

        String lastMessage;

        @Override
        public void navigate(com.transportpro.Page page) {
        }

        @Override
        public void openModal(String title) {
        }

        @Override
        public void toast(String message) {
            lastMessage = message;
        }
    }
}
