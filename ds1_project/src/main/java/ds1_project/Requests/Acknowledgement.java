package ds1_project.Requests;

import java.io.Serializable;

import ds1_project.TwoPhaseBroadcast.Acknowledge;

//participants send acknowledgement when the coordinator sends update request 
public class Acknowledgement implements Serializable {
	public final Acknowledge ack;

	public Acknowledgement(Acknowledge ack) {
		super();
		this.ack = ack;
	}

}
