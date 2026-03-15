-- Mixed script: DDL + DML
CREATE TABLE products (
    id NUMBER(10) PRIMARY KEY,
    name VARCHAR2(100) NOT NULL,
    price NUMBER(10, 2) NOT NULL
);
INSERT INTO products (id, name, price) VALUES (1, 'Widget', 9.99);
INSERT INTO products (id, name, price) VALUES (2, 'Gadget', 29.99);
SELECT id, name, price FROM products WHERE price < 20.00 ORDER BY price ASC;
UPDATE products SET price = 8.99 WHERE name = 'Widget';
SELECT * FROM products;
DELETE FROM products WHERE id = 1;
DROP TABLE products;
