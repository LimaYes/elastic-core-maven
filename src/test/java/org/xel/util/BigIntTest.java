package org.xel.util;

import org.xel.Nxt;
import org.xel.computation.ComputationConstants;
import org.junit.Test;
import org.xel.computation.Scaler;

import java.math.BigInteger;

public class BigIntTest {
    @Test
    public void bigIntTest(){

        BigInteger myTarget = Scaler.get(((long)(Long.MAX_VALUE/10000.0)));
        System.out.println( String.format("%032x", myTarget));
    }
}
