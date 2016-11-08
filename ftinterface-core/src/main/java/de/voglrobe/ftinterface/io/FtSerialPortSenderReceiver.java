package de.voglrobe.ftinterface.io;

import de.voglrobe.ftinterface.exceptions.ComException;
import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

/**
 * A sender and receiver for the communication with the serial port of the interface adapter.
 * 
 * A client can either provide it's own Thread implementation to asynchronously handle incoming data or can provide a
 * callback object that is notified with every received set of ISBs. 
 * 
 * @author robert
 */
public class FtSerialPortSenderReceiver
{
    private static final String NULL_CMD = "$0,0,0,0\n";
    private static final int PORT_SPEED = 19200; // Bit/s
    private static final int PORT_CONNECT_TIMEOUT = 10000; // 10 secs
    
    public static final byte[] NULL_CMD_BYTES = NULL_CMD.getBytes(Charset.forName("US-ASCII"));
    
    private SerialPort serialPort;
    private AbstractSerialReceiverThread serialReceiverThread;
    
    /**
     * Factory method to create a new instance of this class.
     * 
     * @param port the device name of the COM-Port, e.g. '/dev/ttyACM0'.
     * @return A new ready-to-use instance of this class.
     * @throws ComException in case of errors.
     */
    public static FtSerialPortSenderReceiver newInstance(final String port)
            throws ComException
    {
        FtSerialPortSenderReceiver ret = new FtSerialPortSenderReceiver();
        ret.open(port, PORT_SPEED);
        return ret;
    }

    /**
     * Hidden Constructor.
     */
    private FtSerialPortSenderReceiver()
    {
        this.serialPort = null;
        this.serialReceiverThread = null;
    }
    
    /**
     * Destructor.
     * 
     * Must be called at the end of the life-cycle to release system resources (close serial port, stop threads).
     */
    public void destroy()
    {
        this.stopReceiverThread();
        if (serialPort != null)
        {
            serialPort.close();
            this.serialPort = null;
        }
    }
    
    /**
     * Open serial connection.
     * 
     * @throws ComException In case of errors. 
     */
    private void open(final String port, final int speed) throws ComException
    {
        if (port == null || port.isEmpty())
        {
            throw new IllegalArgumentException("Missing port name.");
        }
        try
        {
            CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(port);
            if (portIdentifier.isCurrentlyOwned())
            {
                throw new ComException("Port " + port + "is currently in use.");
            }
            CommPort commPort = portIdentifier.open(this.getClass().getName(), PORT_CONNECT_TIMEOUT);
            if (!(commPort instanceof SerialPort))
            {
                throw new ComException("Only serial ports are supported.");
            }
            this.serialPort = (SerialPort) commPort;
            serialPort.setSerialPortParams(speed, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
            
            Thread.sleep(2000L);
        }
        catch(NoSuchPortException | PortInUseException | UnsupportedCommOperationException e)
        {
            throw new ComException(e);
        }
        catch(InterruptedException dontcare)
        {            
        }
    }
    
    /**
     * Set the callback object that asynchronously receives incoming ISBs.
     * 
     * @param callback The callback object to set.
     * @throws ComException in case of errors.
     */
    public void setInputReceiver(final IFtInputReceiver callback) throws ComException
    {
        this.stopReceiverThread();
        if (callback != null)
        {
            this.serialReceiverThread = new SerialReceiverThread(serialPort, callback);
            this.startReceiverThread();
        }
    }
    
    /**
     * Sets and starts a custom implementation of a Thread to handle the communication with the interface adapter.
     * A previously running thread will be terminated first.
     * 
     * @param thread The custom Thread implementation to set.
     * @throws ComException in case of errors.
     */
    public void setInputReceiver(final AbstractSerialReceiverThread thread) throws ComException
    {
        this.stopReceiverThread();
        if (thread != null)
        {
            this.serialReceiverThread = thread;
            this.startReceiverThread();
        }
    }
    
    /**
     * Stopps the communication with the interface adapter and blocks until the corresponding thread has been properly terminated.
     */
    private void stopReceiverThread()
    {
        if (serialReceiverThread != null && serialReceiverThread.isAlive())
        {
            serialReceiverThread.terminate();
            try
            {
                serialReceiverThread.join(10000L);
            }
            catch(InterruptedException dontcare)
            {
            }
            this.serialReceiverThread = null;
        }
    }
    
    /**
     * Starts the communication with the interface adapter.
     * 
     * @throws ComException in case of errors.
     */
    private void startReceiverThread() throws ComException
    {
        if (serialReceiverThread == null || serialReceiverThread.isAlive())
        {
            return;
        }
        serialReceiverThread.start();        
    }
    
    /**
     * Sends the given data to the interface adapter and returns immediately.
     * May block if there is not enough space in the RS232 TX Buffer.
     * 
     * @param bytes The bytes to send.
     * @throws ComException in case of errors.
     */
    public void send(byte[] bytes) throws ComException
    {
        if (bytes == null || bytes.length == 0)
        {
            return;
        }
        if (serialPort == null)
        {
            throw new ComException("Serial port is not available.");
        }
        
        try(OutputStream os = this.serialPort.getOutputStream())
        {
            os.write(bytes, 0, bytes.length);
        }
        catch(IOException e)
        {
            throw new ComException(e);
        }
    }
    
    
    
}
