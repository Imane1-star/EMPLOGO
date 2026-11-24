
package com.transportpro.dao;

import com.transportpro.model.Bus;
import com.transportpro.model.enums.StatutBus;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BusDAOTest {

    private final BusDAO dao = new BusDAO();

    @Test
    void testFindAll() throws SQLException {

        List<Bus> buses = dao.findAll();

        assertNotNull(buses);
        assertFalse(buses.isEmpty());
    }

    @Test
    void testBusData() throws SQLException {

        List<Bus> buses = dao.findAll();

        Bus bus = buses.get(0);

        assertTrue(bus.getIdBus() > 0);
        assertNotNull(bus.getImmatriculation());
        assertTrue(bus.getCapacite() > 0);
        assertNotNull(bus.getStatut());
    }

    @Test
    void testBusStatusIsValid() throws SQLException {

        List<Bus> buses = dao.findAll();

        for (Bus bus : buses) {
            assertNotNull(bus.getStatut());

            assertTrue(
                bus.getStatut() instanceof StatutBus
            );
        }
    }
}
