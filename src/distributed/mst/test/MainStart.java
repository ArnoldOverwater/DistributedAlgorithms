package distributed.mst.test;

import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Random;

import distributed.mst.Edge;
import distributed.mst.MSTInterface;
import distributed.mst.Process;

public class MainStart {

	public static void main(String[] args) throws RemoteException, MalformedURLException, AlreadyBoundException, InterruptedException, NotBoundException {
		if (args.length < 5 && args.length % 2 == 0) {
			System.err.println("Usage: <Process-ID> <Num-Processes> <binding-URL> <<destination> <weight>>...");
			System.exit(-1);
		}

		int id = Integer.parseInt(args[0]);
		int n = Integer.parseInt(args[1]);
		System.out.println(n+" processes");

		Edge[] edges = new Edge[n];
		for (int i = 3; i < args.length; i++) {
			int destination = Integer.parseInt(args[i++]);
			edges[destination] = new Edge(destination, Long.parseLong(args[i]));
		}
		Process process = new Process(id, edges, new Random());
		String url = args[2]+id;
		Naming.bind(url, process);
		System.out.println("Process with id "+id+" bound at "+url);

		System.out.println("Waiting 5 seconds before other processed are bound");
		Thread.sleep(5000L);

		process.startMST();
		for (Edge e : edges) {
			if (e != null)
				e.process = (MSTInterface) Naming.lookup(args[2]+e.destinationId);
		}
		System.out.println("Network registered successfully");

		System.out.println("Waiting 5 seconds before other processed have set references");
		Thread.sleep(5000L);

		synchronized (process) {
			while (! process.isHalted()) {
				process.wait();
			}
		}

		System.exit(0);
	}

}
