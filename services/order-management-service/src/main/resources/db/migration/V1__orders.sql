CREATE TABLE orders (
    order_id VARCHAR(36) PRIMARY KEY,
    customer_email VARCHAR(320) NOT NULL,
    status VARCHAR(20) NOT NULL,
    placed_at VARCHAR(40) NOT NULL
);

CREATE TABLE order_line_item (
    order_id VARCHAR(36) NOT NULL,
    line_no INTEGER NOT NULL,
    product_id VARCHAR(36) NOT NULL,
    quantity INTEGER NOT NULL,
    PRIMARY KEY (order_id, line_no),
    FOREIGN KEY (order_id) REFERENCES orders (order_id)
);
