package ds1_project;

import ds1_project.Requests.*;
import ds1_project.Actors.*;
import akka.actor.ActorRef;
import akka.actor.AbstractActor;
import akka.actor.ActorSystem;
import akka.actor.Props;
import scala.None;
import scala.concurrent.duration.Duration;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.lang.Thread;
import java.util.Collections;
import java.util.HashMap;
import java.io.IOException;

public class TwoPhaseBroadcast {
	// Variables
	public final static int N_PARTICIPANTS = 5;
	public final static int REQUEST_TIMEOUT = 1000; // timeout for the votes, ms
	// final static int DECISION_TIMEOUT = 2000; // timeout for the decision, ms
	public final static int QUORUM_SIZE = (N_PARTICIPANTS + 1) / 2; // the votes that the participants will send (for testing)
	public static int epoch_global = 0;

	public static HashMap<Key, Integer> history = new HashMap<Key, Integer>();

	// Start message that sends the list of participants to everyone
	public static class StartMessage implements Serializable {
		public final List<ActorRef> group;

		public StartMessage(final List<ActorRef> group) {
			this.group = Collections.unmodifiableList(new ArrayList<>(group));
		}
	}

	public static class UpdateResponse implements Serializable {
		public boolean WRITEOK = false;

		public UpdateResponse(final boolean WRITEOK) {
			this.WRITEOK = WRITEOK;
		}
	}

	public static class ReadResponse implements Serializable {
		public final int value;

		public ReadResponse(final int value2) {
			this.value = value2;
		}

	}

	public static class Key {
		public static int[] keyparams;

		// epoch and sequence numbers
		public Key(final int e, final int i) {
			keyparams[0] = e;
			keyparams[1] = i;
		}
	}

	/*-- Main ------------------------------------------------------------------*/
	public static void main(final String[] args) {

		// Create the actor system
		final ActorSystem system = ActorSystem.create("helloakka");

		// Create the coordinator
		final ActorRef coordinator = system.actorOf(Coordinator.props(), "coordinator");
		System.out.println("Added cordinator node");

		// Create external client
		final ActorRef client = system.actorOf(externalClient.props(), "externalClient");
		System.out.println("Added external client");

		// Create participants
		final List<ActorRef> group = new ArrayList<>();
		for (int i = 0; i < N_PARTICIPANTS; i++) {
			group.add(system.actorOf(Participant.props(i), "participant" + i));
			System.out.println("Added node " + i + " to the group");
		}
		System.out.println(group);
		// Send start messages to the participants to inform them of the group
		final StartMessage start = new StartMessage(group);
		for (final ActorRef peer : group) {
			System.out.println("Sending start message");
			peer.tell(start, null);
		}

		// Send the start messages to the coordinator
		coordinator.tell(start, null);

		group.get(1).tell(new ReadRequest(), client);

		try {
			System.out.println(">>> Press ENTER to exit <<<");
			System.in.read();
		} catch (final IOException ignored) {
		}
		system.terminate();

	}
	/******** External client */
}
