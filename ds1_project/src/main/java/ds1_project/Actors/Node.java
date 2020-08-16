package ds1_project.Actors;

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.AbstractActor;
import ds1_project.TwoPhaseBroadcast.*;
import scala.concurrent.duration.Duration;
import ds1_project.Responses.*;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.HashMap;

/*-- Common functionality for both Coordinator and Participants ------------*/

public abstract class Node extends AbstractActor {
	protected int id; // node ID

	// List of nodes with ID as key
	protected HashMap<Integer, ActorRef> network = new HashMap<Integer, ActorRef>();

	// List of updates
	protected List<ArrayList<Update>> waitingList = new ArrayList<ArrayList<Update>>();

	protected boolean isCrashed = false;
	private boolean isCoordinator = false;
	private int value;
	private ActorRef sender;
	//protected Cancellable currentTimeout;
	protected List<Integer> crashedNodes;

	public enum toMessages {
		UPDATE, WRITEOK, HEARTBEAT, ACK, ELECTION, ELECTION_TOTAL
	};

	public Node(final int id) {
		super();
		this.id = id;
		crashedNodes = new ArrayList<Integer>();
		waitingList.add(new ArrayList<Update>());
	}

	// Getters and Setters
	public int getId() {
		return this.id;
	}

	public int getValue() {
		return this.value;
	}

	public void setValue(int value) {
		this.value = value;
	}

	public void setCoordinator(boolean bool) {
		this.isCoordinator = bool;
	}

	public boolean isCoordinator() {
		return this.isCoordinator;
	}

	public ActorRef getNodeSender() {
		return this.sender;
	}

	public void setSender(ActorRef sender) {
		this.sender = sender;
	}

	public void setGroup(final StartMessage sm) {
		for (final Map.Entry<Integer, ActorRef> b : sm.group.entrySet()) {
			if (!b.equals(getSelf())) {
				// copying all participant refs except for self
				this.network.put(b.getKey(), b.getValue());
			}
		}
		print("starting with " + sm.group.size() + " peer(s)");
	}

	public void multicast(final Serializable m) {
		for (final Map.Entry<Integer, ActorRef> p : network.entrySet()) {
			if (!isCrashed && p!=self()) {
				p.getValue().tell(m, getSelf());
			}
		}
	}

	void delay(int d) { // emulate a delay of d milliseconds
		try {
			Thread.sleep(d);
		} catch (Exception ignored) {
		}
	}

	void delay() { // A random communication delay
		Random rnd = new Random();
		int d = rnd.nextInt(90) + 10;
		delay(d);
	}

	// schedule a Timeout message in specified time
	void setTimeout(int time, toMessages toMess, int node_id) {
		/* this.currentTimeout = */getContext().system().scheduler().scheduleOnce(
				Duration.create(time, TimeUnit.MILLISECONDS), getSelf(), new Timeout(toMess,node_id), // the message to send
				getContext().system().dispatcher(), getSelf());
	}

	// a simple logging function
	public void print(final String s) {
		System.out.format("%2d: %s\n", id, s);
	}

	@Override
	public Receive createReceive() {

		// Empty mapping: we'll define it in the inherited classes
		return receiveBuilder().build();
	}


}