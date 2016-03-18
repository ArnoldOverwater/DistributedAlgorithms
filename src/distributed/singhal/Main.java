package distributed.singhal;

import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Random;

/**
 * Simulation of one process execution of a Singhal's mutual exclusion algorithm.
 * Needs one running VM per process and a running `rmiregistry` for a successful simulation.
 */
public class Main {

	public static void main(String[] args) throws RemoteException, MalformedURLException, AlreadyBoundException, InterruptedException, NotBoundException {
		if (args.length < 3) {
			System.err.println("Usage: <Process-ID> <Num-Processes> <binding-URL>");
			System.exit(-1);
		}

		// Description in usage message above
		int id = Integer.parseInt(args[0]);
		int n = Integer.parseInt(args[1]);
		System.out.println(n+" processes");

		Process process = new Process(id, n);
		String url = args[2]+id;
		Naming.bind(url, process);
		System.out.println("Process with id "+id+" bound at "+url);

		System.out.println("Waiting 5 seconds before other processed are bound");
		Thread.sleep(5000L);

		for (int i = 0; i < n; i++)
			process.setProcess(i, (SinInterface) Naming.lookup(args[2]+i));
		System.out.println("Network registered successfully");

		System.out.println("Waiting 5 seconds before other processed have set references");
		Thread.sleep(5000L);

		// Randomly generate a number of CS requests, time needed in CS, and idle times
		Random rand = new Random();
		int c = 3 + rand.nextInt(4);
		for (int i = 0; i < c; i++) {
			// Random idle time
			Thread.sleep(1000L + rand.nextInt(5) * 1000L);

			// Random time needed for CS
			process.tryAccessCS(100L + rand.nextInt(5) * 100L);
		}

		// Assume every CS execution of all processes is done by then
		System.out.println("Main thread finished; waiting 30 seconds to end process");
		Thread.sleep(30000L);
		System.exit(0);
	}

}
