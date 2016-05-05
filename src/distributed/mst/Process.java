package distributed.mst;

import java.io.PrintStream;
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

	private PrintStream log;

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

	private class SendHalt implements Runnable {

		private Edge edge;

		private SendHalt(Edge edge) {
			this.edge = edge;
		}

		@Override
		public void run() {
			try {
				edge.process.halt(myId);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

	}

	public Process(int id, Edge[] edges) throws RemoteException {
		this(id, edges, System.out);
	}

	public Process(int id, Edge[] edges, PrintStream log) throws RemoteException {
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
		this.log = log;
		log.println("Process "+id+", Edges: "+unknownEdges);
	}

	public void startMST() throws RemoteException {
		synchronized (this) {
			if (state == State.Sleeping)
				wakeup();
		}
	}

	private void wakeup() throws RemoteException {
		log.print("Woken up; ");
		Edge minEdge = unknownEdges.poll();
		minEdge.state = EdgeState.InMST;
		inMSTEdges.add(minEdge);
		level = 0;
		state = State.Found;
		findCount = 0;
		log.println("Sending Connect(Level = 0) along "+minEdge);
		new Thread(new SendConnect(minEdge, 0)).start();
	}

	@Override
	public void initiate(int fromId, int level, long fragment, State state) throws RemoteException {
		initiate(edges[fromId], level, fragment, state);
	}

	private void initiate(Edge from, int level, long fragment, State state) throws RemoteException {
		synchronized (this) {
			log.println("Received Initiate(Level = "+level+", Fragment = "+fragment+", State = "+state+") along "+from);
			this.level = level;
			this.fragment = fragment;
			this.state = state;
			this.toCore = from;
			this.toBestMOE = null;
			this.bestMOE = Long.MAX_VALUE;
			for (Edge e : inMSTEdges) {
				if (e != from) {
					log.println("Sending Initiate(Level = "+level+", Fragment = "+fragment+", State = "+state+") along "+e);
					new Thread(new SendInitiate(e, level, fragment, state)).start();
					if (state == State.Find)
						findCount++;
				}
			}
			if (state == State.Find) {
				log.println("Find count = "+findCount);
				test();
			}
			notifyAll();
		}
	}

	@Override
	public void test(int fromId, int level, long fragment) throws RemoteException {
		test(edges[fromId], level, fragment);
	}

	private void test(Edge from, int level, long fragment) throws RemoteException {
		synchronized (this) {
			log.println("Received Test(Level = "+level+", Fragment = "+fragment+") along "+from);
			if (state == State.Sleeping)
				wakeup();
			if (level > this.level) {
				log.println("Putting Test(Level = "+level+", Fragment = "+fragment+") along "+from+" on hold");
				do {
					try {
						wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					// This can have been changed while waiting
					//if (from.state == EdgeState.InMST)
					//return;
				} while (level > this.level);
				log.println("Resuming Test(Level = "+level+", Fragment = "+fragment+") along "+from);
			}
			if (fragment != this.fragment) {
				log.println("Sending Accept along "+from);
				new Thread(new SendAccept(from)).start();
			} else {
				if (from.state == EdgeState.Unknown) {
					from.state = EdgeState.NotInMST;
					unknownEdges.remove(from);
					notifyAll();
				}
				if (testEdge != from) {
					log.println("Sending Reject along "+from);
					new Thread(new SendReject(from)).start();
				} else
					test();
			}
		}
	}

	private void test() throws RemoteException {
		if (! unknownEdges.isEmpty()) {
			testEdge = unknownEdges.peek();
			log.println("Sending Test(Level = "+level+", Fragment = "+fragment+") along "+testEdge);
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
			log.println("Received Reject along "+from);
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
			log.println("Received Accept along "+from);
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
			log.println("Received Report(Weight = "+weight+") along "+from);
			if (from != toCore) {
				findCount--;
				if (weight < bestMOE) {
					bestMOE = weight;
					toBestMOE = from;
				}
				report();
			} else {
				if (state == State.Find) {
					log.println("Putting Report(Weight = "+weight+") along "+from+" on hold");
					do {
						try {
							wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					} while (state == State.Find);
					log.println("Resuming Report(Weight = "+weight+") along "+from);
				}
				if (weight > bestMOE)
					changeRootInternal();
				else if (weight == Long.MAX_VALUE && bestMOE == Long.MAX_VALUE) {
					log.println("Sending Halt along "+from);
					new Thread(new SendHalt(from)).start();
				}
			}
		}
	}

	private void report() throws RemoteException {
		if (findCount == 0 && testEdge == null) {
			state = State.Found;
			log.println("Sending Report(Weight = "+bestMOE+") along "+toCore);
			new Thread(new SendReport(toCore, bestMOE)).start();
			notifyAll();
		}
	}

	@Override
	public void changeRoot() throws RemoteException {
		synchronized (this) {
			changeRootInternal();
		}
	}

	private void changeRootInternal() throws RemoteException {
		log.println("Changing root");
		if (toBestMOE.state == EdgeState.InMST)
			new Thread(new SendChangeRoot(toBestMOE)).start();
		else {
			log.println("Sending Connect(Level = "+level+") along "+toBestMOE);
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
			log.println("Received Connect(Level = "+level+") along "+from);
			if (state == State.Sleeping)
				wakeup();
			if (level >= this.level && from.state == EdgeState.Unknown) {
				log.println("Putting Connect(Level = "+level+") along "+from+" on hold");
				do {
					try {
						wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				} while (level >= this.level && from.state == EdgeState.Unknown);
				log.println("Resuming Connect(Level = "+level+") along "+from);
			}
			if (level < this.level) {
				from.state = EdgeState.InMST;
				unknownEdges.remove(from);
				inMSTEdges.add(from);
				log.println("Absolving fragment with level "+level+" by this fragment of level "+this.level);
				log.println("Sending Initiate(Level = "+this.level+", Fragment = "+fragment+", State = "+state+") along "+from);
				new Thread(new SendInitiate(from, this.level, fragment, state)).start();
				if (state == State.Find)
					findCount++;
			} else {
				log.println("Merging fragments with level "+level+" to form level "+(this.level + 1));
				log.println("Sending Initiate(Level = "+(this.level + 1)+", Fragment = "+from.weight+", State = Find) along "+from);
				new Thread(new SendInitiate(from, this.level + 1, from.weight, State.Find)).start();
			}
		}
	}

	public void halt(int fromId) {
		halt(edges[fromId]);
	}

	private void halt(Edge from) {
		synchronized (this) {
			log.println("Received Halt along "+from);
			for (Edge e : inMSTEdges) {
				if (e != from) {
					log.println("Sending Halt along "+e);
					new Thread(new SendHalt(e)).start();
				}
			}
			state = State.Halted;
			log.println("Finished, Edges in MST: "+inMSTEdges);
			log.println("Final fragment: "+fragment+", Final level: "+level);
			notifyAll();
		}
	}

	public boolean isHalted() {
		return state == State.Halted;
	}

	public Set<Edge> getTreeEdges() {
		return inMSTEdges;
	}

}
