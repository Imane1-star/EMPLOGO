
package com.emplogo.model;

import org.junit.jupiter.api.Test;

import com.emplogo.model.Inscription;
import com.emplogo.model.enums.StatutPresence;

import static org.junit.jupiter.api.Assertions.*;

class InscriptionTest {

    @Test
    void testEnregistrerPresence() {

        // Arrange
        Inscription inscription = new Inscription();

        // Act
        inscription.enregistrerPresence(StatutPresence.PRESENT);

        // Assert
        assertEquals(
            StatutPresence.PRESENT,
            inscription.getStatutPresence()
        );
    }

    @Test
    void testEnregistrerAbsence() {

        // Arrange
        Inscription inscription = new Inscription();

        // Act
        inscription.enregistrerPresence(StatutPresence.ABSENT);

        // Assert
        assertEquals(
            StatutPresence.ABSENT,
            inscription.getStatutPresence()
        );
    }

    @Test
    void testEnregistrerPresenceNull() {

        // Arrange
        Inscription inscription = new Inscription();

        // Act + Assert
        assertThrows(
            IllegalArgumentException.class,
            () -> inscription.enregistrerPresence(null)
        );
    }
}
