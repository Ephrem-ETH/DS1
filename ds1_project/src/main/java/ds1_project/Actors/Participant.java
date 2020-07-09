package ds1_project.Actors;

import ds1_project.TwoPhaseBroadcast.*;
import ds1_project.Requests.*;

import java.util.ArrayList;
import java.util.List;

import akka.actor.ActorRef;
import akka.actor.Props;
import ds1_project.Responses.*;


public class Participant extends Node {
    ActorRef coordinator;
    int myvalue;

    public Participant(final int id) {
        super(id);
    }

    static public Props props(final int id) {
        return Props.create(Participant.class, () -> new Participant(id));
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(StartMessage.class, this::onStartMessage)
                .match(UpdateResponse.class, this::onUpdateResponse)
                .match(Update.class, this::onUpdate)
                .match(UpdateRequest.class, this::onUpdateRequest)
                .match(ReadRequest.class, this::OnReadRequest)
                // .match(Timeout.class, this::onTimeout)
                // .match(Recovery.class, this::onRecovery)
                .build();
    }

    public void onStartMessage(final StartMessage msg) {
        setGroup(msg);
    }

    public void onUpdateRequest(final UpdateRequest msg) {
        setSender(getSender());
        coordinator.tell(msg, self());

    }

    public void onUpdateResponse(final UpdateResponse msg) {
        if ((msg).WRITEOK) {
            this.getNodeSender().tell(msg, getSelf());
        }
    }
	public void onUpdate(Update msg) {      // Update propagates from coordinator
		
		List<int[]> list = new ArrayList<int[]>();
         
		if(!list.isEmpty()) {
			
			for(int i =0; i<= list.size();i++) {
				if ((list.get(i)[0]< msg.epochs)||(list.get(i)[0]== msg.epochs && list.get(i)[1]< msg.sequencenum)) {
					this.myvalue = (msg).value;
				}
			}
		}
		// keep the epochs and sequence number and then sends acknowledgement to coordinator
		list.add((msg).keepSequence);
		this.coordinator.tell(new Acknowledgement(Acknowledge.ACK), getSelf());
	}
	
}