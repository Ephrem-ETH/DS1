package ds1_project.Responses;

import java.io.Serializable;

public  class Update implements Serializable {
	
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private final int epochs;
	private final int sequencenum;
	private final int value;
	private final int[] keepSequence;

	public Update(final int epochs, final int sequencenum, final int value) {
		super();
		this.epochs = epochs;
		this.sequencenum = sequencenum;
		this.value = value;
		final int[] keepSequence = { this.epochs, this.sequencenum };
		this.keepSequence = keepSequence ;
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
