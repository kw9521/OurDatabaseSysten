# OurDatabaseSystem

# How to run

cd /src/

javac *.java

java Main ./db 4096 10 true

create table bar(a integer primarykey, x double);
insert into bar values(1 10.1),(2 21.2),(9 34.6),(5 2.1),(6 3.7);
select * from bar; 
quit
insert into bar values(3 7.5),(8 15.3),(4 1.2),(7 19.9),(10 5.0);

select * from bar; 

create table foo(a integer primarykey);
INSERT INTO foo VALUES (3358),(3323),(6161),(4950),(5632),(9265),(7305),(5305),(3990),(8599),(6725),(8642),(2604),(7064),(8918),(5448),(5861),(6810),(423),(7168),(4945),(5447),(4722),(4223),(7691),(8245),(504),(6364),(6300),(5001),(8933),(3553),(947),(210),(7842),(6420),(6422),(1241),(4960),(1650),(8412),(8431),(2461),(4924),(2363),(435),(3634),(6666),(8560),(4530),(9088),(5444),(1594),(9291),(8726),(1046),(4795),(6505),(3170),(1449),(8884),(239),(5212),(9469),(1354),(4056),(3446),(560),(7555),(4909),(2975),(6562),(3153),(6671),(5577),(2617),(6723),(8598),(1109),(1695),(4133),(449),(6587),(5645),(3971),(4749),(9071),(5485),(2212),(7221),(1245),(9374),(7185),(2074),(207),(6829),(5860),(3816),(1635),(5174);
type sample_10k.txt | java Main ./db 64 10 true