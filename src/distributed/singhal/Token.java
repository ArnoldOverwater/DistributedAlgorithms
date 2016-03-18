package distributed.singhal;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Token that can be sent and received during a Singhal mutual exclusion algorithm.
 * There should be only one token per distributed system.
 */
public class Token implements Serializable {
	private static final long serialVersionUID = 5214394158064868636L;

	int[] requestIds;
	State[] states;

	/**
	 * Initialise all request ids to zero.
	 * Assume 0th node begins with the token.
	 * @param n Number of nodes
	 */
	public Token(int n) {
		this.requestIds = new int[n];
		this.states = new State[n];
		this.states[0] = State.Holding;
		for (int i = 1; i < n; i++)
			this.states[i] = State.Other;
	}

	
	@Override
	/**
	 * Useful for logging and debugging.
	 */
	public String toString() {
		return "Token(requestIds = "+Arrays.toString(requestIds)+", states = "+Arrays.toString(states)+")";
	}

}
