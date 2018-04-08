package org.xel;

import org.junit.Assert;
import org.junit.Test;
import org.xel.computation.Pair;
import org.xel.computation.TimedCacheList;
import org.xel.crypto.Crypto;
import org.xel.crypto.Curve25519;
import org.xel.util.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Created by anonymous on 27.05.17.
 */
public class BigDecimalTest {

    public static byte[] getPublicKey(String secretPhrase) {
        byte[] publicKey = new byte[32];
        byte[] phr = Crypto.sha256().digest(Convert.toBytes(secretPhrase));
        System.out.println("SHA(mnemonic) = " + Convert.toHexString(phr));

        Curve25519.clamp(phr);
        System.out.println("priv = " + Convert.toHexString(phr));

        Curve25519.keygen(publicKey, null, phr);
        System.out.println("pub = " + Convert.toHexString(publicKey));

        byte[] publicKeyHash = Crypto.sha256().digest(publicKey);
        System.out.println("SHA(pub) = " + Convert.toHexString(publicKeyHash));
        Convert.fullHashToId(publicKeyHash);

        return publicKey;
    }

    @Test
    public void testBigDecimalFraction(){
        long alreadyClaimed=9999999L*Constants.ONE_NXT;
        BigDecimal bd = new BigDecimal(alreadyClaimed, MathContext.DECIMAL32);
        BigDecimal ad = new BigDecimal(Constants.MAX_BALANCE_NQT, MathContext.DECIMAL32);
        ad = ad.divide(bd, 0, RoundingMode.HALF_UP);

        BigInteger bal = new BigInteger(String.valueOf(alreadyClaimed));
        bal = bal.multiply(ad.toBigInteger());

        System.out.println("Factor: " + ad.toPlainString());
        System.out.println("Scaled: " + bal.toString());

        System.out.println(Long.toUnsignedString(-4746849185247918166L));

        Pair<Long, Long> p1 = new Pair<>(43634634634634634L,-4242758255235L);
        Pair<Long, Long> p2 = new Pair<>(43634634634634634L,-4242758255235L);
        //System.out.println("Hashcode comparison: " + p1.hashCode() + " == " + p2.hashCode() + ", equality = " + p1.equals(p2));

        TimedCacheList timedCacheList = new TimedCacheList();
        timedCacheList.put(p1.getElement0(), p1.getElement1());
        boolean has = timedCacheList.has(p2.getElement0(), p2.getElement1());
        System.out.println("Does caching work? " + has);

        Assert.assertTrue(bal.longValue()<=Constants.MAX_BALANCE_NQT);

        getPublicKey("congress return adult flight thing language pencil bamboo explain sting depart stable");
    }
}
