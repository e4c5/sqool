-- CREATE TABLE statements using Oracle types
CREATE TABLE employees (
    id NUMBER(10) PRIMARY KEY,
    name VARCHAR2(100) NOT NULL,
    email VARCHAR2(255),
    salary NUMBER(12, 2),
    hire_date DATE,
    department_id NUMBER(5)
);
