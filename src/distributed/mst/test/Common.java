package distributed.mst.test;

import java.rmi.RemoteException;
import java.util.ArrayDeque;
import java.util.Queue;

import distributed.mst.Edge;
import distributed.mst.Process;

public class Common {

	private static class OriginDestinationPair {

		private final int origin;
		private final int destination;

		private OriginDestinationPair(int origin, int destination) {
			this.origin = origin;
			this.destination = destination;
		}

	}

	public static void doTest(Process[] processes, int startId) throws RemoteException, InterruptedException {
		System.out.println("Started process "+startId);
		processes[startId].startMST();
		for (int i = 0; i < processes.length; i++) {
			synchronized (processes[i]) {
				while (! processes[i].isHalted()) {
					processes[i].wait();
				}
			}
		}
		checkTree(processes);
	}

	public static void checkTree(Process[] processes) {
		boolean[] marked = new boolean[processes.length];
		Queue<OriginDestinationPair> q = new ArrayDeque<OriginDestinationPair>(processes.length);
		boolean isTree = true;
		q.offer(new OriginDestinationPair(-1, 0));
		while (! q.isEmpty()) {
			OriginDestinationPair pair = q.poll();
			if (marked[pair.destination]) {
				System.out.println("Process "+pair.destination+" is part of a cycle");
				isTree = false;
			} else {
				marked[pair.destination] = true;
				for (Edge e : processes[pair.destination].getTreeEdges()) {
					if (e.destinationId != pair.origin)
						q.offer(new OriginDestinationPair(pair.destination, e.destinationId));
				}
			}
		}
		if (isTree)
			System.out.println("No cycles found");
		else
			System.out.println("Cycles found");
		isTree = true;
		for (int i = 0; i < processes.length; i++)
			if (! marked[i]) {
				System.out.println("Process "+i+" is not included in the tree");
				isTree = false;
			}
		if (isTree)
			System.out.println("All processes included");
		else
			System.out.println("Some processes not included");
	}

}
