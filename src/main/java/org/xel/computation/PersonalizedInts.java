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
    public static int[] personalizedIntStream(final byte[] publicKey, final long blockId, final byte[] multiplicator, final long workId) throws Exception {
        final int[] stream = new int[12];

        dig.reset();
        dig.update(multiplicator);
        dig.update(publicKey);

        System.out.println("Calculating Personalized Int Stream");
        System.out.println("Multiplicator: " + Convert.toHexString(multiplicator));
        System.out.println("PublicKey: " + Convert.toHexString(publicKey));

        final byte[] b1 = new byte[16];
        for (int i = 0; i < 8; ++i) b1[i] = (byte) (workId >> ((8 - i - 1) << 3));
        for (int i = 0; i < 8; ++i) b1[i + 8] = (byte) (blockId >> ((8 - i - 1) << 3));

        dig.update(b1);
        System.out.println("TotalBytes: " + (16+multiplicator.length+publicKey.length));

        System.out.println("b1: " + Convert.toHexString(b1));

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
        stream[10] = multi32[1];
        stream[11] = multi32[2];
        System.out.println("10" + ": " + Integer.toHexString(stream[10]));
        System.out.println("11" + ": " + Integer.toHexString(stream[11]));


        return stream;
    }

}
