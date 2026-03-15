-- SELECT with WHERE clause
SELECT id, name FROM employees WHERE salary > 50000;
SELECT id, name FROM employees WHERE department_id = 10 AND salary >= 30000;
SELECT id, name FROM employees WHERE hire_date IS NOT NULL;
SELECT id, name FROM employees WHERE name LIKE 'A%';
