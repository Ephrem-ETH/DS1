package ds1_project.Requests;

import java.io.Serializable;

public  class Update implements Serializable {
	
	public int epochs;
	public int sequencenum;
	public int value;
	public int [] keepSequence;
	
	public Update(int epochs, int sequencenum, int value) {
		super();
		this.epochs = epochs;
		this.sequencenum = sequencenum;
		this.value = value;
		this.keepSequence[0] = this.epochs;
		this.keepSequence[1] = this.sequencenum;
	}
	
}
