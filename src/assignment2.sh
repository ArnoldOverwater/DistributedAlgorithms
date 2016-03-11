/c/Program\ Files/Java/jdk1.8.0_66/bin/rmiregistry.exe &
java distributed.singhal.Main 0 4 rmi://localhost:1099/ > process0.log &
java distributed.singhal.Main 1 4 rmi://localhost:1099/ > process1.log &
java distributed.singhal.Main 2 4 rmi://localhost:1099/ > process2.log &
java distributed.singhal.Main 3 4 rmi://localhost:1099/ > process3.log &
