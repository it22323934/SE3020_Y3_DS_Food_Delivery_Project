CREATE TABLE payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id VARCHAR(255) NOT NULL,
    customer_email VARCHAR(255) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    quantity INT NOT NULL,
    payment_status VARCHAR(50) NOT NULL,
    stripe_payment_id VARCHAR(255),
    payment_date TIMESTAMP NOT NULL
);