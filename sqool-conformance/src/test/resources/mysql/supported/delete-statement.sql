delete from users
where active = 0
order by id
limit 5;
