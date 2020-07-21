package ds1_project.Actors;

import ds1_project.TwoPhaseBroadcast.*;
import ds1_project.Requests.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.Props;
import ds1_project.Responses.*;


public class Participant extends Node {
    private ActorRef coordinator;
    private HashMap<Key,Integer> waitingList = new HashMap<Key,Integer>();
    //List<int[]> list = new ArrayList<int[]>(); //transform into hashmap : waiting list of updates - Maybe in Node class as the coordinator will also need it ?

    public Participant(final int id, ActorRef coord) {
        super(id);
        this.coordinator = coord ;
    }

    static public Props props(final int id, ActorRef coord) {
        return Props.create(Participant.class, () -> new Participant(id,coord));
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(StartMessage.class, this::onStartMessage)
                .match(WriteOk.class, this::onWriteOK)
                .match(Update.class, this::onUpdate)
                .match(UpdateRequest.class, this::onUpdateRequest)
                .match(ReadRequest.class, this::OnReadRequest)
                // .match(Timeout.class, this::onTimeout)
                // .match(Recovery.class, this::onRecovery)
                .build();
    }
    public void setCoordinator(ActorRef coord){
        this.coordinator = coord ;
    }

    public ActorRef getCoordinator(){
        return this.coordinator ;
    }

    public void onStartMessage(final StartMessage msg) {
        setGroup(msg);
    }

    public void onUpdateRequest(final UpdateRequest msg) {
        setSender(getSender()) ;
        coordinator.tell(msg, self());
    }

    public void onWriteOK(final WriteOk msg) {
        print("Received WriteOk");

        int epoch = msg.getRequest_epoch();
        int seq_num = msg.getRequest_seqnum();
        
        for (Map.Entry<Key,Integer> entry : waitingList.entrySet()){
            if (entry.getKey().getE()==epoch && entry.getKey().getS()==seq_num){
                this.setValue(entry.getValue()) ;
                print("Updated value :"+this.getValue());
                waitingList.remove(entry.getKey()) ;
                print("Update removed from queue");
            }
        }
    }

	public void onUpdate(Update msg) {      // Update propagates from coordinator
        
        Key request_id = new Key(msg.getEpochs(),msg.getSequenceNum()) ;
        waitingList.put(request_id, msg.getValue()) ;
        print("queued value"+waitingList.get(request_id)) ;
        Acknowledgement acknowledgement = new Acknowledgement(Acknowledge.ACK, msg.getEpochs(), msg.getSequenceNum()) ;
        this.print(acknowledgement.toString());
        coordinator.tell(acknowledgement, getSelf());
        this.print("ACK sent");
	}
	
}