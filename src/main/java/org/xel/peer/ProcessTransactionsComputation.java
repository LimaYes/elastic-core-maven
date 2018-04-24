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

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.xel.Nxt;
import org.xel.NxtException;
import org.xel.util.JSON;

final class ProcessTransactionsComputation extends PeerServlet.PeerRequestHandler {

    static final ProcessTransactionsComputation instance = new ProcessTransactionsComputation();

    private ProcessTransactionsComputation() {}


    @Override
    JSONStreamAware processRequest(JSONObject request, Peer peer) {

        try {
            Nxt.getTemporaryComputationTransactionProcessor().processPeerTransactions(request);
            return JSON.emptyJSON;
        } catch (RuntimeException | NxtException.ValidationException e) {
            //Logger.logDebugMessage("Failed to parse peer transactions: " + request.toJSONString());
            peer.blacklist(e);
            return PeerServlet.error(e);
        }

    }

    @Override
    boolean rejectWhileDownloading() {
        return true;
    }

}
