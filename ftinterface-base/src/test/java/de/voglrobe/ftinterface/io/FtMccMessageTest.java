package de.voglrobe.ftinterface.io;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author robert
 */
public class FtMccMessageTest
{
    @Test
    public void testToJson() throws Exception
    {
        FtOutput out = new FtOutput()
                .m1(FtOutput.Direction.RIGHT, 100)
                .m2(FtOutput.Direction.RIGHT, 200)
                .m3(FtOutput.Direction.RIGHT, 300)
                .m4(FtOutput.Direction.RIGHT);
        FtMccMessage mcc = new FtMccMessage(out);
        mcc.setDuration(23);
        
        System.out.println(mcc.toJson());        
    }
    
    
    @Test
    public void testFromJson() throws Exception
    {
        String json = "{\"mcc\":{\"seqnr\":0,\"mcb\":85,\"m1steps\":100,\"m2steps\":200,\"m3steps\":300},\"duration\":23}";
        // String json = "{\"mcc\":{\"seqnr\":0,\"mcb\":85,\"m1steps\":100,\"m2steps\":200,\"m3steps\":300}}";
        
        FtMccMessage mcc = FtMccMessage.fromJson(json);
        Assert.assertNotNull(mcc);
    }
}
