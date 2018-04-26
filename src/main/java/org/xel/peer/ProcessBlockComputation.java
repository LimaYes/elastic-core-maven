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
import org.xel.Block;
import org.xel.Nxt;
import org.xel.NxtException;
import org.xel.util.Convert;
import org.xel.util.JSON;

final class ProcessBlockComputation extends PeerServlet.PeerRequestHandler {

    static final ProcessBlockComputation instance = new ProcessBlockComputation();

    private ProcessBlockComputation() {}

    @Override
    JSONStreamAware processRequest(final JSONObject request, final Peer peer) {
        String previousBlockId = (String)request.get("previousBlock");
        Block lastBlock = Nxt.getTemporaryComputationBlockchain().getLastBlock();
        if (lastBlock.getStringId().equals(previousBlockId) ||
                (Convert.parseUnsignedLong(previousBlockId) == lastBlock.getPreviousBlockId()
                        && lastBlock.getTimestamp() > Convert.parseLong(request.get("timestamp")))) {
            Peers.peersService.submit(() -> {
                try {
                    Nxt.getTemporaryComputationBlockchainProcessor().processPeerBlock(request);
                } catch (NxtException | RuntimeException e) {
                    if (peer != null) {
                        peer.blacklist(e);
                    }
                }
            });
        }
        return JSON.emptyJSON;
    }

    @Override
    boolean rejectWhileDownloading() {
        return true;
    }

}
