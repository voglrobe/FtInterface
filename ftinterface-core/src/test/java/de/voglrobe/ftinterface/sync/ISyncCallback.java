package de.voglrobe.ftinterface.sync;

import de.voglrobe.ftinterface.io.FtInputs;

/**
 *
 * @author vlr
 */
public interface ISyncCallback
{
    void onReceived(final FtInputs inputs);
}
