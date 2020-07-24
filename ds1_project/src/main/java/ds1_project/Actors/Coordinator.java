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

    public void onStartMessage(final StartMessage msg) { /* Start */
        setGroup(msg);
        print("Sending vote request");
        // multicast(new UpdateRequest()); // to correct
        // multicastAndCrash(new VoteRequest(), 3000);
        // setTimeout(VOTE_TIMEOUT);
        // crash(5000);
    }

    public void onUpdateRequest(final UpdateRequest msg) {
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

    public void onReceivingAck(Acknowledgement msg) {
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
                    sequenceNum = msg.getRequest_seqnum();
                }
            }
            waitingList.remove(key);
        }
    }
}