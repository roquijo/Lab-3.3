package edu.maestria.observabilidad.records;

import edu.maestria.observabilidad.records.domain.RecordView;
import edu.maestria.observabilidad.records.service.RecordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestPropertySource(properties = "spring.profiles.active=test")
@Sql(scripts = {"/schema-test.sql", "/data-test.sql"})
class RecordServiceTest {

    @Autowired
    private RecordService records;

    @Test
    void readsTheJoinedOrderFromTheDatabase() {
        RecordView view = records.load("ord-001", null);
        assertEquals("LAPTOP-X1", view.productSku());
        assertEquals("WH-BOGOTA", view.warehouse());
        assertTrue(view.stockSufficient());
    }
}
