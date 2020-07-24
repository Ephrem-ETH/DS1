package ds1_project.Actors;

import ds1_project.Key;
import ds1_project.TwoPhaseBroadcast.*;
import ds1_project.Requests.*;
import ds1_project.Actors.*;
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
	private LinkedHashMap<Key, Integer> waitingList = new LinkedHashMap<Key, Integer>();
	private int epochs = 0;
	private int sequenceNum = 0;
	private int value;
	boolean quorum = false;

	// Do we have majority nodes to ensure the update?
	boolean Quorum(Key key) { // returns true if all voted YES
		if (majorityVoters.containsKey(key)) {
			return majorityVoters.get(key).size() >= QUORUM_SIZE;
		} else {
			return false;
		}
	}

	public Coordinator() {
		super(-1); // the coordinator has the id -1
		sequenceNum = 0;
		super.setCoordinator(true);
	}

	static public Props props() {
		return Props.create(Coordinator.class, () -> new Coordinator());
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder().match(StartMessage.class, this::onStartMessage)
				.match(UpdateRequest.class, this::onUpdateRequest)
				.match(ReadRequest.class, this::OnReadRequest)
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
		int currentSeqNum = this.sequenceNum;
		for (Key lastkey : waitingList.keySet()) {
			if (lastkey.getE() == this.epochs && lastkey.getS() > currentSeqNum) {
				currentSeqNum = lastkey.getS();
			}
		}
		
		print("Broadcasting update with sequence number = " +  (currentSeqNum+1) + ":"+ msg.getValue());
		Update update = new Update(this.epochs, currentSeqNum +1, msg.getValue()); // get max of queue +1 instead
																				// sequencenum +1
		multicast(update);
		waitingList.put(new Key(this.epochs, update.getSequenceNum()), update.getValue());
		
	}


	public void onReceivingAck(Acknowledgement msg) {
		delay();
		Acknowledge ack = (msg).ack;
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
				//crash();
			}
	

		if (Quorum(key) && msg.getRequest_seqnum() > this.sequenceNum ) {

			print("Majority of ACK - Sending WriteOK messages for s=" + msg.getRequest_seqnum());
			multicast(new WriteOk(true, msg.getRequest_epoch(), msg.getRequest_seqnum()));

			for (Map.Entry<Key, Integer> entry : waitingList.entrySet()) {
				if (entry.getKey().equals(key)) {
					this.setValue(entry.getValue());
					print("Updated value :" + this.getValue());
					key = entry.getKey();
					sequenceNum = msg.getRequest_seqnum();
				}
			}
			waitingList.remove(key);
			print("Update removed from queue");
		}
	}

	

}