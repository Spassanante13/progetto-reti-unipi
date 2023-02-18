
#compila server
javac -cp lib/jackson-annotations-2.9.7.jar:lib/jackson-core-2.9.7.jar:lib/jackson-databind-2.9.7.jar src/WinsomeServer/*.java src/Risorse/*.java src/WinsomeClient/*.java -d out/production
#esegui
java -cp ./out/production:lib/jackson-annotations-2.9.7.jar:lib/jackson-core-2.9.7.jar:lib/jackson-databind-2.9.7.jar WinsomeServer.WinsomeServerMain