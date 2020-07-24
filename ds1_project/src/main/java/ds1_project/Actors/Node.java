package ds1_project.Actors;

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.AbstractActor.Receive;
import akka.actor.AbstractActor;
import ds1_project.TwoPhaseBroadcast.*;
import scala.concurrent.duration.Duration;
import ds1_project.Responses.*;
import ds1_project.Requests.*;
import java.io.Serializable;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.HashMap;

/*-- Common functionality for both Coordinator and Participants ------------*/

public abstract class Node extends AbstractActor {
	protected int id; // node ID
	protected List<ActorRef> participants; // list of participant nodes
	// protected Decision decision = null; // decision taken by this node
	protected boolean isUpdated = false;
	private boolean isCoordinator = false;
	private int value;
	private ActorRef sender;
	protected Cancellable currentTimeout;

	public enum toMessages {
		UPDATE, WRITEOK, HEARTBEAT
	};

	public Node(final int id) {
		super();
		this.id = id;
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
		if (bool) {
			this.isCoordinator = true;
		} else {
			this.isCoordinator = false;
		}
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
		participants = new ArrayList<>();
		for (final ActorRef b : sm.group) {
			if (!b.equals(getSelf())) {

				// copying all participant refs except for self
				this.participants.add(b);
			}
		}
		print("starting with " + sm.group.size() + " peer(s)");
	}

	public void multicast(final Serializable m) {
		for (final ActorRef p : participants) {
			p.tell(m, getSelf());
		}
	}

	// a multicast implementation that crashes after sending the update to the
	// cohorts
	/*
	 * void multicastAndCrash(Serializable m, int recoverIn) { for (ActorRef p:
	 * participants) { p.tell(m, getSelf()); crash(recoverIn); return; } }
	 */

	// Function to emulate a crash and a recovery in a given time

	/*
	 * void crash( int recoverIn) { getContext().become(crashed());
	 * print(" Crash!!"); // setting a timer to "recover"
	 * getContext().system().scheduler().scheduleOnce( Duration.create(recoverIn,
	 * TimeUnit.MILLISECONDS), getSelf(), new Recovery(), // message sent to myself
	 * getContext().system().dispatcher(), getSelf() ); }
	 */

	// emulate a delay of d milliseconds
	void delay(int d) {
		try {
			Thread.sleep(d);
		} catch (Exception ignored) {
		}
	}
	void delay() {
		Random rnd =new Random();
		int d = rnd.nextInt(90)+10;
		delay(d);
	}

	// schedule a Timeout message in specified time
	void setTimeout(int time, toMessages toMess) {
	
		this.currentTimeout = getContext().system().scheduler().scheduleOnce(Duration.create(time, TimeUnit.MILLISECONDS), getSelf(),
				new Timeout(toMess), // the message to send
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

	public Receive crashed() {
		return receiveBuilder()
				// .match(Recovery.class, this::onRecovery)
				.matchAny(msg -> {
				}).build();
	}

	public void OnReadRequest(final ReadRequest msg) {
		getSender().tell(new ReadResponse(value), self());
	}

}