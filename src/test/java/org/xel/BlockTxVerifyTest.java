package org.xel;

import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
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
        AbstractForgingTest.shutdown();
    }

    @Test
    public void testStrangeBlock(){
        Block b = Nxt.getBlockchain().getBlock(Nxt.getBlockchain().getBlockIdAtHeight(11594));
        System.out.println("TX: " + b.getTransactions().size());
        System.out.println("POW: " + b.getPowMass());
        System.out.println(b.getJSONObject().toJSONString());

        /*Nxt.getBlockchainProcessor().popOffTo(11593);
        try {
            Nxt.getBlockchainProcessor().pushBlock((BlockImpl)b);
        } catch (BlockchainProcessor.BlockNotAcceptedException e) {
            e.printStackTrace();
        }*/

        if(1==1) return;
        for(Transaction t : b.getTransactions()){
            Appendix.PrunablePlainMessage m = t.getPrunablePlainMessage();
            if(m==null || !m.hasPrunableData()) {
                System.out.println("Skipping tx " + t.getStringId());
                continue;
            }
            if(MessageEncoder.checkMessageForPiggyback(m, true, false)){
                try {
                    Appendix.PrunablePlainMessage[] reconstructedChain = MessageEncoder.extractMessages(t);

                    // Allow the decoding of the attachment
                    IComputationAttachment att = MessageEncoder.decodeAttachment(reconstructedChain);
                    if(att == null) {
                        System.out.println("Skipping tx " + t.getStringId() + ", zero attachment");

                        continue;
                    }
                    System.out.println("Applying tx " + t.getStringId() + ", correct");



                } catch (Exception e) {
                    System.out.println("Skipping tx " + t.getStringId() + ", appearently no piggyback");
                    // generous catch, do not allow anything to cripple the blockchain integrity
                    continue;
                }
            }
        }
    }



}
