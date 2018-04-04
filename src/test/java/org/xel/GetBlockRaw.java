package org.xel;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xel.computation.IComputationAttachment;
import org.xel.computation.MessageEncoder;
import org.xel.util.Convert;

import java.util.Properties;

public class GetBlockRaw extends AbstractForgingTest {

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

    @Test
    public void getRawBlock(){
        Block b = Nxt.getBlockchain().getBlock(Nxt.getBlockchain().getBlockIdAtHeight(0));

        System.out.println("JSN: " + b.getJSONObject().toJSONString());
        System.out.println("RAW: " + Convert.toHexString(b.getBytes()));
        System.out.println("HASH: " + Convert.toHexString(b.getBlockHash()));
        System.out.println("ID: " + b.getStringId());

    }



}
