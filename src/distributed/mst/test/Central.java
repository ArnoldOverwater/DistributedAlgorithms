package distributed.mst.test;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.rmi.RemoteException;
import java.util.Random;

import distributed.mst.Edge;
import distributed.mst.Process;

public class Central {

	public static void main(String[] args) throws FileNotFoundException, RemoteException, InterruptedException {
		if (args.length == 0) {
			System.err.println("Usage: <num-processes>");
			System.exit(-1);
		}

		int n = Integer.parseInt(args[0]);

		// Create centralised graph (already a tree), with each edge created for both end points
		Random rand = new Random();
		Edge[][] edges = new Edge[n][n];
		for (int i = 1; i < n; i++) {
			edges[0][i] = new Edge(i, i);
			edges[i][0] = new Edge(0, i);
		}

		Process[] processes = new Process[n];
		PrintStream[] logs = new PrintStream[n];
		for (int i = 0; i < n; i++) {
			logs[i] = new PrintStream("process"+i+".log");
			processes[i] = new Process(i, edges[i], logs[i]);
		}

		// Set references after processed have been created
		for (int i = 1; i < n; i++) {
			edges[0][i].process = processes[i];
			edges[i][0].process = processes[0];
		}

		// Choose random process to start to ensure generality
		int id = rand.nextInt(n);
		System.out.println("Started process "+id);
		processes[id].startMST();
		for (int i = 0; i < n; i++) {
			synchronized (processes[i]) {
				while (! processes[i].isHalted()) {
					processes[i].wait();
				}
				logs[i].close();
			}
		}

		System.exit(0);
	}

}
