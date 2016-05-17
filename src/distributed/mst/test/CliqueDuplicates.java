package distributed.mst.test;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.rmi.RemoteException;
import java.util.Random;

import distributed.mst.Edge;
import distributed.mst.Process;

public class CliqueDuplicates {

	public static void main(String[] args) throws FileNotFoundException, RemoteException, InterruptedException {
		if (args.length == 0) {
			System.err.println("Usage: <num-processes>");
			System.exit(-1);
		}

		int n = Integer.parseInt(args[0]);

		// Create clique, with each edge created for both end points
		Random rand = new Random();
		Edge[][] edges = new Edge[n][n];
		for (int i = 0; i < n; i++) {
			for (int j = i+1; j < n; j++) {
				long weight = rand.nextLong();
				edges[i][j] = new Edge(j, weight);
				edges[j][i] = new Edge(i, weight);
			}
		}

		Process[] processes = new Process[n];
		PrintStream[] logs = new PrintStream[n];
		for (int i = 0; i < n; i++) {
			logs[i] = new PrintStream("mst_process"+i+".log");
			processes[i] = new Process(i, edges[i], logs[i]);
		}

		// Set references after processed have been created
		for (int i = 0; i < n; i++) {
			for (int j = i+1; j < n; j++) {
				edges[i][j].process = processes[j];
				edges[j][i].process = processes[i];
			}
		}

		// Choose random process to start to ensure generality
		Common.doTest(processes, rand.nextInt(n));

		for (int i = 0; i < n; i++) {
			logs[i].close();
		}

		System.exit(0);
	}

}
