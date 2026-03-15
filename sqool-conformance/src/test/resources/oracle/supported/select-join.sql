-- SELECT with JOIN (normalized to SelectStatement)
SELECT e.id, e.name, d.name AS dept_name
FROM employees e
INNER JOIN departments d ON e.department_id = d.id;

SELECT e.id, e.name
FROM employees e
LEFT JOIN departments d ON e.department_id = d.id
WHERE d.id IS NOT NULL;

SELECT e.id, o.amount
FROM employees e
JOIN orders o ON e.id = o.employee_id;
