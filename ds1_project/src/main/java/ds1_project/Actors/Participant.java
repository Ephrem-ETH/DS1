package ds1_project.Actors;

import ds1_project.TwoPhaseBroadcast.*;
import ds1_project.Requests.*;
import akka.actor.ActorRef;
import akka.actor.Props;


public class Participant extends Node {
    ActorRef coordinator;

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
                .match(UpdateRequest.class, this::onUpdateRequest).match(ReadRequest.class, this::OnReadRequest)
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
}