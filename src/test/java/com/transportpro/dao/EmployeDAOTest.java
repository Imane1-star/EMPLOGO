
package com.transportpro.dao;

import com.transportpro.model.Employe;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EmployeDAOTest {

    private final EmployeDAO dao = new EmployeDAO();

    // Create a unique employee for each test
    private Employe createEmployee() {
        String unique = UUID.randomUUID().toString().substring(0, 8);

        return new Employe(
                0,
                "TEST-" + unique,
                "TestNom",
                "TestPrenom",
                "test-" + unique + "@example.com",
                "0612345678",
                "Agadir",
                "Informatique",
                LocalDate.of(2024, 1, 15),
                true
        );
    }

    @Test
    void testFindAll() throws SQLException {

        List<Employe> employees = dao.findAll();

        assertNotNull(employees);
        assertFalse(employees.isEmpty());
    }

    @Test
    void testInsertAndFindById() throws SQLException {

        Employe employee = createEmployee();

        try {
            dao.insert(employee);

            assertTrue(employee.getIdEmploye() > 0);

            Employe found = dao.findById(employee.getIdEmploye());

            assertNotNull(found);
            assertEquals(employee.getNom(), found.getNom());
            assertEquals(employee.getEmail(), found.getEmail());

        } finally {
            if (employee.getIdEmploye() > 0) {
                dao.delete(employee.getIdEmploye());
            }
        }
    }

    @Test
    void testUpdate() throws SQLException {

        Employe employee = createEmployee();

        try {
            dao.insert(employee);

            employee.setNom("NomModifie");
            employee.setEmail(
                    "modified-" + UUID.randomUUID().toString().substring(0, 8)
                            + "@example.com"
            );

            dao.update(employee);

            Employe updated = dao.findById(employee.getIdEmploye());

            assertNotNull(updated);
            assertEquals("NomModifie", updated.getNom());
            assertEquals(employee.getEmail(), updated.getEmail());

        } finally {
            if (employee.getIdEmploye() > 0) {
                dao.delete(employee.getIdEmploye());
            }
        }
    }

    @Test
    void testDelete() throws SQLException {

        Employe employee = createEmployee();

        dao.insert(employee);

        int id = employee.getIdEmploye();

        dao.delete(id);

        Employe deleted = dao.findById(id);

        assertNull(deleted);
    }
}
