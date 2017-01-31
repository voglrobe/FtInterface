package de.voglrobe.ftinterface.io;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Holds the digital and analog input data (ISBs).
 *
 * @author robert
 */
public class FtInputs
{
    @SerializedName("seqnr")    
    private final int seqNr;
    
    @SerializedName("di")    
    private final List<Boolean> digitalIn = new ArrayList<>();
    private final int ex;
    private final int ey;
    
    @SerializedName("flags")    
    private FtInputsFlags flags;

    /**
     * The digital input names.
     */
    public enum IN
    {
        E1, E2, E3, E4, E5, E6, E7, E8
    };
    
    /**
     * Factory to create a new instance of this class from a JSON string.
     * 
     * @param json A valid JSON string.
     * @return A new instance of this class.
     */
    public static FtInputs fromJson(final String json)
    {
        Gson gson = new Gson();
        return gson.fromJson(json, FtInputs.class);
    }

    /**
     * Constructor.
     *
     * @param seqNr The received sequence number of the corresponding MCC.
     * @param digitalIn Digital inputs bitwise at the lower end in the order E1-E2-...-E8.
     * @param ex 8-bit-quantified value of the analog input EX. Unsigned [0, 255].
     * @param ey 8-bit-quantified value of the analog input EY. Unsigned [0, 255].
     */
    public FtInputs(final int seqNr, final int digitalIn, final int ex, final int ey)
    {
        this.seqNr = seqNr;
        this.digitalIn.clear();
        for(int i=7; i>=0; i--)
        {
            boolean e = ((digitalIn >> i) & 1) != 0; 
            this.digitalIn.add(e);
        }
        this.ex = ex;
        this.ey = ey;
    }
    
    /**
     * Returns the sequence number of the corresponding MCC.
     * 
     * @return The received sequence number. 
     */
    public int getSeqNr()
    {
        return seqNr;
    }

    /**
     * Returns the values of all digital inputs as a list.
     * 
     * @return The digital inputs. Index 0 = E1, Index 7 = E8, TRUE = +5V, FALSE = 0V.
     */
    public List<Boolean> getDigitalIn()
    {
        return Collections.unmodifiableList(digitalIn);
    }
    
    /**
     * Returns the value of a digital input denoted by it's name (enum identifier).
     * 
     * @param in enum identifier of the digital input. 
     * @return The boolean value of the input (TRUE = +5V, FALSE = 0V). 
     */
    public Boolean getDigitalIn(final FtInputs.IN in)
    {
        return digitalIn.get(in.ordinal());
    }

    /**
     * Returns the value of the Analog input EX.
     * 
     * @return The 8-bit linear quantified value of the analog input EX in the range [0, 255].
     * 0 = low resistance (0 Ohm), 255 = high resistance, 230 = 10 KOhm. Noise &plusmn;1.
     */
    public int getEx()
    {
        return ex;
    }

    /**
     * Returns the value of the analog input EY.
     * 
     * @return The 8-bit linear quantified value of the analog input EY in the range [0, 255].
     * 0 = low resistance (0 Ohm), 255 = high resistance, 230 = 10 KOhm. Noise &plusmn;1.
     */
    public int getEy()
    {
        return ey;
    }

    /**
     * Returns the optional server flags.
     * 
     * @return the optional server flags or NULL if not set.
     */
    public FtInputsFlags getFlags()
    {
        return flags;
    }

    /**
     * Sets the optional server flags.
     * 
     * @param flags optional server flags.
     */
    public void setFlags(final FtInputsFlags flags)
    {
        this.flags = flags;
    }
  
    
    /**
     * Compares the states of this digital inputs with the states of given digital inputs.
     * 
     * @param rhs The digital inputs to compare in the order index 0 = E1, index 7 = E8.
     * A list item value of NULL stands for don't care. 
     * @return TRUE if all non-NULL digital input values are equal or
     * if the size of 'rhs' is 8 and all it's items are NULL.
     * Otherwise FALSE.
     */
    public boolean compareDigitalIn(final List<Boolean> rhs)
    {
        if (rhs == null || rhs.size() != 8)
        {
            return false;
        }
        
        for (int i=0; i<8; i++)
        {
            Boolean diRhs = rhs.get(i);
            if (diRhs != null)
            {
                if (!diRhs.equals(this.getDigitalIn().get(i)))
                {
                    return false;
                }
            }
        }
        return true;
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
        hcb.append(this.getSeqNr());
        hcb.append(this.getDigitalIn());
        hcb.append(this.getEx());
        hcb.append(this.getEy());
        hcb.append(this.getFlags());
        return hcb.toHashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof FtInputs))
        {
            return false;
        }
        FtInputs rhs = (FtInputs) obj;
        EqualsBuilder eqb = new EqualsBuilder();
        eqb.append(this.getSeqNr(), rhs.getSeqNr());
        eqb.append(this.getDigitalIn(), rhs.getDigitalIn());
        eqb.append(this.getEx(), rhs.getEx());
        eqb.append(this.getEy(), rhs.getEy());
        eqb.append(this.getFlags(), rhs.getFlags());
        return eqb.isEquals();        
    }



}
