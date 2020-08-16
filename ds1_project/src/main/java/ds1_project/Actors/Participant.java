package ds1_project.Actors;

import ds1_project.Key;
import ds1_project.TwoPhaseBroadcast;
import ds1_project.TwoPhaseBroadcast.*;
import scala.unchecked;
import scala.concurrent.duration.Duration;
import ds1_project.Requests.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.omg.CORBA.WCharSeqHolder;

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import ds1_project.Responses.*;

public class Participant extends Node {
	// Timeout values
	final static int WRITEOK_TIMEOUT = 9000;
	final static int UPDATE_TIMEOUT = 3000;
	final static int ACK_TIMEOUT = 3000;
	final static int HEARTBEAT_DELAY = 5000;
	final static int ELECTION_TIMEOUT = 3000 ;

	public static final int QUORUM_SIZE = ds1_project.TwoPhaseBroadcast.QUORUM_SIZE;

	// Lists & maps
	private final HashMap<Key, HashSet<ActorRef>> majorityVoters = new HashMap<Key, HashSet<ActorRef>>();

	// Working variables
	private int epoch = 0;
	private int sequenceNumber = 0;

	private Cancellable heartbeat_timeout;
	private Cancellable election_timeout ;

	private HashMap<Integer, Cancellable> nodeTimeouts = new HashMap<Integer, Cancellable>();
	private ActorRef coordinator;
	private int coordinator_id;

	private ElectionMessage lastElectionMessage ;
	// Flags
	boolean quorum = false;
	private boolean isElecting = false;
	private boolean newUpdate = false;

	// Standard participant constructor
	public Participant(final int id, ActorRef coord) {
		super(id);
		this.coordinator = coord;
		this.coordinator_id = 0;
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
		waitingList.add(new ArrayList<Update>());
		if (this.isCoordinator()) {
			coordinatorEpochLaunch();
		}
	}

	public void setCoordinator(ActorRef coord, int id) {
		this.coordinator = coord;
		this.coordinator_id = id;
	}

	public ActorRef getCoordinator() {
		return this.coordinator;
	}

	// Message reception methods

	// Common
	public void onStartMessage(final StartMessage msg) {
		setGroup(msg);
		if (this.isCoordinator()) {
			sendHeartbeat();
		}
		this.heartbeat_timeout = getContext().system().scheduler().scheduleOnce(
				Duration.create(HEARTBEAT_DELAY, TimeUnit.MILLISECONDS), getSelf(),
				new Timeout(toMessages.HEARTBEAT, coordinator_id), getContext().system().dispatcher(), getSelf());

	}

	public void OnReadRequest(final ReadRequest msg) {
		getSender().tell(new ReadResponse(this.getValue()), self());
	}

	public void onUpdateRequest(final UpdateRequest msg) {
		delay();
		if (isCoordinator()) {
			print("Update received");
			int currentSeqNum = waitingList.get(epoch).size();
			print("Broadcasting update with sequence number = " + (currentSeqNum) + ":" + msg.getValue());
			Update update = new Update(this.epoch, currentSeqNum, msg.getValue(), this.id);
			multicast(update);
			waitingList.get(epoch).add(update);

			// start timeout for each node in the system

			for (Map.Entry<Integer, ActorRef> entry : network.entrySet()) {
				if (entry.getKey() != this.id && !crashedNodes.contains(entry.getValue())) {
					// print("Setting timeout for node " + entry.getKey()) ;
					nodeTimeouts.put(entry.getKey(),
							getContext().system().scheduler().scheduleOnce(
									Duration.create(UPDATE_TIMEOUT, TimeUnit.MILLISECONDS), getSelf(),
									new Timeout(toMessages.UPDATE, entry.getKey()), // the message to send
									getContext().system().dispatcher(), getSelf()));
				}
			}

		} else {
			coordinator.tell(msg, self()); // forward to corrdinator
			// Start timeout
			nodeTimeouts.put(coordinator_id,
					getContext().system().scheduler().scheduleOnce(
							Duration.create(UPDATE_TIMEOUT, TimeUnit.MILLISECONDS), getSelf(),
							new Timeout(toMessages.UPDATE, coordinator_id), // the
							// message
							// to
							// send
							getContext().system().dispatcher(), getSelf()));
		}

	}

	public void onElectionMessage(ElectionMessage msg) {
		// Enter election mode
		if (!isElecting){
			isElecting = true ;
			getContext().become(electionReceive());
		}
		// Cancel timeouts
		for (Map.Entry<Integer, Cancellable> entry : nodeTimeouts.entrySet()) {
			entry.getValue().cancel();
		}
		heartbeat_timeout.cancel();
		// print("ElectionStep");

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
				coordinatorEpochLaunch();
			} else { // Node lost
				print("I lost : " + msg.getCandidatesID().get(iMax) + " won");
			}

			if (msg.getEmmiter_id() != this.id) { // Forward the election message
				int destinationID = this.id + 1;

				if (this.id == TwoPhaseBroadcast.N_PARTICIPANTS) {
					destinationID = 0;
				}
				while (crashedNodes.contains(destinationID)) {
					destinationID++;
				}
				lastElectionMessage = msg ; // save in case of crash of the next node
				network.get(destinationID).tell(msg, self());
			}

			setCoordinator(network.get(msg.getCandidatesID().get(iMax)), msg.getCandidatesID().get(iMax));
			this.coordinator_id = msg.getCandidatesID().get(iMax);
			// print("Coordinator id " + this.coordinator_id) ;

		} else { // first turn
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
			lastElectionMessage = msg ; // save in case of crash of the next node
			this.election_timeout = scheduleTimeout(ELECTION_TIMEOUT, toMessages.ELECTION, destinationID) ;
			network.get(destinationID).tell(msg, self());
		}
	}

	public void onElectionAck(ElectionAck msg){
		// TODO Ephrem
	}

	void OnCrashedNodeWarning(CrashedNodeWarning msg) {
		if (!this.crashedNodes.contains(msg.getNode())) {
			print("Adding node " + msg.getNode() + " to crashed list");
			crashedNodes.add(msg.getNode());
		}
	}

	public void onTimeout(Timeout msg) {

		if (msg.getWatchingNode() != this.id) {

			if (msg.toMess == toMessages.UPDATE) {
				print("Update Timeout:" + "Node " + msg.getWatchingNode() + " Crashed");
				this.crashedNodes.add(msg.getWatchingNode()) ;
				multicast(new CrashedNodeWarning(msg.getWatchingNode()));

				if (msg.getWatchingNode() == this.coordinator_id && !this.isCoordinator()) {
					print(" Send election message");
					startElection();
				}

			}

			if (msg.toMess == toMessages.WRITEOK) {

				print("WriteOk Timeout:" + "Coordinator Crash");
				this.crashedNodes.add(msg.getWatchingNode()) ;
				multicast(new CrashedNodeWarning(this.coordinator_id));
				print(" Send election message");
				startElection();

			}

			if (msg.toMess == toMessages.ELECTION ) {
				print ("Election timeout : Node "+msg.getWatchingNode()+" crashed") ;
				this.crashedNodes.add(msg.getWatchingNode()) ;
				multicast(new CrashedNodeWarning(msg.getWatchingNode())) ;

				//Resend election message
				int destinationID = msg.getWatchingNode() ;
				if (this.id == TwoPhaseBroadcast.N_PARTICIPANTS) {
					destinationID = 0;
				}
				while (crashedNodes.contains(destinationID)) {
					destinationID++;
				}
				network.get(destinationID).tell(lastElectionMessage, self());
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
				crash();
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
		this.heartbeat_timeout = getContext().system().scheduler().scheduleOnce(
				Duration.create(HEARTBEAT_DELAY / 5, TimeUnit.MILLISECONDS), getSelf(),
				new Timeout(toMessages.HEARTBEAT, coordinator_id), // the message to send
				getContext().system().dispatcher(), getSelf());
		multicast(new Heartbeat());
	}

	// Participant methods

	public void onSychronizationMessage(Synchronization msg){
		delay() ;

		print("Received WriteOK for epoch synchronization") ;
		this.waitingList.get(epoch).add(msg.getUpdate()) ;
		this.setValue(msg.getUpdate().getValue());
		this.sequenceNumber = 0;
		this.epoch ++;
		waitingList.add(new ArrayList<Update>());

		//Changing to standard mode
		getContext().become(createReceive());
		
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
		this.print("ACK sent for (" + msg.getEpoch() + ", " + msg.getSequenceNumber() + ", " + msg.getValue() + ")");
		nodeTimeouts.put(coordinator_id,
				getContext().system().scheduler().scheduleOnce(Duration.create(WRITEOK_TIMEOUT, TimeUnit.MILLISECONDS),
						getSelf(), new Timeout(toMessages.WRITEOK, coordinator_id), // the
						// message
						// to
						// send
						getContext().system().dispatcher(), getSelf()));
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
			this.heartbeat_timeout = getContext().system().scheduler().scheduleOnce(
					Duration.create(HEARTBEAT_DELAY, TimeUnit.MILLISECONDS), getSelf(),
					new Timeout(toMessages.HEARTBEAT, coordinator_id), // the
					// message
					// to
					// send
					getContext().system().dispatcher(), getSelf());
		}
	}

	@Override
	public Receive createReceive() { // Standard mode
		return receiveBuilder()
				.match(StartMessage.class, this::onStartMessage)
				.match(WriteOk.class, this::onWriteOK)
				.match(Update.class, this::onUpdate)
				.match(UpdateRequest.class, this::onUpdateRequest)
				.match(ReadRequest.class, this::OnReadRequest)
				.match(Timeout.class, this::onTimeout)
				.match(CrashedNodeWarning.class, this::OnCrashedNodeWarning)
				.match(ElectionMessage.class, this::onElectionMessage)
				.match(Acknowledgement.class, this::onReceivingAck)
				.match(Heartbeat.class, this::onHeartbeat)
				.match(CrashRequest.class, this::onCrashRequest)
				// .match(Recovery.class, this::onRecovery)
				.build();
	}

	public Receive electionReceive() { //Election mode
		return receiveBuilder().match(CrashedNodeWarning.class, this::OnCrashedNodeWarning)
				.match(ElectionMessage.class, this::onElectionMessage)
				.match(ElectionAck.class, this::onElectionAck)
				.match(Synchronization.class, this::onSychronizationMessage)
				.match(Timeout.class, this::onTimeout)
				.match(CrashedNodeWarning.class, this::OnCrashedNodeWarning).build() ;

	}

	public Receive crashed() { //Crashed mode
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
			election_timeout = scheduleTimeout(ELECTION_TIMEOUT, toMessages.ELECTION, destinationID) ;
			network.get(destinationID).tell(msg, self()); // to fix

		}
	}

	public void crash() {
		getContext().become(crashed());
		this.isCrashed = true;
		print(" Crash!!");
	}

	public Cancellable scheduleTimeout(int duration, toMessages type, int watchingNode){
		return getContext().system().scheduler().scheduleOnce(
			Duration.create(duration, TimeUnit.MILLISECONDS), getSelf(),
			new Timeout(type, watchingNode), // the message to send
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
				Update toImplement = waitingList.get(epoch - 1).get(i);
				print("Multicasting last known update");
				multicast(new Synchronization(toImplement));
			}
			i--;
		}
		sendHeartbeat();
		epoch = epoch++;
		sequenceNumber = 0 ;
		isElecting = false ;
		getContext().become(createReceive());
	}

}