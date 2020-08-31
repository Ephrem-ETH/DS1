package ds1_project.Actors;

import ds1_project.Key;
import ds1_project.TwoPhaseBroadcast;
import ds1_project.TwoPhaseBroadcast.*;
import scala.unchecked;
import scala.concurrent.duration.Duration;
import ds1_project.Requests.*;

import java.beans.DesignMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

//import org.omg.CORBA.WCharSeqHolder;

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import ds1_project.Responses.*;

public class Participant extends Node  {
	// Timeout values
	final static int WRITEOK_TIMEOUT = 9000;
	final static int UPDATE_TIMEOUT = 3000;
	final static int ACK_TIMEOUT = 3000;
	final static int HEARTBEAT_DELAY = 5000;
	final static int ELECTION_TIMEOUT = 5000;
	final static int TOTAL_ELECTION_TO = 15000;
	final static int SYNCH_TIMEOUT = 5000;

	public static final int QUORUM_SIZE = ds1_project.TwoPhaseBroadcast.QUORUM_SIZE;
	// Majority Voters : needed for the coordinator to determine the sending of a
	// WriteOK
	private final HashMap<Key, HashSet<ActorRef>> majorityVoters = new HashMap<Key, HashSet<ActorRef>>();

	// Pending updates
	private List<Update> pendingUpdates = new ArrayList<Update>();
	// Working variables

	//Last sent ack for termination protocol
	private Acknowledgement lastSentAck;

	// Current stable state
	private int epoch = 0;
	private int sequenceNumber = 0;

	// Timeout variables storage
	private Cancellable heartbeat_timeout;
	private Cancellable election_timeout;
	private Cancellable election_duration_timeout;
	private Cancellable synch_timeout;
	private HashMap<Integer, Cancellable> nodeTimeouts = new HashMap<Integer, Cancellable>();

	// Coordinator ref needed for sending specific messages
	private ActorRef coordinator;
	private int coordinator_id;

	// Memory in case of failed sending of the election message
	private ElectionMessage lastElectionMessage;

	// Flags

	private boolean quorum = false;
	private boolean isElecting = false;
	private boolean newUpdate = false;
	private boolean isCoordinatorFound = false;

	// Standard participant constructor
	public Participant(final int id, ActorRef coord) {
		super(id);
		this.coordinator = coord;
		this.coordinator_id = 0;
	}

	// Coordinator constructor
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

	public void setCoordinator(ActorRef coord, int id) {
		this.coordinator = coord;
		this.coordinator_id = id;
	}

	public ActorRef getCoordinator() {
		return this.coordinator;
	}

	public void incrementEpoch(){
		this.epoch = this.epoch + 1 ;
	}

	// Message reception methods

	// Common
	public void onStartMessage(final StartMessage msg) {
		setGroup(msg);
		if (this.isCoordinator()) {
			sendHeartbeat();
		}
		this.heartbeat_timeout = scheduleTimeout(HEARTBEAT_DELAY, toMessages.HEARTBEAT, coordinator_id) ;

		log.debug("Starting ...");
	}

	public void OnReadRequest(final ReadRequest msg) {
		getSender().tell(new ReadResponse(this.getValue()), self());
		log.debug("The read request({}) sent by {}", msg, getSender());
	}

	public void onUpdateRequest(final UpdateRequest msg) {
		delay();
		if (isCoordinator()) {
			print("Update received");
			int currentSeqNum = waitingList.get(this.epoch).size();
			print("Broadcasting update with sequence number = " + (currentSeqNum) + ":" + msg.getValue());
			Update update = new Update(this.epoch, currentSeqNum, msg.getValue(), this.id);
			multicast(update);
			waitingList.get(this.epoch).add(update);
            log.debug(" At epoch {} update with sequence number = {} : "
            		+ "{} is broadcasted by {}",this.epoch,currentSeqNum, msg.getValue(),this.id);
			// start timeout for each node in the system

			for (Map.Entry<Integer, ActorRef> entry : network.entrySet()) {
				if (entry.getKey() != this.id && !crashedNodes.contains(entry.getValue())
						&& !this.nodeTimeouts.containsKey(entry.getKey())) {
					// print("Setting timeout for node " + entry.getKey()) ;
					nodeTimeouts.put(entry.getKey(), scheduleTimeout(UPDATE_TIMEOUT, toMessages.UPDATE, entry.getKey())) ;
				}
			}

		} else {
			coordinator.tell(msg, self()); // forward to corrdinator
			log.debug("update request forwarded from {}", self());
			// Start timeout
			nodeTimeouts.put(coordinator_id, scheduleTimeout(UPDATE_TIMEOUT, toMessages.UPDATE, coordinator_id)) ;
							}

	}

	public void onElectionMessage(ElectionMessage msg) {

		election_duration_timeout = scheduleTimeout(TOTAL_ELECTION_TO, toMessages.ELECTION_TOTAL, this.id);

		if (lastElectionMessage == null) {

			// Enter election mode
			getContext().become(electionReceive());
			log.debug("Enter into election mode ...");
			isElecting = true;

			// Cancel standard timeouts
			for (Map.Entry<Integer, Cancellable> entry : nodeTimeouts.entrySet()) {
				entry.getValue().cancel();
			}
			heartbeat_timeout.cancel();

			if (msg.getElectionEpoch() == this.epoch) {

				int destinationID = this.id + 1;
				if (this.id == TwoPhaseBroadcast.N_PARTICIPANTS) {
					destinationID = 0;
				}
				while (crashedNodes.contains(destinationID)) {
					destinationID++;
				}
				msg.addCandidate(this.id, this.sequenceNumber); // last implemented update -> maybe go for most recent
				log.debug("Node {} with sequence number {} is added to the ring", this.id, this.sequenceNumber);										// update in
				// waiting list
				lastElectionMessage = msg; // save in case of crash of the next node
				network.get(destinationID).tell(msg, self());
				log.debug("Node {} passes election message to {}",this.id, destinationID);
				this.election_timeout = scheduleTimeout(ELECTION_TIMEOUT, toMessages.ELECTION, destinationID);

				//
			}
		}
		print("Election message Ack");
		getSender().tell(new ElectionAck(Acknowledge.ACK, msg, this.id), self());

	}

	public void onElectionModeMessage(ElectionMessage msg) {

		boolean won = false;

		// print("Election mode receive");
		if (msg.getCandidatesID().contains(this.id)) { // Second turn
			int iMax = 0;
			for (int i = 0; i < msg.getCandidatesID().size(); i++) {
				if (msg.getLastUpdates().get(i) > msg.getLastUpdates().get(iMax)) {
					iMax = i;
				} else if (msg.getLastUpdates().get(i) == msg.getLastUpdates().get(iMax)
						&& msg.getCandidatesID().get(iMax) > msg.getCandidatesID().get(i)) {
					iMax = i;
				}
			}

			if (msg.getCandidatesID().get(iMax) == this.id) { // Node won the election

				this.setCoordinator(true);
				print("I won the election");
				log.debug("{} won the election", this.id);
				isCoordinatorFound = true;
				coordinatorEpochLaunch();
				won = true;
				election_duration_timeout.cancel();

			} else { // Node lost

				isCoordinatorFound = true;
				print("I lost : " + msg.getCandidatesID().get(iMax) + " won");

			}

			setCoordinator(network.get(msg.getCandidatesID().get(iMax)), msg.getCandidatesID().get(iMax));
			this.coordinator_id = msg.getCandidatesID().get(iMax);

		} else {
			if (msg.getElectionEpoch() == this.epoch && !won) {

				int destinationID = this.id + 1;
				if (this.id == TwoPhaseBroadcast.N_PARTICIPANTS) {
					destinationID = 0;
				}
				while (crashedNodes.contains(destinationID)) {
					destinationID++;
				}
				msg.addCandidate(this.id, this.sequenceNumber); // last implemented update -> maybe go for most
																// recent
																// update in
				// waiting list
				lastElectionMessage = msg; // save in case of crash of the next node
				network.get(destinationID).tell(msg, self());
				if (this.election_timeout != null) {
					this.election_timeout.cancel();
				}
				this.election_timeout = scheduleTimeout(ELECTION_TIMEOUT, toMessages.ELECTION, destinationID);

				//
			}
		}
		// print("Coordinator id " + this.coordinator_id) ;

		getSender().tell(new ElectionAck(Acknowledge.ACK, msg, this.id), self());

	}

	public void onElectionAck(ElectionAck msg) {
	
		print("Received Election ACK");
		if (msg.ack == Acknowledge.ACK) {
			if (election_timeout != null) {
				election_timeout.cancel();
			}
		}
		log.debug("Election ACK received");
	}

	void OnCrashedNodeWarning(CrashedNodeWarning msg) {
		if (!this.crashedNodes.contains(msg.getNode())) {
			print("Adding node " + msg.getNode() + " to crashed list");
			log.debug("Adding node {}  to crashed list", msg.getNode());
			crashedNodes.add(msg.getNode());
		}
	}

	public void onTimeout(Timeout msg) {

		if (msg.getWatchingNode() != this.id) {

			if (msg.toMess == toMessages.UPDATE) {
				print("Update Timeout:" + "Node " + msg.getWatchingNode() + " Crashed");
				this.crashedNodes.add(msg.getWatchingNode());
				multicast(new CrashedNodeWarning(msg.getWatchingNode()));

				if (msg.getWatchingNode() == this.coordinator_id && !this.isCoordinator()) {
					print(" Send election message");
					startElection();
				}

			}

			if (msg.toMess == toMessages.WRITEOK) {

				print("WriteOk Timeout:" + "Coordinator Crash");
				this.crashedNodes.add(msg.getWatchingNode());
				multicast(new CrashedNodeWarning(this.coordinator_id));
				print(" Send election message");
				startElection();

			}

			if (msg.toMess == toMessages.ELECTION) {
				if (isElecting) {
					print("Election timeout : Node " + msg.getWatchingNode() + " crashed");
					this.crashedNodes.add(msg.getWatchingNode());
					multicast(new CrashedNodeWarning(msg.getWatchingNode()));

					// Re-send election message
					int destinationID = msg.getWatchingNode();
					if (destinationID == TwoPhaseBroadcast.N_PARTICIPANTS) {
						destinationID = 0;
					}
					while (crashedNodes.contains(destinationID)) {
						destinationID++;
						if (destinationID == TwoPhaseBroadcast.N_PARTICIPANTS) {
							destinationID = 0;
						}
					}
					network.get(destinationID).tell(lastElectionMessage, self());
					this.election_timeout = scheduleTimeout(ELECTION_TIMEOUT, toMessages.ELECTION, destinationID);
				}
			}
		}

		if (msg.toMess == toMessages.HEARTBEAT) {
			if (this.isCoordinator()) {
				sendHeartbeat();
			} else {
				print("Heartbeat timeout : Coordinator crashed");
				this.crashedNodes.add(this.coordinator_id);
				multicast(new CrashedNodeWarning(this.coordinator_id));
				print(" Send election message");
				startElection();
			}
		}

		if (msg.toMess == toMessages.ELECTION_TOTAL) {
			getContext().unbecome();
			lastElectionMessage = null;
			isElecting = false;
			print("Election did not terminate - restarting process");
			startElection();
		}

		if (msg.toMess == toMessages.SYNCH) {

			if (pendingUpdates.size() != 0) {
				Update toSend = pendingUpdates.get(0);
				int count = 0;
				int i = 0;
				while (count < QUORUM_SIZE - 1 || i < pendingUpdates.size()) { // Quorum minus 1 because previous coordinator has agreedg
					toSend = pendingUpdates.get(i);
					for (Update up : pendingUpdates) {
						if (up.getSequenceNumber() == toSend.getSequenceNumber()) {
							count++;
						}
					}
					i++;
				}
				if (count >= QUORUM_SIZE - 1) {
					multicast(toSend);
					waitingList.get(epoch).add(toSend);
					this.setValue(toSend.getValue());
					this.sequenceNumber = toSend.getSequenceNumber();
				} else {
					multicast(new EndEpoch()) ;
				}
			} else {
				multicast(new EndEpoch());
			}
			pendingUpdates = new ArrayList<Update>();
			sendHeartbeat();
			incrementEpoch();
			this.sequenceNumber = 0;
			waitingList.add(new ArrayList<Update>()) ;
			waitingList.get(epoch).add(new Update(epoch,0,this.getValue(),this.coordinator_id)) ;
			isElecting = false;
			getContext().become(createReceive());
		}

	}

	public void onCrashRequest(CrashRequest msg) {
		this.crash();
	}

	// Coordinator methods

	public void onReceivingAck(Acknowledgement msg) {
		delay();
		if (nodeTimeouts.containsKey(msg.getSender_id()) && nodeTimeouts.get(msg.getSender_id()) != null) {
			nodeTimeouts.get(msg.getSender_id()).cancel();
			// print("Cancelled Update Timeout for "+msg.getSender_id());
		}
		print("received ack from " + msg.getSender_id());
		Acknowledge ack = (msg).ack;
		// print("Received ACK from "+sender()+" with seqnum "+msg.getRequest_seqnum())
		// ;
		Key key = new Key(msg.getRequest_epoch(), msg.getRequest_seqnum());
		HashSet<ActorRef> voters = new HashSet<>();
		boolean flag = false;

		if (ack == Acknowledge.ACK && msg.getRequest_epoch()==this.epoch && msg.getRequest_seqnum() > this.sequenceNumber) {

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
			Update toImplement = new Update(0, 0, 0, this.coordinator_id);
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
				// Coordinator crashed before sending writeOk message
				// this.crash();
				print("Majority of ACK - Sending WriteOK messages for s=" + toImplement.getSequenceNumber());
				multicast(new WriteOk(true, toImplement.getEpoch(), toImplement.getSequenceNumber(), this.id));
				// crash();
				sequenceNumber = toImplement.getSequenceNumber();
				this.setValue(toImplement.getValue());
				print("Updated value :" + this.getValue());
				newUpdate = false;
			}
		}
	}

	public void sendHeartbeat() {
		if (heartbeat_timeout != null) {
			heartbeat_timeout.cancel();
			heartbeat_timeout = null;
		}
		// print("sent new heartbeat");
		this.heartbeat_timeout = scheduleTimeout(HEARTBEAT_DELAY/5, toMessages.HEARTBEAT, coordinator_id) ;
		multicast(new Heartbeat());
	}

	// Participant methods

	public void onSychronizationMessage(Synchronization msg) {
		delay();
		if (election_timeout!=null){
		election_timeout.cancel();
		election_timeout = null;
		}
		
		print("Received WriteOK for epoch synchronization");
		if (msg.getUpdate() != null && lastSentAck != null) {
			// Is there a more recent ACKed update ?
			if (msg.getUpdate().getSequenceNumber() < lastSentAck.getRequest_seqnum()
					&& lastSentAck.getRequest_epoch() == epoch) {
				waitingList.get(epoch).get(lastSentAck.getRequest_seqnum()).setEpochConsolidation(true);
				coordinator.tell(waitingList.get(epoch).get(lastSentAck.getRequest_seqnum()), self());
			}
			// First synchronization
			this.waitingList.get(epoch).add(msg.getUpdate());
			this.setValue(msg.getUpdate().getValue());
		} else {
			if (waitingList.size() > 0 && lastSentAck != null && lastSentAck.getRequest_epoch() == epoch) {
				waitingList.get(epoch).get(waitingList.get(epoch).size() - 1).setEpochConsolidation(true);
				coordinator.tell(waitingList.get(epoch).get(waitingList.get(epoch).size() - 1), self());
			}
		}
		this.isCoordinatorFound = false;
		lastElectionMessage = null;

	}

	public void onConsolidationUpdate(Update msg) {
		delay();
		election_duration_timeout.cancel();
		if (msg.isEpochConsolidation() && isElecting){
			// Add pending updates sent by other participants to a list
			if (this.isCoordinator()) {
				pendingUpdates.add(msg);
			} else { // Implements last update
				print("Received last update for consolidation");
				this.waitingList.get(epoch).add(msg);
				this.waitingList.get(epoch).get(waitingList.get(epoch).size() - 1).setValidity(true);
				this.setValue(waitingList.get(epoch).get(waitingList.get(epoch).size() - 1).getValue());
				print("Updated value :" + this.getValue());

				// Changing to standard mode

				getContext().unbecome();
				isElecting = false;
				this.sequenceNumber = 0;
				incrementEpoch();
				print("New epoch : " + this.epoch);
				waitingList.add(new ArrayList<Update>());
				waitingList.get(epoch).add(new Update(epoch,0,this.getValue(),this.coordinator_id)) ;
			}
		}

	}

	public void onEpochEndMessage(EndEpoch msg) {
		delay ();
		election_duration_timeout.cancel();
		getContext().unbecome();
		isElecting = false;
		this.sequenceNumber = 0;
		incrementEpoch();
		print("New Epoch : " + this.epoch ) ;
		waitingList.add(new ArrayList<Update>());
		waitingList.get(epoch).add(new Update(epoch,0,this.getValue(),this.coordinator_id)) ;

	}

	public void onWriteOK(final WriteOk msg) {
		delay();
		if (nodeTimeouts.containsKey(msg.getSender_id()) && nodeTimeouts.get(msg.getSender_id()) != null) {
			nodeTimeouts.get(msg.getSender_id()).cancel();
		}

		print("Received WriteOk for e=" + msg.getRequest_epoch() + " and s=" + msg.getRequest_seqnum());
		if (msg.getRequest_seqnum() > sequenceNumber) {
			this.waitingList.get(msg.getRequest_epoch()).get(msg.getRequest_seqnum()).setValidity(true);
			this.setValue(waitingList.get(msg.getRequest_epoch()).get(msg.getRequest_seqnum()).getValue());
			print("Updated value :" + this.getValue());
			sequenceNumber = msg.getRequest_seqnum();
			print("SequenceNumber : " + this.sequenceNumber);
		}
	}

	public void onUpdate(Update msg) { // Update propagates from coordinator

		delay();
		if (nodeTimeouts.containsKey(msg.getSender_id()) && nodeTimeouts.get(msg.getSender_id()) != null) {
			nodeTimeouts.get(msg.getSender_id()).cancel();
		}

		waitingList.get(msg.getEpoch()).add(msg); // Causes errror if several un-initialized epochs chain
		print("queued value" + waitingList.get(msg.getEpoch()).get(msg.getSequenceNumber()).getValue()
				+ "at sequence number " + msg.getSequenceNumber());
		Acknowledgement acknowledgement = new Acknowledgement(Acknowledge.ACK, msg.getEpoch(), msg.getSequenceNumber(),
				this.id);
		this.coordinator.tell(acknowledgement, getSelf());
		lastSentAck = acknowledgement;
		this.print("ACK sent for (" + msg.getEpoch() + ", " + msg.getSequenceNumber() + ", " + msg.getValue() + ")");
		nodeTimeouts.put(coordinator_id, scheduleTimeout(WRITEOK_TIMEOUT, toMessages.WRITEOK, coordinator_id)) ;
		System.out.println("");
	}

	public void onHeartbeat(Heartbeat ping) {
		if (!this.isCoordinator()) {
			if (this.heartbeat_timeout != null) {
				this.heartbeat_timeout.cancel();
				for (Map.Entry<Integer, Cancellable> entry : nodeTimeouts.entrySet()) {
					entry.getValue().cancel();
				}
			}

			// print("Heartbeat received");
			this.heartbeat_timeout = scheduleTimeout(HEARTBEAT_DELAY, toMessages.HEARTBEAT, coordinator_id) ;
			
		}
	}

	@Override
	public Receive createReceive() { // Standard mode
		return receiveBuilder().match(StartMessage.class, this::onStartMessage).match(WriteOk.class, this::onWriteOK)
				.match(Update.class, this::onUpdate).match(UpdateRequest.class, this::onUpdateRequest)
				.match(ReadRequest.class, this::OnReadRequest).match(Timeout.class, this::onTimeout)
				.match(CrashedNodeWarning.class, this::OnCrashedNodeWarning)
				.match(ElectionMessage.class, this::onElectionMessage)
				.match(Acknowledgement.class, this::onReceivingAck).match(Heartbeat.class, this::onHeartbeat)
				.match(CrashRequest.class, this::onCrashRequest)
				// .match(Recovery.class, this::onRecovery)
				.build();
	}

	public Receive electionReceive() { // Election mode
		return receiveBuilder().match(CrashedNodeWarning.class, this::OnCrashedNodeWarning)
				.match(ElectionMessage.class, this::onElectionModeMessage).match(ElectionAck.class, this::onElectionAck)
				.match(Synchronization.class, this::onSychronizationMessage)
				.match(EndEpoch.class, this::onEpochEndMessage).match(Update.class, this::onConsolidationUpdate)
				.match(Timeout.class, this::onTimeout).match(CrashedNodeWarning.class, this::OnCrashedNodeWarning)
				.build();

	}

	public Receive crashed() { // Crashed mode
		return receiveBuilder()
				// .match(Recovery.class, this::onRecovery)
				.matchAny(msg -> {
				}).build();
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

		if (!isElecting) {
			getContext().become(electionReceive());
			isElecting = true;
			for (Map.Entry<Integer, Cancellable> entry : nodeTimeouts.entrySet()) {
				entry.getValue().cancel();
			}
			heartbeat_timeout.cancel();
			print("Starting election process");
			ElectionMessage msg = new ElectionMessage(this.epoch, this.id);
			int destinationID = this.id + 1;
			while (crashedNodes.contains(destinationID)) {
				destinationID++;
			}
			if (destinationID > TwoPhaseBroadcast.N_PARTICIPANTS) {
				destinationID = 0;
				while (crashedNodes.contains(destinationID)) {
					destinationID++;
				}
			}
			// msg.addCandidate(this.id, this.sequenceNumber); // last implemented update ->
			// maybe go for most recent
			// update in
			// waiting list
			network.get(destinationID).tell(msg, self()); // to fix

			election_duration_timeout = scheduleTimeout(TOTAL_ELECTION_TO, toMessages.ELECTION_TOTAL, this.id);
			election_timeout = scheduleTimeout(ELECTION_TIMEOUT, toMessages.ELECTION, destinationID);

		}
	}

	public void crash() {
		getContext().become(crashed());
		this.isCrashed = true;
		print(" Crash!!");
	}

	public Cancellable scheduleTimeout(int duration, toMessages type, int watchingNode) {
		return getContext().system().scheduler().scheduleOnce(Duration.create(duration, TimeUnit.MILLISECONDS),
				getSelf(), new Timeout(type, watchingNode), // the message to send
				getContext().system().dispatcher(), getSelf());
	}

	public void coordinatorEpochLaunch() {
		// Enforce last known update
		// search backwards the waiting list : first update .isValidated == true ->
		// multicast writeok
		boolean found = false;
		this.coordinator_id = this.id;
		int i = waitingList.get(epoch).size() - 1;
		while (!found && i >= 0) {
			if (waitingList.get(epoch).get(i).isValidated()) {
				// multicast writeOKs
				found = true;
				Update toImplement = waitingList.get(epoch).get(i);
				print("Multicasting last known update");
				multicast(new Synchronization(toImplement));
			}
			i--;
		}
		if (!found) {
			multicast(new Synchronization(null));
		}
		synch_timeout = scheduleTimeout(5000, toMessages.SYNCH, this.id);
		/*
		 * sendHeartbeat(); epoch = epoch++; sequenceNumber = 0; isElecting = false;
		 * getContext().become(createReceive());
		 */
	}

}