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
import org.xel.Block;
import org.xel.Nxt;
import org.xel.util.Convert;
import org.xel.util.JSON;

import java.util.ArrayList;
import java.util.List;

final class GetNextBlocksComputation extends PeerServlet.PeerRequestHandler {

    static final GetNextBlocksComputation instance = new GetNextBlocksComputation();

    static final JSONStreamAware TOO_MANY_BLOCKS_REQUESTED;
    static {
        JSONObject response = new JSONObject();
        response.put("error", Errors.TOO_MANY_BLOCKS_REQUESTED);
        TOO_MANY_BLOCKS_REQUESTED = JSON.prepare(response);
    }

    private GetNextBlocksComputation() {}


    @Override
    JSONStreamAware processRequest(JSONObject request, Peer peer) {

        JSONObject response = new JSONObject();
        JSONArray nextBlocksArray = new JSONArray();
        List<? extends Block> blocks;
        long blockId = Convert.parseUnsignedLong((String) request.get("blockId"));
        List<String> stringList = (List<String>)request.get("blockIds");
        if (stringList != null) {
            if (stringList.size() > 36) {
                return TOO_MANY_BLOCKS_REQUESTED;
            }
            List<Long> idList = new ArrayList<>();
            stringList.forEach(stringId -> idList.add(Convert.parseUnsignedLong(stringId)));
            blocks = Nxt.getTemporaryComputationBlockchain().getBlocksAfter(blockId, idList);
        } else {
            long limit = Convert.parseLong(request.get("limit"));
            if (limit > 36) {
                return TOO_MANY_BLOCKS_REQUESTED;
            }
            blocks = Nxt.getTemporaryComputationBlockchain().getBlocksAfter(blockId, limit > 0 ? (int)limit : 36);
        }
        // preload pubkeys
        blocks.forEach(block -> block.getGeneratorPubkeyComputational());


        blocks.forEach(block -> nextBlocksArray.add(block.getJSONObjectComputational()));
        response.put("nextBlocks", nextBlocksArray);

        return response;
    }

    @Override
    boolean rejectWhileDownloading() {
        return true;
    }

}
