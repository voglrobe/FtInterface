package de.voglrobe.ftinterface.io;

import de.voglrobe.ftinterface.async.SequenceLockHelper;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Thread implementation to asynchronously receive ISBs from the interface adapter.
 * 
 * @author robert
 */
public class SerialReceiverThread extends AbstractSerialReceiverThread
{
    private static final Logger LOGGER = Logger.getLogger(SerialReceiverThread.class.getName());

    private static final int PORT_RECEIVE_TIMEOUT = 2000; // 2 secs

    private IFtInputReceiver callback;
    private volatile FtInputsFlags flags;
    private volatile boolean stopped;

    /**
     * Convert signed byte to an unsigned int value, e.g. Byte = -1 => Integer = 255.
     * 
     * @param b The byte to convert.
     * @return The unsigned byte value.
     */
    private static int toUnsignedInt(final byte b)
    {
        return (int) b & 0xFF;
    }
    
    /**
     * Decode a Manchester-encoded byte, given by LSB and MSB. The mapping is: '10' -> 1, '01' -> 0.
     * 
     * @param lsb The MSB builds the upper nibble of the resulting byte.
     * @param msb The LSB builds th lower nibble of the resultig byte.
     * @return The unsinged byte value.
     */
    private static int decodeManchester(final byte lsb, final byte msb)
    {
        int decoded = 0;
        int encoded = toUnsignedInt(msb) << 8 | toUnsignedInt(lsb);
        
        for (int i=7; i>=0; i--)
        {
            decoded = decoded << 1;
            int value = (encoded >> 2*i) & 3;
            switch(value)
            {
                case 2:
                    decoded |= 1;
                    break;
                case 1:
                    break;
                default:
                    throw new NumberFormatException("Invalid Manchester code.");
            }
        }
        return decoded;
    }
    
    /**
     * Check whether the given byte is a Start Byte (0B00_xx_xx_xx).
     * 
     * @param b The byte to check.
     * @return TRUE if the given byte is a Start Byte, otherwise FALSE.
     */
    private static boolean isStartByte(final byte b)
    {
        return (toUnsignedInt(b) >> 6) == 0;
    }

    /**
     * Constructor.
     * 
     * @param serPort A {@link SerialPort} object connected to the interface adapter and ready to use.
     */
    public SerialReceiverThread(final SerialPort serPort)
    {
        this(serPort, null);
    }

    /**
     * Constructor.
     * 
     * @param serPort A {@link SerialPort} object connected to the interface adapter and ready to use.
     * @param callback The callback object to notify on each incoming set of ISBs. Can be NULL if not required.
     */
    public SerialReceiverThread(final SerialPort serPort, final IFtInputReceiver callback)
    {
        super(serPort);
        this.callback = callback;
        this.stopped = false;
        this.flags = null;
    }

    @Override
    public void setFlags(final FtInputsFlags flags)
    {
        this.flags = flags;
    }
    
    @Override
    public void terminate()
    {
        LOGGER.log(Level.INFO, "Stopping SerialReceiverThread...");
        this.stopped = true;
        this.callback = null;
        SequenceLockHelper.INSTANCE.flush();
    }
    
    @Override
    public void run()
    {
        if (serPort == null || stopped)
        {
            return;
        }

        try
        {
            // set receive timeout in order to check the stopped-flag
            serPort.enableReceiveTimeout(PORT_RECEIVE_TIMEOUT);
        }
        catch (UnsupportedCommOperationException e)
        {
            LOGGER.log(Level.SEVERE, "Unable to set receive timeout. ", e);
            return;
        }
        
        List<Byte> commandBuffer = new ArrayList<>();
        try(InputStream is = serPort.getInputStream())
        {
            LOGGER.log(Level.INFO, "Waiting for ISBs...");
            int len;
            byte[] buffer = new byte[255];            
            while((len = is.read(buffer)) > -1)
            {
                if (stopped)
                {
                    break;
                }
                for (int i=0; i<len; i++)
                {
                    byte inbyte = buffer[i];
                    if (isStartByte(inbyte)) // Start Byte = sequence number [0, 63].
                    {
                        commandBuffer.clear();
                    }
                    commandBuffer.add(inbyte);
                    if (commandBuffer.size() == 7)
                    {
                        this.processISBs(commandBuffer);
                    }                  
                }
            }
            LOGGER.log(Level.INFO, "SerialReceiverThread stopped.");
        }
        catch(IOException e)
        {
            LOGGER.log(Level.SEVERE, "Unintended termination of SerialReceiverThread.", e);
        }
    }
    
    /**
     * Process received ISBs.
     * 
     * @param isbs The ISBs. Index 0: Magic Byte,
     * Index 1, 2 = digital inputs, Index 3, 4 EX, Index 5, 6 = EY. 
     */
    private void processISBs(List<Byte> isbs)
    {
        if (callback == null)
        {
            return;
        }
        
        try
        {
            int seqNr = toUnsignedInt(isbs.get(0));
            int di = decodeManchester(isbs.get(1), isbs.get(2));
            int ex = decodeManchester(isbs.get(3), isbs.get(4));
            int ey = decodeManchester(isbs.get(5), isbs.get(6));
            FtInputs inputs = new FtInputs(seqNr, di, ex, ey);
            inputs.setFlags(flags);
            
            SequenceLockHelper.INSTANCE.removeSeqNr(seqNr);
            callback.onDataReceived(inputs);
        }
        catch(NumberFormatException e)
        {
            LOGGER.log(Level.SEVERE, "Invalid ISBs received.", e);
        }
        finally
        {
            this.flags = null;
        }
    }

    
}
