# --- !Ups

create table application(
id int primary key auto_increment,
name varchar(100),
version varchar(100),
version_code varchar(100)
);

create table behavior (
id int primary key auto_increment,
app_id int,
name varchar(300)
);

create table device (
id int primary key auto_increment,
tag varchar(100),
serial_id varchar(50),
model varchar(50),
model_version varchar(50),
imei varchar(50)
);

create table oc (
id int primary key auto_increment,
name varchar(50),
relay_host varchar(200),
dormancy int,
version varchar(50),
version_code varchar(50)
);

create table latency(
id int primary key auto_increment,
behavior_id int,
device_id int,
oc_id int,
latency double,
test_time varchar(20)
);

create table loaded_data (
id int primary key auto_increment,
name varchar(200)
);

# --- !Downs

drop table if exists application;
drop table if exists behavior;
drop table if exists device;
drop table if exists oc;
drop table if exists latency;
drop table if exists loaded_data;