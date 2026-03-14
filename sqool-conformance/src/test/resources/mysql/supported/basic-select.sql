select u.id as user_id, u.name
from users u
where u.active = 1
order by u.name
limit 10;
