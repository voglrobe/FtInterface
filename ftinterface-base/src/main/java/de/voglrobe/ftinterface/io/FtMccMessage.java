package de.voglrobe.ftinterface.io;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.Serializable;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * This class implements a MCC message with additional information.
 * 
 * @author robert
 */
public class FtMccMessage implements Serializable
{

    private static final long serialVersionUID = -2605271547507826857L;
    private final FtOutput mcc;
    private int duration;
    
    /**
     * Factory to create a new instance of this class from a JSON string.
     * 
     * @param json A valid JSON string.
     * @return A new instance of this class.
     * @throws JsonSyntaxException in case of invalid JSON.
     */
    public static FtMccMessage fromJson(final String json) throws JsonSyntaxException
    {
        Gson gson = new Gson();
        return gson.fromJson(json, FtMccMessage.class);
    }
    
    /**
     * Constructor.
     * 
     * @param mcc The MCC itself. 
     */
    public FtMccMessage(final FtOutput mcc)
    {
        this.mcc = mcc;
        this.duration = 0;
    }
    
    /**
     * Sets the duration to hold the given MCC.
     * 
     * @param duration duration in seconds.<br>
     * If duration &gt; 0 the MCC is executed as long as the given duration.<br>
     * If the duration = 0 the MCC is executed once. 0 is required for stepping commands.<br>
     * If the duration &lt; 0 then the MCC is excecuted until it gets overwritten with a new command.
     */
    public void setDuration(final int duration)
    {
        this.duration = duration;
    }
    
    /**
     * Returns the duration to hold the MCC.
     * 
     * @return The duratation in seconds.
     */
    public int getDuration()
    {
        return duration;
    }
    
    /**
     * Returns the MCC.
     * 
     * @return The MCC. 
     */
    public FtOutput getMcc()
    {
        return mcc;
    }
    
    /**
     * Returns an instance of this class in JSON.
     * 
     * @return This class as JSON. 
     */
    public String toJson()
    {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
    
    @Override
    public int hashCode()
    {
        HashCodeBuilder hcb = new HashCodeBuilder(17, 67);
        hcb.append(this.getMcc());
        hcb.append(this.getDuration());
        return hcb.toHashCode();
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof FtMccMessage))
        {
            return false;
        }
        FtMccMessage rhs = (FtMccMessage) obj;
        EqualsBuilder eqb = new EqualsBuilder();
        eqb.append(this.getMcc(), rhs.getMcc());
        eqb.append(this.getDuration(), rhs.getDuration());
        return eqb.isEquals();        
    }    
    
}
