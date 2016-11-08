package de.voglrobe.ftinterface.io;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.nio.charset.Charset;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * A builder for the Motor Control Command (MCC).
 * <br>
 * Motors are controlled by the two attributes <em>direction</em> and <em>steps</em> which could be contradictory.
 * The following table shows the resulting MCC action in any possible combination:
 * <table class="memberSummary" cellpadding="3" cellspacing="0" border="0" summary="MCC attributes">
 * <caption><span>MCC attributes relationship</span></caption>
 * <tbody>
 * <tr class="rowColor">
 * <th class="colFirst">direction</th>
 * <th class="colFirst">steps</th>
 * <th class="colLast">resulting MCC action</th>
 * </tr>
 * <tr class="altColor">
 * <td class="colFirst">OFF</td>
 * <td class="colFirst">don't care</td>
 * <td class="colLast">OFF</td>
 * </tr>
 * <tr class="rowColor">
 * <td class="colFirst">ON</td>
 * <td class="colFirst">don't care</td>
 * <td class="colLast">turn RIGHT until hardware timeout</td>
 * </tr>
 * <tr class="altColor">
 * <td class="colFirst">RIGHT</td>
 * <td class="colFirst">[1, 32767]</td>
 * <td class="colLast">turn RIGHT number of steps</td>
 * </tr>
 * <tr class="rowColor">
 * <td class="colFirst">LEFT</td>
 * <td class="colFirst">[1, 32767]</td>
 * <td class="colLast">turn LEFT number of steps</td>
 * </tr>
 * <tr class="altColor">
 * <td class="colFirst">RIGHT</td>
 * <td class="colFirst">0</td>
 * <td class="colLast">turn RIGHT until hardware timeout</td>
 * </tr>
 * <tr class="rowColor">
 * <td class="colFirst">LEFT</td>
 * <td class="colFirst">0</td>
 * <td class="colLast">turn LEFT until hardware timeout</td>
 * </tr>
 * <tr class="altColor">
 * <td class="colFirst">don't care</td>
 * <td  class="colFirst">&lt;0</td>
 * <td class="colLast">OFF</td>
 * </tr>
 * <tr class="rowColor">
 * <td class="colFirst">don't care</td>
 * <td  class="colFirst">&gt;32767</td>
 * <td class="colLast">OFF</td>
 * </tr>
 * </tbody>
 * </table>
 * 
 * @author robert
 */
public class FtOutput
{
    public static final int M1_RIGHT = 0B01_00_00_00;
    public static final int M1_LEFT  = 0B10_00_00_00;
    public static final int M1_ON    = M1_RIGHT;
    public static final int M1_OFF   = 0B00_11_11_11;
    
    public static final int M2_RIGHT = 0B00_01_00_00;
    public static final int M2_LEFT  = 0B00_10_00_00;
    public static final int M2_ON    = M2_RIGHT;
    public static final int M2_OFF   = 0B11_00_11_11;
    
    public static final int M3_RIGHT = 0B00_00_01_00;
    public static final int M3_LEFT  = 0B00_00_10_00;
    public static final int M3_ON    = M3_RIGHT;
    public static final int M3_OFF   = 0B11_11_00_11;

    public static final int M4_RIGHT = 0B00_00_00_01;
    public static final int M4_LEFT  = 0B00_00_00_10;
    public static final int M4_ON    = M4_RIGHT;
    public static final int M4_OFF   = 0B11_11_11_00;

    public static final int MAX_STEPS = 32767;
    
    /**
     * Turn direction of the motors.
     */
    public enum Direction
    {
        OFF,
        ON,
        LEFT,
        RIGHT
    }
    
    @SerializedName("seqnr")
    private int seqNr;
    
    private int mcb;    
    private int m1steps;
    private int m2steps;
    private int m3steps;
    

    /**
     * Factory to create a new instance of this class from a JSON string.
     * 
     * @param json A valid JSON string.
     * @return A new instance of this class.
     */
    public static FtOutput fromJson(final String json)
    {
        Gson gson = new Gson();
        return gson.fromJson(json, FtOutput.class);
    }  

    /**
     * Constructor.
     * 
     * The default output is all OFF, Sequence Number = 0.
     */
    public FtOutput()
    {
        this.seqNr = 0;
        this.mcb = 0;
        this.m1steps = 0;
        this.m2steps = 0;
        this.m3steps = 0;        
    }
    
    /**
     * Sets the sequence number.
     * 
     * @param seqNr The sequence number. Allowed values are in the range of [0, 63].
     * @return THIS.
     */
    public FtOutput seqNr(final int seqNr)
    {
        if (seqNr < 0 || seqNr > 63)
        {
            throw new IllegalArgumentException("Sequence number out of range [0, 63].");
        }
        this.seqNr = seqNr;
        return this;
    }
    
    /**
     * Run motor M1 in the given turn direction for the given number of steps.
     * 
     * @param direction The commanded turn direction.
     * ON is equivalent to direction RIGHT, steps count 0. OFF takes precedence over steps count.
     * @param steps [1, 32767]. The amount of steps to turn. A step is a FALSE-TRUE-transition on digital input E2.
     * A negative value is equivalent to OFF and overrides direction.
     * 0 = run unlimited in given direction until hardware timeout (0.5 seconds).
     * @return THIS
     */
    public FtOutput m1(final Direction direction, final int steps)
    {
        mcb &= M1_OFF;
        switch(direction)
        {
            case RIGHT:
                mcb |= M1_RIGHT;
                m1steps = steps;
                break;
            case LEFT:
                mcb |= M1_LEFT;
                m1steps = steps;
                break;
            case ON:
                mcb |= M1_ON;
                m1steps = 0;
                break;
            case OFF:
                mcb &= M1_OFF;
                m1steps = 0;
                break;
            default:
                mcb &= M1_OFF;
                m1steps = 0;
                break;
        }
        if (m1steps > MAX_STEPS)
        {
            m1steps = MAX_STEPS;
        }
        else if (m1steps < 0)
        {
            mcb &= M1_OFF;
            m1steps = 0;
        }
        return this;
    }
    
    /**
     * Run motor M2 in the given turn direction for the given number of steps.
     * 
     * @param direction The commanded turn direction.
     * ON is equivalent to direction RIGHT, steps count 0. OFF takes precedence over steps count.
     * @param steps [1, 32767]. The amount of steps to turn. A step is a FALSE-TRUE-transition on digital input E4.
     * A negative value is equivalent to OFF and overrides direction.
     * 0 = run unlimited in given direction until hardware timeout (0.5 seconds).
     * @return THIS
     */
    public FtOutput m2(final Direction direction, final int steps)
    {
        mcb &= M2_OFF;
        switch(direction)
        {
            case RIGHT:
                mcb |= M2_RIGHT;
                m2steps = steps;
                break;
            case LEFT:
                mcb |= M2_LEFT;
                m2steps = steps;
                break;
            case ON:
                mcb |= M2_ON;
                m2steps = 0;
                break;
            case OFF:
                mcb &= M2_OFF;
                m2steps = 0;
                break;
            default:
                mcb &= M2_OFF;
                m2steps = 0;
                break;
        }
        if (m2steps > MAX_STEPS)
        {
            m2steps = MAX_STEPS;
        }
        else if (m2steps < 0)
        {
            mcb &= M2_OFF;
            m2steps = 0;
        }
        return this;
    }
    
    /**
     * Run motor M3 in the given turn direction for the given number of steps.
     * 
     * @param direction The commanded turn direction.
     * ON is equivalent to direction RIGHT, steps count 0. OFF takes precedence over steps count.
     * @param steps [1, 32767]. The amount of steps to turn. A step is a FALSE-TRUE-transition on digital input E6.
     * A negative value is equivalent to OFF and overrides direction.
     * 0 = run unlimited in given direction until hardware timeout (0.5 seconds).
     * @return THIS
     */
    public FtOutput m3(final Direction direction, final int steps)
    {
        mcb &= M3_OFF;
        switch(direction)
        {
            case RIGHT:
                mcb |= M3_RIGHT;
                m3steps = steps;
                break;
            case LEFT:
                mcb |= M3_LEFT;
                m3steps = steps;
                break;
            case ON:
                mcb |= M3_ON;
                m3steps = 0;
                break;
            case OFF:
                mcb &= M3_OFF;
                m3steps = 0;
                break;
            default:
                mcb &= M3_OFF;
                m3steps = 0;
                break;
        }
        if (m3steps > MAX_STEPS)
        {
            m3steps = MAX_STEPS;
        }
        else if (m3steps < 0)
        {
            mcb &= M3_OFF;
            m3steps = 0;
        }
        return this;
    }
    
    /**
     * Run motor M1 in the given direction until hardware timeout. Equivalent to steps = 0.
     * 
     * @param direction The turn direction. ON is equivalent to direction RIGHT.
     * @return THIS.
     */
    public FtOutput m1(final Direction direction)
    {
        return this.m1(direction, 0);
    }

    /**
     * Run motor M2 in the given direction until hardware timeout. Equivalent to steps = 0.
     * 
     * @param direction The turn direction. ON is equivalent to direction RIGHT.
     * @return THIS.
     */
    public FtOutput m2(final Direction direction)
    {
        return this.m2(direction, 0);
    }

    /**
     * Run motor M3 in the given direction until hardware timeout. Equivalent to steps = 0.
     * 
     * @param direction The turn direction. ON is equivalent to direction RIGHT.
     * @return THIS.
     */
    public FtOutput m3(final Direction direction)
    {
        return this.m3(direction, 0);
    }

    /**
     * Run motor M4 in the given direction until hardware timeout. M4 does not support stepping.
     * 
     * @param direction The turn direction. ON is equivalent to direction RIGHT.
     * @return THIS.
     */
    public FtOutput m4(final Direction direction)
    {
        mcb &= M4_OFF;
        switch(direction)
        {
            case RIGHT:
            case ON:
                mcb |= M4_RIGHT;
                break;
            case LEFT:
                mcb |= M4_LEFT;
                break;
            case OFF:
                mcb &= M4_OFF;
                break;
            default:
                mcb &= M4_OFF;
                break;
        }
        return this;
    }
    
    /**
     * Builds the MCC as a UTF-8 String.
     * 
     * @return The MCC string in the format '$mcb,step1,step2,step3,seqNr\n'.
     * The sequence number is ommited if seqNr = 0. '\n' = LF. 
     */
    public String build()
    {
        String ret;
        if (seqNr != 0)
        {
            ret = String.format("$%d,%d,%d,%d,%d\n", mcb, m1steps, m2steps, m3steps, seqNr);
        }
        else
        {
            ret = String.format("$%d,%d,%d,%d\n", mcb, m1steps, m2steps, m3steps);            
        }
        return ret;
    }
    
    /**
     * Builds the MCC as a byte array of US-ASCII-encoded characters.
     * 
     * @return A byte array.
     */
    public byte[] bytes()
    {
        return this.build().getBytes(Charset.forName("US-ASCII"));
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
        hcb.append(this.build());
        return hcb.toHashCode();
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof FtOutput))
        {
            return false;
        }
        FtOutput rhs = (FtOutput) obj;
        EqualsBuilder eqb = new EqualsBuilder();
        eqb.append(this.build(), rhs.build());
        return eqb.isEquals();        
    }    
    
}
