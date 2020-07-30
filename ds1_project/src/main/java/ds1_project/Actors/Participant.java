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
	// private HashMap<Key, Integer> waitingList = new HashMap<Key, Integer>();
	// forwarding to coordinator
	final static int WRITEOK_TIMEOUT = 3000;
	final static int UPDATE_TIMEOUT = 3000;
	private List<ArrayList<Update>> waitingList = new ArrayList<ArrayList<Update>>();
	private int lastSequenceNumber = 0;
	// private final Random rnd;

	public Participant(final int id, ActorRef coord) {
		super(id);
		this.coordinator = coord;
		this.waitingList.add(new ArrayList<Update>());
	}

	static public Props props(final int id, ActorRef coord) {
		return Props.create(Participant.class, () -> new Participant(id, coord));
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder().match(StartMessage.class, this::onStartMessage).match(WriteOk.class, this::onWriteOK)
				.match(Update.class, this::onUpdate)
				.match(UpdateRequest.class, this::onUpdateRequest)
				.match(ReadRequest.class, this::OnReadRequest)
				.match(Timeout.class, this::onTimeout)
				.match(CrashedNodeWarning.class, this::OnCrashedNodeWarning)
				.match(ElectionMessage.class, this::onElectionMessage)
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
		// setSender(getSender());
		coordinator.tell(msg, self());
	}

	public void epochInit() {
		lastSequenceNumber = 0;
	}

	public void onWriteOK(final WriteOk msg) {
		// delay();
		if (this.currentTimeout != null) {
			this.currentTimeout.cancel();
		}
		print("Received WriteOk for e=" + msg.getRequest_epoch() + " and s=" + msg.getRequest_seqnum());
		if (msg.getRequest_seqnum() > lastSequenceNumber) {
			//Key removeKey = new Key(msg.getRequest_epoch(), msg.getRequest_seqnum());
			/*
			for (Map.Entry<Key, Integer> entry : waitingList.entrySet()) {
				if (entry.getKey().equals(removeKey)) {
					int rmkeyS = removeKey.getS() ;
					int entrykeyS = entry.getKey().getS() ;
					lastSequenceNumber = entry.getKey().getS() ;
					print("SequenceNumber : "+this.lastSequenceNumber);
					this.setValue(entry.getValue());
					print("Updated value :" + this.getValue());
					removeKey = entry.getKey();
				}
			}*/
			this.setValue(waitingList.get(msg.getRequest_epoch()).get(msg.getRequest_seqnum()).getValue()) ;
			print("Updated value :" + this.getValue());
			lastSequenceNumber = msg.getRequest_seqnum() ;
			print("SequenceNumber : "+this.lastSequenceNumber);
			//waitingList.remove(removeKey);
			//print("Update removed from queue");
		}
	}

	public void onUpdate(Update msg) { // Update propagates from coordinator
		// delay();
		if (this.currentTimeout != null) {
			this.currentTimeout.cancel();
		}
		//Key request_id = new Key(msg.getEpochs(), msg.getSequenceNum());

		waitingList.get(msg.getEpochs()).add(msg) ;
		print("queued value" + waitingList.get(msg.getEpochs()).get(msg.getSequenceNum()).getValue() + "at sequence number "+msg.getSequenceNum());
		Acknowledgement acknowledgement = new Acknowledgement(Acknowledge.ACK, msg.getEpochs(), msg.getSequenceNum());
		this.coordinator.tell(acknowledgement, getSelf());
		this.print("ACK sent for ("+msg.getEpochs()+", "+msg.getSequenceNum()+", "+msg.getValue()+")");
		// setTimeout(WRITEOK_TIMEOUT, toMessages.WRITEOK);
	}

	public void onTimeout(Timeout msg) {

		if (msg.toMess == toMessages.UPDATE) {
			print(" update Timeout:" + "Coordinator Crash");
			multicast(new CrashedNodeWarning(0));
			
			print(" Send election message");
			startElection();

		}

		if (msg.toMess == toMessages.WRITEOK) {

			print("WriteOk Timeout:" + "Coordinator Crash");
			print(" Send election message");
			startElection();

		}

	}

}