# OurDatabaseSystem

# How to run

cd /src/

javac *.java

java Main ./db 4096 10 true

create table bar(a integer primarykey, x double);
insert into bar values(1 10.1),(2 21.2),(9 34.6),(5 2.1),(6 3.7);
quit
insert into bar values(3 7.5),(8 15.3),(4 1.2),(7 19.9),(10 5.0);

create table foo(a integer primarykey);
type sample_10k.txt | java Main ./db 64 10 true