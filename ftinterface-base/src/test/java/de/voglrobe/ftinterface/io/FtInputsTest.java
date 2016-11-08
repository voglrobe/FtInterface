package de.voglrobe.ftinterface.io;

import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author robert
 */
public class FtInputsTest
{
    @Test
    public void testToJson() throws Exception
    {
        FtInputs inputs = new FtInputs(63, 64, 231, 255);
        
        String json = inputs.toJson();
        System.out.println(json);       
    }
    
    @Test
    public void testFromJson() throws Exception
    {
        String json = "{\"seqnr\":63,\"di\":[false,true,false,false,false,false,false,false],\"ex\":231,\"ey\":255}";
        
        FtInputs inputs = FtInputs.fromJson(json);
        Assert.assertNotNull(json);
        
        Assert.assertEquals(63, inputs.getSeqNr());
        Assert.assertEquals(231, inputs.getEx());
        Assert.assertEquals(255, inputs.getEy());
        
        List<Boolean> digitalInList = inputs.getDigitalIn();
        Assert.assertEquals(8, digitalInList.size());
        
        Assert.assertTrue(inputs.getDigitalIn(FtInputs.IN.E2));
        
        Assert.assertFalse(digitalInList.get(0));
        Assert.assertFalse(digitalInList.get(2));
        Assert.assertFalse(digitalInList.get(3));
        Assert.assertFalse(digitalInList.get(4));
        Assert.assertFalse(digitalInList.get(5));
        Assert.assertFalse(digitalInList.get(6));
        Assert.assertFalse(digitalInList.get(7));
    }
}
