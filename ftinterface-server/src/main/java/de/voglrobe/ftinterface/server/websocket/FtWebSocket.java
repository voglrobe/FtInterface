package de.voglrobe.ftinterface.server.websocket;

import com.google.gson.JsonSyntaxException;
import de.voglrobe.ftinterface.exceptions.ComException;
import de.voglrobe.ftinterface.io.FtMccMessage;
import de.voglrobe.ftinterface.server.FtServer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

/**
 * This class implements the WebSocket receiver callbacks (open, close. message).
 * 
 * @author robert
 */
@WebSocket
public class FtWebSocket
{
    private static final Logger LOGGER = Logger.getLogger(FtWebSocket.class.getName());
    
    /**
     * Called on connection opened.
     * 
     * @param session A session instance. 
     */
    @OnWebSocketConnect
    public void onConnected(final Session session)
    {
        LOGGER.log(Level.INFO, "Connected to WebSocket.");
        FtServer.getSessions().add(session);
    }
    
    /**
     * Called on connection closed.
     * 
     * @param session A session instance.
     * @param statusCode The status code.
     * @param reason A textual reason for the termination.
     */
    @OnWebSocketClose
    public void onClosed(final Session session, final int statusCode, final String reason)
    {
        LOGGER.log(Level.INFO, "WebSocket closed.");
        FtServer.getSessions().remove(session);
    }
    
    /**
     * Called on received messages.
     * 
     * @param session A session instance.
     * @param message The received message.
     * Must be an instance of {@link FtMccMessage} in JSON format. Example:
     * <pre> {
     *  "mcc": {
     *     "seqnr": 42,
     *     "mcb": 85,
     *     "m1steps": 100,
     *     "m2steps": 200,
     *     "m3steps": 300
     *  },
     *  "duration": 0
     * }</pre>
     */
    @OnWebSocketMessage
    public void onMessage(final Session session, final String message)
    {
        if (message == null || FtServer.getFtInterface() == null)
        {
            return;
        }
        LOGGER.log(Level.INFO, "WebSocket message received: {0}.", message);
        
        try
        {
            FtMccMessage ftMessage = FtMccMessage.fromJson(message);
            int duration = ftMessage.getDuration();
            if (duration > 0)
            {
                // blocking!!
                FtServer.getFtInterface().send(ftMessage.getMcc(), duration);                
            }
            else if (duration == 0)
            {
                FtServer.getFtInterface().send(ftMessage.getMcc());                
            }
            else
            {
                FtServer.getFtInterface().sendInfinite(ftMessage.getMcc());
            }
        }
        catch(JsonSyntaxException e)
        {
            LOGGER.log(Level.SEVERE, "Invalid JSON message received.", e);            
        }
        catch(ComException e)
        {
            LOGGER.log(Level.SEVERE, "Unable to send output to interface.", e);
        }
    }
    
}
