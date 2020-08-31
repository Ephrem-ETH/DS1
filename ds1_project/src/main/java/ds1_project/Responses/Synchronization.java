package ds1_project.Responses;

import java.io.Serializable;

public class Synchronization implements Serializable {
    
    /**
     * Synchronization: Message sent by the newly elected coordinator
     * to initiate the termination protocol. It takes as arguments the
     * last stable update that should be implemented.
     * 
     * Upon reception, participants implement this stable update, and if
     * the last acknowledged update is more recent than the stable update,
     * they send this update to the coordinator which will decide if it
     * has to be implemented.
     */

    private static final long serialVersionUID = 1L;
    private Update update;
    private int coordinator_id ;


    public Synchronization (Update update){
        this.update = update ;
    }

    public Update getUpdate() {
        return update;
    }
}