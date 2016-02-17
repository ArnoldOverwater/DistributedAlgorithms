package distributed.schiperegglisandoz;

import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

public class Main {

	public static void main(String[] args) throws RemoteException, MalformedURLException, AlreadyBoundException, NotBoundException {
		LocateRegistry.createRegistry(1099);
		int n = 3;
		Process[] processes = new Process[n];
		SESInterface[] remoteRefs = new SESInterface[n];
		for (int i = 0; i < n; i++) {
			processes[i] = new Process(i, n);
			String url = "rmi://localhost:1099/process"+i;
			Naming.bind(url, processes[i]);
			remoteRefs[i] = (SESInterface) Naming.lookup(url);
		}
		for (int i = 0; i < n; i++)
			for (int j = 0; j < n; j++)
				processes[i].setProcess(j, remoteRefs[j]);
	}

}
