package ds1_project;

import akka.actor.ActorRef;
import akka.actor.AbstractActor;
import akka.actor.ActorSystem;
import akka.actor.Props;
import scala.concurrent.duration.Duration;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.lang.Thread;
import java.util.Collections;

import java.io.IOException;

public class TwoPhaseBroadcast {
	final static int N_PARTICIPANTS = 5;
	final static int REQUEST_TIMEOUT = 1000; // timeout for the votes, ms
	//final static int DECISION_TIMEOUT = 2000; // timeout for the decision, ms
	final static int QUORUM_SIZE = (N_PARTICIPANTS + 1) / 2; // the votes that the participants will send (for testing)

	// Start message that sends the list of participants to everyone
	public static class StartMessage implements Serializable {
		public final List<ActorRef> group;

		public StartMessage(List<ActorRef> group) {
			this.group = Collections.unmodifiableList(new ArrayList<>(group));
		}
	}

	public static class UpdateRequest implements Serializable {
	}

	public static class UpdateResponse implements Serializable {
		boolean WRITEOK = false;

		public UpdateResponse(boolean WRITEOK) {
			this.WRITEOK = WRITEOK;
		}
	}

	public static class ReadRequest implements Serializable {
	}

	public static class ReadResponse implements Serializable {
		String value;

		public ReadResponse(String v) {
			this.value = v;
		}

	}

	/*-- Common functionality for both Coordinator and Participants ------------*/

	public abstract static class Node extends AbstractActor {
		protected int id; // node ID
		protected List<ActorRef> participants; // list of participant nodes
		// protected Decision decision = null; // decision taken by this node
		protected boolean isUpdated = false;
		private boolean iAmCoordinator = false;
		private String value;
		ActorRef sender;

		public Node(int id) {
			super();
			this.id = id;
		}

		void setGroup(StartMessage sm) {
			participants = new ArrayList<>();
			for (ActorRef b : sm.group) {
				if (!b.equals(getSelf())) {

					// copying all participant refs except for self
					this.participants.add(b);
				}
			}
			print("starting with " + sm.group.size() + " peer(s)");
		}

		void multicast(Serializable m) {
			for (ActorRef p : participants)
				p.tell(m, getSelf());
		}

//		void sendWriteOk(boolean isUpdated) {
//			if (isUpdated) {
//				return;
//			}
//		}

		// a simple logging function
		void print(String s) {
			System.out.format("%2d: %s\n", id, s);
		}

		@Override
		public Receive createReceive() {

			// Empty mapping: we'll define it in the inherited classes
			return receiveBuilder().build();
		}

		public void OnReadRequest(ReadRequest msg) {
			getSender().tell(new ReadResponse(value), self());
		}
	}

	/*-- Coordinator -----------------------------------------------------------*/

	public static class Coordinator extends Node {

		// here all the nodes that sent YES are collected
		private final Set<ActorRef> majorityVoters = new HashSet<>();

		boolean Quorum() { // returns true if all voted YES
			return majorityVoters.size() >= QUORUM_SIZE;
		}

		public Coordinator() {
			super(-1); // the coordinator has the id -1
		}

		static public Props props() {
			return Props.create(Coordinator.class, Coordinator::new);
		}

		@Override
		public Receive createReceive() {
			return receiveBuilder().match(StartMessage.class, this::onStartMessage)
					.match(UpdateRequest.class, this::onUpdateRequest)
					// .match(Timeout.class, this::onTimeout)
					// .match(DecisionRequest.class, this::onDecisionRequest)
					.build();
		}

		public void onStartMessage(StartMessage msg) { /* Start */
			setGroup(msg);
			print("Sending vote request");
			multicast(new UpdateRequest());
			// multicastAndCrash(new VoteRequest(), 3000);
			// setTimeout(VOTE_TIMEOUT);
			// crash(5000);
		}

		public void onUpdateRequest(UpdateRequest msg) {
			UpdateResponse res;
			if (Quorum()) {
				res = new UpdateResponse(true);
				multicast(msg);
			} else
				res = new UpdateResponse(false);
		}

		/*-- Participant -----------------------------------------------------------*/
		public static class Participant extends Node {
			ActorRef coordinator;

			public Participant(int id) {
				super(id);
			}

			static public Props props(int id) {
				return Props.create(Participant.class, () -> new Participant(id));
			}

			@Override
			public Receive createReceive() {
				return receiveBuilder().match(StartMessage.class, this::onStartMessage)
						.match(UpdateResponse.class, this::onUpdateResponse)
						.match(UpdateRequest.class, this::onUpdateRequest)
						// .match(Timeout.class, this::onTimeout)
						// .match(Recovery.class, this::onRecovery)
						.build();
			}

			public void onStartMessage(StartMessage msg) {
				setGroup(msg);
			}

			public void onUpdateRequest(UpdateRequest msg) {
				sender = getSender();
				coordinator.tell(msg, self());

			}

			public void onUpdateResponse(UpdateResponse msg) {
				if ((msg).WRITEOK) {
					sender.tell(msg, getSelf());
				}
			}
		}

		/*-- Main ------------------------------------------------------------------*/
		public static void main(String[] args) {

			// Create the actor system
			final ActorSystem system = ActorSystem.create("helloakka");

			// Create the coordinator
			ActorRef coordinator = system.actorOf(Coordinator.props(), "coordinator");

			// Create participants
			List<ActorRef> group = new ArrayList<>();
			for (int i = 0; i < N_PARTICIPANTS; i++) {
				group.add(system.actorOf(Participant.props(i), "participant" + i));
			}

			// Send start messages to the participants to inform them of the group
			StartMessage start = new StartMessage(group);
			for (ActorRef peer : group) {
				peer.tell(start, null);
			}

			// Send the start messages to the coordinator
			coordinator.tell(start, null);

			try {
				System.out.println(">>> Press ENTER to exit <<<");
				System.in.read();
			} catch (IOException ignored) {
			}
			system.terminate();
		}
		/******** External client */

	}
}
