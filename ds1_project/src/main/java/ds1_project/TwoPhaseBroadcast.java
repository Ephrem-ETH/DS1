package ds1_project;

import ds1_project.Requests.*;
import ds1_project.TwoPhaseBroadcast.Coordinator.Participant;
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
	final static int N_PARTICIPANTS = 5;
	final static int REQUEST_TIMEOUT = 1000; // timeout for the votes, ms
	// final static int DECISION_TIMEOUT = 2000; // timeout for the decision, ms
	final static int QUORUM_SIZE = (N_PARTICIPANTS + 1) / 2; // the votes that the participants will send (for testing)
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

	public static class externalClient extends AbstractActor {
		
		public externalClient () {
		}

		static public Props props() {
			return Props.create(externalClient.class, () -> new externalClient());
		}

		@Override
		public Receive createReceive() {
			// TODO Auto-generated method stub
			return receiveBuilder().match(ReadResponse.class, this::onReadResponse).build();
		}

		public void onReadResponse(ReadResponse msg){
			System.out.println("Read value "+msg.value);
		}
	}

	/*-- Common functionality for both Coordinator and Participants ------------*/

	public abstract static class Node extends AbstractActor {
		protected int id; // node ID
		protected List<ActorRef> participants; // list of participant nodes
		// protected Decision decision = null; // decision taken by this node
		protected boolean isUpdated = false;
		private boolean isCoordinator = false;
		private int value ;
		ActorRef sender;

		public Node(final int id) {
			super();
			this.id = id;
		}

		int getId (){
			return this.id ;
		}

		void setGroup(final StartMessage sm) {
			participants = new ArrayList<>();
			for (final ActorRef b : sm.group) {
				if (!b.equals(getSelf())) {

					// copying all participant refs except for self
					this.participants.add(b);
				}
			}
			print("starting with " + sm.group.size() + " peer(s)");
		}

		void multicast(final Serializable m) {
			for (final ActorRef p : participants)
				p.tell(m, getSelf());
		}

		// void sendWriteOk(boolean isUpdated) {
		// if (isUpdated) {
		// return;
		// }
		// }

		// a simple logging function
		void print(final String s) {
			System.out.format("%2d: %s\n", id, s);
		}

		@Override
		public Receive createReceive() {

			// Empty mapping: we'll define it in the inherited classes
			return receiveBuilder().build();
		}

		public void OnReadRequest(final ReadRequest msg) {
			getSender().tell(new ReadResponse(value), self());
		}

	}

	/*-- Coordinator -----------------------------------------------------------*/

	public static class Coordinator extends Node {

		// here all the nodes that sent YES are collected
		private final Set<ActorRef> majorityVoters = new HashSet<>();
		public static int sequence_num;

		boolean Quorum() { // returns true if all voted YES
			return majorityVoters.size() >= QUORUM_SIZE;
		}

		public Coordinator() {
			super(-1); // the coordinator has the id -1
			sequence_num = 0;
			super.isCoordinator = true;
		}

		static public Props props() {
			return Props.create(Coordinator.class, Coordinator::new);
		}

		@Override
		public Receive createReceive() {
			return receiveBuilder().match(StartMessage.class, this::onStartMessage)
					.match(UpdateRequest.class, this::onUpdateRequest)
					.match(ReadRequest.class, this::OnReadRequest)
					// .match(Timeout.class, this::onTimeout)
					// .match(DecisionRequest.class, this::onDecisionRequest)
					.build();
		}

		public void onStartMessage(final StartMessage msg) { /* Start */
			setGroup(msg);
			print("Sending vote request");
			//multicast(new UpdateRequest()); // to correct
			// multicastAndCrash(new VoteRequest(), 3000);
			// setTimeout(VOTE_TIMEOUT);
			// crash(5000);
		}

		public void onUpdateRequest(final UpdateRequest msg) {
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

			public Participant(final int id) {
				super(id);
			}

			static public Props props(final int id) {
				return Props.create(Participant.class, () -> new Participant(id));
			}

			@Override
			public Receive createReceive() {
				return receiveBuilder().match(StartMessage.class, this::onStartMessage)
						.match(UpdateResponse.class, this::onUpdateResponse)
						.match(UpdateRequest.class, this::onUpdateRequest)
						.match(ReadRequest.class, this::OnReadRequest)
						// .match(Timeout.class, this::onTimeout)
						// .match(Recovery.class, this::onRecovery)
						.build();
			}

			public void onStartMessage(final StartMessage msg) {
				setGroup(msg);
			}

			public void onUpdateRequest(final UpdateRequest msg) {
				sender = getSender();
				coordinator.tell(msg, self());

			}

			public void onUpdateResponse(final UpdateResponse msg) {
				if ((msg).WRITEOK) {
					sender.tell(msg, getSelf());
				}
			}
		}
	}

	/*-- Main ------------------------------------------------------------------*/
	public static void main(final String[] args) {

		// Create the actor system
		final ActorSystem system = ActorSystem.create("helloakka");

		// Create the coordinator
		final ActorRef coordinator = system.actorOf(Coordinator.props(), "coordinator");
		System.out.println("Added cordinator node");

		//Create external client
		final ActorRef client = system.actorOf(externalClient.props(),"externalClient") ;
		System.out.println("Added external client");		

		// Create participants
		final List<ActorRef> group = new ArrayList<>();
		for (int i = 0; i < N_PARTICIPANTS; i++) {
			group.add(system.actorOf(Participant.props(i), "participant" + i));
			System.out.println("Added node "+i+" to the group");
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
