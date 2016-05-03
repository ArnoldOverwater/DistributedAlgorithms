package distributed.mst.test;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.rmi.RemoteException;
import java.util.Random;

import distributed.mst.Edge;
import distributed.mst.Process;

public class Clique {

	public static void main(String[] args) throws FileNotFoundException, RemoteException, InterruptedException {
		if (args.length == 0) {
			System.err.println("Usage: <num-processes>");
			System.exit(-1);
		}

		int n = Integer.parseInt(args[0]);
		int e = (n*(n-1))/2;

		// Create clique, with each edge created for both end points
		Random rand = new Random();
		Edge[][] edges = new Edge[n][n];
		// Mechanism to make all edges have unique weight
		boolean[] weights = new boolean[e];
		for (int i = 0; i < n; i++) {
			for (int j = i+1; j < n; j++) {
				int index = rand.nextInt(e--);
				for (int k = 0; k <= index; k++)
					if (weights[k])
						index++;
				// Extra (post-)increment to ensure non-zero positive
				weights[index++] = true;
				edges[i][j] = new Edge(j, index);
				edges[j][i] = new Edge(i, index);
			}
		}

		Process[] processes = new Process[n];
		PrintStream[] logs = new PrintStream[n];
		for (int i = 0; i < n; i++) {
			logs[i] = new PrintStream("process"+i+".log");
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
		processes[rand.nextInt(n)].startMST();
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
