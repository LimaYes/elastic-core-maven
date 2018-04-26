package org.xel;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xel.computation.IComputationAttachment;
import org.xel.computation.MessageEncoder;
import org.xel.crypto.Crypto;
import org.xel.db.DbIterator;
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
    public void testACP(){
        byte[] pb = Crypto.getPublicKey("tester");
        long id = Account.getId(pb);
        AlternativeChainPubkeys.addKnownIdentity(id,pb);
        AlternativeChainPubkeys ret = AlternativeChainPubkeys.getKnownIdentity(id);
        System.out.println(Convert.toHexString(pb));
        System.out.println(Convert.toHexString(ret.getPubkey()));
    }
    @Test
    public void getRawBlock(){
        Work w = Work.getWorkById(Long.parseUnsignedLong("16396654591714241603"));

        System.out.println("Inspecting work " + w.getId());
        try(DbIterator<PowAndBounty> it = PowAndBounty.getLastBountiesRelevantForStorageGeneration(w.getId(),1,0,1)){
            while(it.hasNext()){
                PowAndBounty b = it.next();
                if(b.is_pow==true) continue;
                String raw = Convert.toHexString(b.getSubmitted_storage());
                int[] resbty = Convert.byte2int(b.getSubmitted_storage());
                System.out.println("Bty " + b.getId() + " - storage = " + resbty[0] + " - raw " + raw);
            }
        }


    }



}
