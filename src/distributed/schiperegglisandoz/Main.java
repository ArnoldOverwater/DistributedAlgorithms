package distributed.schiperegglisandoz;

import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.util.Random;

public class Main {
	// Starts a single instance of the distributed system running the Schiper-Eggli-Sandoz algorithm
	public static void main(String[] args) throws AlreadyBoundException, NotBoundException, IOException, InterruptedException {
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

		System.out.println("Waiting 10 seconds before other processed are bound");
		Thread.sleep(10000L);

		for (int i = 0; i < n; i++)
			process.setProcess(i, (SESInterface) Naming.lookup(args[i + 2]));
		System.out.println("Network registered successfully");

		// Start execution after RMI setup has been completed
		// Send 5 to 10 messages to a random process after a random delay of 100 to 500 ms
		Random rand = new Random();
		int m = 5 + rand.nextInt(5);
		for (int i = 0; i < m; i++) {
			// Random idle time
			Thread.sleep(rand.nextInt(5) * 100L);

			// Simple prevention against sending to self
			int recipient = rand.nextInt(n - 1);
			if (recipient >= id)
				recipient++;

			// Send with random delay
			process.send(id+"-"+i, recipient, rand.nextInt(5) * 1000L);
		}

		System.out.println("Main thread finished; waiting 10 seconds to end process");
		Thread.sleep(10000L);
		System.exit(0);
	}

}
