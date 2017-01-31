package de.voglrobe.ftinterface.async;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is a Singleton, that helps to synchronize the execution of blocking and non-blocking send()-Methods.
 * 
 * @author robert
 */
public enum SequenceLockHelper
{
    INSTANCE;
    
    private static final Logger LOGGER = Logger.getLogger(SequenceLockHelper.class.getName());
    
    // list of Sequence Numbers of uncompleted MCCs (waiting for ISB).
    private final List<Integer> processingSeqNrs = new ArrayList<>();
    
    /**
     * Constructor.
     */
    private SequenceLockHelper()
    {
    }
    
    /**
     * Adds a Sequence Number to the list of unconpleted MCCs.
     * 
     * @param seqNr The Sequence Number to add.
     */
    public void addSeqNr(final int seqNr)
    {
        if (!processingSeqNrs.contains(seqNr))
        {
            synchronized(processingSeqNrs)
            {
                processingSeqNrs.add(seqNr);
            }
        }        
    }
    
    /**
     * Removes a Sequence Number because the MCC has been finished (by incoming ISB).
     * 
     * @param seqNr The Sequence Number to remove.
     */
    public void removeSeqNr(final int seqNr)
    {
        if (processingSeqNrs.contains(seqNr))
        {
            this.processingSeqNrs.remove(Integer.valueOf(seqNr));
            synchronized(processingSeqNrs)
            {
                processingSeqNrs.notify();                    
            }
        }        
    }
    
    /**
     * Blocks until all MCCs are completed, i.e. confirmed by received ISB.
     */
    public void check()
    {
        while(!processingSeqNrs.isEmpty())
        {
            try
            {
                synchronized(processingSeqNrs)
                {
                    processingSeqNrs.wait();
                }
            }
            catch (InterruptedException e)
            {
            }
        }
        LOGGER.log(Level.INFO, "SequenceLockHelper.removeSeqNr(): Sync-Lock is FREE.");
    }
    
    /**
     * Removes all registered Sequence Numbers and frees the Sync-Lock.
     */
    public void flush()
    {
        this.processingSeqNrs.clear();
        synchronized(processingSeqNrs)
        {
            processingSeqNrs.notify();            
        }
    }
    
    
    
}
