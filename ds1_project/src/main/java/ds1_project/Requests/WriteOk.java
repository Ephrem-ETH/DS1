package ds1_project.Requests;

import java.io.Serializable;

public class WriteOk implements Serializable{
	boolean writeOk;
	public WriteOk(boolean writeOk) {
		this.writeOk = writeOk;
	}
}
