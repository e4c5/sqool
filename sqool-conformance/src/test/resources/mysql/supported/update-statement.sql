update users
set name = coalesce(nickname, name), score = score + 1
where id = 1
order by id
limit 1;
