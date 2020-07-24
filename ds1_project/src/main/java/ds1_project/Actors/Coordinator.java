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
	public static int sequence_num;
	public static final int QUORUM_SIZE = ds1_project.TwoPhaseBroadcast.QUORUM_SIZE;
	private HashMap<Key, Integer> waitingList = new HashMap<Key, Integer>();
	int epochs = 0;
	int sequenceNum = 0;
	int value;
	boolean quorum = false;

	/*
	 * // here all the nodes that sent YES are collected private final HashMap<Key,
	 * HashSet<ActorRef>> majorityVoters = new HashMap<Key, HashSet<ActorRef>>();
	 * public static int sequence_num; public static final int QUORUM_SIZE =
	 * ds1_project.TwoPhaseBroadcast.QUORUM_SIZE; private HashMap<Key, Integer>
	 * waitingList = new HashMap<Key, Integer>(); int epochs = 0; int sequenceNum =
	 * 0;
	 */



	/*
	 * // Do we have majority nodes to ensure the update? boolean Quorum(Key key) {
	 * // returns true if all voted YES return majorityVoters.get(key).size() >=
	 * QUORUM_SIZE; }
	 */

    // Do we have majority nodes to ensure the update?
    boolean Quorum(Key key) { // returns true if all voted YES
        if (majorityVoters.containsKey(key)) {
            return majorityVoters.get(key).size() >= QUORUM_SIZE;
        } else {
            return false;
        }
    }


	public Coordinator(int quorum_size) {
		super(-1); // the coordinator has the id -1
		sequence_num = 0;
		super.setCoordinator(true);
	}

	static public Props props() {
		return Props.create(Coordinator.class, () -> new Coordinator(QUORUM_SIZE));
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder().match(StartMessage.class, this::onStartMessage)
				.match(UpdateRequest.class, this::onUpdateRequest).match(ReadRequest.class, this::OnReadRequest)
				.match(Acknowledgement.class, this::onReceivingAck)
				// .match(Timeout.class, this::onTimeout)
				// .match(DecisionRequest.class, this::onDecisionRequest)
				.build();
	}

	public int getSequenceNum() {
		return this.sequenceNum;
	}

	public int getEpoch() {
		return this.epochs;
	}

	public void increaseEpoch() {
		this.epochs = this.epochs + 1;
	}

	/*
	 * @Override public void onRecovery(Recovery msg) {
	 * getContext().become(createReceive()); if (!writeOk_Sent && quorum) {
	 * print("recovering, writeOk is not sent but reaches quorum"); multicast(new
	 * WriteOk(true, msg.getEpochs(), msg.getSeqnum()));
	 * 
	 * } else if(!writeOk_Sent && !quorum) {
	 * print("recovering, quorum is not reached and writeOk is also not sent"); }
	 * else if(!updateBroadcasted){ print("recovering,  update is not broadcasted");
	 * Update update = new Update(this.epochs, this.sequenceNum + 1,
	 * msg.getValue()); multicast(update); waitingList.put(new
	 * Key(update.getEpochs(), update.getSequenceNum()), update.getValue());
	 * sequenceNum = sequenceNum + 1;
	 * 
	 * } else { print(" Not recovering"); } }
	 */
	public void onStartMessage(final StartMessage msg) { /* Start */
		setGroup(msg);
		// print("Sending vote request");
		// multicast(new UpdateRequest()); // to correct
		// multicastAndCrash(new VoteRequest(), 3000);
		// setTimeout(VOTE_TIMEOUT);
		// crash(5000);
	}
	/*
	 *
	 * system .scheduler() .scheduleOnce( Duration.ofMillis(50), new Runnable() {
	 * 
	 * @Override public void run() { multicast(new Heartbeat()); } },
	 * system.dispatcher());
	 * 
	 */

	public void crash() {
		getContext().become(crashed());
		print(" Crash!!");
	}
    public void onUpdateRequest(final UpdateRequest msg) {
    	delay();
        int currentSeqNum = this.sequenceNum ;
        for (Key lastkey : this.waitingList.keySet()){
            if (lastkey.getE()==this.epochs && lastkey.getS()>currentSeqNum){
                currentSeqNum = lastkey.getS() ;
            }
        }
        print("Broadcasting update with sequence number = "+ (currentSeqNum+1));
        Update update = new Update(this.epochs, currentSeqNum+1, msg.getValue()); //get max of queue +1 instead sequencenum +1
        multicast(update);
        waitingList.put(new Key(update.getEpochs(), update.getSequenceNum()), update.getValue());
    }



	/*
	 * public void onUpdateRequest(final UpdateRequest msg) { delay();
	 * print("Boadcasting update"); Update update = new Update(this.epochs,
	 * this.sequenceNum + 1, msg.getValue()); // if (id == -1) { delay(3000);
	 * return;} multicast(update); waitingList.put(new Key(update.getEpochs(),
	 * update.getSequenceNum()), update.getValue()); sequenceNum = sequenceNum + 1;
	 * }
	 */

	/*
	 * public void onReceivingAck(Acknowledgement msg) { delay(); Acknowledge ack =
	 * (msg).ack; Key key = new Key(msg.getRequest_epoch(),
	 * msg.getRequest_seqnum()); HashSet<ActorRef> voters = new HashSet<>(); boolean
	 * flag = false;
	 */


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

		if (ack == Acknowledge.ACK) {
			for (Map.Entry<Key, HashSet<ActorRef>> entry : majorityVoters.entrySet()) {
				if (entry.getKey().getE() == key.getE() && entry.getKey().getS() == key.getS()) {
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



			// print("Received WriteOKs :" + majorityVoters.get(key).size());
		}

		

        if (Quorum(key) && msg.getRequest_seqnum()>this.sequenceNum) {
            print("Majority of ACK - Sending WriteOK messages for s="+msg.getRequest_seqnum());
            multicast(new WriteOk(true, msg.getRequest_epoch(), msg.getRequest_seqnum()));
            for (Map.Entry<Key, Integer> entry : waitingList.entrySet()) {
                if (entry.getKey().equals(key)) {
                    this.setValue(entry.getValue());
                    print("Updated value :" + this.getValue());
                    key = entry.getKey() ;
                    //waitingList.remove(entry.getKey());
                    //print("Update removed from queue");
                    sequenceNum = sequenceNum + 1;
                }
            }
            waitingList.remove(key);
        }
    }

}