insert into users (id, name)
values (1, 'alice'), (2, default)
on duplicate key update name = 'updated';
