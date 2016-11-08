package de.voglrobe.ftinterface;

import de.voglrobe.ftinterface.io.FtInputs;
import de.voglrobe.ftinterface.io.FtOutput;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;

/**
 *
 * @author vlr
 */
public class FtInterfaceTest
{
    private static final String PORT = "/dev/ttyACM0";

    // @Test
    public void testNewInstance() throws Exception
    {
        FtInterface iface = null;
        try
        {
            iface = FtInterface.newInstance(PORT);
            Assert.assertNotNull(iface);
        }
        finally
        {
            if (iface != null)
            {
                iface.destroy();
            }
        }
    }
    
    // @Test
    public void testSend_Command1() throws Exception
    {
        FtInterface iface = null;
        try
        {
            iface = FtInterface.newInstance(PORT);
            FtOutput output1 = new FtOutput()
                    .m1(FtOutput.Direction.LEFT, 256)
                    .m3(FtOutput.Direction.RIGHT, 3);
            
            FtOutput output2 = new FtOutput()
                    .m1(FtOutput.Direction.RIGHT, 256)
                    .m2(FtOutput.Direction.LEFT, 3);

            FtInputs inputs = iface.send(output1);
            Assert.assertNotNull(inputs);
            System.out.println("DI: " + inputs.getDigitalIn() + ", EX: " + inputs.getEx() + ", EY: " + inputs.getEy());
            
            inputs = iface.send(output2);
            Assert.assertNotNull(inputs);
            System.out.println("DI: " + inputs.getDigitalIn() + ", EX: " + inputs.getEx() + ", EY: " + inputs.getEy());            
        }
        finally
        {
            if (iface != null)
            {
                iface.destroy();
            }
        }
    }

    // @Test
    public void testSend_Command2() throws Exception
    {
        FtInterface iface = null;
        try
        {
            iface = FtInterface.newInstance(PORT);
            
            FtOutput outputM1_l = new FtOutput().m1(FtOutput.Direction.LEFT, 256).m4(FtOutput.Direction.ON);
            FtOutput outputM1_r = new FtOutput().m1(FtOutput.Direction.RIGHT, 256).m4(FtOutput.Direction.ON);
            FtOutput outputM2 = new FtOutput().m2(FtOutput.Direction.ON);
            FtOutput outputM3 = new FtOutput().m3(FtOutput.Direction.ON);
            FtOutput outputM4 = new FtOutput().m4(FtOutput.Direction.ON);
            FtOutput outputOff = new FtOutput();
            
            iface.send(outputM2, 2);
            iface.send(outputM3, 2);
            iface.send(outputM4, 2);
            FtInputs inputs = iface.send(outputM1_l);
            if (inputs.getDigitalIn(FtInputs.IN.E6)) // check E6
            {
                iface.send(outputM1_r);
            }
            
            // some blinking...
            for (int i=0; i<5; i++)
            {
                iface.send(outputM2, 1);
                iface.send(outputM3, 1);
            }
            
            // ...and all OFF
            inputs = iface.send(outputOff);
            System.out.println("DI: " + inputs.getDigitalIn() + ", EX: " + inputs.getEx() + ", EY: " + inputs.getEy());            
        }
        finally
        {
            if (iface != null)
            {
                iface.destroy();
            }
        }
    }
    
    // @Test
    public void testSend_ConditionalCommand() throws Exception
    {
        FtInterface iface = null;
        try
        {
            iface = FtInterface.newInstance(PORT);
            
            FtOutput outputM4 = new FtOutput().m4(FtOutput.Direction.ON);
            FtOutput outputOff = new FtOutput();
            
            // Wait for E4 + E6
            List<Boolean> cond = Arrays.asList(new Boolean[]{null, null, null, true, null, true, null, null});

            FtInputs inputs = iface.send(outputM4, cond);
            iface.send(outputOff);
            System.out.println("DI: " + inputs.getDigitalIn() + ", EX: " + inputs.getEx() + ", EY: " + inputs.getEy());            
        }
        finally
        {
            if (iface != null)
            {
                iface.destroy();
            }
        }
    }



}
