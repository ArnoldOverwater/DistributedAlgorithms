/c/Program\ Files/Java/jdk1.8.0_66/bin/rmiregistry.exe &
java distributed.schiperegglisandoz.Main 0 rmi://localhost:1099/0 rmi://localhost:1099/0 rmi://localhost:1099/1 rmi://localhost:1099/2 rmi://localhost:1099/3 > ses_process0.log &
java distributed.schiperegglisandoz.Main 1 rmi://localhost:1099/1 rmi://localhost:1099/0 rmi://localhost:1099/1 rmi://localhost:1099/2 rmi://localhost:1099/3 > ses_process1.log &
java distributed.schiperegglisandoz.Main 2 rmi://localhost:1099/2 rmi://localhost:1099/0 rmi://localhost:1099/1 rmi://localhost:1099/2 rmi://localhost:1099/3 > ses_process2.log &
java distributed.schiperegglisandoz.Main 3 rmi://localhost:1099/3 rmi://localhost:1099/0 rmi://localhost:1099/1 rmi://localhost:1099/2 rmi://localhost:1099/3 > ses_process3.log &
