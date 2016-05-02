package distributed.mst;

public class Edge implements Comparable<Edge> {

	final int destinationId;
	final long weight;
	EdgeState state;
	public MSTInterface process;

	public Edge(int destinationId, long weight) {
		this.destinationId = destinationId;
		this.weight = weight;
		this.state = EdgeState.Unknown;
	}

	@Override
	public int compareTo(Edge that) {
		if (this.weight < that.weight)
			return -1;
		else if (this.weight > that.weight)
			return 1;
		else
			return this.destinationId - that.destinationId;
	}

	@Override
	public String toString() {
		return "Edge(destination = "+destinationId+", weight = "+weight+", state = "+state+")";
	}

}
