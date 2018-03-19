package org.xel.computation;

import org.xel.util.Convert;
import static java.security.MessageDigest.getInstance;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PersonalizedInts {
    public static MessageDigest dig = null;

    static {
        try {
            dig = getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            // Should always work
            e.printStackTrace();
            System.exit(1);
        }
    }
    public static int toInt(final byte[] bytes, final int offset) {
        int ret = 0;
        for (int i = 0; (i < 4) && ((i + offset) < bytes.length); i++) {
            ret <<= 8;
            ret |= bytes[i + offset] & 0xFF;
        }
        return ret;
    }

    public static int swap (int value)
    {
        int b1 = (value >>  0) & 0xff;
        int b2 = (value >>  8) & 0xff;
        int b3 = (value >> 16) & 0xff;
        int b4 = (value >> 24) & 0xff;

        return b1 << 24 | b2 << 16 | b3 << 8 | b4 << 0;
    }

    public static int[] personalizedIntStream(final byte[] publicKey, final long blockId, final byte[] multiplicator, final long workId) throws Exception {
        final int[] stream = new int[12];

        dig.reset();
        System.out.println("Calculating Personalized Int Stream");
        System.out.println("Multiplicator: " + Convert.toHexString(multiplicator));
        System.out.println("PublicKey: " + Convert.toHexString(publicKey));
        System.out.println("WID: " + workId);
        System.out.println("BID: " + blockId);

        final byte[] b1 = new byte[80];
        for (int i = 0; i < 32; ++i) b1[i] = multiplicator[i];
        for (int i = 0; i < 32; ++i) b1[32+i] = publicKey[i];
        for (int i = 0; i < 8; ++i) b1[64+i] = (byte) (workId >> ((8 - i - 1) << 3));
        for (int i = 0; i < 8; ++i) b1[72+i] = (byte) (blockId >> ((8 - i - 1) << 3));

        dig.update(b1);
        System.out.println("Input: " + Convert.toHexString(b1));

        byte[] digest = dig.digest();

        System.out.println("Digest: " + Convert.toHexString(digest));

        int ln = digest.length;
        if (ln == 0) {
            throw new Exception("Bad digest calculation");
        }

        int[] multi32 = Convert.byte2int(multiplicator);
        System.out.println("Resultierende Ints");

        for (int i = 0; i < 10; ++i) {
            int got = toInt(digest, (i * 4) % ln);
            if (i > 4) got = got ^ stream[i - 3];
            stream[i] = got;
            System.out.println(i + ": " + Integer.toHexString(stream[i]));

        }
        stream[10] = swap(multi32[1]);
        stream[11] = swap(multi32[2]);
        System.out.println("10" + ": " + Integer.toHexString(stream[10]));
        System.out.println("11" + ": " + Integer.toHexString(stream[11]));


        return stream;
    }

}
