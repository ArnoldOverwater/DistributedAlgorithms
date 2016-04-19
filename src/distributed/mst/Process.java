package distributed.mst;

import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.LinkedList;
import java.util.List;

public class Process extends UnicastRemoteObject implements MSTInterface {
	private static final long serialVersionUID = 2588333077731364976L;

	private int myId;
	private Edge[] edges;
	private State state;
	private long fragment;
	private int level;
	private Edge toCore;
	private int findCount;
	private Edge toBestMOE;
	private long bestMOE;
	private Edge testEdge;

	private List<Edge> unknownEdges;
	private List<Edge> inMSTEdges;

	public Process(int id, Edge[] edges) throws RemoteException {
		super(0);
		this.myId = id;
		this.edges = edges;
		this.state = State.Sleeping;
		this.unknownEdges = new LinkedList<Edge>();
		for (Edge e : edges) {
			this.unknownEdges.add(e);
		}
		this.inMSTEdges = new LinkedList<Edge>();
	}

	public void startMST() {
		if (state == State.Sleeping)
			wakeup();
	}

	private void wakeup() {
		long minWeight = Long.MAX_VALUE;
		Edge minEdge = null;
		for (Edge e : edges) {
			if (e.weight < minWeight)
				minEdge = e;
		}
		minEdge.state = EdgeState.InMST;
		unknownEdges.remove(minEdge);
		inMSTEdges.add(minEdge);
		level = 0;
		state = State.Found;
		findCount = 0;
		minEdge.process.connect(myId, 0);
	}

	@Override
	public void initiate(int fromId, int level, long fragment, State state)
			throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public void test(int fromId, int level, long fragment)
			throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public void reject(int fromId) throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public void accept(int fromId) throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public void report(int fromId, long weight) throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public void changeRoot(int fromId) throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public void connect(int fromId, int level) throws RemoteException {
		// TODO Auto-generated method stub

	}

}
