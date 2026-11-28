
package com.emplogo.dao;

import org.junit.jupiter.api.Test;

import com.emplogo.dao.ChauffeurDAO;
import com.emplogo.model.Chauffeur;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChauffeurDAOTest {

    private final ChauffeurDAO chauffeurDAO = new ChauffeurDAO();

    @Test
    void testFindAll() throws Exception {
        List<Chauffeur> chauffeurs = chauffeurDAO.findAll();

        assertNotNull(chauffeurs);
        assertFalse(chauffeurs.isEmpty());
    }

    @Test
    void testChauffeurData() throws Exception {
        List<Chauffeur> chauffeurs = chauffeurDAO.findAll();

        Chauffeur chauffeur = chauffeurs.get(0);

        assertTrue(chauffeur.getIdChauffeur() > 0);
        assertNotNull(chauffeur.getNom());
        assertNotNull(chauffeur.getNumeroPermis());
    }

    @Test
    void testChauffeurAvailability() throws Exception {
        List<Chauffeur> chauffeurs = chauffeurDAO.findAll();

        for (Chauffeur chauffeur : chauffeurs) {
            assertNotNull(chauffeur);
        }
    }
}
