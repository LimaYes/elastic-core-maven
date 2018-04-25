package org.xel;

import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xel.computation.CommandPowBty;
import org.xel.computation.IComputationAttachment;
import org.xel.computation.MessageEncoder;
import org.xel.crypto.Crypto;
import org.xel.helpers.RedeemFunctions;
import org.xel.util.Logger;

import java.util.*;

import static org.xel.helpers.TransactionBuilder.make;

public class BlockTxVerifyTest extends AbstractForgingTest {

    protected static boolean isNxtInitted = false;


    @Before
    public void init() {
        if(!isNxtInitted && !Nxt.isInitialized()) {
            Properties properties = AbstractForgingTest.newTestProperties();
            properties.setProperty("nxt.disableGenerateBlocksThread", "false");
            properties.setProperty("nxt.enableFakeForging", "true");
            properties.setProperty("nxt.timeMultiplier", "1000");
            AbstractForgingTest.init(properties);
            Assert.assertTrue("nxt.fakeForgingAccount must be defined in nxt.properties", Nxt.getStringProperty("nxt.fakeForgingAccount") != null);
            isNxtInitted = true;
        }
    }

    @After
    public void destroy() {
        AbstractForgingTest.shutdown(); Nxt.shutdown();
    }

    // strange block
    // 2018-04-04 08:58:56 INFO: Block 13877: new minimal target = d99ce8a43b6414b994a309a6e9d, powMass = 4, thisTarget = 41527181659603, newT = 49002074358331, actTime = 42, targetTime = 24.0, adjRatio = 1.18
    // 2018-04-04 09:00:23 INFO: Block 13878: new minimal target = 100c8832831971a7ed33b43b59d5, powMass = 0, thisTarget = 49002074358331, newT = 57822447742830, actTime = 87, targetTime = 0.0, adjRatio = 1.18
    /*

    Clearly, the GUI indicated something different
    13878	4/4/2018 9:00:02	0	3.6	36	XEL-2S3Z-QQYR-WTJU-9MUX6	9 KB	5054 %
    13877	4/4/2018 8:58:35	0	3.5	35	XEL-2S3Z-QQYR-WTJU-9MUX6	9 KB	5054 %
     */
    @Test
    public void testStrangeBlock(){

        int popofftarget = 18685;
        Nxt.getBlockchainProcessor().popOffTo(popofftarget);
        Block b = Nxt.getBlockchain().getBlock(Nxt.getBlockchain().getBlockIdAtHeight(popofftarget));
        Nxt.getBlockchainProcessor().popOffTo(popofftarget-1);
        try {
            Nxt.getBlockchainProcessor().pushBlock((BlockImpl) b);
        } catch (BlockchainProcessor.BlockNotAcceptedException e) {
            e.printStackTrace();
        }

        System.out.println("TX: " + b.getTransactions().size());
        System.out.println("POW: " + b.getPowMass());
        System.out.println(b.getJSONObject().toJSONString());


        if(1==1)return;
        /*Nxt.getBlockchainProcessor().popOffTo(11593);
        try {
            Nxt.getBlockchainProcessor().pushBlock((BlockImpl)b);
        } catch (BlockchainProcessor.BlockNotAcceptedException e) {
            e.printStackTrace();
        }*/

        for(Transaction t : b.getTransactions()){
            Appendix.Message m = t.getMessage();
            if(m==null) {
                System.out.println("Skipping tx " + t.getStringId());
                continue;
            }
            if(MessageEncoder.checkMessageForPiggyback(m, true, false)){
                try {
                    Appendix.Message[] reconstructedChain = MessageEncoder.extractMessages(t);

                    // Allow the decoding of the attachment
                    IComputationAttachment att = MessageEncoder.decodeAttachment(reconstructedChain);
                    if(att == null) {
                        System.out.println("Skipping tx " + t.getStringId() + ", zero attachment");

                        continue;
                    }

                    String attval = "N/A";
                    if(att instanceof CommandPowBty)
                        attval = ((CommandPowBty)att).validate(t, true)?"true":"false";
                    System.out.println("Applying tx " + t.getStringId() + ", correct -> type was " + att.toString() + ", validation = " + attval);



                } catch (Exception e) {
                    System.out.println("Skipping tx " + t.getStringId() + ", appearently no piggyback");
                    // generous catch, do not allow anything to cripple the blockchain integrity
                    continue;
                }
            }
        }
    }



}
