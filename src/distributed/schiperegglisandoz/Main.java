package distributed.schiperegglisandoz;

import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.NotBoundException;

public class Main {

	//	public static void main(String[] args) throws RemoteException, MalformedURLException, AlreadyBoundException, NotBoundException {
	//		LocateRegistry.createRegistry(1099);
	//		int n = 3;
	//		Process[] processes = new Process[n];
	//		SESInterface[] remoteRefs = new SESInterface[n];
	//		for (int i = 0; i < n; i++) {
	//			processes[i] = new Process(i, n);
	//			String url = "rmi://localhost:1099/process"+i;
	//			Naming.bind(url, processes[i]);
	//			remoteRefs[i] = (SESInterface) Naming.lookup(url);
	//		}
	//		for (int i = 0; i < n; i++)
	//			for (int j = 0; j < n; j++)
	//				processes[i].setProcess(j, remoteRefs[j]);
	//	}

	public static void main(String[] args) throws AlreadyBoundException, NotBoundException, IOException {
		if (args.length <= 2) {
			System.err.println("Usage: <Process-ID> <binding-URL> <lookup-URLs>...");
			System.exit(-1);
		}

		int id = Integer.parseInt(args[0]);
		int n = args.length - 2;
		System.out.println(n+" processes");
		Process process = new Process(id, n);
		Naming.bind(args[1], process);
		System.out.println("Process with id "+id+" bound at "+args[1]);

		System.out.print("Press key when all processes are bound");
		System.in.read();

		for (int i = 0; i < n; i++)
			process.setProcess(i, (SESInterface) Naming.lookup(args[i + 2]));
		System.out.println("Network registered successfully");
	}

}
