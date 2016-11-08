package de.voglrobe.ftinterface.server;

import de.voglrobe.ftinterface.io.FtInputs;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.jetty.websocket.api.Session;


/**
 * A Thread implementation to send back ISBs to all registered sessions.
 * <p>
 * The ISBs are an instance of {@link FtInputs} in JSON format. Example:
 * <pre> {
 *     "seqnr": 63,
 *     "di": [false, true, false, false, false, false, false, false],
 *     "ex": 231,
 *     "ey": 255
 *  }</pre>
 * 
 * @author robert
 */
public class ISBThread extends Thread
{
    private static final Logger LOGGER = Logger.getLogger(ISBThread.class.getName());
    
    private volatile boolean stopped;
    private final BlockingQueue<FtInputs> queue;
    
    
    /**
     * Constructor
     * 
     * @param queue The queue for incoming ISBs.
     */
    public ISBThread(final BlockingQueue<FtInputs> queue)
    {
        this.stopped = false;
        this.queue = queue;
    }
    
    /**
     * Sets the terminate flag and returns immediately.
     * The caller should wait until this thread has been terminated.
     */
    public synchronized void terminate()
    {
        this.stopped = true;
        this.queue.clear();        
    }

    @Override
    public void run()
    {
        LOGGER.log(Level.INFO, "Starting ISBThread.");
        while(true)
        {
            try
            {
                FtInputs inputs = queue.poll(5, TimeUnit.SECONDS);
                if (stopped)
                {
                    break;
                }
                if (inputs != null)
                {
                    FtServer.getSessions().forEach((Session session)->
                    {
                        try
                        {
                            session.getRemote().sendString(inputs.toJson());
                        }
                        catch (IOException e)
                        {
                            LOGGER.log(Level.SEVERE, "Error sending ISBS back!", e);
                        }
                    });
                }
            }
            catch (InterruptedException dontcare)
            {
            }
        }
        LOGGER.log(Level.INFO, "ISBThread terminated.");
    }
    
}
