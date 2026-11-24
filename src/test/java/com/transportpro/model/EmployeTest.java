
package com.transportpro.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EmployeTest {

    @Test
    void testGetNomComplet() {

        // Arrange: Create an employee
        Employe employe = new Employe();
        employe.setNom("Ali");
        employe.setPrenom("Amine");

        // Act: Get the full name
        String resultat = employe.getNomComplet();

        // Assert: Check the result
        assertEquals("Ali Amine", resultat);
    }

    @Test
    void testDesactiver() {

        // Arrange
        Employe employe = new Employe();
        employe.setActif(true);

        // Act
        employe.desactiver();

        // Assert
        assertFalse(employe.isActif());
    }

    @Test
    void testEquals() {

        // Arrange
        Employe employe1 = new Employe();
        employe1.setIdEmploye(1);

        Employe employe2 = new Employe();
        employe2.setIdEmploye(1);

        // Assert
        assertEquals(employe1, employe2);
    }
}
