package distributed.singhal;

/**
 * The four states each process can be in during a Singhal mutual exclusion algorithm.
 */
public enum State {
	Requesting,
	Executing,
	Holding,
	Other
}
