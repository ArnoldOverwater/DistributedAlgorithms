package distributed.mst.test;

import java.rmi.RemoteException;

import distributed.mst.Edge;
import distributed.mst.Process;

public class Two {

	public static void main(String[] args) throws RemoteException, InterruptedException {
		Edge[] e0 = new Edge[] {null, new Edge(1, 1)};
		Edge[] e1 = new Edge[] {new Edge(0, 1), null};
		Process p0 = new Process(0, e0);
		Process p1 = new Process(1, e1);
		e0[1].process = p1;
		e1[0].process = p0;
		p0.startMST();
		synchronized (p0) {
			while (! p0.isHalted()) {
				p0.wait();
			}
		}
		synchronized (p1) {
			while (! p1.isHalted()) {
				p1.wait();
			}
		}
		System.out.println(p0.getTreeEdges());
		System.out.println(p1.getTreeEdges());
		System.exit(0);
	}

}
