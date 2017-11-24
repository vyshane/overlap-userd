create table users (
  id varchar(36) not null,
  first_name varchar(255) not null,
  last_name varchar(255) not null,
  email varchar(255) not null,
  password_hash varchar(255) not null,
  status varchar(255) not null,
  signed_up timestamp not null
);

