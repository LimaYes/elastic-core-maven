package org.xel.computation;

import org.xel.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class ExecutionEngine {

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

    public int[] getDummyStorage(long jobId, long storage_idx) throws FileNotFoundException {
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
        c_code += "uint m[12];\n";
        c_code += "uint s[" + t.state.ast_submit_sz + "];\n";
        c_code += result;

        return c_code;
    }

    public ComputationResult compute(final byte[] publicKey, final long blockId, final byte[] multiplicator, final long workId, final int storage_idx) throws Exception {
        String epl = getEplCode(workId);
        String c = convertToC(epl);
        int[] pInts = PersonalizedInts.personalizedIntStream(publicKey, blockId, multiplicator, workId);
        int[] storage = getStorage(workId, storage_idx);

        ComputationResult r = null;

        return r;
    }

    private class ComputationResult {
        public boolean isPow;
        public boolean isBounty;
    }
}
