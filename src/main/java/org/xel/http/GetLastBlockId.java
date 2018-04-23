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

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.xel.Account;
import org.xel.Nxt;
import org.xel.UnconfirmedGetter;
import org.xel.peer.Hallmark;

import javax.servlet.http.HttpServletRequest;

import java.util.Map;

import static org.xel.http.JSONResponses.INCORRECT_HALLMARK;
import static org.xel.http.JSONResponses.MISSING_HALLMARK;

public final class GetLastBlockId extends APIServlet.APIRequestHandler {

    static final GetLastBlockId instance = new GetLastBlockId();
    public static long lastBlockId = 0;

    private GetLastBlockId() {
        super(new APITag[] {APITag.INFO}, "none");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) {

        JSONObject o = new JSONObject();
        o.put("lastBlock", getLastBlock());

        Account account = null;
        try {
            account = ParameterParser.getAccount(req, false);
        } catch (ParameterException e) {
        }
        if (account != null) {
            long adjuster = 0;
            Map<Long, Long> unmap = UnconfirmedGetter.refreshMap();
            if(unmap.containsKey(account.getId())){
                adjuster = unmap.get(account.getId());
            }
            o.put("unconfirmedBalanceNQT", String.valueOf(account.getBalanceNQT() + adjuster));
        }
        return o;
    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }

    public static long getLastBlock() {
        if(lastBlockId==0){
            lastBlockId = Nxt.getBlockchain().getLastBlock().getId();
        }
        return lastBlockId;
    }
}
