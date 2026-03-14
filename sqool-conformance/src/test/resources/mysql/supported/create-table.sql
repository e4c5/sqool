create table if not exists users (
  id bigint primary key auto_increment,
  name varchar(255) not null,
  created_at timestamp
);
