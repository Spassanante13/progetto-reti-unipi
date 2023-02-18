#compila client
javac -cp ./lib/jackson-annotations-2.9.7.jar:./lib/jackson-core-2.9.7.jar:./lib/jackson-databind-2.9.7.jar src/WinsomeClient/*.java src/WinsomeServer/*.java src/Risorse/*.java -d out/production
#esegui client
java -cp ./out/production:./lib/jackson-annotations-2.9.7.jar:./lib/jackson-core-2.9.7.jar:./lib/jackson-databind-2.9.7.jar WinsomeClient.WinsomeClientMain