package de.voglrobe.ftinterface.io;

import com.google.gson.Gson;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Optional flags for the {@link FtInputs}.
 * 
 * @author robert
 */
public class FtInputsFlags
{
    private boolean durationFinished;
   
    /**
     * Factory to create a new instance of this class from a JSON string.
     * 
     * @param json A valid JSON string.
     * @return A new instance of this class.
     */
    public static FtInputsFlags fromJson(final String json)
    {
        Gson gson = new Gson();
        return gson.fromJson(json, FtInputsFlags.class);
    }
    
    /**
     * Constructor.
     */
    public FtInputsFlags()
    {
        this.durationFinished = false;
    }

    /**
     * @return TRUE if this ISB is the last one from a command with 'duration' != 0 set.
     */
    public boolean isDurationFinished()
    {
        return durationFinished;
    }

    /**
     * A command with 'duration' != 0 usually responds every 200 ms with an ISB. To help the client
     * to recognise the end of such commands, it is required that the last ISB must set this flag to TRUE.
     * After this flag was received, an ISB with the same sequence number is not allowed.
     * 
     * @param durationFinished TRUE if duration has ended and this ist the last ISB.
     */
    public void setDurationFinished(final boolean durationFinished)
    {
        this.durationFinished = durationFinished;
    }
    
    /**
     * Reset all flags to their default state.
     */
    public void reset()
    {
        this.durationFinished = false;
    }
    
    /**
     * Serialize to JSON.
     * 
     * @return An instance of this class serialized into JSON. 
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
        hcb.append(this.isDurationFinished());
        return hcb.toHashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof FtInputsFlags))
        {
            return false;
        }
        FtInputsFlags rhs = (FtInputsFlags) obj;
        EqualsBuilder eqb = new EqualsBuilder();
        eqb.append(this.isDurationFinished(), rhs.isDurationFinished());
        return eqb.isEquals();        
    }
    
    
}
