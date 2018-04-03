package org.xel.computation;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;

public class Scaler {
    public static BigInteger get(long powTarget){
        MathContext mc = new MathContext(32, RoundingMode.HALF_EVEN);
        BigDecimal myTarget = new BigDecimal(ComputationConstants.MAXIMAL_WORK_TARGET);
        myTarget = myTarget.divide(BigDecimal.valueOf(((double)Long.MAX_VALUE/10000.0)), mc); // Note, our target in compact form is in range 1..LONG_MAX/100
        myTarget = myTarget.multiply(BigDecimal.valueOf(powTarget));
        BigInteger myTargetInt = myTarget.toBigInteger();
        if(myTargetInt.compareTo(ComputationConstants.MAXIMAL_WORK_TARGET) == 1)
            myTargetInt = ComputationConstants.MAXIMAL_WORK_TARGET;
        if(myTargetInt.compareTo(BigInteger.ONE) == -1)
            myTargetInt = BigInteger.ONE;
        return myTargetInt;
    }
}
