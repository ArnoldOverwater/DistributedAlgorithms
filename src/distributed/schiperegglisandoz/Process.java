package distributed.schiperegglisandoz;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class Process extends UnicastRemoteObject implements SESInterface {
	private static final long serialVersionUID = -6056565009547143029L;

	private int myId;
	private int[] clock;
	private int[][] buffer;
	private SESInterface[] processes;
	private Deque<Message> messageBuffer;

	protected List<Message> sent;
	protected List<Message> delivered;

	private class SendJob implements Runnable {

		private Message message;
		private int recipient;
		private long delay;

		public SendJob(Message m, int recipient, long delay) {
			this.message = m;
			this.recipient = recipient;
			this.delay = delay;
		}

		@Override
		public void run() {
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
			} finally {
				try {
					processes[recipient].receive(message);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}

	}

	private class ReceiveJob implements Runnable {

		private Message message;

		public ReceiveJob(Message m) {
			this.message = m;
		}

		@Override
		public void run() {
			synchronized (messageBuffer) {
				messageBuffer.addFirst(message);
				checkDeliveries();
			}
		}

	}

	public Process(int id, int n) throws RemoteException {
		super(0);
		this.myId = id;
		this.clock = new int[n];
		this.buffer = new int[n][];
		this.processes = new SESInterface[n];
		this.messageBuffer = new LinkedList<Message>();
		this.sent = new ArrayList<Message>();
		this.delivered = new ArrayList<Message>();
	}

	public void send(String text, int recipient) throws RemoteException {
		Message m;
		synchronized (this) {
			clock[myId]++;
			m = new Message(text, myId, deepClone(buffer), clock.clone());
			buffer[recipient] = clock.clone();
			sent.add(m);
			System.out.println("Sent "+m);
		}
		processes[recipient].receive(m);
	}

	public void send(String text, int recipient, long delay) throws RemoteException {
		Message m;
		synchronized (this) {
			clock[myId]++;
			m = new Message(text, myId, deepClone(buffer), clock.clone());
			buffer[recipient] = clock.clone();
			sent.add(m);
			System.out.println("Sent "+m);
		}
		new Thread(new SendJob(m, recipient, delay)).start();
	}

	@Override
	public void receive(Message m) {
		new Thread(new ReceiveJob(m)).start();
	}

	protected void deliver(Message m) {
		synchronized (this) {
			clock[myId]++;
			clock = vMax(clock, m.getTimestamp());
			for (int i = 0; i < buffer.length; i++) {
				int[] iBuffer = m.getBuffer(i);
				if (buffer[i] == null)
					buffer[i] = iBuffer;
				else if (iBuffer != null)
					buffer[i] = vMax(buffer[i], iBuffer);
			}
			System.out.println("Delivered "+m);
		}
	}

	public int getMyId() {
		return myId;
	}

	public SESInterface getProcess(int i) {
		return processes[i];
	}

	public void setProcess(int i, SESInterface process) {
		processes[i] = process;
	}

	private void checkDeliveries() {
		Iterator<Message> iterator = messageBuffer.iterator();
		while (iterator.hasNext()) {
			Message m = iterator.next();
			int[] mBuffer = m.getBuffer(myId);
			if (mBuffer == null || vLtEq(mBuffer, clock)) {
				deliver(m);
				//remove message from messageBuffer
				iterator.remove();
				//reset the iterator so we can check all messages again
				iterator = messageBuffer.iterator();
			}
		}
	}

	private int[] vMax(int[] vector1, int[] vector2) {
		int[] res = new int[vector1.length];
		for (int i = 0; i < vector1.length; i++) {
			res[i] = Math.max(vector1[i],  vector2[i]);
		}
		return res;
	}

	private boolean vLtEq(int[] vector1, int[] vector2) {
		for (int i = 0; i < vector1.length; i++)
			if (vector1[i] > vector2[i])
				return false;
		return true;
	}

	private int[][] deepClone(int[][] original) {
		int[][] result = new int[original.length][];
		for (int i = 0; i < original.length; i++) {
			if (original[i] != null)
				result[i] = original[i].clone();
		}
		return result;
	}
}
