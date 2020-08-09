package ds1_project.Responses;

import java.io.Serializable;

import ds1_project.TwoPhaseBroadcast.Acknowledge;

//participants send acknowledgement when the coordinator sends update request 
public class Acknowledgement implements Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public final Acknowledge ack;

	private final int request_epoch;
	private final int request_seqnum;

	private final int sender_id;

	public Acknowledgement(final Acknowledge ack, final int request_epoch, final int request_seqnum, int sender_id) {
		super();
		this.ack = ack;
		this.request_epoch = request_epoch;
		this.request_seqnum = request_seqnum;
		this.sender_id = sender_id ;
	}

	public int getRequest_epoch() {
		return request_epoch;
	}

	public int getRequest_seqnum() {
		return request_seqnum;
	}

	public int getSender_id() {
		return sender_id;
	}

	@Override
	public String toString() {
		return "epoch : " + this.request_epoch + " ; seqnum : " + this.request_seqnum;
	}
}
