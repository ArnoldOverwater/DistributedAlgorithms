Assignment 1: Implementing the Schiper-Eggli-Sandoz algorithm for Message Ordering.

To run the implementation of the Schiper-Eggli-Sandoz algorithm, execute assignment1.sh in a linux shell.
To assert that the order in which the messages were received corresponds to the message they were sent,
run Causal_order_check.py in the same folder as the logfiles generated by asignment1.sh. If the script executes
without raising Assertion Errors, the message ordering was causal.
For manual inspection, see the logfiles called ses_processX.log, where X denotes the process ID.

Assignment 2: Implement Singhal's algorithm for Mutual Exclusion.

To run the implementation of Singhal's algorithm, execute assignment2.sh in a lunix shell. Correct execution
of the algorithm can be trivially verified by inspecting the generated logfiles of the processes, called
singhal_processX.log, where X denotes the process ID.