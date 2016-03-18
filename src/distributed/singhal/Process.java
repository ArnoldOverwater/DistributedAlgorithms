package distributed.singhal;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;

/**
 * Implementation of a distributed system node using the Singhal mutual exclusion algorithm.
 */
public class Process extends UnicastRemoteObject implements SinInterface {
	private static final long serialVersionUID = 664743885333959074L;

	private int myId;
	private int[] requestIds;
	private State[] states;
	private Token token;
	private SinInterface[] processes;

	private long csTime;

	/**
	 * Handles a REQUEST message.
	 */
	public class RequestJob implements Runnable {

		private int process;
		private int requestId;

		public RequestJob(int process, int requestId) {
			this.process = process;
			this.requestId = requestId;
		}

		@Override
		public void run() {
			// Ensure states and requestIds are not changed by other threads
			// Lock the Process `this`, not the RequestJob `this`
			synchronized (this) {
				System.out.println("Received request with id "+requestId+" from "+process);
				switch (states[myId]) {
				case Executing:
				case Other:
					states[process] = State.Requesting;
					break;
				case Requesting:
					if (states[process] != State.Requesting) {
						states[process] = State.Requesting;
						System.out.println("Sending request with id "+requestIds[myId]+" to "+process+" (RequestJob)");
						try {
							processes[process].requestToken(myId, requestIds[myId]);
						} catch (RemoteException e) {
							e.printStackTrace();
						}
					}
					break;
				case Holding:
					states[process] = State.Requesting;
					states[myId] = State.Other;
					//requestId[process] = requestId; // Not documented in lecture notes, not necessary
					token.states[process] = State.Requesting;
					token.states[myId] = State.Other; // Not documented in lecture notes, but necessary
					token.requestIds[process] = requestId;
					Token token = Process.this.token;
					System.out.println("Sending "+token+" to "+process+" (RequestJob)");
					Process.this.token = null;
					try {
						processes[process].receiveToken(token);
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}
			}
		}

	}

	/**
	 * Handles receiving the token and CS execution.
	 */
	private class CriticalJob implements Runnable {

		@Override
		public void run() {
			// Ensure states and requestIds are not changed by other threads
			// Lock the Process `this`, not the CriticalJob `this`
			synchronized (Process.this) {
				System.out.println(token+" received");
				states[myId] = State.Executing;
			}
			doCS(); // Release lock during (possibly long) run of CS
			synchronized (Process.this) {
				// Reset simulated CS time
				csTime = 0L;

				states[myId] = State.Other;
				token.states[myId] = State.Other;

				// Update token and local knowledge based on request ids
				for (int i = 0; i < processes.length; i++)
					if (requestIds[i] > token.requestIds[i]) {
						token.requestIds[i] = requestIds[i];
						token.states[i] = states[i];
					} else {
						requestIds[i] = token.requestIds[i];
						states[i] = token.states[i];
					}

				// Heuristic to send to process with least requests
				// Inspired by Suzuki's and Kasami's algorithm
				int minRequestId = Integer.MAX_VALUE;
				int processWithMinRequestId = -1;
				for (int i = myId-1; i >= 0; i--)
					if (states[i] == State.Requesting && requestIds[i] <= minRequestId) {
						minRequestId = requestIds[i];
						processWithMinRequestId = i;
					}
				for (int i = processes.length-1; i >= myId; i--)
					if (states[i] == State.Requesting && requestIds[i] <= minRequestId) {
						minRequestId = requestIds[i];
						processWithMinRequestId = i;
					}

				if (processWithMinRequestId < 0) // There are no requests
					states[myId] = State.Holding;
				else {
					Token token = Process.this.token;
					System.out.println("Sending "+token+" to "+processWithMinRequestId+" (CriticalJob)");
					Process.this.token = null;
					try {
						processes[processWithMinRequestId].receiveToken(token);
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}
			}
		}

	}

	/**
	 * Initialise all request ids to zero.
	 * @param id node id in [0,n)
	 * @param n Number of nodes
	 */
	public Process(int id, int n) throws RemoteException {
		super(0);
		this.myId = id;
		this.requestIds = new int[n];
		this.states = new State[n];

		// Assume this process and all processes with higher ids have state Other
		for (int i = id; i < n; i++)
			this.states[i] = State.Other;

		// Process 0 is the special process that begins with the token (overrides previous assignment to Other)
		if (id == 0) {
			this.token = new Token(n);
			this.states[0] = State.Holding;
		} else
			// Assume all processed with lower ids have state Requesting
			for (int i = 0; i < id; i++)
				this.states[i] = State.Requesting;

		this.processes = new SinInterface[n];
		System.out.println("Created process "+id+" with states "+Arrays.toString(states));
	}

	/**
	 * Called when the process needs to enter the CS.
	 * @param time Simulated time needed in CS
	 */
	public void tryAccessCS(long time) throws RemoteException {
		// Ensure states and requestIds are not changed by other threads
		synchronized (this) {
			// If previous CS requests are pending, assume max time simulation
			if (csTime < time)
				csTime = time;

			System.out.println("Require access to CS");
			if (states[myId] == State.Holding) {
				System.out.println("Sending requesting to self");
				processes[myId].requestToken(myId, requestIds[myId]);
				return;
			}
			states[myId] = State.Requesting;
			requestIds[myId]++;
			for (int i = 0; i < processes.length; i++)
				if (i != myId && states[i] == State.Requesting) {
					System.out.println("Sending request with id "+requestIds[myId]+" to "+i+" (tryAccessCS)");
					processes[i].requestToken(myId, requestIds[myId]);
				}
		}
	}

	@Override
	/**
	 * Method for processing an incoming request for CS access.
	 * Runs the request algorithm in a RequestJob in a separate thread to prevent the caller having to wait and deadlock.
	 * @param process The id of the process that wants access
	 * @param requestId Each request should be unique and incrementing per process
	 */
	public void requestToken(int process, int requestId) {
		new Thread(new RequestJob(process, requestId)).start();
	}

	@Override
	/**
	 * Method for receiving the token.
	 * It gives the callee permission to enter the CS.
	 * Runs the receive procedure in a CriticalJob in a separate thread to prevent the caller having to wait and deadlock.
	 */
	public void receiveToken(Token token) {
		this.token = token;
		new Thread(new CriticalJob()).start();
	}

	/**
	 * The actual execution of the CS, simulated by Thread.sleep .
	 */
	private void doCS() {
		System.out.println("Entering Critical Section");
		try {
			Thread.sleep(csTime);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			System.out.println("Exiting Critical Section");
		}
	}

	public SinInterface getProcess(int i) {
		return processes[i];
	}

	public void setProcess(int i, SinInterface process) {
		this.processes[i] = process;
	}

}
