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

	private class CriticalJob implements Runnable {

		@Override
		public void run() {
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
					int minRequestId = Integer.MAX_VALUE;
					int processWithMinRequestId = -1;
					for (int i = myId; i >= 0; i--)
						if (requestIds[i] <= minRequestId) {
							minRequestId = requestIds[i];
							processWithMinRequestId = i;
						}
					for (int i = processes.length-1; i > myId; i--)
						if (requestIds[i] <= minRequestId) {
							minRequestId = requestIds[i];
							processWithMinRequestId = i;
						}
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
				processes[myId].requestToken(myId, requestIds[myId]);
				return;
			}
			states[myId] = State.Requesting;
			requestIds[myId]++;
			for (int i = 0; i < processes.length; i++)
				if (i != myId && states[i] == State.Requesting)
					processes[i].requestToken(myId, requestIds[myId]);
		}
	}

	@Override
	public void requestToken(int process, int requestId) throws RemoteException {
		synchronized (this) {
			switch (states[myId]) {
			case Executing:
			case Other:
				states[process] = State.Requesting;
				break;
			case Requesting:
				if (states[process] != State.Requesting) {
					states[process] = State.Requesting;
					processes[process].requestToken(myId, requestIds[myId]);
				}
				break;
			case Holding:
				states[process] = State.Requesting;
				states[myId] = State.Other;
				token.states[process] = State.Requesting;
				token.requestIds[process] = requestId;
				processes[process].receiveToken(token);
				token = null;
			}
		}
	}

	@Override
	public void receiveToken(Token token) {
		synchronized (this) {
			this.token = token;
			states[myId] = State.Executing;
		}
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
