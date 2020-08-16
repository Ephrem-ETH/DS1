package ds1_project.Responses;

import java.io.Serializable;

import ds1_project.TwoPhaseBroadcast.Acknowledge;
import ds1_project.Requests.ElectionMessage;

public class ElectionAck implements Serializable {
	
	
	public final Acknowledge ack;

	private final ElectionMessage msg;
	private final int sender_id;
	

	public ElectionAck(Acknowledge ack, ElectionMessage msg,int sender_id) {
		super();
		this.ack = ack;
		this.msg = msg;
		this.sender_id = sender_id;
	}


	public int getSender_id() {
		return sender_id;
	}
	public ElectionMessage getMsg() {
		return msg;
	}

	

    
}