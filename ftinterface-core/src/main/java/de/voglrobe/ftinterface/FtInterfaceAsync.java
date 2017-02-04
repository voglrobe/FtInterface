package de.voglrobe.ftinterface;

import de.voglrobe.ftinterface.async.MccExecutorThread;
import de.voglrobe.ftinterface.async.SequenceLockHelper;
import de.voglrobe.ftinterface.exceptions.ComException;
import de.voglrobe.ftinterface.io.FtInputsFlags;
import de.voglrobe.ftinterface.io.FtOutput;
import de.voglrobe.ftinterface.io.FtSerialPortSenderReceiver;
import de.voglrobe.ftinterface.io.IFtInputReceiver;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class implements an abstraction of the ft-Interface. It supports asynchronous communication
 * with the hardware.
 *
 * @author robert
 */
public class FtInterfaceAsync
{
    private final Lock lock = new ReentrantLock();
    private FtSerialPortSenderReceiver senderReceiver;
    private MccExecutorThread mccExecutor;
    
    private volatile FtOutput interruptAction;
    
    
    /**
     * A factory to create a new instance of this class.
     * 
     * @param port The device name of the serial port, e.g. '/dev/ttyACM0'. 
     * @param inputReceiver The input receiver callback object to set.
     * The InputReceiver will be asynchronously notified on each incoming ISB set.
     * @return A new and ready-to-use instance of this class.
     * @throws ComException in case of errors.
     */
    public static FtInterfaceAsync newInstance(final String port, final IFtInputReceiver inputReceiver)
            throws ComException
    {
        FtInterfaceAsync ret = new FtInterfaceAsync();
        ret.init(port, inputReceiver);
        return ret;
    }

    /**
     * A factory to create a new instance of this class.
     * Unless a input receiver callback object is set (see {@link #setInputReceiver(IFtInputReceiver)})
     * this instance is not capable to read analog and digital inputs.
     * 
     * @param port The device name of the serial port, e.g. '/dev/ttyACM0'. 
     * @return A new and ready-to-use instance of this class.
     * @throws ComException in case of errors.
     */
    public static FtInterfaceAsync newInstance(final String port)
            throws ComException
    {
        FtInterfaceAsync ret = new FtInterfaceAsync();
        ret.init(port, null);
        return ret;
    }

    /**
     * Hidden constructor.
     */
    private FtInterfaceAsync()
    {
        this.senderReceiver = null;
        this.interruptAction = null;
        this.mccExecutor = null;
    }

    /**
     * Initialization.
     * 
     * @param port name of the serial COM port.
     * @param inputReceiver The input receiver callback object to set.
     * @throws ComException 
     */
    private void init(final String port, final IFtInputReceiver inputReceiver) throws ComException
    {
        this.senderReceiver = FtSerialPortSenderReceiver.newInstance(port);
        senderReceiver.setInputReceiver(inputReceiver);
        
        // start Runnable with new MCC.
        this.mccExecutor = new MccExecutorThread(senderReceiver);
        this.mccExecutor.start();
    }

    /**
     * Destructor.
     * 
     * Must be called at the end of the life-cycle to release system resources.
     */
    public void destroy()
    {
        this.terminateMccExecutor();
        if (senderReceiver != null)
        {
            senderReceiver.destroy();
        }
    }

    /**
     * Replaces or sets the input receiver callback object to handle incoming ISBs.
     * 
     * @param inputReceiver The callback object to set.
     * @throws ComException in case of errors.
     */
    public void setInputReceiver(final IFtInputReceiver inputReceiver) throws ComException
    {
        if (senderReceiver != null)
        {
            senderReceiver.setInputReceiver(inputReceiver);
        }
    }

    /**
     * Sends a MCC to the interface and returns immediately.
     * May block if the RS232 TX Buffer has not enough free space.
     * <p>
     * Stepping commands are not interruptible.
     * <p>
     * This method is locked as long as other send()-methods are being executed.
     * 
     * @param output The MCC to send.
     * @throws ComException in case of errors.
     */
    public void send(final FtOutput output) throws ComException
    {
        this.send(output, true, null);
    }
            
    /**
     * Sends a MCC to the interface and returns immediately.
     * May block if the RS232 TX Buffer has not enough free space.
     * <p>
     * Stepping commands are not interruptible.
     * <p>
     * This method is locked as long as other send()-methods are being executed.
     * 
     * @param output The MCC to send.
     * @param syncLock If TRUE a Sync-Lock is set. A Sync-Lock blocks any following send-with-duration
     * until the ISB of this MCC has been received.
     * @param flags Optional flags for the sender and receiver or NULL.
     * @throws ComException in case of errors.
     */
    public void send(final FtOutput output, final boolean syncLock, final FtInputsFlags flags) throws ComException
    {
        if (output == null)
        {
            throw new IllegalArgumentException("No output data to send.");
        }
        
        lock.lock();
        this.mccExecutor.pause();
        try
        {
            if (syncLock)
            {
                SequenceLockHelper.INSTANCE.addSeqNr(output.getSeqNr());
            }
            this.senderReceiver.send(output.bytes(), flags);
        }
        finally
        {
            this.interruptAction = null;
            lock.unlock();
        }
    }
    
    /**
     * Sends a MCC to the interface and executes it for a given duration of time.
     * Blocks until duration has been elapsed.
     * <p>
     * This method is only applicable for <b>non-stepping</b> commands.
     * <p>
     * If an input receiver callback object is set it will be notfied every 200 ms with the current state
     * of the analog and digital inputs.
     * <p>
     * This method is interruptible with {@link #softInterrupt(de.voglrobe.ftinterface.io.FtOutput)}.
     * The interrupt action will be executed as soon as possible (within 200 ms).
     * <p>
     * In order to allow a seamless execution with the probably following MCC, this method does NOT execute a
     * OFF-command when the duration time has been elapsed, i.e. all outputs remain active
     * until either the client sends a OFF-command or the hardware times out.
     * 
     * @param output The MCC to send.
     * @param duration The duration in seconds. Must be &gt;0.
     * @throws ComException In case of errors.
     */
    public void send(final FtOutput output, final long duration) throws ComException
    {
        if (output == null || duration <= 0)
        {
            throw new IllegalArgumentException("No output data to send or invalid duration.");
        }
        
        lock.lock();
        
        // blocks until a previous async MCC has been finished (by received ISB).
        SequenceLockHelper.INSTANCE.check();
        this.interruptAction = null;
        try
        {
            long stop = System.currentTimeMillis() + duration * 1000L;
            do
            {
                if (this.interruptAction != null)
                {
                    this.send(interruptAction, false, null);
                    break;
                }
                this.send(output, false, null);
                try
                {
                    Thread.sleep(200);
                }
                catch(InterruptedException dontcare)
                {
                }
            }
            while(System.currentTimeMillis() < stop);

            // 'durationFinished' flag for the final ISB.
            FtInputsFlags flags = new FtInputsFlags();
            flags.setDurationFinished(true);
            this.send(output, false, flags);
        }
        finally
        {
            lock.unlock();
        }
    }
    
    /**
     * Sends a MCC to the interface and executes it until it either gets overwritten with a new MCC
     * or the execution is interrupted. This method is <b>non-blocking</b> and returns immediately.
     * <p>
     * This method is only applicable for <b>non-stepping</b> commands.
     * <p>
     * If an input receiver callback object is set it will be notfied every 200 ms with the current state
     * of the analog and digital inputs.
     * <p>
     * This method is interruptible with {@link #softInterrupt(de.voglrobe.ftinterface.io.FtOutput)}.
     * The interrupt action will be executed as soon as possible (within 200 ms).
     * <p>
     * In order to allow a seamless execution with the probably following MCC, this method does NOT execute a
     * OFF-command after termination (unless otherwise specified by the interrupt action).
     * 
     * @param output The MCC to send.
     * @throws ComException In case of errors.
     */
    public void sendInfinite(final FtOutput output) throws ComException
    {
        if (output == null)
        {
            throw new IllegalArgumentException("No output data to send or invalid duration.");
        }
        
        lock.lock();
        try
        {
            // pause Runnable of old MCC.
            this.mccExecutor.pause();
            
            // resume with new MCC.
            this.mccExecutor.activate(output);
        }
        finally
        {
            lock.unlock();
        }
    }
    
    /**
     * Interrupts a long running command by the given command.
     * 
     * @param output The command to perform as interrupt action. 
     */
    public synchronized void softInterrupt(final FtOutput output)
    {
        this.interruptAction = output;      
        
        // stop Runnable of old MCC.
        if (mccExecutor != null)
        {
            try
            {
                this.send(interruptAction);
            }
            catch (ComException dontcare)
            {
            }
        }
    }

    /**
     * Stopp and eliminate a running MCC executor.
     */
    private synchronized void terminateMccExecutor()
    {
        if (mccExecutor != null)
        {
            mccExecutor.terminate();
            try
            {
                mccExecutor.join();
            }
            catch(InterruptedException dontcare)
            {
            }
            finally
            {
                this.mccExecutor = null;
            }
        }
    }
    
}
