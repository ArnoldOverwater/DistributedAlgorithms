package distributed.mst.test.sh;

import java.io.PrintStream;

import distributed.mst.Edge;

public class Common {

	public static void generateSHFile(PrintStream file, Edge[][] edges, boolean[] startProcesses) {
		file.println("rmiregistry");
		for (int i = 0; i < startProcesses.length; i++) {
			if (startProcesses[i])
				file.print("java distributed.mst.test.MainStart");
			else
				file.print("java distributed.mst.test.Main");
			file.print(" "+i+" rmi://localhost:1099/");
			for (int j = 0; j < edges[i].length; j++) {
				if (edges[i][j] != null)
					file.print(" "+j+" "+edges[i][j].weight);
			}
			file.println(" &");
		}
	}

}
