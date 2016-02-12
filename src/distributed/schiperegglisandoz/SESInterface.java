package distributed.schiperegglisandoz;

import java.rmi.Remote;

public interface SESInterface extends Remote {

	public void receive(Message m);

}
