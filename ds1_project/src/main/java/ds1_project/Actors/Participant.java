package ds1_project.Actors;

import ds1_project.Key;
import ds1_project.TwoPhaseBroadcast;
import ds1_project.TwoPhaseBroadcast.*;
import ds1_project.Requests.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.Props;
import ds1_project.Responses.*;

public class Participant extends Node {
	// Timeout values
	final static int WRITEOK_TIMEOUT = 3000;
	final static int UPDATE_TIMEOUT = 3000;
	final static int ACK_TIMEOUT = 3000;

	public static final int QUORUM_SIZE = ds1_project.TwoPhaseBroadcast.QUORUM_SIZE;

	// Lists & maps
	private final HashMap<Key, HashSet<ActorRef>> majorityVoters = new HashMap<Key, HashSet<ActorRef>>();

	// Working variables
	private int epoch = 0;
	private int sequenceNumber = 0;

	private ActorRef coordinator;

	// Flags
	boolean quorum = false;
	private boolean newUpdate = false;

	// Standard participant constructor
	public Participant(final int id, ActorRef coord) {
		super(id);
		this.coordinator = coord;
	}

	// Coordinator
	public Participant() {
		super(0); // the coordinator has the id 0
		super.setCoordinator(true);
	}

	// Akka declaration methods
	// Standard Participant
	static public Props props(final int id, ActorRef coord) {
		return Props.create(Participant.class, () -> new Participant(id, coord));
	}

	// Coordinator
	static public Props props() {
		return Props.create(Participant.class, () -> new Participant());
	}

	// Getters and Setters
	public int getSequenceNumber() {
		return this.sequenceNumber;
	}

	public int getEpoch() {
		return this.epoch;
	}

	public void epochInit() {
		sequenceNumber = 0;
		this.epoch = this.epoch + 1;
	}

	public void setCoordinator(ActorRef coord) {
		this.coordinator = coord;
	}

	public ActorRef getCoordinator() {
		return this.coordinator;
	}

	// Message reception methods

	// Common
	public void onStartMessage(final StartMessage msg) {
		setGroup(msg);
	}

	public void OnReadRequest(final ReadRequest msg) {
		getSender().tell(new ReadResponse(this.getValue()), self());
	}

	public void onUpdateRequest(final UpdateRequest msg) {
		delay();
		if (isCoordinator()) {
			int currentSeqNum = waitingList.get(epoch).size();
			print("Broadcasting update with sequence number = " + (currentSeqNum) + ":" + msg.getValue());
			Update update = new Update(this.epoch, currentSeqNum, msg.getValue());
			multicast(update);
			waitingList.get(epoch).add(update);
		} else {
			coordinator.tell(msg, self()); // forward to corrdinator
		}

	}

	public void onElectionMessage(ElectionMessage msg) {
		if (msg.getCandidatesID().contains(this.id)) {
			int iMax = msg.getLastUpdates().indexOf(Collections.max(msg.getLastUpdates()));
			if (iMax == this.id) {
				this.setCoordinator(true);
			} else {
				this.coordinator = network.get(iMax);
			}
			if (this.id <= TwoPhaseBroadcast.N_PARTICIPANTS) {
				int destinationID = this.id + 1;
				while (crashedNodes.contains(destinationID)) {
					destinationID++;
				}
				network.get(destinationID).tell(msg, self());
			}
			epochInit();
		} else {
			msg.addCandidate(this.id, this.sequenceNumber);
			int destinationID = this.id + 1;
			if (this.id == TwoPhaseBroadcast.N_PARTICIPANTS) {
				destinationID = 0;
			}
			while (crashedNodes.contains(destinationID)) {
				destinationID++;
			}
			msg.addCandidate(this.id, this.sequenceNumber); // last implemented update -> maybe go for most recent
															// update in
															// waiting list
			network.get(destinationID).tell(msg, self());
		}
	}

	void OnCrashedNodeWarning(CrashedNodeWarning msg) {
		if (!this.crashedNodes.contains(msg.getNode())) {
			crashedNodes.add(msg.getNode());
		}
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

	// Coordinator methods

	public void onReceivingAck(Acknowledgement msg) {
		// delay();
		Acknowledge ack = (msg).ack;
		// print("Received ACK from "+sender()+" with seqnum "+msg.getRequest_seqnum())
		// ;
		Key key = new Key(msg.getRequest_epoch(), msg.getRequest_seqnum());
		HashSet<ActorRef> voters = new HashSet<>();
		boolean flag = false;

		if (ack == Acknowledge.ACK && msg.getRequest_seqnum() > this.sequenceNumber) {

			for (Map.Entry<Key, HashSet<ActorRef>> entry : majorityVoters.entrySet()) {
				if (entry.getKey().equals(key)) {
					key = entry.getKey();
					entry.getValue().add(getSender());
					flag = true;
				}
			}
			if (!flag) {
				voters.add(getSender());
				majorityVoters.put(key, voters);
			}

			print("Received ACKs :" + majorityVoters.get(key).size());
		}

		Quorum(key);

		if (newUpdate) {
			Update toImplement = new Update(0, 0, 0);
			int i = waitingList.get(epoch).size() - 1;
			boolean found = false;
			while (i >= 0 && !found) {
				if (waitingList.get(epoch).get(i).isValidated() && i > sequenceNumber) {
					found = true;
					toImplement = waitingList.get(epoch).get(i);
				}
				i--;
			}
			if (found) {
				print("Majority of ACK - Sending WriteOK messages for s=" + toImplement.getSequenceNumber());
				multicast(new WriteOk(true, toImplement.getEpoch(), toImplement.getSequenceNumber()));
				sequenceNumber = toImplement.getSequenceNumber();
				this.setValue(toImplement.getValue());
				print("Updated value :" + this.getValue());
				newUpdate = false;
			}
		}
	}

	// Participant methods

	public void onWriteOK(final WriteOk msg) {
		delay();
		if (this.currentTimeout != null) {
			this.currentTimeout.cancel();
		}
		print("Received WriteOk for e=" + msg.getRequest_epoch() + " and s=" + msg.getRequest_seqnum());
		if (msg.getRequest_seqnum() > sequenceNumber) {
			this.setValue(waitingList.get(msg.getRequest_epoch()).get(msg.getRequest_seqnum()).getValue());
			print("Updated value :" + this.getValue());
			sequenceNumber = msg.getRequest_seqnum();
			print("SequenceNumber : " + this.sequenceNumber);
		}
	}

	public void onUpdate(Update msg) { // Update propagates from coordinator
		delay();
		if (this.currentTimeout != null) {
			this.currentTimeout.cancel();
		}

		waitingList.get(msg.getEpoch()).add(msg);
		print("queued value" + waitingList.get(msg.getEpoch()).get(msg.getSequenceNumber()).getValue()
				+ "at sequence number " + msg.getSequenceNumber());
		Acknowledgement acknowledgement = new Acknowledgement(Acknowledge.ACK, msg.getEpoch(), msg.getSequenceNumber());
		this.coordinator.tell(acknowledgement, getSelf());
		this.print("ACK sent for (" + msg.getEpoch() + ", " + msg.getSequenceNumber() + ", " + msg.getValue() + ")");
		setTimeout(WRITEOK_TIMEOUT, toMessages.WRITEOK);
	}

	

	@Override
	public Receive createReceive() {
		return receiveBuilder().match(StartMessage.class, this::onStartMessage).match(WriteOk.class, this::onWriteOK)
				.match(Update.class, this::onUpdate).match(UpdateRequest.class, this::onUpdateRequest)
				.match(ReadRequest.class, this::OnReadRequest).match(Timeout.class, this::onTimeout)
				.match(CrashedNodeWarning.class, this::OnCrashedNodeWarning)
				.match(ElectionMessage.class, this::onElectionMessage).match(UpdateRequest.class, this::onUpdateRequest)
				.match(ReadRequest.class, this::OnReadRequest).match(Acknowledgement.class, this::onReceivingAck)
				// .match(Recovery.class, this::onRecovery)
				.build();
	}

	// Utility methods
	boolean Quorum(Key key) { // returns true if all voted YES
		if (majorityVoters.containsKey(key)) {
			waitingList.get(key.getE()).get(key.getS()).setValidity(true);
			newUpdate = true;
			return majorityVoters.get(key).size() >= QUORUM_SIZE;
		} else {
			return false;
		}
	}

	public void startElection() {
		String electionID = "" + epoch + id;
		ElectionMessage msg = new ElectionMessage(Integer.parseInt(electionID));
		int destinationID = this.id + 1;
		if (this.id == TwoPhaseBroadcast.N_PARTICIPANTS) {
			destinationID = 0;
		}
		while (crashedNodes.contains(destinationID)) {
			destinationID++;
		}
		msg.addCandidate(this.id, this.sequenceNumber); // last implemented update -> maybe go for most recent update in
														// waiting list
		network.get(destinationID).tell(msg, self());
	}

	public void crash() {
		getContext().become(crashed());
		print(" Crash!!");
	}

	public void coordinatorEpochLaunch (){
		this.setCoordinator(true);
		epochInit();
		//Enforce last known update
	}

}