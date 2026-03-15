-- SELECT with ORDER BY
SELECT id, name, salary FROM employees ORDER BY salary DESC;
SELECT id, name FROM employees ORDER BY name ASC;
SELECT id, name, salary FROM employees WHERE department_id = 10 ORDER BY salary DESC;
