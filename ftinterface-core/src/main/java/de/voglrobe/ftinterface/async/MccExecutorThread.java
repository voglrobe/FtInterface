package de.voglrobe.ftinterface.async;

import de.voglrobe.ftinterface.exceptions.ComException;
import de.voglrobe.ftinterface.io.FtOutput;
import de.voglrobe.ftinterface.io.FtSerialPortSenderReceiver;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class implements a Thread that executes the given MCC until it's termination.
 * This Thread is not reusable. Once terminated it cannot be restarted.
 * 
 * @author vlr
 */
public class MccExecutorThread extends Thread
{
    private static final Logger LOGGER = Logger.getLogger(MccExecutorThread.class.getName());
    
    private final FtSerialPortSenderReceiver ftSenderReceiver;
    
    private volatile FtOutput mcc;    
    private volatile boolean stopped;
    private volatile boolean paused;
    
    
    /**
     * Constructor.
     * 
     * @param ftSenderReceiver An object to access of the serial interface.
     */
    public MccExecutorThread(final FtSerialPortSenderReceiver ftSenderReceiver)
    {
        this.ftSenderReceiver = ftSenderReceiver;
        this.paused = true;
        this.mcc = null;
        this.stopped = false;
    }
    
    /**
     * Pauses the Executor from sending MCCs.
     */
    public synchronized void pause()
    {
        this.paused = true;
    }
    
    /**
     * Proceeds sending the given MCC. Does nothing if mcc argument is NULL.
     * 
     * @param mcc The MCC to send recurrently. 
     */
    public synchronized void activate(final FtOutput mcc)
    {
        this.mcc = mcc;
        this.paused = false;
    }
    
    /**
     * Asks the Thread to terminate and returns immediately.
     * The caller should sync with the Thread and await it's termination.
     */
    public synchronized void terminate()
    {
        LOGGER.log(Level.INFO, "Stopping MccExecutorThread...");
        this.mcc = null;
        this.stopped = true;
    }
    
    @Override
    public void run()
    {
        if (ftSenderReceiver == null || stopped)
        {
            return;
        }
        
        do
        {
            try
            {
                if (!paused &&  mcc != null)
                {
                    ftSenderReceiver.send(mcc.bytes());
                }
                Thread.sleep(200);
            }
            catch(InterruptedException dontcare)
            {
            }
            catch(ComException e)
            {
                LOGGER.log(Level.SEVERE, "The MccExecutorThread dies unexpectedly.", e);
                break;
            }
        } while(!stopped);
        LOGGER.log(Level.INFO, "MccExecutorThread stopped.");
    }


}
