package ds1_project.Responses;

import java.io.Serializable;

public class Heartbeat implements Serializable {

    /**
     * Heartbeat message : Empty message sent periodically to
     * participants by the coordinator, to make sure it is still
     * alive.
     * Upon reception, a timer is reset.
     */
    private static final long serialVersionUID = 1L;

    public Heartbeat() {
        
    }
    
}