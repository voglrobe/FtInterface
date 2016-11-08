package de.voglrobe.ftinterface.sync;

import de.voglrobe.ftinterface.io.FtInputs;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author vlr
 */
public class SyncTester
{
    private final CyclicBarrier barrier;
    private SyncTesterThread thread;
    private volatile int lastSeqNr;
    
    /**
     * 
     * @return 
     */
    public static SyncTester newInstance()
    {
        SyncTester ret = new SyncTester();
        ret.init();
        return ret;
    }
    
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
     * Hidden constructor.
     */
    private SyncTester()
    {
        this.barrier = new CyclicBarrier(2);
    }
    
    /**
     * Destructor.
     */
    public void destroy()
    {
        if (thread != null)
        {
            thread.terminate();
            try
            {
                thread.join();
            }
            catch (InterruptedException dontcare)
            {
            }
        }
    }
    
    /**
     * 
     */
    private void init()
    {
        this.lastSeqNr = -1;
        thread = new SyncTesterThread((final FtInputs inputs)->
        {
            lastSeqNr = inputs.getSeqNr();
            try
            {
                System.out.println("Waiting (receive)...");
                barrier.await(3, TimeUnit.SECONDS);
                System.out.println("...proceeding (receive)");
            }
            catch(TimeoutException e)
            {
                barrier.reset();
                System.err.println("TimeoutException in callback.");
            }
            catch (InterruptedException | BrokenBarrierException e)
            {
                System.err.println("Exception on CyclicBarrier.await() in callback.");
            }
        });
        thread.start();
    }
    
    /**
     * 
     * @return 
     */
    public int send()
    {
        // snore(ThreadLocalRandom.current().nextInt(1, 6) * 1000L);
        snore(10000L);
        
        try
        {
            System.out.println("Waiting (send)...");
            barrier.await(3, TimeUnit.SECONDS);
            System.out.println("...proceeding (send)");
            barrier.reset();
            return lastSeqNr;
        }
        catch (TimeoutException e)
        {
            System.err.println("TimeoutException.");
            barrier.reset();
            return -1;
        }
        catch (InterruptedException | BrokenBarrierException e)
        {
            System.err.println("Exception on CyclicBarrier.await() in send().");
            return -1;
        }
    }
    
}
