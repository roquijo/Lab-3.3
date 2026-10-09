CREATE TABLE IF NOT EXISTS inventory (
    product_sku VARCHAR(32) PRIMARY KEY,
    available INT NOT NULL,
    warehouse VARCHAR(32) NOT NULL,
    reserved INT NOT NULL
);

CREATE TABLE IF NOT EXISTS orders (
    order_id VARCHAR(36) PRIMARY KEY,
    product_sku VARCHAR(32) NOT NULL,
    quantity INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    customer_id VARCHAR(32) NOT NULL
);

INSERT INTO inventory (product_sku, available, warehouse, reserved) VALUES
    ('LAPTOP-X1', 15, 'WH-BOGOTA', 2),
    ('MOUSE-PRO', 40, 'WH-BOGOTA', 0),
    ('KEYBOARD-MK', 12, 'WH-MEDELLIN', 1),
    ('MONITOR-4K', 7, 'WH-BOGOTA', 0),
    ('HEADSET-Z', 20, 'WH-CALI', 3);

INSERT INTO orders (order_id, product_sku, quantity, status, customer_id) VALUES
    ('ord-001', 'LAPTOP-X1', 1, 'delivered', 'cust-100'),
    ('ord-002', 'MOUSE-PRO', 2, 'paid', 'cust-100'),
    ('ord-003', 'KEYBOARD-MK', 1, 'paid', 'cust-220'),
    ('ord-004', 'MONITOR-4K', 1, 'reserved', 'cust-220'),
    ('ord-005', 'HEADSET-Z', 1, 'paid', 'cust-310');
