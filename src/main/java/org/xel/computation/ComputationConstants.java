package org.xel.computation;

import org.xel.Constants;

import java.math.BigInteger;

/******************************************************************************
 * Copyright © 2017 The XEL Core Developers.                                  *
 *                                                                            *
 * See the AUTHORS.txt, DEVELOPER-AGREEMENT.txt and LICENSE.txt files at      *
 * the top-level directory of this distribution for the individual copyright  *
 * holder information and the developer policies on copyright and licensing.  *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * XEL software, including this file, may be copied, modified, propagated,    *
 * or distributed except according to the terms contained in the LICENSE.txt  *
 * file.                                                                      *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

public class ComputationConstants {

    // maximum number of block for each open work package
    public static final int DEADLINE_MIN = 3;
    public static final int DEADLINE_MAX = 1440;


    public static final long XEL_POW_MIN = 1;
    public static final long XEL_POW_MAX = Constants.MAX_CURRENCY_TOTAL_SUPPLY;
    public static final long XEL_BTY_MIN = 1;
    public static final long XEL_BTY_MAX = Constants.MAX_CURRENCY_TOTAL_SUPPLY;
    public static final int BTY_PER_ITER_MIN = 1;
    public static final int BTY_PER_ITER_MAX = 10;
    public static final int ITER_MIN = 1;
    public static final int ITER_MAX = 100;
    public static final long WORK_MESSAGE_RECEIVER_ACCOUNT = 100010001000L;
    public static final long MAX_MAIN_WCET = 6000L;
    public static final long MAX_VERIFY_WCET = 1000L;
    public static final int MAX_CHAINED_TX_ACCEPTED = 10; // no more than 10 chained TX
    public static final int MAX_UNCOMPRESSED_WORK_SIZE = 4*1024*1024; // 4 MB is the maximum for now
    public static final short WORK_TRANSACTION_DEADLINE_VALUE = 48;
    public static final short MULTIPLIER_LENGTH = 32;
    public static final int POW_MIN = 10;
    public static final int POW_MAX = Integer.MAX_VALUE;
    public static final int MAX_EXECUTION_TIME_IN_S = 10;
    public static final int MAX_STORAGE_SIZE = 500;
    public static final short MD5LEN = 16 ;
    public static final int WE_WANT_X_POW_PER_MINUTE = 10;
    public static final int POW_RETARGET_DEPTH = 14;
    public static final BigInteger MAXIMAL_WORK_TARGET = new BigInteger("0000FFFFFFFFFFFFFFFFFFFFFFFFFFFF", 16); // TODO: Readjust that
    public static final BigInteger TESTVM_WORK_TARGET = new BigInteger("0FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", 16);

    public static final int ONLY_VERIFY_WORK_YOUNGER_THAN_SECONDS = 500;
}
