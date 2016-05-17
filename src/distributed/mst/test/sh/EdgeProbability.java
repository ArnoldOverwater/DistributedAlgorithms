package distributed.mst.test.sh;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.rmi.RemoteException;
import java.util.Random;

import distributed.mst.Edge;

public class EdgeProbability {

	public static void main(String[] args) throws FileNotFoundException, RemoteException, InterruptedException {
		if (args.length < 3) {
			System.err.println("Usage: <num-processes> <edge-probability> <repetitions>");
			System.exit(-1);
		}

		int n = Integer.parseInt(args[0]);
		double edgeProbability = Double.parseDouble(args[1]);
		int r = Integer.parseInt(args[2]);

		Random rand = new Random();
		Edge[][] edges = distributed.mst.test.EdgeProbability.generateGraph(n, edgeProbability, rand);

		for (int i = 0; i < r; i++) {
			boolean[] startProcesses = new boolean[n];
			do {
				startProcesses[rand.nextInt(n)] = true;
			} while (rand.nextFloat() < 0.5f);
			PrintStream file = new PrintStream("edgeprobability"+n+"-"+edgeProbability+"-"+i+".sh");
			Common.generateSHFile(file, edges, startProcesses);
			file.close();
		}
	}

}
