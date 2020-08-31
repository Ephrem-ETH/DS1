package ds1_project.Responses;

import java.io.Serializable;

import ds1_project.TwoPhaseBroadcast.Acknowledge;

public class Acknowledgement implements Serializable {
	/**
	 * Acknowledgement : Participants send acknowledgement after they receive
	   an update request from the coordinator. If the number of collected ACKs
	   by the coordinator exceeds the quorum, the update is implemented.

	   This function takes the coordinates of an update (epoch, sequence) and the
	   sender ID as argument. It also supports the replying of a NACK in case the
	   node is alive but cannot support the update, but this case is not implemented.
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
