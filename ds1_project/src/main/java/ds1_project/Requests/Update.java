package ds1_project.Requests;

import java.io.Serializable;

public  class Update implements Serializable {
	
	private int epochs;
	private int sequencenum;
	private int value;
	private int [] keepSequence;
	
	public Update(int epochs, int sequencenum, int value) {
		super();
		this.epochs = epochs;
		this.sequencenum = sequencenum;
		this.value = value;
		this.keepSequence[0] = this.epochs;
		this.keepSequence[1] = this.sequencenum;
	}

	public int getEpochs(){
		return this.epochs ;
	}

	public int getSequenceNum(){
		return this.sequencenum;
	}

	public int getValue(){
		return this.value;
	}

	public int[] getKeepSequence(){
		return this.keepSequence ;
	}
	
}
