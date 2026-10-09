DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS inventory;
CREATE TABLE inventory (
    product_sku VARCHAR(32) PRIMARY KEY,
    available INT NOT NULL,
    warehouse VARCHAR(32) NOT NULL
);
CREATE TABLE orders (
    order_id VARCHAR(36) PRIMARY KEY,
    product_sku VARCHAR(32) NOT NULL,
    quantity INT NOT NULL,
    status VARCHAR(32) NOT NULL
);
