package ds1_project.Responses;

import java.io.Serializable;

public class WriteOk implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	boolean writeOk;
	private final int request_epoch;
	private final int request_seqnum;
	private final int sender_id ;
	private final boolean isElectionInstalment ;

	public WriteOk(final boolean writeOk, final int request_epoch, final int request_seqnum, int sender_id, boolean isNewEpoch) {
		this.writeOk = writeOk;
		this.request_epoch = request_epoch;
		this.request_seqnum = request_seqnum;
		this.sender_id = sender_id ;
		this.isElectionInstalment = isNewEpoch ;
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

	public boolean isElectionInstalment (){
		return this.isElectionInstalment ;
	}
}
