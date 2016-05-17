package distributed.mst.test;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.rmi.RemoteException;
import java.util.Random;

import distributed.mst.Edge;
import distributed.mst.Process;

public class EdgeProbability {

	public static void main(String[] args) throws FileNotFoundException, RemoteException, InterruptedException {
		if (args.length < 2) {
			System.err.println("Usage: <num-processes> <edge-probability>");
			System.exit(-1);
		}

		int n = Integer.parseInt(args[0]);
		double edgeProbability = Double.parseDouble(args[1]);

		// Create graph, with each edge possibility at probability edgeProbability
		Random rand = new Random();
		Edge[][] edges = generateGraph(n, edgeProbability, rand);

		Process[] processes = new Process[n];
		PrintStream[] logs = new PrintStream[n];
		for (int i = 0; i < n; i++) {
			logs[i] = new PrintStream("mst_process"+i+".log");
			processes[i] = new Process(i, edges[i], logs[i]);
		}

		// Set references after processed have been created
		for (int i = 0; i < n; i++) {
			for (int j = i+1; j < n; j++) {
				if (edges[i][j] != null) {
					edges[i][j].process = processes[j];
					edges[j][i].process = processes[i];
				}
			}
		}

		// Choose random process to start to ensure generality
		Common.doTest(processes, rand.nextInt(n));

		for (int i = 0; i < n; i++) {
			logs[i].close();
		}

		System.exit(0);
	}

	public static Edge[][] generateGraph(int n, double edgeProbability, Random rand) {
		int[] prüfer = new int[n-2];
		for (int i = 0; i < prüfer.length; i++)
			prüfer[i] = rand.nextInt(n);
		Edge[][] edges = new Edge[n][n];
		int[] degrees = new int[n];
		for (int p : prüfer) {
			degrees[p]++;
		}
		int e = n;
		boolean[] weights = new boolean[--e];
		for (int p : prüfer)
			for (int j = 0; j < n; j++)
				if (p != j && degrees[j] == 0) {
					int index = rand.nextInt(e--);
					for (int k = 0; k <= index; k++)
						if (weights[k])
							index++;
					// Extra (post-)increment to ensure non-zero positive
					weights[index++] = true;
					edges[p][j] = new Edge(j, index);
					edges[j][p] = new Edge(p, index);
					degrees[p]--;
					degrees[j]--;
					break;
				}
		int u = -1, v = -1;
		for (int i = 0; i < n; i++)
			if (degrees[i] == 0) {
				if (u < 0)
					u = i;
				else {
					v = i;
					break;
				}
			}
		int index = 0;
		for (int k = 0; k <= index; k++)
			if (weights[k])
				index++;
		// Extra (post-)increment to ensure non-zero positive
		weights[index++] = true;
		edges[u][v] = new Edge(v, index);
		edges[v][u] = new Edge(u, index);

		for (int i = 0; i < n; i++)
			for (int j = i+1; j < n; j++)
				if (edges[i][j] == null && rand.nextDouble() < edgeProbability) {
					long weight = Math.abs(rand.nextLong());
					if (weight < n)
						weight = n;
					edges[i][j] = new Edge(j, weight);
					edges[j][i] = new Edge(i, weight);
				}

		return edges;
	}

}
