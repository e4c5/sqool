select distinct category, count(*) as total
from sales
where amount between 10 and 20
group by category
having count(*) > 1
order by total desc
limit 3;
