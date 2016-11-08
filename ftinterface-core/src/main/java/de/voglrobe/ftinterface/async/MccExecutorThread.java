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
    
    private final FtOutput mcc;
    private final FtSerialPortSenderReceiver ftSenderReceiver;
    
    private volatile boolean stopped;
    
    
    /**
     * Constructor.
     * 
     * @param ftSenderReceiver An object to access of the serial interface.
     * @param mcc The MCC to execute.
     */
    public MccExecutorThread(final FtSerialPortSenderReceiver ftSenderReceiver, final FtOutput mcc)
    {
        this.ftSenderReceiver = ftSenderReceiver;
        this.stopped = false;
        this.mcc = mcc;
    }
    
    /**
     * Asks the Thread to terminate and returns immediately.
     * The caller should sync with the Thread and await it's termination.
     */
    public synchronized void terminate()
    {
        LOGGER.log(Level.INFO, "Stopping MccExecutorThread...");
        this.stopped = true;
    }
    
    @Override
    public void run()
    {
        if (ftSenderReceiver == null || mcc == null || stopped)
        {
            return;
        }
        
        do
        {
            try
            {
                ftSenderReceiver.send(mcc.bytes());
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
