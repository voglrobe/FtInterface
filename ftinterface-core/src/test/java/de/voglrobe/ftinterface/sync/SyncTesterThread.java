package de.voglrobe.ftinterface.sync;

import de.voglrobe.ftinterface.io.FtInputs;

/**
 *
 * @author vlr
 */
public class SyncTesterThread extends Thread
{
    private ISyncCallback callback;
    private volatile boolean stopped;
    
    private static void snore(final long duration)
    {
        try
        {
            Thread.sleep(duration);
        }
        catch(InterruptedException dontcare)
        {
        }
    }
    
    /**
     * Constructor.
     * 
     * @param callback 
     */
    public SyncTesterThread(final ISyncCallback callback)
    {
        this.callback = callback;
        this.stopped = false;
    }
    
    /**
     * Terminate Thread.
     */
    public void terminate()
    {
        this.stopped = true;
        this.callback = null;
    }
    
    @Override
    public void run()
    {
        while (!stopped)
        {
            for (int seqNr = 0; seqNr < 64; seqNr++)
            {
                if (stopped)
                {
                    break;
                }
                
                // snore(5000L);
                // snore(ThreadLocalRandom.current().nextInt(1, 6) * 1000L);
                
                FtInputs inputs = new FtInputs(seqNr, 0, 0, 0);
                if (callback != null)
                {
                    callback.onReceived(inputs);
                }
            }
        }
        System.out.println("SyncTesterThread stopped.");
    }
    
}
