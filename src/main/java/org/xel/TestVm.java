package org.xel;


import com.realitysink.cover.CoverMain;
import org.xel.util.Logger;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import static org.xel.Nxt.NXT_DEFAULT_TESTVM_PROPERTIES;
import static org.xel.Nxt.loadProperties;


public class TestVm {

    private static class OutputStreamCombiner extends OutputStream {
        private List<OutputStream> outputStreams;

        public OutputStreamCombiner(List<OutputStream> outputStreams) {
            this.outputStreams = outputStreams;
        }

        public void write(int b) throws IOException {
            for (OutputStream os : outputStreams) {
                os.write(b);
            }
        }

        public void flush() throws IOException {
            for (OutputStream os : outputStreams) {
                os.flush();
            }
        }

        public void close() throws IOException {
            for (OutputStream os : outputStreams) {
                os.close();
            }
        }
    }

    private static final Properties defaultProperties = new Properties();

    static{
        System.out.println(System.getProperty("java.class.path"));
        loadProperties(defaultProperties,NXT_DEFAULT_TESTVM_PROPERTIES,true);
    }

    public static String getStringProperty(String name) {
        return getStringProperty(name, null, false);
    }

    public static String getStringProperty(String name, String defaultValue) {
        return getStringProperty(name, defaultValue, false);
    }

    private static ByteArrayOutputStream baos;
    private static PrintStream previous;
    private static PrintStream previous2;

    private static boolean capturing;

    public static void start() {
        if (capturing) {
            return;
        }

        capturing = true;
        previous = System.out;
        previous2 = System.err;
        baos = new ByteArrayOutputStream();

        OutputStream outputStreamCombiner =
                new OutputStreamCombiner(Arrays.asList(previous, baos));
        OutputStream outputStreamCombiner2 =
                new OutputStreamCombiner(Arrays.asList(previous2, baos));
        PrintStream custom = new PrintStream(outputStreamCombiner);
        PrintStream custom2 = new PrintStream(outputStreamCombiner2);

        System.setOut(custom);
        System.setErr(custom2);
    }

    public static String stop() {
        if (!capturing) {
            return "";
        }

        System.setOut(previous);
        System.setErr(previous2);
        String capturedValue = baos.toString();

        baos = null;
        previous = null;
        previous2 = null;
        capturing = false;

        return capturedValue;
    }

    public static String getStringProperty(String name, String defaultValue, boolean doNotLog) {
        String value = defaultProperties.getProperty(name);
        if (value != null && !"".equals(value)) {
            Logger.logMessage(name + " = \"" + (doNotLog ? "{not logged}" : value) + "\"");
            return value;
        } else {
            Logger.logMessage(name + " not defined");
            return defaultValue;
        }
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

    public static void main(String[] args) {
        String file = getStringProperty("nxt.test_file");
        String content = null;
        try {
            content = new Scanner(new File(file)).useDelimiter("\\Z").next();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }
        String result = exec(content);
        //System.out.println(result);
    }

    public static synchronized String exec(String content){

        start();
        boolean dumpTokens = getBooleanProperty("nxt.dump_tokens");
        boolean dumpAst = getBooleanProperty("nxt.dump_ast");
        boolean dumpCode = getBooleanProperty("nxt.dump_code");
        boolean exec = getBooleanProperty("nxt.execute_code");
        try {
            String c_code = CodeGetter.convert(content);

            if (dumpCode) {
                Logger.logMessage("Dumping generated source code");
                System.out.flush();
                System.err.flush();
                System.out.println("--BEGIN CODE\n" + c_code + "\n--END CODE");
            }

            if (exec) {

                int validator_offset_index = 0;
                Logger.logMessage("We will now execute the code");
                CoverMain.executeSource(c_code, System.in, System.out);
            }
        }
        catch (Exception e) {
            Logger.logErrorMessage("The following syntax error has been found");
            System.out.flush();
            System.err.flush();
            e.printStackTrace();
        }
        return stop();
    }
    private static int[] fakeInts() {
        int[] m = new int[12]; //personalizedIntStream(publicKey, 123456789, multiplier, 12345);
        return m;
    }
}
