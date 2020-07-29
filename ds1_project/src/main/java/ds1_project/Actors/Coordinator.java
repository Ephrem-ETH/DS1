package ds1_project.Actors;

import ds1_project.Key;
import ds1_project.Requests.*;
import ds1_project.Actors.*;
import ds1_project.TwoPhaseBroadcast.*;
import akka.actor.ActorRef;
import akka.actor.AbstractActor;
import akka.actor.ActorSystem;
import akka.actor.Props;
import scala.None;
import scala.annotation.elidable;
import scala.concurrent.duration.Duration;
import ds1_project.Responses.*;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.lang.Thread;
import java.util.Collections;
import java.util.HashMap;
import java.io.IOException;

public class Coordinator extends Node {

	// here all the nodes that sent YES are collected
	private final HashMap<Key, HashSet<ActorRef>> majorityVoters = new HashMap<Key, HashSet<ActorRef>>();
	public static final int QUORUM_SIZE = ds1_project.TwoPhaseBroadcast.QUORUM_SIZE;
	private List<ArrayList<Update>> waitingList = new ArrayList<ArrayList<Update>>();
	private int epochs = 0;
	private int sequenceNum = 0;
	boolean quorum = false;
	private boolean newUpdate = false;

	// Do we have majority nodes to ensure the update?
	boolean Quorum(Key key) { // returns true if all voted YES
		if (majorityVoters.containsKey(key)) {
			waitingList.get(key.getE()).get(key.getS()).setValidity(true);
			newUpdate = true ;
			return majorityVoters.get(key).size() >= QUORUM_SIZE;
		} else {
			return false;
		}
	}

	public Coordinator() {
		super(0); // the coordinator has the id 0
		sequenceNum = 0;
		super.setCoordinator(true);
		waitingList.add(new ArrayList<Update>());

	}

	static public Props props() {
		return Props.create(Coordinator.class, () -> new Coordinator());
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder().match(StartMessage.class, this::onStartMessage)
				.match(UpdateRequest.class, this::onUpdateRequest).match(ReadRequest.class, this::OnReadRequest)
				.match(Acknowledgement.class, this::onReceivingAck)
				// .match(Timeout.class, this::onTimeout)
				.build();
	}

	public int getSequenceNum() {
		return this.sequenceNum;
	}

	public int getEpoch() {
		return this.epochs;
	}

	public void increaseSequenceNum() {
		this.sequenceNum = this.sequenceNum + 1;
	}

	public void increaseEpoch() {
		this.epochs = this.epochs + 1;
	}

	public void onStartMessage(final StartMessage msg) { /* Start */
		setGroup(msg);
		;
	}

	public void crash() {
		getContext().become(crashed());
		print(" Crash!!");
	}

	public void onUpdateRequest(final UpdateRequest msg) {
		delay();
		int currentSeqNum = waitingList.get(epochs).size() ;
		print("Broadcasting update with sequence number = " + (currentSeqNum) + ":" + msg.getValue());
		Update update = new Update(this.epochs, currentSeqNum, msg.getValue()); // get max of queue +1 instead
																					// sequencenum +1
		multicast(update);
		waitingList.get(epochs).add(update);

	}

	public void onReceivingAck(Acknowledgement msg) {
		// delay();
		Acknowledge ack = (msg).ack;
		//print("Received ACK from "+sender()+" with seqnum "+msg.getRequest_seqnum()) ;
		Key key = new Key(msg.getRequest_epoch(), msg.getRequest_seqnum());
		HashSet<ActorRef> voters = new HashSet<>();
		boolean flag = false;

		if (ack == Acknowledge.ACK && msg.getRequest_seqnum() > this.sequenceNum) {

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
			int i = waitingList.get(epochs).size()-1;
			boolean found = false;
			while (i >= 0 && !found) {
				if (waitingList.get(epochs).get(i).isValidated() && i>sequenceNum ) {
					found = true;
					toImplement = waitingList.get(epochs).get(i);
				}
				i-- ;
			}
			if (found) {
				print("Majority of ACK - Sending WriteOK messages for s=" + toImplement.getSequenceNum());
				multicast(new WriteOk(true, toImplement.getEpochs(), toImplement.getSequenceNum()));
				sequenceNum = toImplement.getSequenceNum() ;
				/*
				 * for (Map.Entry<Key, Integer> entry : waitingList.entrySet()) { if
				 * (entry.getKey().equals(key)) { this.setValue(entry.getValue());
				 * print("Updated value :" + this.getValue()); key = entry.getKey(); sequenceNum
				 * = msg.getRequest_seqnum(); } }
				 */
				this.setValue(toImplement.getValue());
				print("Updated value :" + this.getValue());
				newUpdate = false ;
			}
		}
	}

}