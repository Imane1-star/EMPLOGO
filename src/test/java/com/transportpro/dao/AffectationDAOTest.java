
package com.transportpro.dao;

import com.transportpro.model.Affectation;

import org.junit.jupiter.api.Test;



import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AffectationDAOTest {

    private final AffectationDAO dao = new AffectationDAO();
// *********
    // @Test
    // void testFindAll() throws SQLException {

    //     List<?> affectations = dao.findAll();

    //     assertNotNull(affectations);
    // }

// ******
@Test
void testFindAll() throws SQLException {

    List<Affectation> affectations = dao.findAll();

    assertNotNull(affectations);
    assertFalse(affectations.isEmpty());
}
// ********

    // @Test
    // void testCountConfirmedInscriptions()
    //         throws SQLException {

    //     int count = dao.countConfirmedInscriptions(-1);

    //     assertEquals(0, count);
    // }

@Test
void testCountConfirmedInscriptions() throws SQLException {

    int count = dao.countConfirmedInscriptions(1);

    assertEquals(14, count);
}

    @Test
    void testBusDisponibleEtSuffisant()
            throws SQLException {

        boolean disponible =
                dao.busDisponibleEtSuffisant(-1, 10);

        assertFalse(disponible);
    }

    @Test
    void testChauffeurDisponible()
            throws SQLException {

        boolean disponible = dao.chauffeurDisponible(-1);

        assertFalse(disponible);
    }

    @Test
    void testHasConflit() throws SQLException {

        boolean conflit = dao.hasConflit(
                -1,
                -1,
                LocalDate.of(2026, 11, 18),
                LocalTime.of(8, 0),
                LocalTime.of(10, 0)
        );

        assertFalse(conflit);
    }
    @Test
void testInsert() throws SQLException {

    Affectation a = new Affectation(
        1,
        1,
        1,
       LocalDate.of(2028, 6, 15),
        LocalTime.of(8, 0),
        LocalTime.of(10, 0),
        "PLANIFIEE"
    );

    int id = dao.insert(a);
    try {
        assertTrue(id > 0);
        assertEquals(id, a.getIdAffectation());
    } finally {
        // nettoyage : sinon le test échoue au lancement suivant (le bus serait déjà pris à cette heure)
        if (id > 0) dao.delete(id);
    }
}
}
