package distributed.schiperegglisandoz.loremipsum;

import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Random;

import distributed.schiperegglisandoz.Message;
import distributed.schiperegglisandoz.Process;
import distributed.schiperegglisandoz.SESInterface;

public class Ipsum extends Process {
	private static final long serialVersionUID = 3306239984356610042L;

	public Ipsum() throws RemoteException {
		super(1, 2);
	}

	@Override
	protected void deliver(Message m) {
		super.deliver(m);
		if (m.getMessage().equals("Lorem")) {
			try {
				send("Ipsum", 0);
				Random rand = new Random();
				for (int i = 0; i < 5; i++) {
					Thread.sleep(rand.nextInt(5) * 1000L);
					send(Integer.toString(i), 0);
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) throws AlreadyBoundException, IOException, NotBoundException {
		if (args.length < 1) {
			System.err.println("Usage: <Lorem-URL>");
			System.exit(-1);
		}

		Ipsum process = new Ipsum();
		Naming.bind("rmi://localhost:1099/ipsum", process);
		System.out.println("Process Lorem bound at rmi://localhost:1099/ipsum");

		System.out.print("Press key when Lorem is bound");
		System.in.read();

		process.setProcess(0, (SESInterface) Naming.lookup(args[0]));
		System.out.println("Network registered successfully");
	}

}
