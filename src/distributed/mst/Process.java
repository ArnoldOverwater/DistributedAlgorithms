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

	private class SendInitiate implements Runnable {

		private Edge edge;
		private int level;
		private long fragment;
		private State state;

		private SendInitiate(Edge edge, int level, long fragment, State state) {
			this.edge = edge;
			this.level = level;
			this.fragment = fragment;
			this.state = state;
		}

		@Override
		public void run() {
			try {
				edge.process.initiate(myId, level, fragment, state);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		
	}

	private class SendTest implements Runnable {

		private Edge edge;
		private int level;
		private long fragment;

		private SendTest(Edge edge, int level, long fragment) {
			this.edge = edge;
			this.level = level;
			this.fragment = fragment;
		}

		@Override
		public void run() {
			try {
				edge.process.test(myId, level, fragment);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		
	}

	private class SendReject implements Runnable {

		private Edge edge;

		private SendReject(Edge edge) {
			this.edge = edge;
		}

		@Override
		public void run() {
			try {
				edge.process.reject(myId);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		
	}

	private class SendAccept implements Runnable {

		private Edge edge;

		private SendAccept(Edge edge) {
			this.edge = edge;
		}

		@Override
		public void run() {
			try {
				edge.process.accept(myId);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		
	}

	private class SendReport implements Runnable {

		private Edge edge;
		private long weight;

		private SendReport(Edge edge, long weight) {
			this.edge = edge;
			this.weight = weight;
		}

		@Override
		public void run() {
			try {
				edge.process.report(myId, weight);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		
	}

	private static class SendChangeRoot implements Runnable {

		private Edge edge;

		private SendChangeRoot(Edge edge) {
			this.edge = edge;
		}

		@Override
		public void run() {
			try {
				edge.process.changeRoot();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		
	}

	private class SendConnect implements Runnable {

		private Edge edge;
		private int level;

		private SendConnect(Edge edge, int level) {
			this.edge = edge;
			this.level = level;
		}

		@Override
		public void run() {
			try {
				edge.process.connect(myId, level);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		
	}

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
		new Thread(new SendConnect(minEdge, 0)).start();
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
					new Thread(new SendInitiate(e, level, fragment, state)).start();
					if (state == State.Find)
						findCount++;
				}
			}
			if (state == State.Find)
				test();
			notifyAll();
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
			while (level > this.level) {
				try {
					wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			if (fragment != this.fragment)
				new Thread(new SendAccept(from)).start();
			else {
				if (from.state == EdgeState.Unknown) {
					from.state = EdgeState.NotInMST;
					unknownEdges.remove(from);
					notifyAll();
				}
				if (testEdge != from)
					new Thread(new SendReject(from)).start();
				else
					test();
			}
		}
	}

	private void test() throws RemoteException {
		if (! unknownEdges.isEmpty()) {
			testEdge = unknownEdges.peek();
			new Thread(new SendTest(testEdge, level, fragment)).start();
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
				notifyAll();
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
			} else {
				while (state == State.Find) {
					try {
						wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				if (weight > bestMOE)
					changeRootInternal();
				else if (weight == Long.MAX_VALUE && bestMOE == Long.MAX_VALUE) {
					state = State.Halted;
					notifyAll();
				}
			}
		}
	}

	private void report() throws RemoteException {
		if (findCount == 0 && testEdge == null) {
			state = State.Found;
			new Thread(new SendReport(toCore, bestMOE)).start();
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
			new Thread(new SendChangeRoot(toBestMOE)).start();
		else {
			new Thread(new SendConnect(toBestMOE, level)).start();
			toBestMOE.state = EdgeState.InMST;
			unknownEdges.remove(toBestMOE);
			inMSTEdges.add(toBestMOE);
			notifyAll();
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
				new Thread(new SendInitiate(from, this.level, fragment, state)).start();
				if (state == State.Find)
					findCount++;
			} else {
				while (from.state == EdgeState.Unknown) {
					try {
						wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				new Thread(new SendInitiate(from, this.level + 1, from.weight, State.Find)).start();
			}
		}
	}

	public boolean isHalted() {
		return state == State.Halted;
	}

}
