package distributed.mst;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

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

	private Queue<Edge> unknownEdges;
	private Set<Edge> inMSTEdges;

	public Process(int id, Edge[] edges) throws RemoteException {
		super(0);
		this.myId = id;
		this.edges = edges;
		this.state = State.Sleeping;
		this.unknownEdges = new PriorityQueue<Edge>();
		for (Edge e : edges) {
			if (e != null)
				this.unknownEdges.add(e);
		}
		this.inMSTEdges = new TreeSet<Edge>();
	}

	public void startMST() throws RemoteException {
		synchronized (this) {
			if (state == State.Sleeping)
				wakeup();
		}
	}

	private void wakeup() throws RemoteException {
		long minWeight = Long.MAX_VALUE;
		Edge minEdge = null;
		for (Edge e : edges) {
			if (e != null && e.weight < minWeight) {
				minWeight = e.weight;
				minEdge = e;
			}
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
	public void initiate(int fromId, int level, long fragment, State state) throws RemoteException {
		initiate(edges[fromId], level, fragment, state);
	}

	private void initiate(Edge from, int level, long fragment, State state) throws RemoteException {
		synchronized (this) {
			this.level = level;
			this.fragment = fragment;
			this.state = state;
			this.toCore = from;
			this.toBestMOE = null;
			this.bestMOE = Long.MAX_VALUE;
			for (Edge e : inMSTEdges) {
				if (e != from) {
					e.process.initiate(myId, level, fragment, state);
					if (state == State.Find)
						findCount++;
				}
			}
			if (state == State.Find)
				test();
		}
	}

	@Override
	public void test(int fromId, int level, long fragment) throws RemoteException {
		test(edges[fromId], level, fragment);
	}

	private void test(Edge from, int level, long fragment) throws RemoteException {
		synchronized (this) {
			if (state == State.Sleeping)
				wakeup();
			if (level > this.level) {
				// TODO Message queue
			} else if (fragment != this.fragment)
				from.process.accept(myId);
			else {
				if (from.state == EdgeState.Unknown) {
					from.state = EdgeState.NotInMST;
					unknownEdges.remove(from);
				}
				if (testEdge != from)
					from.process.reject(myId);
				else
					test();
			}
		}
	}

	private void test() throws RemoteException {
		if (! unknownEdges.isEmpty()) {
			testEdge = unknownEdges.peek();
			testEdge.process.test(myId, level, fragment);
		} else {
			testEdge = null;
			report();
		}
	}

	@Override
	public void reject(int fromId) throws RemoteException {
		reject(edges[fromId]);
	}

	private void reject(Edge from) throws RemoteException {
		synchronized (this) {
			if (from.state == EdgeState.Unknown) {
				from.state = EdgeState.NotInMST;
				unknownEdges.remove(from);
			}
			test();
		}
	}

	@Override
	public void accept(int fromId) throws RemoteException {
		accept(edges[fromId]);
	}

	private void accept(Edge from) throws RemoteException {
		synchronized (this) {
			testEdge = null;
			if (from.weight < bestMOE) {
				toBestMOE = from;
				bestMOE = toBestMOE.weight;
			}
			report();
		}
	}

	@Override
	public void report(int fromId, long weight) throws RemoteException {
		report(edges[fromId], weight);
	}

	private void report(Edge from, long weight) throws RemoteException {
		synchronized (this) {
			if (from != toCore) {
				findCount--;
				if (weight < bestMOE) {
					bestMOE = weight;
					toBestMOE = from;
				}
				report();
			} else if (state == State.Find) {
				// TODO: Message queue
			} else if (weight > bestMOE)
				changeRootInternal();
			else if (weight == Long.MAX_VALUE && bestMOE == Long.MAX_VALUE) {
				// TODO: HALT
			}
		}
	}

	private void report() throws RemoteException {
		if (findCount == 0 && testEdge == null) {
			state = State.Found;
			toCore.process.report(myId, bestMOE);
		}
	}

	@Override
	public void changeRoot() throws RemoteException {
		synchronized (this) {
			changeRootInternal();
		}
	}

	private void changeRootInternal() throws RemoteException {
		if (toBestMOE.state == EdgeState.InMST)
			toBestMOE.process.changeRoot();
		else {
			toBestMOE.process.connect(myId, level);
			toBestMOE.state = EdgeState.InMST;
			unknownEdges.remove(toBestMOE);
			inMSTEdges.add(toBestMOE);
		}
	}

	@Override
	public void connect(int fromId, int level) throws RemoteException {
		connect(edges[fromId], level);
	}

	private void connect(Edge from, int level) throws RemoteException {
		synchronized (this) {
			if (state == State.Sleeping)
				wakeup();
			if (level < this.level) {
				from.state = EdgeState.InMST;
				unknownEdges.remove(from);
				inMSTEdges.add(from);
				from.process.initiate(myId, this.level, fragment, state);
				if (state == State.Find)
					findCount++;
			} else if (from.state == EdgeState.Unknown) {
				// TODO: Message queue
			} else
				from.process.initiate(myId, this.level + 1, from.weight, State.Find);
		}
	}

}
