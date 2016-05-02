package distributed.mst.test;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.rmi.RemoteException;
import java.util.Random;

import distributed.mst.Edge;
import distributed.mst.Process;

public class Ring {

	public static void main(String[] args) throws FileNotFoundException, RemoteException, InterruptedException {
		if (args.length == 0) {
			System.err.println("Usage: <num-processes>");
			System.exit(-1);
		}

		int n = Integer.parseInt(args[0]);

		Random rand = new Random();
		Edge[][] edges = new Edge[n][n];
		edges[n-1][0] = new Edge(0, n);
		edges[0][n-1] = new Edge(n-1, n);
		boolean[] weights = new boolean[n-1];
		for (int i = 1; i < n; i++) {
			int index = rand.nextInt(n-i);
			for (int j = 0; j <= index; j++)
				if (weights[j])
					index++;
			weights[index] = true;
			edges[i-1][i] = new Edge(i, index+1);
			edges[i][i-1] = new Edge(i-1, index+1);
		}
		Process[] processes = new Process[n];
		PrintStream[] logs = new PrintStream[n];
		for (int i = 0; i < n; i++) {
			logs[i] = new PrintStream("process"+i+".log");
			processes[i] = new Process(i, edges[i], logs[i]);
		}
		edges[n-1][0].process = processes[0];
		edges[0][n-1].process = processes[n-1];
		for (int i = 1; i < n; i++) {
			edges[i-1][i].process = processes[i];
			edges[i][i-1].process = processes[i-1];
		}

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
