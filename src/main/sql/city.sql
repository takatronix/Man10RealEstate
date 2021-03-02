create table city
(
	id int auto_increment,
	name varchar(16) not null,
	x double default 0.0 null,
	y double default 0.0 null,
	z double default 0.0 null,
	pitch float default 0.0 null,
	yaw float default 0.0 null,
	server varchar(16) null,
	world varchar(16) null,
	sx int default 0 null,
	sy int default 0 null,
	sz int default 0 null,
	ex int default 0 null,
	ey int default 0 null,
	ez int default 0 null,
	tax double default 0.0 null,
	max_user int default 0 null,
	constraint city_pk
		primary key (id)
);

