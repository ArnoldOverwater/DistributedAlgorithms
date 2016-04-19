package distributed.mst;

public class Edge {

	final int destinationId;
	final long weight;
	EdgeState state;
	MSTInterface process;

	public Edge(int destinationId, long weight) {
		this.destinationId = destinationId;
		this.weight = weight;
		this.state = EdgeState.Unknown;
	}

}
