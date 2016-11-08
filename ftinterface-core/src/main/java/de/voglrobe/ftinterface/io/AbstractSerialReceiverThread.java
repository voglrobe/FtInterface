package de.voglrobe.ftinterface.io;

import gnu.io.SerialPort;

/**
 * The abstract base class for a thread to receive incoming data from the serial port.
 * The thread must be implemented in such a way that it can be terminted by calling {@link #terminate()}.
 * 
 * @author robert
 */
public abstract class AbstractSerialReceiverThread extends Thread
{
    protected final SerialPort serPort;
    
    /**
     * Constructor.
     * 
     * @param serPort A {@link SerialPort} object connected to the interface adapter and ready to use.
     */
    public AbstractSerialReceiverThread(final SerialPort serPort)
    {
        this.serPort = serPort;
    }
    
    @Override
    abstract public void run();

    /**
     * Must be called at the end of the life-cycle to request this thread to terminate.
     * The caller should join with this thread to await it's termination.
     */
    abstract public void terminate();
}
