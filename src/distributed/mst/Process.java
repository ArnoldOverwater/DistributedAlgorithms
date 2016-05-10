package distributed.mst;

import java.io.PrintStream;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
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

	// For maintaining causal ordering between merger Connect and merger Initiate
	private boolean merged;

	private Queue<Edge> unknownEdges;
	private Set<Edge> inMSTEdges;

	private PrintStream log;
	private Random delayer;

	private class SendInitiate implements Runnable {

		private Edge edge;
		private int level;
		private long fragment;
		private State state;
		private long delay;

		private SendInitiate(Edge edge, int level, long fragment, State state, long delay) {
			this.edge = edge;
			this.level = level;
			this.fragment = fragment;
			this.state = state;
			this.delay = delay;
		}

		@Override
		public void run() {
			try {
				if (delay > 0L)
					Thread.sleep(delay);
			} catch (InterruptedException e) {
			} finally {
				try {
					edge.process.initiate(myId, level, fragment, state);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}

	}

	private class SendTest implements Runnable {

		private Edge edge;
		private int level;
		private long fragment;
		private long delay;

		private SendTest(Edge edge, int level, long fragment, long delay) {
			this.edge = edge;
			this.level = level;
			this.fragment = fragment;
			this.delay = delay;
		}

		@Override
		public void run() {
			try {
				if (delay > 0L)
					Thread.sleep(delay);
			} catch (InterruptedException e) {
			} finally {
				try {
					edge.process.test(myId, level, fragment);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}

	}

	private class SendReject implements Runnable {

		private Edge edge;
		private long delay;

		private SendReject(Edge edge, long delay) {
			this.edge = edge;
			this.delay = delay;
		}

		@Override
		public void run() {
			try {
				if (delay > 0L)
					Thread.sleep(delay);
			} catch (InterruptedException e) {
			} finally {
				try {
					edge.process.reject(myId);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}

	}

	private class SendAccept implements Runnable {

		private Edge edge;
		private long delay;

		private SendAccept(Edge edge, long delay) {
			this.edge = edge;
			this.delay = delay;
		}

		@Override
		public void run() {
			try {
				if (delay > 0L)
					Thread.sleep(delay);
			} catch (InterruptedException e) {
			} finally {
				try {
					edge.process.accept(myId);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}

	}

	private class SendReport implements Runnable {

		private Edge edge;
		private long weight;
		private long delay;

		private SendReport(Edge edge, long weight, long delay) {
			this.edge = edge;
			this.weight = weight;
			this.delay = delay;
		}

		@Override
		public void run() {
			try {
				if (delay > 0L)
					Thread.sleep(delay);
			} catch (InterruptedException e) {
			} finally {
				try {
					edge.process.report(myId, weight);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}

	}

	private static class SendChangeRoot implements Runnable {

		private Edge edge;
		private long delay;

		private SendChangeRoot(Edge edge, long delay) {
			this.edge = edge;
			this.delay = delay;
		}

		@Override
		public void run() {
			try {
				if (delay > 0L)
					Thread.sleep(delay);
			} catch (InterruptedException e) {
			} finally {
				try {
					edge.process.changeRoot();
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}

	}

	private class SendConnect implements Runnable {

		private Edge edge;
		private int level;
		private long delay;

		private SendConnect(Edge edge, int level, long delay) {
			this.edge = edge;
			this.level = level;
			this.delay = delay;
		}

		@Override
		public void run() {
			try {
				if (delay > 0L)
					Thread.sleep(delay);
			} catch (InterruptedException e) {
			} finally {
				try {
					edge.process.connect(myId, level);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}

	}

	private class SendHalt implements Runnable {

		private Edge edge;
		private long delay;

		private SendHalt(Edge edge, long delay) {
			this.edge = edge;
			this.delay = delay;
		}

		@Override
		public void run() {
			try {
				if (delay > 0L)
					Thread.sleep(delay);
			} catch (InterruptedException e) {
			} finally {
				try {
					edge.process.halt(myId);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}

	}

	public Process(int id, Edge[] edges) throws RemoteException {
		this(id, edges, System.out, null);
	}

	public Process(int id, Edge[] edges, PrintStream log) throws RemoteException {
		this(id, edges, log, null);
	}

	public Process(int id, Edge[] edges, Random delayer) throws RemoteException {
		this(id, edges, System.out, delayer);
	}

	public Process(int id, Edge[] edges, PrintStream log, Random delayer) throws RemoteException {
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
		this.delayer = delayer;
		log.println("Process "+id+", Edges: "+unknownEdges);
	}

	public void startMST() throws RemoteException {
		synchronized (this) {
			if (state == State.Sleeping)
				wakeup();
		}
	}

	private void wakeup() throws RemoteException {
		log.println("Woken up");
		Edge minEdge = unknownEdges.poll();
		minEdge.state = EdgeState.InMST;
		inMSTEdges.add(minEdge);
		level = 0;
		state = State.Found;
		findCount = 0;
		log.println("Sending Connect(Level = 0) along "+minEdge);
		long delay = 0L;
		if (delayer != null)
			delay = 10 * delayer.nextInt(5);
		new Thread(new SendConnect(minEdge, 0, delay)).start();
	}

	@Override
	public void initiate(int fromId, int level, long fragment, State state) throws RemoteException {
		initiate(edges[fromId], level, fragment, state);
	}

	private void initiate(Edge from, int level, long fragment, State state) throws RemoteException {
		synchronized (this) {
			log.println("Received Initiate(Level = "+level+", Fragment = "+fragment+", State = "+state+") along "+from);
			// A merger is happening, but the Connect hasn't been received
			if (from.weight == fragment && ! merged) {
				log.println("Putting Initiate(Level = "+level+", Fragment = "+fragment+", State = "+state+") along "+from+" on hold");
				do {
					try {
						wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				} while (! merged);
				log.println("Resuming Initiate(Level = "+level+", Fragment = "+fragment+", State = "+state+") along "+from);
			}
			this.level = level;
			this.fragment = fragment;
			this.state = state;
			this.toCore = from;
			this.toBestMOE = null;
			this.bestMOE = Long.MAX_VALUE;
			this.merged = false;
			for (Edge e : inMSTEdges) {
				if (e != from) {
					log.println("Sending Initiate(Level = "+level+", Fragment = "+fragment+", State = "+state+") along "+e);
					long delay = 0L;
					if (delayer != null)
						delay = 10 * delayer.nextInt(5);
					new Thread(new SendInitiate(e, level, fragment, state, delay)).start();
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
				} while (level > this.level);
				log.println("Resuming Test(Level = "+level+", Fragment = "+fragment+") along "+from);
			}
			if (fragment != this.fragment) {
				log.println("Sending Accept along "+from);
				long delay = 0L;
				if (delayer != null)
					delay = delayer.nextInt(8);
				new Thread(new SendAccept(from, delay)).start();
			} else {
				if (from.state == EdgeState.Unknown) {
					from.state = EdgeState.NotInMST;
					unknownEdges.remove(from);
					notifyAll();
				}
				if (testEdge != from) {
					log.println("Sending Reject along "+from);
					long delay = 0L;
					if (delayer != null)
						delay = delayer.nextInt(8);
					new Thread(new SendReject(from, delay)).start();
				} else
					test();
			}
		}
	}

	private void test() throws RemoteException {
		if (! unknownEdges.isEmpty()) {
			testEdge = unknownEdges.peek();
			log.println("Sending Test(Level = "+level+", Fragment = "+fragment+") along "+testEdge);
			long delay = 0L;
			if (delayer != null)
				delay = delayer.nextInt(8);
			new Thread(new SendTest(testEdge, level, fragment, delay)).start();
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
					long delay = 0L;
					if (delayer != null)
						delay = 10 * delayer.nextInt(5);
					new Thread(new SendHalt(from, delay)).start();
				}
			}
		}
	}

	private void report() throws RemoteException {
		if (findCount == 0 && testEdge == null) {
			state = State.Found;
			log.println("Sending Report(Weight = "+bestMOE+") along "+toCore);
			long delay = 0L;
			if (delayer != null)
				delay = 10 * delayer.nextInt(5);
			new Thread(new SendReport(toCore, bestMOE, delay)).start();
			notifyAll();
		}
	}

	@Override
	public void changeRoot() throws RemoteException {
		synchronized (this) {
			log.println("Received ChangeRoot");
			changeRootInternal();
		}
	}

	private void changeRootInternal() throws RemoteException {
		if (toBestMOE.state == EdgeState.InMST) {
			log.println("Sending ChangeRoot along "+toBestMOE);
			long delay = 0L;
			if (delayer != null)
				delay = delayer.nextInt(8);
			new Thread(new SendChangeRoot(toBestMOE, delay)).start();
		} else {
			log.println("Sending Connect(Level = "+level+") along "+toBestMOE);
			long delay = 0L;
			if (delayer != null)
				delay = 10 * delayer.nextInt(5);
			new Thread(new SendConnect(toBestMOE, level, delay)).start();
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
				long delay = 0L;
				if (delayer != null)
					delay = 10 * delayer.nextInt(5);
				new Thread(new SendInitiate(from, this.level, fragment, state, delay)).start();
				if (state == State.Find)
					findCount++;
			} else {
				log.println("Merging fragments with level "+level+" to form level "+(this.level + 1));
				merged = true;
				log.println("Sending Initiate(Level = "+(this.level + 1)+", Fragment = "+from.weight+", State = Find) along "+from);
				long delay = 0L;
				if (delayer != null)
					delay = 10 * delayer.nextInt(5);
				new Thread(new SendInitiate(from, this.level + 1, from.weight, State.Find, delay)).start();
				notifyAll();
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
					long delay = 0L;
					if (delayer != null)
						delay = 10 * delayer.nextInt(5);
					new Thread(new SendHalt(e, delay)).start();
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
