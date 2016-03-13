package distributed.singhal;

import java.io.Serializable;
import java.util.Arrays;

public class Token implements Serializable {
	private static final long serialVersionUID = 5214394158064868636L;

	int[] requestIds;
	State[] states;

	public Token(int n) {
		this.requestIds = new int[n];
		this.states = new State[n];
		this.states[0] = State.Holding;
		for (int i = 1; i < n; i++)
			this.states[i] = State.Other;
	}

	@Override
	public String toString() {
		return "Token(requestIds = "+Arrays.toString(requestIds)+", states = "+Arrays.toString(states)+")";
	}

}
