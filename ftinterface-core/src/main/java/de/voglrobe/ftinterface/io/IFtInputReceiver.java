package de.voglrobe.ftinterface.io;

/**
 * Callback interface to handle asynchronously received data.
 * 
 * @author robert
 */
public interface IFtInputReceiver
{
    /**
     * Called when new input data were received.
     * 
     * @param inputs The received data. 
     */
    void onDataReceived(FtInputs inputs);
}
