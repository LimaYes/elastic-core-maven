package org.xel.computation;

import com.realitysink.cover.ComputationResult;
import com.realitysink.cover.CoverMain;
import org.xel.*;
import org.xel.util.Logger;

import java.io.*;
import java.util.Properties;
import java.util.Scanner;

import static com.realitysink.cover.CoverMain.executeSource;
import static org.xel.Nxt.NXT_DEFAULT_TESTVM_PROPERTIES;
import static org.xel.Nxt.loadProperties;

public class ExecutionEngine {

    private static final Properties defaultProperties = new Properties();
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();


    public static byte[] getMaximumTargetForTesting() {
        byte[] target = new byte[32];
        for(int i=0; i<32; ++i) target[i] = (byte)0xff;
        return target;
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


    /**Writes to nowhere*/
    private class NullOutputStream extends OutputStream {
        @Override
        public void write(int b) throws IOException {
        }
    }


    public String getEplCode(String filename) throws FileNotFoundException {
        String content = null;
        content = new Scanner(new File(filename)).useDelimiter("\\Z").next();
        return content;
    }

    public String getEplCode(long jobId) throws FileNotFoundException {
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
        int[] storage = new int[sz];
        int[] combined_storage = w.getCombined_storage();


        return storage;
    }

    public int[] getDummyStorage(long jobId) throws FileNotFoundException {
        Work w = Work.getWork(jobId);
        if(w==null) throw new FileNotFoundException("No job with id " + jobId);
        int[] storage = new int[w.getStorage_size()];
        return storage;

    }

    public String convertToC(String code) throws Exceptions.SyntaxErrorException {
        TokenManager t = new TokenManager();
        t.build_token_list(code);
        ASTBuilder.parse_token_list(t.state);
        CodeConverter.convert_verify(t.state);

        String result = "";
        for (int i = 0; i < t.state.stack_code.size(); ++i) {
            result += t.state.stack_code.get(i);
        }

        String c_code = "";
        c_code += "int i[" + t.state.ast_vm_ints + "];\n";
        c_code += "uint u[" + t.state.ast_vm_uints + "];\n";
        c_code += "double d[" + t.state.ast_vm_doubles + "];\n";
        c_code += "float f[" + t.state.ast_vm_floats + "];\n";
        c_code += "long l[" + t.state.ast_vm_longs + "];\n";
        c_code += "ulong ul[" + t.state.ast_vm_ulongs + "];\n";
        c_code += "int bounty_found = 0;\n";
        c_code += "int pow_found = 0;\n";
        // c_code += "uint s[" + t.state.ast_submit_sz + "];\n"; !! This one gets filled elsewhere
        c_code += result;

        return c_code;
    }

    public ComputationResult compute(final byte[] target, final byte[] publicKey, final long blockId, final byte[] multiplicator, final long workId, final int storage_idx) throws Exception {
        String epl;
        if(workId == -1)
            epl = getEplCode(getStringProperty("nxt.test_file"));
        else
            epl = getEplCode(workId);


        String c = convertToC(epl);

        System.err.println(c);

        ComputationResult comp = CoverMain.getComputationResult();
        int[] pInts = PersonalizedInts.personalizedIntStream(publicKey, blockId, multiplicator, workId);

        boolean debugInts = getBooleanProperty("nxt.debug_job_execution");


        int[] storage = null;
        if(storage_idx == -1)
            storage = new int[0];
        else if(storage_idx == -2)
            storage = getDummyStorage(workId);
        else
            storage = getStorage(workId, storage_idx);

        comp.storage = storage;
        comp.personalized_ints = pInts;
        comp.targetWas = target;

        String title = null;
        String title_line = null;
        String other_line = null;

        if(debugInts){
            title = "Dumping personalized-ints for job " + workId;
            title_line = new String(new char[title.length()]).replace("\0", "=");
            other_line = new String(new char[title.length()]).replace("\0", "-");

            System.out.println(title_line);
            System.out.println("Dumping personalized-ints for job " + workId);
            System.out.println(title_line);
            System.out.println("Public Key:");
            System.out.println(bytesToHex(publicKey));
            System.out.println("Multiplicator:");
            System.out.println(bytesToHex(publicKey));
            System.out.println("Block: " + blockId);
            System.out.println(other_line);
            System.out.println("Storage Ints (#" + storage.length + "):");
            for(int x : storage){
                System.out.println(x);
            }
        }

        ComputationResult r = CoverMain.executeSource(c, System.in, new PrintStream(new NullOutputStream()), storage);

        if(debugInts) {
            System.out.println(other_line);
            System.out.println("Result is POW: " + r.isPow);
            System.out.println("Result is BTY: " + r.isBounty);
            System.out.println("Pow Hash: " + bytesToHex(r.powHash));
            System.out.println("Pow Trgt: " + bytesToHex(r.targetWas));
            System.out.println(title_line);
        }

        return r;
    }


}
