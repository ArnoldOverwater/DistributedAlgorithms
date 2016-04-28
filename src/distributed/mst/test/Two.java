package distributed.mst.test;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.rmi.RemoteException;

import distributed.mst.Edge;
import distributed.mst.Process;

public class Two {

	public static void main(String[] args) throws RemoteException, InterruptedException, FileNotFoundException {
		Edge[] e0 = new Edge[] {null, new Edge(1, 1)}, e1 = new Edge[] {new Edge(0, 1), null};
		PrintStream log0 = new PrintStream("process0.log"), log1 = new PrintStream("process1.log");
		Process p0 = new Process(0, e0, log0), p1 = new Process(1, e1, log1);
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
		log0.close();
		log1.close();
		System.out.println(p0.getTreeEdges());
		System.out.println(p1.getTreeEdges());
		System.exit(0);
	}

}
