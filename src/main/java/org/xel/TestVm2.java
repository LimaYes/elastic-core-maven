package org.xel;


import com.realitysink.cover.ComputationResult;
import org.xel.computation.ComputationConstants;
import org.xel.computation.ExecutionEngine;

public class TestVm2 {

    public static void main(String[] args) {

        ExecutionEngine e = new ExecutionEngine();
        byte[] target =  e.getMaximumTargetForTesting();
        target[0] = 0x4D; // make it a bit more difficult
        byte[] publicKey = new byte[]{(byte)0xF1, (byte)0x6D, (byte)0x48, (byte)0x25, (byte)0x0C, (byte)0xE2, (byte)0xA2, (byte)0xA4, (byte)0xFD, (byte)0x4D, (byte)0x9B, (byte)0x08, (byte)0x57, (byte)0x7B, (byte)0x2D, (byte)0x3F, (byte)0x92, (byte)0xC6, (byte)0x4D, (byte)0x09, (byte)0x3C, (byte)0xD9, (byte)0x68, (byte)0xE6, (byte)0xC7, (byte)0x32, (byte)0x5E, (byte)0x40, (byte)0x30, (byte)0xB7, (byte)0xF2, (byte)0x06 };
        long blockId = 123456789;
        long workId = -1;
        byte[] multi = publicKey; // leave it empty ffs
        int storage_id = -1;


        try {
            ComputationResult r = e.compute(target, publicKey, blockId, multi, workId, storage_id);
            System.out.println("Result is: POW=" + r.isPow + ", BTY=" + r.isBounty);
        } catch (Exception e1) {
            e1.printStackTrace();
        }


    }
}
