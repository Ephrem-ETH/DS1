package ds1_project.Responses;

import java.io.Serializable;

public class WriteOk implements Serializable {

	/**
	 * WriteOK : A message sent by the coordinator to other participants when
	 * a decision to implement an Update is taken. It takes as argument the
	 * coordinates of the Update (epoch, sequence number), a boolean (theoritically
	 * allowing to discard the Update if needed - unused feature) and the sender
	 * ID.
	 * 
	 * Upon reception, participant update their value and the sequence number to match
	 * the implemented Update and mark the Update as validated in the history.
	 */

	private static final long serialVersionUID = 1L;
	boolean writeOk;
	private final int request_epoch;
	private final int request_seqnum;
	private final int sender_id ;

	public WriteOk(final boolean writeOk, final int request_epoch, final int request_seqnum, int id) {
		this.writeOk = writeOk;
		this.request_epoch = request_epoch;
		this.request_seqnum = request_seqnum;
		this.sender_id = id ;
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
}
