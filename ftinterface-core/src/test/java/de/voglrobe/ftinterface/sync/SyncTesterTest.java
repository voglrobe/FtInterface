package de.voglrobe.ftinterface.sync;

/**
 *
 * @author vlr
 */
public class SyncTesterTest
{
    // @Test
    public void testSend() throws Exception
    {
        SyncTester tester = SyncTester.newInstance();
        int seqNr = tester.send();
        System.out.println("Received seqNr: " + seqNr);
        
//        seqNr = tester.send();
//        System.out.println("Received seqNr: " + seqNr);
        
        tester.destroy();
    }
}
