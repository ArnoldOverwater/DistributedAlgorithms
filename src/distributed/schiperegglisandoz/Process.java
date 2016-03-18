package distributed.schiperegglisandoz;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
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

	private class SendJob implements Runnable {
		// Represents the thread that sends messages to other processes
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
				//Add a random delay before sending the message to model network delay
				Thread.sleep(delay);
			} catch (InterruptedException e) {
			} finally {
				try {
					// Sending a message means making the other process receive it
					processes[recipient].receive(message);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}

	}

	private class ReceiveJob implements Runnable {
		// Represents the thread that starts running when a message is received.
		private Message message;

		public ReceiveJob(Message m) {
			this.message = m;
		}

		@Override
		public void run() {
			// Upon receiving a message, check if it (or any other messages) can be delivered.
			// Note that only receiving a message can trigger the delivery of a message in the queue.
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
	}

	// Send a message without delay
	public void send(String text, int recipient) throws RemoteException {
		Message m;
		synchronized (this) {
			// Update clock before sending
			clock[myId]++;
			// A message consists of it's content, the ID of the sender, the buffer of the sender, and the vectorclock timestamp of the sender.
			m = new Message(text, myId, deepClone(buffer), clock.clone());
			buffer[recipient] = clock.clone();
			sent.add(m);
			System.out.println("Sent "+m);
		}
		processes[recipient].receive(m);
	}
	
	// Send a message with delay through a SendJob thread, so that this action is not blocking
	public void send(String text, int recipient, long delay) throws RemoteException {
		Message m;
		synchronized (this) {
			// Update clock before sending
			clock[myId]++;
			// A message consists of it's content, the ID of the sender, the buffer of the sender, and the vectorclock timestamp of the sender.
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
		// Deliver the message, update our clock and update the buffer with the buffer from the message
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
			// Delivering the message, in our case print it's contents
			System.out.println("Delivered "+m+" at "+Arrays.toString(clock));
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
		// Check for all messages in the queue if they can be delivered yet
		while (iterator.hasNext()) {
			Message m = iterator.next();
			int[] mBuffer = m.getBuffer(myId);
			// Deliver the message if we don't have any information yet, or if it's due
			if (mBuffer == null || vLtEq(mBuffer, clock)) {
				deliver(m);
				//remove message from messageBuffer
				iterator.remove();
				//reset the iterator so we can check all messages again
				iterator = messageBuffer.iterator();
			}
		}
	}
	
	// Component-wise maximum
	private int[] vMax(int[] vector1, int[] vector2) {
		int[] res = new int[vector1.length];
		for (int i = 0; i < vector1.length; i++) {
			res[i] = Math.max(vector1[i],  vector2[i]);
		}
		return res;
	}

	// Component-wise less-than-or-equal for vector clocks
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
