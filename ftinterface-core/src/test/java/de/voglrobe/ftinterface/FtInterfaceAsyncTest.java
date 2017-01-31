package de.voglrobe.ftinterface;

import de.voglrobe.ftinterface.io.FtInputs;
import de.voglrobe.ftinterface.io.FtInputsFlags;
import de.voglrobe.ftinterface.io.FtOutput;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import junit.framework.Assert;

/**
 *
 * @author vlr
 */
public class FtInterfaceAsyncTest
{
    private static final String PORT = "/dev/ttyACM0";
    private volatile boolean stopped;
    
    private void snort(final long duration)
    {
        try
        {
            Thread.sleep(duration);
        }
        catch(InterruptedException dontcare)
        {
        }       
    }
    
    // @Test
    public void testNewInstance() throws Exception
    {
        FtInterfaceAsync iface = null;
        try
        {
            iface = FtInterfaceAsync.newInstance(PORT, null);
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
    public void testNewInstanceWithReceiver() throws Exception
    {
        FtInterfaceAsync iface = null;
        try
        {
            iface = FtInterfaceAsync.newInstance(PORT, (final FtInputs input)->
            {
                // Do nothing
            });
            Assert.assertNotNull(iface);       
            snort(10000L);
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
        FtInterfaceAsync iface = null;
        try
        {
            iface = FtInterfaceAsync.newInstance(PORT, null);
            FtOutput output = new FtOutput()
                    .m4(FtOutput.Direction.ON);

            iface.send(output);
            snort(10000L);
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
        FtInterfaceAsync iface = null;
        try
        {
            iface = FtInterfaceAsync.newInstance(PORT, (final FtInputs inputs)->
            {
                System.out.println("DI: " + inputs.getDigitalIn() + ", EX: " + inputs.getEx() + ", EY: " + inputs.getEy());
            });
            FtOutput outputM2 = new FtOutput().m2(FtOutput.Direction.ON);
            FtOutput outputM3 = new FtOutput().m3(FtOutput.Direction.ON);
            FtOutput outputM4 = new FtOutput().m4(FtOutput.Direction.ON);
            FtOutput outputOff = new FtOutput();

            iface.send(outputM2, 2);
            iface.send(outputOff, 1);
            iface.send(outputM3, 2);
            iface.send(outputOff, 1);
            iface.send(outputM4, 2);
            iface.send(outputOff);
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
    public void testSend_Command3() throws Exception
    {
        FtInterfaceAsync iface = null;
        try
        {
            CountDownLatch latch = new CountDownLatch(2);
            iface = FtInterfaceAsync.newInstance(PORT, (final FtInputs inputs)->
            {
                System.out.println("DI: " + inputs.getDigitalIn() + ", EX: " + inputs.getEx() + ", EY: " + inputs.getEy());
                latch.countDown();
            });
            
            FtOutput outputM1_left = new FtOutput().m1(FtOutput.Direction.LEFT, 256);
            FtOutput outputM1_right = new FtOutput().m1(FtOutput.Direction.RIGHT, 256);

            iface.send(outputM1_left);
            iface.send(outputM1_right);
            latch.await();
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
    public void testSend_Command4() throws Exception
    {
        FtInterfaceAsync iface = null;
        try
        {
            CountDownLatch latch = new CountDownLatch(1);
            iface = FtInterfaceAsync.newInstance(PORT, (final FtInputs inputs)->
            {
                System.out.println("DI: " + inputs.getDigitalIn() + ", EX: " + inputs.getEx() + ", EY: " + inputs.getEy());
                if (inputs.getDigitalIn().get(5))
                {
                    latch.countDown();
                }
            });
            
            final FtOutput output = new FtOutput().m4(FtOutput.Direction.ON);
            while (latch.getCount() > 0)
            {
                iface.send(output);
                snort(500L);
            }
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
    public void testSend_Command5() throws Exception
    {
        FtInterfaceAsync iface = null;
        try
        {
            iface = FtInterfaceAsync.newInstance(PORT);
            FtOutput outputM1_left = new FtOutput().m1(FtOutput.Direction.LEFT, 256);
            iface.send(outputM1_left);
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
    public void testSend_Interrupt() throws Exception
    {
        final FtInterfaceAsync iface = FtInterfaceAsync.newInstance(PORT);
        iface.setInputReceiver((final FtInputs inputs)-> 
        {
            System.out.println("DI: " + inputs.getDigitalIn() + ", EX: " + inputs.getEx() + ", EY: " + inputs.getEy());
            if (inputs.getDigitalIn(FtInputs.IN.E6))
            {
                iface.softInterrupt(new FtOutput());
            }
        });

        try
        {
            final FtOutput outputM2 = new FtOutput().m2(FtOutput.Direction.ON);
            final FtOutput outputM3 = new FtOutput().m3(FtOutput.Direction.ON);
            
            iface.send(outputM2, 10);
            iface.send(outputM3, 10);
        }
        finally
        {
            iface.destroy();
        }
    }
    
    // @Test
    public void testSendUntilNew_1() throws Exception
    {
        final FtInterfaceAsync iface = FtInterfaceAsync.newInstance(PORT);
        iface.setInputReceiver((final FtInputs inputs)-> 
        {
            System.out.println("DI: " + inputs.getDigitalIn() + ", EX: " + inputs.getEx() + ", EY: " + inputs.getEy());
            if (inputs.getDigitalIn(FtInputs.IN.E6))
            {
                iface.softInterrupt(new FtOutput());
            }
        });

        try
        {
            final FtOutput outputM2 = new FtOutput().m2(FtOutput.Direction.ON);
            final FtOutput outputM3 = new FtOutput().m3(FtOutput.Direction.ON);
            final FtOutput outputM4 = new FtOutput().m4(FtOutput.Direction.ON);
            final FtOutput outputOff = new FtOutput();
            
            iface.sendInfinite(outputM2);
            snort(5000L); // let it run for 5 seconds
            System.out.println("--> test mark 1");
            
            iface.send(outputM4, 5); // must interrupt M2
            System.out.println("--> test mark 2");

            iface.sendInfinite(outputM3);
            snort(10000L); // let it run for 10 secods
            System.out.println("--> test mark 3");

            iface.softInterrupt(outputOff);           
        }
        finally
        {
            iface.destroy();
        }
    }
    
    // @Test
    public void testCheckFlags_1() throws Exception
    {
        final FtInterfaceAsync iface = FtInterfaceAsync.newInstance(PORT);
        CountDownLatch latch = new CountDownLatch(1);
        
        iface.setInputReceiver((final FtInputs inputs)-> 
        {
            System.out.println(inputs.toJson());
            FtInputsFlags flags = inputs.getFlags();
            if (flags != null && flags.isDurationFinished())
            {
                System.out.println("Received Flag: 'durationFinished.");
                latch.countDown();
            }
        });

        try
        {
            final FtOutput outputM2 = new FtOutput().m2(FtOutput.Direction.ON);

            iface.send(outputM2, 5);
            latch.await(8, TimeUnit.SECONDS);
        }
        finally
        {
            iface.destroy();
        }
    }
    
    
}
