create table if not exists audit_log (
  id bigint primary key auto_increment,
  message varchar(255)
);
insert into audit_log (id, message) values (1, 'hello');
update audit_log set message = 'updated' where id = 1;
delete from audit_log where id = 2;
select id, message from audit_log;
