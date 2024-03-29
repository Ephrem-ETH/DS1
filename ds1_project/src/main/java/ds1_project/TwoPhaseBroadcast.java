package ds1_project;

import ds1_project.Requests.*;
import ds1_project.Actors.*;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import java.io.Serializable;
import java.util.Map;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import java.util.Collections;
import java.util.HashMap;
import java.io.IOException;

public class TwoPhaseBroadcast {

	/**
	 * Main class, used for initializing the system and testing it.
	 */

	// Variables
	public final static int N_PARTICIPANTS = 5;
	public final static int REQUEST_TIMEOUT = 1000; // timeout for the votes, ms
	// final static int DECISION_TIMEOUT = 2000; // timeout for the decision, ms
	public final static int QUORUM_SIZE = (N_PARTICIPANTS + 1) / 2 +1; // the votes that the participants will send (for
																	// testing)
	public static int epoch_global = 0;
	public static int value;

	public enum Acknowledge {
		ACK, NAK
	} // NAK for test purposes only

	public static HashMap<Key, Integer> history = new HashMap<Key, Integer>();

	// Start message that sends the list of participants to everyone
	public static class StartMessage implements Serializable {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		public final Map<Integer, ActorRef> group;

		public StartMessage(final HashMap<Integer,ActorRef> group) {
			this.group = Collections.unmodifiableMap(new HashMap<Integer,ActorRef>(group));
		}
	}


	public static class Key {
		public static int[] keyparams;

		// epoch and sequence numbers
		public Key(final int e, final int s) {
			int[] params = { e, s };
			keyparams = params;
		}

		public int getE() {
			return keyparams[0];
		}

		public int getS() {
			return keyparams[1];
		}

		@Override
		public boolean equals(Object obj) {
			return (this.getE() == ((Key) obj).getE() && ((Key) obj).getS() == this.getS());
		}
	}


	/*-- Main ------------------------------------------------------------------*/
	public static void main(final String[] args) {

		// FIRST SECTION - INITIALIZATION - DO NOT EDIT

		Config customConf = ConfigFactory.load("application.conf");
		// Create the actor system
		final ActorSystem system = ActorSystem.create("helloakka",customConf);

		// Create the coordinator
		final ActorRef coordinator = system.actorOf(Participant.props(), "coordinator");
		System.out.println("Added cordinator node");

		// Create external client
		final ActorRef client = system.actorOf(externalClient.props(), "externalClient");
		System.out.println("Added external client");

		// Create participants
		final HashMap<Integer,ActorRef> group = new HashMap<>();
		for (int i = 1; i <= N_PARTICIPANTS; i++) {
			group.put(i,system.actorOf(Participant.props(i, coordinator), "participant" + i));
			System.out.println("Added node " + i + " to the group");
		}
		System.out.println(group);
		// Send start messages to the participants to inform them of the group
		final StartMessage start = new StartMessage(group);
		for (final Map.Entry<Integer,ActorRef> peer : group.entrySet()) {
			System.out.println("Sending start message");
			peer.getValue().tell(start, null);
		}

		// Send the start messages to the coordinator
		coordinator.tell(start, null);

		// END OF INITIALIZATION - EDIT THE SIMULATION STEPS BELOW

		//Launch simulation
		try {
			System.out.println(">>> Press ENTER to continue <<<");
			System.in.read();
		} catch (final IOException ignored) {
		}

		//First read request
		group.get(1).tell(new ReadRequest(), client);

		try {
			System.out.println(">>> Press ENTER to continue <<<");
			System.in.read();
		} catch (final IOException ignored) {
		}
		
		// 1st update
		coordinator.tell(new UpdateRequest(55), client);
		try {
			System.out.println(">>> Press ENTER to continue <<<");
			System.in.read();
		} catch (final IOException ignored) {
		}
		// Second Read request
		group.get(3).tell(new ReadRequest(), client);
		try {
			System.out.println(">>> Press ENTER to continue <<<");
			System.in.read();
		} catch (final IOException ignored) {
		}
		//Second Update
		group.get(2).tell(new UpdateRequest(15), client);
		try {
			Thread.sleep(80);
		} catch (Exception ignored) {
			//TODO: handle exception
		}
		//Coordinator crash
		coordinator.tell(new CrashRequest(),client);
		//Third update -> This one is not treated by the systems as it enters into election mode
		group.get(1).tell(new UpdateRequest(10), client);

		try {
			System.out.println(">>> Press ENTER to continue <<<");
			System.in.read();
		} catch (final IOException ignored) {
		}

		group.get(3).tell(new UpdateRequest(4), client);

		try {
			System.out.println(">>> Press ENTER to crash coordinator <<<");
			System.in.read();
		} catch (final IOException ignored) {
		}
		
		group.get(1).tell(new CrashRequest(), client);

		try {
			System.out.println(">>> Press ENTER to crash coordinator <<<");
			System.in.read();
		} catch (final IOException ignored) {
		}
		
		group.get(2).tell(new CrashRequest(), client);

		try {
			System.out.println(">>> Press ENTER to continue <<<");
			System.in.read();
		} catch (final IOException ignored) {
		}
		group.get(1).tell(new ReadRequest(), client);
		group.get(2).tell(new ReadRequest(), client);
		group.get(3).tell(new ReadRequest(), client);
		group.get(4).tell(new ReadRequest(), client);
		group.get(5).tell(new ReadRequest(), client);

		try {
			System.out.println(">>> Press ENTER to exit <<<");
			System.in.read();
		} catch (final IOException ignored) {
		}
		system.terminate();

	}
	/******** External client */
}
