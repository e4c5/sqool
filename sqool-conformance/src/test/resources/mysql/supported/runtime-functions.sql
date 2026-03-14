select coalesce(nickname, name) as display_name,
       if(score > 10, score, 0) as normalized,
       mod(total, 10) as remainder,
       date(created_at) as created_day,
       now(3) as current_time,
       current_user() as actor
from users;
