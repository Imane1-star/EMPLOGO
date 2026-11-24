
package com.transportpro.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

import com.transportpro.model.enums.StatutBus;

class BusTest {

    @Test
    void testEstDisponible() {

        // Arrange: Create an available bus
        Bus bus = new Bus();
        bus.setStatut(StatutBus.DISPONIBLE);

        // Act
        boolean resultat = bus.estDisponible(
            LocalDate.now(),
            LocalTime.now()
        );

        // Assert
        assertTrue(resultat);
    }

    @Test
    void testEstDisponibleWhenBusIsInMaintenance() {

        // Arrange
        Bus bus = new Bus();
        bus.setStatut(StatutBus.EN_MAINTENANCE);

        // Act
        boolean resultat = bus.estDisponible(
            LocalDate.now(),
            LocalTime.now()
        );

        // Assert
        assertFalse(resultat);
    }

    @Test
    void testPeutAccueillir() {

        // Arrange
        Bus bus = new Bus();
        bus.setCapacite(50);

        // Act
        boolean resultat = bus.peutAccueillir(30);

        // Assert
        assertTrue(resultat);
    }

    @Test
    void testNePeutPasAccueillir() {

        // Arrange
        Bus bus = new Bus();
        bus.setCapacite(50);

        // Act
        boolean resultat = bus.peutAccueillir(50);

        // Assert
        assertFalse(resultat);
    }
}
