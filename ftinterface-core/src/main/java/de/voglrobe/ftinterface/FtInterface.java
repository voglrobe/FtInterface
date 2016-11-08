package de.voglrobe.ftinterface;

import de.voglrobe.ftinterface.exceptions.ComException;
import de.voglrobe.ftinterface.io.FtInputs;
import de.voglrobe.ftinterface.io.FtOutput;
import de.voglrobe.ftinterface.io.FtSerialPortSenderReceiver;
import de.voglrobe.ftinterface.sync.ISBBuffer;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * This class implements an abstraction of the ft-Interface. It supports synchronous communication
 * with the hardware.
 * 
 * @author robert
 */
public class FtInterface
{
    private final ISBBuffer isbBuffer;
    private final CyclicBarrier barrier;
    private FtSerialPortSenderReceiver senderReceiver;
    
    /**
     * Hidden constructor.
     */
    private FtInterface()
    {
        this.senderReceiver = null;
        this.isbBuffer = new ISBBuffer();
        this.barrier = new CyclicBarrier(2);
    }
    
    /**
     * A factory to create a new instance of this class.
     * 
     * @param port The device name of the serial port, e.g. '/dev/ttyACM0'. 
     * @return A new and ready-to-use instance of this class.
     * @throws ComException in case of errors.
     */
    public static FtInterface newInstance(final String port) throws ComException
    {
        FtInterface ret = new FtInterface();
        ret.init(port);
        return ret;
    }
    
    /**
     * Initialization.
     * 
     * @param port Name of the serial COM port.
     * @throws ComException 
     */
    private void init(final String port) throws ComException
    {
        this.senderReceiver = FtSerialPortSenderReceiver.newInstance(port);
        senderReceiver.setInputReceiver((final FtInputs inputs)->
        {
            isbBuffer.store(inputs.getSeqNr(), inputs);
            try
            {
                barrier.await(60L, TimeUnit.SECONDS);
            }
            catch (InterruptedException | BrokenBarrierException | TimeoutException e)
            {
                barrier.reset();
            }
        });
    }
    
    /**
     * Destructor.
     * 
     * Must be called at the end of the life-cycle to release system resources.
     */
    public void destroy()
    {
        if (senderReceiver != null)
        {
            senderReceiver.destroy();
        }
    }
    
    /**
     * Sends a MCC to the interface and blocks until the corresponding ISBs have been returned.
     * Times out after 60 seconds.
     * 
     * @param output The MCC to send. The sequence number will be overwritten.
     * @return A single set of ISBs.
     * @throws ComException in case of errors or timeout.
     */
    public FtInputs send(final FtOutput output) throws ComException
    {
        if (output == null)
        {
            throw new IllegalArgumentException("No output data to send.");
        }
        
        int seqNr = isbBuffer.requestSeqNr();
        
        // send and wait
        this.senderReceiver.send(output.seqNr(seqNr).bytes());
        try
        {
            do
            {
                barrier.await(60L, TimeUnit.SECONDS);
                barrier.reset();
            }
            while (!isbBuffer.contains(seqNr));
            return isbBuffer.pop(seqNr);
        }
        catch(InterruptedException | BrokenBarrierException | TimeoutException e)
        {
            barrier.reset();
            throw new ComException(e);
        }
    }
    
    /**
     * Sends a MCC to the interface and executes it for a given duration of time.
     * This is only applicable for <b>non-stepping</b> commands.
     * <p>
     * In order to allow a seamless execution eith the probably following MCC, this method does NOT execute a
     * OFF-command when the duration time has been elapsed, i.e. all outputs remain active
     * until either the client sends a OFF-command or the hardware times out.
     * 
     * @param output The MCC to send.
     * @param duration The duration in seconds. Must be &gt;0.
     * @return The last ISBs got from the interface immediately before the hold time has been elapsed.
     * @throws ComException in case of errors.
     */
    public FtInputs send(final FtOutput output, final long duration) throws ComException
    {
        if (output == null || duration <= 0)
        {
            throw new IllegalArgumentException("No output data to send or invalid duration.");
        }
        
        FtInputs ret;        
        long stop = System.currentTimeMillis() + duration * 1000L;
        do
        {
            ret = this.send(output);
            try
            {
                Thread.sleep(200);
            }
            catch(InterruptedException dontcare)
            {
            }
        }
        while(System.currentTimeMillis() < stop);
        return ret;
    }
    
    /**
     * Sends a MCC to the interface and executes it until the given digital input states and received
     * digital inputs states from the ISB do match.
     * Times out after 60 Seconds.
     * <p>
     * In order to allow a seamless execution with the propably following MCC, this method does NOT execute a
     * OFF-command before it returns, i.e. all outputs remain active
     * until either the client sends a OFF-command or the hardware times out.
     * 
     * @param output The MCC to send.
     * @param digitalInput A list of digital input states in the order index 0 = E1, index 7 = E8.
     * An item value of NULL stands for <i>don't care</i>. If all items are NULL this method
     * executes the MCC only once and returns without any condition checks.
     * @return The ISBs either when the input states condition was fulfilled or at timeout.
     * @throws ComException in case of errors.
     */
    public FtInputs send(final FtOutput output, final List<Boolean> digitalInput) throws ComException
    {
        if (output == null || digitalInput == null || digitalInput.size() != 8)
        {
            throw new IllegalArgumentException("Invalid output or condition data.");
        }
        
        FtInputs ret;
        long start = System.currentTimeMillis();
        do
        {
            ret = this.send(output);
            if (ret.compareDigitalIn(digitalInput))
            {
                break;
            }
            try
            {
                Thread.sleep(200);
            }
            catch(InterruptedException dontcare)
            {
            }
        }
        while (System.currentTimeMillis() - start < 60000L);
        return ret;
    }
            
    
}
