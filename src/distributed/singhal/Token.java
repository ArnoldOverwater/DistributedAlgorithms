package distributed.singhal;

public class Token {

	int[] requestIds;
	State[] states;

	public Token(int n) {
		this.requestIds = new int[n];
		this.states = new State[n];
		this.states[0] = State.Holding;
		for (int i = 1; i < n; i++)
			this.states[i] = State.Other;
	}

}
