package edu.maestria.observabilidad.records.repository;

import edu.maestria.observabilidad.records.domain.RecordView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RecordRepository {

    private final JdbcTemplate jdbc;

    public RecordRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<RecordView> findOrder(String orderId) {
        return jdbc.query("""
                SELECT o.order_id, o.product_sku, o.quantity, o.status,
                       i.available, i.warehouse
                FROM orders o
                JOIN inventory i ON i.product_sku = o.product_sku
                WHERE o.order_id = ?
                """, (rs, row) -> new RecordView(
                rs.getString("order_id"),
                rs.getString("product_sku"),
                rs.getInt("quantity"),
                rs.getString("status"),
                rs.getInt("available"),
                rs.getString("warehouse"),
                rs.getInt("available") >= rs.getInt("quantity"),
                0,
                null
        ), orderId);
    }
}
