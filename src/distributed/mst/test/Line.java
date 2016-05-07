package distributed.mst.test;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.rmi.RemoteException;
import java.util.Random;

import distributed.mst.Edge;
import distributed.mst.Process;

public class Line {

	public static void main(String[] args) throws FileNotFoundException, RemoteException, InterruptedException {
		if (args.length == 0) {
			System.err.println("Usage: <num-processes>");
			System.exit(-1);
		}

		int n = Integer.parseInt(args[0]);

		// Create line (already a tree), with each edge created for both end points
		Random rand = new Random();
		Edge[][] edges = new Edge[n][n];
		// Mechanism to make all edges have unique weight
		boolean[] weights = new boolean[n-1];
		for (int i = 1; i < n; i++) {
			int index = rand.nextInt(n-i);
			for (int j = 0; j <= index; j++)
				if (weights[j])
					index++;
			// Extra (post-)increment to ensure non-zero positive
			weights[index++] = true;
			edges[i-1][i] = new Edge(i, index);
			edges[i][i-1] = new Edge(i-1, index);
		}

		Process[] processes = new Process[n];
		PrintStream[] logs = new PrintStream[n];
		for (int i = 0; i < n; i++) {
			logs[i] = new PrintStream("process"+i+".log");
			processes[i] = new Process(i, edges[i], logs[i]);
		}

		// Set references after processed have been created
		for (int i = 1; i < n; i++) {
			edges[i-1][i].process = processes[i];
			edges[i][i-1].process = processes[i-1];
		}

		// Choose random process to start to ensure generality
		Common.doTest(processes, rand.nextInt(n));

		for (int i = 0; i < n; i++) {
			logs[i].close();
		}

		System.exit(0);
	}

}
