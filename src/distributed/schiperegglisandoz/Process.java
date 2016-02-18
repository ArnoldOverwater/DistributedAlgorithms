package distributed.schiperegglisandoz;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Process extends UnicastRemoteObject implements SESInterface {
	private static final long serialVersionUID = -6056565009547143029L;

//	private int numberNodes;
	private int myId;
	private int[] clock;
	private int[][] buffer;
	private SESInterface[] processes;
	private List<Message> messageBuffer;
	private List<String> messages;

	public Process(int id, int n) throws RemoteException {
		super(0);
//		this.numberNodes = n;
		this.myId = id;
		this.clock = new int[n];
		this.buffer = new int[n][];
//		this.buffer[id] = new int[n];
		this.processes = new SESInterface[n];
		this.messageBuffer = new ArrayList<Message>();
		this.messages = new ArrayList<String>();
	}

	public synchronized void send(String text, int recipient) throws RemoteException {
		clock[myId]++;
		Message m = new Message(text, myId, buffer, clock);
		buffer[recipient] = clock.clone();
		processes[recipient].receive(m);
	}

	@Override
	public synchronized void receive(Message m) {
		messageBuffer.add(m);
		checkDeliveries();
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
				//deliver message
				messages.add(m.getMessage());
				clock[myId]++;
				//remove message from messageBuffer
				iterator.remove();
				//Update vector clock
				for (int i = 0; i < buffer.length; i++) {
					int[] iBuffer = m.getBuffer(i);
					if (buffer[i] == null)
						buffer[i] = iBuffer;
					else if (iBuffer != null)
						buffer[i] = vMax(buffer[i], iBuffer);
				}
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

}
