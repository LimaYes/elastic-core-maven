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

import org.json.simple.JSONArray;
import org.xel.*;
import org.xel.peer.Peers;
import org.xel.util.UPnP;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;

public final class GetState extends APIServlet.APIRequestHandler {

    static final GetState instance = new GetState();

    private GetState() {
        super(new APITag[] {APITag.INFO}, "includeCounts", "adminPassword");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) {

        JSONObject response = GetBlockchainStatus.instance.processRequest(req);
        boolean includeLastTargets = false;

        try {
            includeLastTargets = ParameterParser.getBooleanByString(req, "includeLastTargets", false);
        } catch (ParameterException e) {
        }

        boolean includeTasks = false;

        try {
            includeTasks = ParameterParser.getBooleanByString(req, "includeTasks", false);
        } catch (ParameterException e) {
        }

        if ("true".equalsIgnoreCase(req.getParameter("includeCounts")) && API.checkPassword(req)) {
            response.put("numberOfTransactions", Nxt.getBlockchain().getTransactionCount());
            response.put("numberOfAccounts", Account.getCount());
            response.put("numberOfPolls", Poll.getCount());
            response.put("numberOfVotes", Vote.getCount());
            response.put("numberOfPrunableMessages", PrunableMessage.getCount());
            response.put("numberOfTaggedData", TaggedData.getCount());
            response.put("numberOfDataTags", TaggedData.Tag.getTagCount());
            response.put("numberOfAccountLeases", Account.getAccountLeaseCount());
            response.put("numberOfActiveAccountLeases", Account.getActiveLeaseCount());
            response.put("numberOfPhasingOnlyAccounts", AccountRestrictions.PhasingOnly.getCount());
        }
        response.put("numberOfPeers", Peers.getAllPeers().size());
        response.put("numberOfActivePeers", Peers.getActivePeers().size());
        response.put("numberOfUnlockedAccounts", Generator.getAllGenerators().size());
        response.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        response.put("maxMemory", Runtime.getRuntime().maxMemory());
        response.put("totalMemory", Runtime.getRuntime().totalMemory());
        response.put("freeMemory", Runtime.getRuntime().freeMemory());
        response.put("peerPort", Peers.getDefaultPeerPort());
        response.put("isOffline", Constants.isOffline);
        response.put("needsAdminPassword", !API.disableAdminPassword);
        if (includeTasks) {
            response.put("totalOpen", Work.getActiveCount());
            response.put("totalClosed", Work.getCount()-Work.getActiveCount());
            response.put("grabs", Work.getGrabs());
        }
        try {
            Account account = ParameterParser.getAccount(req, false);
            if (account == null) {
                response.put("balanceNQT", "0");
                response.put("unconfirmedBalanceNQT", "0");
                response.put("forgedBalanceNQT", "0");

            } else {
                response.put("balanceNQT", String.valueOf(account.getBalanceNQT()));
                response.put("unconfirmedBalanceNQT", String.valueOf(account.getUnconfirmedBalanceNQT()));
                response.put("forgedBalanceNQT", String.valueOf(account.getForgedBalanceNQT()));
                if (includeTasks) {
                    response.put("myOpen", Work.getActiveCount(account.getId()));
                    response.put("myClosed", Work.getCount(account.getId())-Work.getActiveCount(account.getId()));
                }


            }
        } catch (ParameterException e) {

        }

        InetAddress externalAddress = UPnP.getExternalAddress();
        if (externalAddress != null) {
            response.put("upnpExternalAddress", externalAddress.getHostAddress());
        }
        if(includeLastTargets){
            JSONArray arr = new JSONArray();
            int counter = 0;
            Block bl = Nxt.getBlockchain().getLastBlock();
            for(counter=0; counter < 6; counter++){
                if(bl==null) break;
                JSONObject obj = new JSONObject();
                obj.put(bl.getHeight(), bl.getPowTarget());
                arr.add(obj);
                bl=Nxt.getBlockchain().getBlock(bl.getPreviousBlockId());
            }
            response.put("lastTargets", arr);
        }
        return response;
    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }

}
