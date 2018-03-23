package org.xel.computation;

import org.xel.*;
import org.xel.util.Logger;

import java.io.*;
import java.util.Properties;
import java.util.Scanner;

import static org.xel.Nxt.NXT_DEFAULT_TESTVM_PROPERTIES;
import static org.xel.Nxt.loadProperties;

public class ExecutionEngine {

    private static final Properties defaultProperties = new Properties();
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();


    public static byte[] getMaximumTargetForTesting() {
        byte[] target = new byte[16];
        for(int i=0; i<16; ++i) target[i] = (byte)0xff;
        return target;
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
    static{
        System.out.println(System.getProperty("java.class.path"));
        loadProperties(defaultProperties,NXT_DEFAULT_TESTVM_PROPERTIES,true);
    }

    public static Boolean getBooleanProperty(String name) {
        String value = defaultProperties.getProperty(name);
        if (Boolean.TRUE.toString().equals(value)) {
            Logger.logMessage(name + " = \"true\"");
            return true;
        } else if (Boolean.FALSE.toString().equals(value)) {
            Logger.logMessage(name + " = \"false\"");
            return false;
        }
        Logger.logMessage(name + " not defined, assuming false");
        return false;
    }

    public static String getStringProperty(String name) {
        String value = defaultProperties.getProperty(name);
        return value;
    }


    public static String getEplCode(String filename) throws FileNotFoundException {
        String content = null;
        content = new Scanner(new File(filename)).useDelimiter("\\Z").next();
        return content;
    }

    public static String getEplCode(long jobId) throws FileNotFoundException {
        Work w = Work.getWork(jobId);
        if(w==null) throw new FileNotFoundException("No job with id " + jobId);
        String res = w.getSource_code();
        if(res==null) throw new FileNotFoundException("Seems we already pruned job " + jobId);
        return res;
    }
    public int[] getStorage(long jobId, long storage_idx) throws FileNotFoundException {
        Work w = Work.getWork(jobId);
        if(w==null) throw new FileNotFoundException("No job with id " + jobId);
        int sz = w.getStorage_size();
        if(sz==0) return new int[0];
        int[] combined_storage = w.getCombined_storage();
        return combined_storage;
    }

    public int[] getDummyStorage() throws FileNotFoundException {
        int[] storage = new int[1000];
        return storage;

    }


    public ComputationResult compute(final byte[] target, final byte[] publicKey, final long blockId, final byte[] multiplicator, final long workId, final int storage_idx) throws Exception {
        String epl;
        if (workId == -1)
            epl = getEplCode(getStringProperty("nxt.test_file"));
        else
            epl = getEplCode(workId);

        return compute(target, publicKey, blockId, multiplicator, workId, epl, storage_idx);
    }

    public ComputationResult compute(final byte[] target, final byte[] publicKey, final long blockId, final byte[] multiplicator, final long workId, String epl, final int storage_idx) throws Exception {
        int[] storage = null;
        if(workId == -1)
            storage = getDummyStorage();
        else
            storage = getStorage(workId, storage_idx);

        FileWriter fileWriter = new FileWriter("work/code.epl");
        PrintWriter printWriter = new PrintWriter(fileWriter);
        printWriter.print(epl);
        printWriter.close();


        ComputationResult r = new ComputationResult();

        String cmd = String.format("./xel_miner --test-target %s --test-publickey %s --test-multiplicator %s --test-block %d --test-work %d --verify-only --test-vm code.epl", bytesToHex(target), bytesToHex(publicKey), bytesToHex(multiplicator), blockId, workId);
        System.out.println(cmd);
        Process process=Runtime.getRuntime().exec(cmd,
                null, new File("./work/"));
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        process.waitFor();


        while ( (line = reader.readLine()) != null) {
            line = line.replaceAll("\\[\\d+m", "").trim();

            if(line.contains("ERROR") || line.contains("Error")) throw new IOException("EPL code produced error: " + line);
            if(line.contains("DEBUG: POW Found:")){
                Boolean res = Boolean.parseBoolean(line.substring(line.lastIndexOf(":")+2));
                r.isPow = res;
            }
            if(line.contains("DEBUG: Bounty Found:")){
                Boolean res = Boolean.parseBoolean(line.substring(line.lastIndexOf(":")+2));
                r.isBty = res;
            }
            if(line.contains("DEBUG: storage size:")){
                Integer res = Integer.parseInt(line.substring(line.lastIndexOf(":")+2));
                r.storage_size = res;
            }
            if(line.contains("DEBUG: POW Hash:")){
                byte[] res = hexStringToByteArray(line.substring(line.lastIndexOf(":")+2,line.lastIndexOf(":")+2+32));
                r.powHash = res;
            }
        }

        if(process.exitValue()!=0) throw new IOException("EPL code exited with error code.");


        if(getBooleanProperty("nxt.dump_pow_info")) {
            System.out.println("Result is POW: " + r.isPow);
            System.out.println("Result is BTY: " + r.isBty);
            System.out.println("Pow Hash: " + bytesToHex(r.powHash));
        }

        return r;
    }


}
