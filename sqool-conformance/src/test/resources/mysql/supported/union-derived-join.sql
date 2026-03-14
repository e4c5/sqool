select summary.id
from (
  select u.id
  from users u
  inner join orders o using (id)
  union all
  select archived.id
  from archived_users archived
) summary
order by summary.id
limit 5;
