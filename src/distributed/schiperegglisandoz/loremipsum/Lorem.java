package distributed.schiperegglisandoz.loremipsum;

import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Random;

import distributed.schiperegglisandoz.Process;
import distributed.schiperegglisandoz.SESInterface;

public class Lorem extends Process {
	private static final long serialVersionUID = 1678707249854691176L;

	public Lorem() throws RemoteException {
		super(0, 2);
	}

	public void start() {
		try {
			send("Lorem", 1, 10000L);
			Random rand = new Random();
			for (int i = 0; i < 5; i++) {
				Thread.sleep(rand.nextInt(5) * 1000L);
				send(Integer.toString(i), 1);
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws AlreadyBoundException, IOException, NotBoundException {
		if (args.length < 1) {
			System.err.println("Usage: <Ipsum-URL>");
			System.exit(-1);
		}

		Lorem process = new Lorem();
		Naming.bind("rmi://localhost:1099/lorem", process);
		System.out.println("Process Lorem bound at rmi://localhost:1099/lorem");

		System.out.print("Press key when Ipsum is bound");
		System.in.read();

		process.setProcess(1, (SESInterface) Naming.lookup(args[0]));
		System.out.println("Network registered successfully");

		System.out.println("Press key when Ipsum registered network to start");
		System.in.read();

		process.start();
	}

}
