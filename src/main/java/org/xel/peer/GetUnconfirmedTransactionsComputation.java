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

package org.xel.peer;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.xel.Nxt;
import org.xel.Transaction;
import org.xel.util.JSON;

import java.util.List;
import java.util.SortedSet;

final class GetUnconfirmedTransactionsComputation extends PeerServlet.PeerRequestHandler {

    static final GetUnconfirmedTransactionsComputation instance = new GetUnconfirmedTransactionsComputation();

    private GetUnconfirmedTransactionsComputation() {}


    @Override
    JSONStreamAware processRequest(JSONObject request, Peer peer) {

        List<String> exclude = (List<String>)request.get("exclude");
        if (exclude == null) {
            return JSON.emptyJSON;
        }

        SortedSet<? extends Transaction> transactionSet = Nxt.getTemporaryComputationTransactionProcessor().getCachedUnconfirmedTransactions(exclude);
        JSONArray transactionsData = new JSONArray();
        for (Transaction transaction : transactionSet) {
            if (transactionsData.size() >= 100) {
                break;
            }

            // preload pubkeys
            transaction.getSenderPublicKeyComputational();

            transactionsData.add(transaction.getJSONObjectComputational());
        }
        JSONObject response = new JSONObject();
        response.put("unconfirmedTransactions", transactionsData);

        return response;
    }

    @Override
    boolean rejectWhileDownloading() {
        return true;
    }

}
