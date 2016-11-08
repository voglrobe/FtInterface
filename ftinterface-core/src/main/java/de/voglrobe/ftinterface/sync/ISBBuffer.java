package de.voglrobe.ftinterface.sync;

import de.voglrobe.ftinterface.io.FtInputs;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A ringbuffer to hold up to 64 ISBs.
 * 
 * @author robert
 */
public class ISBBuffer
{
    private int seqNr;
    
    private final ConcurrentMap<Integer, FtInputs> inputMap = new ConcurrentHashMap<>();
    
    /**
     * Constructor.
     */
    public ISBBuffer()
    {
        this.seqNr = 0;
    }
    
    /**
     * Returns the next free sequence number.
     * 
     * @return The next free sequence number from the cyclic range [0, 63].
     * Starts from 0 again if the last sequence number was 63.
     */
    public synchronized int requestSeqNr()
    {
        int ret = seqNr;
        this.seqNr = (seqNr + 1) % 63;
        if (inputMap.containsKey(ret))
        {
            inputMap.remove(ret);
        }
        return ret;
    }
    
    /**
     * Stores a single set of ISBs with the given sequence number.
     * 
     * @param seqNr The sequence number.
     * @param input The ISBs to store.
     */
    public void store(final int seqNr, final FtInputs input)
    {
        this.inputMap.put(seqNr, input);
    }
    
    /**
     * Removes from the ringbuffer and returns a single set of ISBs corresponding to the given sequence number.
     * 
     * @param seqNr The sequence number.
     * @return The input data or NULL if not present.
     */
    public FtInputs pop(final int seqNr)
    {
        return inputMap.get(seqNr);
    }
    
    /**
     * Checks whether the ringbuffer contains the given sequence number.
     * 
     * @param seqNr The sequence number to check.
     * @return TRUE if present, otherwise FALSE.
     */
    public boolean contains(final int seqNr)
    {
        return inputMap.containsKey(seqNr);
    }
    
}
