update users u, orders o
set u.name = 'merged'
where u.id = o.user_id;
