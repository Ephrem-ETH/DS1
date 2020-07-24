package ds1_project.Actors;

import ds1_project.Key;
import ds1_project.TwoPhaseBroadcast.*;
import ds1_project.Requests.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.Props;
import ds1_project.Responses.*;

public class Participant extends Node {
	private ActorRef coordinator;
	//private HashMap<Key, Integer> waitingList = new HashMap<Key, Integer>();
	// forwarding to coordinator
	final static int WRITEOK_TIMEOUT = 3000;
	final static int UPDATE_TIMEOUT = 3000;
	private LinkedHashMap<Key, Integer> waitingList = new LinkedHashMap<Key, Integer>();
	// private final Random rnd;

	public Participant(final int id, ActorRef coord) {
		super(id);
		this.coordinator = coord;
	}

	static public Props props(final int id, ActorRef coord) {
		return Props.create(Participant.class, () -> new Participant(id, coord));
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder().match(StartMessage.class, this::onStartMessage)
				.match(WriteOk.class, this::onWriteOK)
				.match(Update.class, this::onUpdate)
				.match(UpdateRequest.class, this::onUpdateRequest)
				.match(ReadRequest.class, this::OnReadRequest)
				.match(Timeout.class, this::onTimeout)
				// .match(Recovery.class, this::onRecovery)
				.build();
	}

	public void setCoordinator(ActorRef coord) {
		this.coordinator = coord;
	}

	public ActorRef getCoordinator() {
		return this.coordinator;
	}

	public void onStartMessage(final StartMessage msg) {
		setGroup(msg);
	}

	public void onUpdateRequest(final UpdateRequest msg) {
		//setSender(getSender());
		coordinator.tell(msg, self());
	}

	public void onWriteOK(final WriteOk msg) {
		//delay();
		if (this.currentTimeout != null) {
			this.currentTimeout.cancel();
		}
		print("Received WriteOk for e=" + msg.getRequest_epoch() + " and s=" + msg.getRequest_seqnum());

		Key removeKey = new Key(msg.getRequest_epoch(), msg.getRequest_seqnum());

		for (Map.Entry<Key, Integer> entry : waitingList.entrySet()) {
			if (entry.getKey().equals(removeKey)) {
				this.setValue(entry.getValue());
				print("Updated value :" + this.getValue());
				removeKey = entry.getKey();
			}
		}
		waitingList.remove(removeKey);
		print("Update removed from queue");
	}

	public void onUpdate(Update msg) { // Update propagates from coordinator
		//delay();
		if (this.currentTimeout != null) {
			this.currentTimeout.cancel();
		}
		Key request_id = new Key(msg.getEpochs(), msg.getSequenceNum());
				
		waitingList.put(request_id, msg.getValue());
		print("queued value" + waitingList.get(request_id));
		Acknowledgement acknowledgement = new Acknowledgement(Acknowledge.ACK, msg.getEpochs(), msg.getSequenceNum());
		coordinator.tell(acknowledgement, getSelf());
		this.print("ACK sent");
		//setTimeout(WRITEOK_TIMEOUT, toMessages.WRITEOK);
	}

	public void onTimeout(Timeout msg) {

		if (msg.toMess == toMessages.UPDATE) {
			print(" update Timeout:" + "Coordinator Crash");

			// send election message
			print(" Send election message");

		}

		if (msg.toMess == toMessages.WRITEOK) {

			print("WriteOk Timeout:" + "Coordinator Crash");
			print(" Send election message");

			// send election message

		}

	}

}