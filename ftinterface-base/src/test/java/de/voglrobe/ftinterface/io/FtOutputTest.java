package de.voglrobe.ftinterface.io;

import junit.framework.Assert;
import org.junit.Test;

/**
 *
 * @author vlr
 */
public class FtOutputTest
{
    @Test
    public void testBuild_AllOff() throws Exception
    {
        FtOutput out = new FtOutput();
        String ret = out.build();
        
        Assert.assertEquals("$0,0,0,0\n", ret);
    }

    @Test
    public void testBuild_AllLeft() throws Exception
    {
        FtOutput out = new FtOutput()
                .m1(FtOutput.Direction.RIGHT, 100)
                .m1(FtOutput.Direction.LEFT, 100)
                .m2(FtOutput.Direction.LEFT, 200)
                .m3(FtOutput.Direction.LEFT, 300)
                .m4(FtOutput.Direction.LEFT);
        String ret = out.seqNr(63).build();
        
        // 128, 64, 32, 16, 8, 4, 2, 1
        Assert.assertEquals("$170,100,200,300,63\n", ret);
    }

    @Test
    public void testBuild_AllRight() throws Exception
    {
        FtOutput out = new FtOutput()
                .m1(FtOutput.Direction.RIGHT, 100)
                .m2(FtOutput.Direction.RIGHT, 200)
                .m3(FtOutput.Direction.RIGHT, 300)
                .m4(FtOutput.Direction.RIGHT);
        String ret = out.build();
        
        // 128, 64, 32, 16, 8, 4, 2, 1
        Assert.assertEquals("$85,100,200,300\n", ret);
    }

    @Test
    public void testToJson() throws Exception
    {
        FtOutput out = new FtOutput()
                .m1(FtOutput.Direction.RIGHT, 100)
                .m2(FtOutput.Direction.RIGHT, 200)
                .m3(FtOutput.Direction.RIGHT, 300)
                .m4(FtOutput.Direction.RIGHT);

        String json = out.toJson();
        System.out.println(json);
    }
    
    @Test
    public void testFromJson() throws Exception
    {
        String json = "{\"seqnr\":0,\"mcb\":85,\"m1steps\":100,\"m2steps\":200,\"m3steps\":300}";
        
        FtOutput out = FtOutput.fromJson(json);
        Assert.assertNotNull(out);
        
        System.out.println(out.build());
    }
    
    
}
