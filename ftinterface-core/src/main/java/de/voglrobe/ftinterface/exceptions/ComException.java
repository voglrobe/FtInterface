package de.voglrobe.ftinterface.exceptions;

/**
 * Checked Exception thrown on communication errors with the hardware.
 * 
 * @author vlr
 */
public class ComException extends Exception
{

    private static final long serialVersionUID = -493574981829131715L;

    public ComException()
    {
    }

    public ComException(String message)
    {
        super(message);
    }

    public ComException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public ComException(Throwable cause)
    {
        super(cause);
    }

    public ComException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }
    
}
