package distributed.singhal;

import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Random;

public class Main {

	public static void main(String[] args) throws RemoteException, MalformedURLException, AlreadyBoundException, InterruptedException, NotBoundException {
		if (args.length < 3) {
			System.err.println("Usage: <Process-ID> <Num-Processes> <binding-URL>");
			System.exit(-1);
		}

		int id = Integer.parseInt(args[0]);
		int n = Integer.parseInt(args[1]);
		System.out.println(n+" processes");

		Process process = new Process(id, n);
		String url = args[2]+id;
		Naming.bind(url, process);
		System.out.println("Process with id "+id+" bound at "+url);

		System.out.println("Waiting 10 seconds before other processed are bound");
		Thread.sleep(10000L);

		for (int i = 0; i < n; i++)
			process.setProcess(i, (SinInterface) Naming.lookup(args[2]+i));
		System.out.println("Network registered successfully");

		Random rand = new Random();
		int c = 3 + rand.nextInt(4);
		for (int i = 0; i < c; i++) {
			// Random idle time
			Thread.sleep(rand.nextInt(5) * 100L);

			process.tryAccessCS();
		}
	}

}
