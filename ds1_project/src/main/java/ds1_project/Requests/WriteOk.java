package ds1_project.Requests;

import java.io.Serializable;

public class WriteOk implements Serializable{

	boolean writeOk;
	private int[] request_id ;
	

	public WriteOk(boolean writeOk, int[] request_id) {
		this.writeOk = writeOk;
		this.request_id[0] = request_id[0] ;
		this.request_id[1] = request_id[1] ;
	}

	public int[] getRequest_id() {
		return request_id;
	}
}
