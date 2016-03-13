package distributed.singhal;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class Process extends UnicastRemoteObject implements SinInterface {
	private static final long serialVersionUID = 664743885333959074L;

	private int myId;
	private int[] requestIds;
	private State[] states;
	private Token token;
	private SinInterface[] processes;

	public class RequestJob implements Runnable {

		private int process;
		private int requestId;

		public RequestJob(int process, int requestId) {
			this.process = process;
			this.requestId = requestId;
		}

		@Override
		public void run() {
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
					token.states[process] = State.Requesting;
					token.requestIds[process] = requestId;
					System.out.println("Sending "+token+" to "+process+" (RequestJob)");
					try {
						processes[process].receiveToken(token);
						token = null;
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}
			}
		}

	}

	private class CriticalJob implements Runnable {

		@Override
		public void run() {
			synchronized (Process.this) {
				System.out.println(token+" received");
				states[myId] = State.Executing;
			}
			doCS();
			synchronized (Process.this) {
				states[myId] = State.Other;
				token.states[myId] = State.Other;
				for (int i = 0; i < processes.length; i++)
					if (requestIds[i] > token.requestIds[i]) {
						token.requestIds[i] = requestIds[i];
						token.states[i] = states[i];
					} else {
						requestIds[i] = token.requestIds[i];
						states[i] = token.states[i];
					}
				boolean noRequests = true;
				for (int i = 0; i < processes.length; i++)
					if (states[i] != State.Other) {
						noRequests = false;
						break;
					}
				if (noRequests)
					states[myId] = State.Holding;
				else {
					// Heuristic to send to process with lowest requests
					int minRequestId = Integer.MAX_VALUE;
					int processWithMinRequestId = -1;
					for (int i = myId-1; i >= 0; i--)
						if (requestIds[i] <= minRequestId) {
							minRequestId = requestIds[i];
							processWithMinRequestId = i;
						}
					for (int i = processes.length-1; i > myId; i--)
						if (requestIds[i] <= minRequestId) {
							minRequestId = requestIds[i];
							processWithMinRequestId = i;
						}
					System.out.println("Sending "+token+" to "+processWithMinRequestId+" (CriticalJob)");
					try {
						processes[processWithMinRequestId].receiveToken(token);
						token = null;
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}
			}
		}

	}

	public Process(int id, int n) throws RemoteException {
		super(0);
		this.myId = id;
		this.requestIds = new int[n];
		this.states = new State[n];
		for (int i = id; i < n; i++)
			this.states[i] = State.Other;
		if (id == 0) {
			this.token = new Token(n);
			this.states[0] = State.Holding;
		} else
			for (int i = 0; i < id; i++)
				this.states[i] = State.Requesting;
		this.processes = new SinInterface[n];
	}

	public void tryAccessCS() throws RemoteException {
		synchronized (this) {
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
	public void requestToken(int process, int requestId) {
		new Thread(new RequestJob(process, requestId)).start();
	}

	@Override
	public void receiveToken(Token token) {
		this.token = token;
		new Thread(new CriticalJob()).start();
	}

	private void doCS() {
		System.out.println("Critical Section!");
	}

	public SinInterface getProcess(int i) {
		return processes[i];
	}

	public void setProcess(int i, SinInterface process) {
		this.processes[i] = process;
	}

}
