/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package org.xel.http;

import com.google.gson.JsonObject;
import org.json.simple.JSONStreamAware;
import org.xel.*;
import org.xel.computation.Pair;
import org.xel.crypto.Crypto;

import javax.servlet.http.HttpServletRequest;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class Faucet extends APIServlet.APIRequestHandler {

    static final Faucet instance = new Faucet();
    static final Map<String, Long> blocks = new HashMap<>();

    private Faucet() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.CREATE_TRANSACTION}, "account");
    }

    @Override
    protected boolean requirePost() {
        return false;
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        if(Nxt.getBooleanProperty("nxt.faucet")==false){
            return JSONResponses.FAUCET_DISABLED;
        }

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {

        }

        long currTm = (new Date()).getTime();
        for(Iterator<Map.Entry<String ,Long>> it = blocks.entrySet().iterator(); it.hasNext();){
            Map.Entry<String, Long> entry = it.next();
            if ((currTm - entry.getValue())/1000 > Long.parseLong(Nxt.getStringProperty("nxt.faucetLockout"))) {
                it.remove();
            }
        }

        if(blocks.containsKey(req.getRemoteAddr())){
            return JSONResponses.faucetWait(String.valueOf(Nxt.getStringProperty("nxt.faucetLockout")));
        }

        long amount = Long.parseLong(Nxt.getStringProperty("nxt.faucetAmount"));
        long amountHave = 0;
        Account acc = Account.getAccount(Crypto.getPublicKey(Nxt.getStringProperty("nxt.faucetPassphrase")));
        if(acc==null)
            amountHave=0;
        else
            amountHave=acc.getBalanceNQT();

        if(amountHave<amount+Constants.TENTH_NXT)
            return JSONResponses.FAUCET_EMPTY;

        long recipient = ParameterParser.getAccountId(req, "account", true);
        long amountNQT = Long.parseLong(Nxt.getStringProperty("nxt.faucetAmount"));
        Transaction.Builder builder = Nxt.newTransactionBuilder(Crypto.getPublicKey(Nxt.getStringProperty("nxt.faucetPassphrase")), amountNQT, Constants.TENTH_NXT,
                (short) 64, Attachment.ORDINARY_PAYMENT).recipientId(recipient);
        Transaction t1 = builder.build(Nxt.getStringProperty("nxt.faucetPassphrase"));
        Nxt.getTransactionProcessor().broadcast(t1);

        DecimalFormat df2 = new DecimalFormat("#.######");

        blocks.put(req.getRemoteAddr(), (new Date()).getTime());

        return JSONResponses.faucetThanks(df2.format(((double)amountNQT*1.0)/Constants.ONE_NXT));
    }

}
