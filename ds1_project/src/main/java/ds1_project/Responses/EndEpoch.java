package ds1_project.Responses;

import java.io.Serializable;

public class EndEpoch implements Serializable {

    /**
     EndEpoch : Empty message sent by a newly elected coordinator to all
     other nodes in the network to terminate the epoch. Upon reception,
     participants switch to standard receiving mode and transition to
     the next epoch.
     */
    private static final long serialVersionUID = 1L;

    public EndEpoch() {
        
    }
}