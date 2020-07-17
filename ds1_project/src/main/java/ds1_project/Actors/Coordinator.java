package ds1_project.Actors;

import ds1_project.TwoPhaseBroadcast.*;
import ds1_project.Requests.*;
import ds1_project.Actors.*;
import akka.actor.ActorRef;
import akka.actor.AbstractActor;
import akka.actor.ActorSystem;
import akka.actor.Props;
import scala.None;
import scala.concurrent.duration.Duration;
import ds1_project.Responses.*;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.lang.Thread;
import java.util.Collections;
import java.util.HashMap;
import java.io.IOException;

public class Coordinator extends Node {

    // here all the nodes that sent YES are collected
    private final Set<ActorRef> majorityVoters = new HashSet<>();
    public static int sequence_num;
    public static final int QUORUM_SIZE = ds1_project.TwoPhaseBroadcast.QUORUM_SIZE;
    int epochs = 0;
    int sequenceNum = 0;
    int value;

    // Do we have majority nodes to ensure the update?
    boolean Quorum() { // returns true if all voted YES
        return majorityVoters.size() >= QUORUM_SIZE;
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

    public void onStartMessage(final StartMessage msg) { /* Start */
        setGroup(msg);
        print("Sending vote request");
        // multicast(new UpdateRequest()); // to correct
        // multicastAndCrash(new VoteRequest(), 3000);
        // setTimeout(VOTE_TIMEOUT);
        // crash(5000);
    }

    public void onUpdateRequest(final UpdateRequest msg) {
        System.out.println("Boadcasting update");
        multicast(msg);
    }

    public void onReceivingAck(Acknowledgement msg) {

        Acknowledge ack = (msg).ack;

        if (ack == Acknowledge.ACK) {
            majorityVoters.add(getSender());
        }

        if (Quorum()) {
            System.out.println("Majority of ACK - Sending WriteOK messages");
            multicast(new WriteOk(true));
        }

        System.out.println("No majority - Aborting update");

    }
}