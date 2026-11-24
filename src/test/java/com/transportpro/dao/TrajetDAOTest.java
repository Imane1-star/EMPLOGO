
package com.transportpro.dao;

import com.transportpro.model.Trajet;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TrajetDAOTest {

    private final TrajetDAO trajetDAO = new TrajetDAO();

    @Test
    void testFindAll() throws Exception {
        List<Trajet> trajets = trajetDAO.findAll();

        assertNotNull(trajets);
        assertFalse(trajets.isEmpty());
    }

    @Test
    void testTrajetData() throws Exception {
        List<Trajet> trajets = trajetDAO.findAll();

        Trajet trajet = trajets.get(0);

        assertTrue(trajet.getIdTrajet() > 0);
        assertNotNull(trajet.getDestination());
    }

    @Test
    void testTrajetStatusIsValid() throws Exception {
        List<Trajet> trajets = trajetDAO.findAll();

        for (Trajet trajet : trajets) {
            assertNotNull(trajet.getStatut());
        }
    }
}
