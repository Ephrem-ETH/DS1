package ds1_project.Requests;

import java.io.Serializable;

import ds1_project.TwoPhaseBroadcast.Acknowledge;

//participants send acknowledgement when the coordinator sends update request 
public class Acknowledgement implements Serializable {
	public final Acknowledge ack;

	private int[] request_id ;

	public Acknowledgement(Acknowledge ack, int [] request_id) {
		super();
		this.ack = ack;
		this.request_id[0] = request_id[0] ;
		this.request_id[1] = request_id[1] ;
	}

}
