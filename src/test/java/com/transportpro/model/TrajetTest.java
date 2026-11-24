
package com.transportpro.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class TrajetTest {

    @Test
    void testAjouterArret() {

        // Arrange
        Trajet trajet = new Trajet();
        Arret arret = new Arret();

        // Act
        trajet.ajouterArret(arret);

        // Assert
        assertEquals(1, trajet.getArrets().size());
    }

    @Test
    void testNombreInscrits() {

        // Arrange
        Trajet trajet = new Trajet();
        trajet.getInscriptions().add(new Inscription());
        trajet.getInscriptions().add(new Inscription());

        // Act
        int resultat = trajet.nombreInscrits();

        // Assert
        assertEquals(2, resultat);
    }

    @Test
    void testEstComplet() {

        // Arrange
        Trajet trajet = new Trajet();

        Bus bus = new Bus();
        bus.setCapacite(2);

        trajet.setBus(bus);
        trajet.getInscriptions().add(new Inscription());
        trajet.getInscriptions().add(new Inscription());

        // Act
        boolean resultat = trajet.estComplet();

        // Assert
        assertTrue(resultat);
    }

    @Test
    void testChevauche() {

        // Arrange
        Trajet trajet1 = new Trajet();
        trajet1.setDate(LocalDate.of(2026, 11, 18));
        trajet1.setHeureDepart(LocalTime.of(10, 0));

        Trajet trajet2 = new Trajet();
        trajet2.setDate(LocalDate.of(2026, 11, 18));
        trajet2.setHeureDepart(LocalTime.of(11, 0));

        // Act
        boolean resultat = trajet1.chevauche(trajet2);

        // Assert
        assertTrue(resultat);
    }

    @Test
    void testNeChevauchePas() {

        // Arrange
        Trajet trajet1 = new Trajet();
        trajet1.setDate(LocalDate.of(2026, 11, 18));
        trajet1.setHeureDepart(LocalTime.of(10, 0));

        Trajet trajet2 = new Trajet();
        trajet2.setDate(LocalDate.of(2026, 11, 18));
        trajet2.setHeureDepart(LocalTime.of(13, 0));

        // Act
        boolean resultat = trajet1.chevauche(trajet2);

        // Assert
        assertFalse(resultat);
    }
}
