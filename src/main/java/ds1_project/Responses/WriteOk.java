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

	public WriteOk(final boolean writeOk, final int request_epoch, final int request_seqnum) {
		this.writeOk = writeOk;
		this.request_epoch = request_epoch;
		this.request_seqnum = request_seqnum;
	}

	public int getRequest_epoch() {
		return request_epoch;
	}

	public int getRequest_seqnum() {
		return request_seqnum;
	}
}
