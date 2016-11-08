package de.voglrobe.ftinterface.server;

import de.voglrobe.ftinterface.FtInterfaceAsync;
import de.voglrobe.ftinterface.exceptions.ComException;
import de.voglrobe.ftinterface.io.FtInputs;
import de.voglrobe.ftinterface.server.websocket.FtWebSocket;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.jetty.websocket.api.Session;
import static spark.Spark.init;
import static spark.Spark.port;
import static spark.Spark.stop;
import static spark.Spark.webSocket;


/**
 * The main executable class of the FtServer.
 * <p>
 * It opens a WebSocket on port 9091 (default) and waits for incoming MCCs.
 * MCCs are expected in JSON. See de.voglrobe.ftinterface.io.FtMccMessage.
 * The URL is: ws://&lt;host&gt;:9091/ftinterface.
 * <p>
 * ISBs are returned asynchronously in JSON format. See {@link de.voglrobe.ftinterface.io.FtInputs}. 
 * <p>
 * The port number and the name of the serial device are configurable in 'Application.properties'.
 * 
 * @author robert
 */
public class FtServer
{
    private static final Logger LOGGER = Logger.getLogger(FtServer.class.getName());

    // Configuration property keys
    private static final String PROP_KEY_WEBSOCKET_PORT = "de.voglrobe.ftserver.websocket.port";
    private static final String PROP_SERIAL_DEVICE_NAME = "de.voglrobe.ftserver.serial.devicename";

    private static final List<Session> SESSIONS = new ArrayList<>();
    private static FtInterfaceAsync IFACE = null;
    
    private final Properties props;
    
    /**
     * Returns the interface instance.
     * 
     * @return the interface instance.
     */
    public static FtInterfaceAsync getFtInterface()
    {
        return IFACE;
    }
    
    /**
     * Returns the registered sessions.
     * 
     * @return The registered sessions (open WebSocket connections). 
     */
    public static List<Session> getSessions()
    {
        return SESSIONS;
    }
    
    /**
     * Factory to create and initialize a new instance of this class.
     * 
     * @return A new ready-to-use instance.
     * @throws IOException in case of errors.
     */
    private static FtServer newInstance() throws IOException
    {
        FtServer server = new FtServer();
        server.readProperties();
        return server;
    }
    
    /**
     * Constructor. Do NOT use. Use newInstance() instead.
     */
    private FtServer()
    {
        this.props = new Properties();
    }
    
    /**
     * Read configuration properties.
     * 
     * @throws IOException in case of errors.
     */
    private void readProperties() throws IOException
    {
        String configFile = System.getProperty("config", null);
        if (configFile != null)
        {
            try(InputStream isr = new FileInputStream(configFile))
            {
                LOGGER.log(Level.INFO, "Reading configuration from {0}", configFile);
                this.props.load(isr);
            }
        }
    }
    
    /**
     * The main thread runner.
     */
    private void run() throws ComException
    {
        // Configurable options
        final int webSocketPort = Integer.parseInt(this.props.getProperty(PROP_KEY_WEBSOCKET_PORT, "9091"));
        LOGGER.log(Level.INFO, "WebSocket port: {0}", String.valueOf(webSocketPort));
        
        final String serialDeviceName = this.props.getProperty(PROP_SERIAL_DEVICE_NAME, "/dev/ttyACM0");
        LOGGER.log(Level.INFO, "Serial device name: {0}", serialDeviceName);
        
        // Init interface
        final BlockingQueue<FtInputs> isbQueue = new LinkedBlockingQueue<>();
        FtServer.IFACE = FtInterfaceAsync.newInstance(serialDeviceName, (final FtInputs inputs)->
        {
            isbQueue.offer(inputs);
        });

        // Start ISB Thread
        final ISBThread isbThread = new ISBThread(isbQueue);
        isbThread.start();

        // Open WebSocket
        port(webSocketPort);
        webSocket("/ftinterface", FtWebSocket.class);
        init();

        // Wait for ENTER to terminate
        try(BufferedReader br = new BufferedReader(new InputStreamReader(System.in)))
        {
            System.out.println("Press Enter to exit...");
            br.readLine();
        }
        catch(IOException dontcare)
        {
        }
        System.out.println("Shutdown...");
        
        // Close WebSocket
        stop();
        
        // Stop ISB Thread
        isbThread.terminate();
        try
        {
            isbThread.join();
        }
        catch (InterruptedException dontcare)
        {
        }
        
        // Stop interface
        FtServer.IFACE.destroy();
    }
    
    /**
     * Start the server.
     * <p>
     * Test with curl, e.g.:
     * <code>
     * curl -i -N 
     * -H "Connection: Upgrade"
     * -H "Upgrade: websocket"
     * -H "Host: localhost"
     * -H "Origin: http://localhost:9091/ftinterface"
     * -H "Sec-WebSocket-Version: 13"
     * -H "Sec-WebSocket-Key: huhu" http://localhost:9091/ftinterface
     * </code>
     * @param args not used.
     */
    public static void main(String[] args)
    {
        try
        {
            FtServer server = FtServer.newInstance();
            server.run();
        }
        catch(NumberFormatException e)
        {
            LOGGER.log(Level.SEVERE, "Invalid configuration option.", e);
            System.err.println("Invalid configuration option. See logfile for details.");
            System.exit(1);
        }
        catch(IOException | ComException e)
        {
            LOGGER.log(Level.SEVERE, "Unable to run server.", e);
            System.err.println("Unable to run server. See logfile for details.");
            System.exit(1);
        }
    }
}
