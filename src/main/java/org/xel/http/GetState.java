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
import org.xel.computation.CustomTransactionBuilder;
import org.xel.computation.Pair;
import org.xel.db.DbIterator;
import org.xel.peer.Peers;

import org.xel.util.Logger;
import org.xel.util.UPnP;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.math.BigInteger;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        response.put("lastBlock", GetLastBlockId.getLastBlock());
        response.put("lastBlockComputation", GetLastBlockId.getLastBlockComputational());

        if (includeTasks) {
            response.put("totalOpen", Work.getActiveCount());
            response.put("totalClosed", Work.getCount()-Work.getActiveCount());
            response.put("grabs", Work.getGrabs());
        }
        try {

            long myid;
            try {
                final String readParam = ParameterParser.getParameterMultipart(req, "account");
                final BigInteger b = new BigInteger(readParam);
                myid = b.longValue();
            } catch (final Exception e) {
                myid = 0;
            }

            Account account = Account.getAccount(myid);
            if (account == null) {
                response.put("balanceNQT", "0");
                long adjuster = 0;
                Map<Long, Long> unmap = UnconfirmedGetter.refreshMap();
                if(unmap.containsKey(myid)){
                    adjuster = unmap.get(myid);
                }
                response.put("unconfirmedBalanceNQT",  String.valueOf(adjuster));
                response.put("forgedBalanceNQT", "0");

            } else {
                response.put("balanceNQT", String.valueOf(account.getBalanceNQT()));
                long adjuster = 0;
                Map<Long, Long> unmap = UnconfirmedGetter.refreshMap();
                if(unmap.containsKey(account.getId())){
                    adjuster = unmap.get(account.getId());
                }
                response.put("unconfirmedBalanceNQT", String.valueOf(account.getBalanceNQT() + adjuster));
                response.put("forgedBalanceNQT", String.valueOf(account.getForgedBalanceNQT()));

            }



            /* Here, we begin to decide which payments need to be done */
            // The UnconfirmedGetter from above is up to date ... we can use that to determine which BTYs have been paid for
            JSONArray awork = new JSONArray();
            AlternativeChainPubkeys pp = AlternativeChainPubkeys.getKnownIdentity(myid);
            byte[] publicKey = (pp!=null)?pp.getPubkey():null;
            if(publicKey!=null) {
                Map<Long,Pair<String, Long>> earnings = new HashMap<>();
                List<Work> itw = Work.getActiveAndRecentlyClosedByAccountId(myid);
                for (Work w : itw) {
                    //Logger.logDebugMessage(" > open work " + w.getId());
                    try (DbIterator<PowAndBounty> unpaidit = PowAndBounty.getUnpaidSubmission(w.getId())) {
                        while (unpaidit.hasNext()) {
                            PowAndBounty b = unpaidit.next();
                            //Logger.logDebugMessage("    > unpaid bty " + b.getId() + ", isPOW = " + b.is_pow + ", payout = " + ((b.is_pow) ? w.getXel_per_pow() : w.getXel_per_bounty()));
                            Pair<String, Long> oldpair = null;
                            if(earnings.containsKey(b.getAccountId())){
                                oldpair = earnings.get(b.getAccountId());
                            }else
                            {
                                oldpair = new Pair<>("", 0L);
                            }


                            if(oldpair.getElement0().length()==0)
                                oldpair.setElement0("/!" + String.valueOf(b.getId()));
                            else
                                oldpair.setElement0(oldpair.getElement0() + ",/!" + String.valueOf(b.getId()));
                            oldpair.setElement1(oldpair.getElement1() + ((b.is_pow) ? w.getXel_per_pow() : w.getXel_per_bounty()));
                            earnings.put(b.getAccountId(), oldpair);

                            // Flush if message too long
                            if(oldpair.getElement0().length() > Constants.MAX_ARBITRARY_MESSAGE_LENGTH-40){ // arbitrary safegap
                                Appendix.Message prunablePlainMessage = new Appendix.Message(oldpair.getElement0(), true);
                                try {
                                    Pair<JSONStreamAware, JSONStreamAware> pr = CustomTransactionBuilder.createTransactionPubkey(prunablePlainMessage, publicKey,3, oldpair.getElement1(), b.getAccountId());
                                    JSONArray ffb = new JSONArray();
                                    ffb.add(pr.getElement0());
                                    ffb.add(pr.getElement1());
                                    awork.add(ffb);
                                    earnings.remove(b.getAccountId());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                        }
                    }
                }
                /* Here, we end to decide which payments need to be done */


                // Build all remaining TX
                for(Long k : earnings.keySet()){
                    Pair<String, Long> payment = earnings.get(k);
                    Appendix.Message prunablePlainMessage = new Appendix.Message(payment.getElement0(), true);
                    try {
                        Pair<JSONStreamAware, JSONStreamAware> pr = CustomTransactionBuilder.createTransactionPubkey(prunablePlainMessage, publicKey,1, payment.getElement1(), k);
                        JSONArray ffb = new JSONArray();
                        ffb.add(pr.getElement0());
                        ffb.add(pr.getElement1());
                        awork.add(ffb);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                response.put("pendingPayouts", awork);

                if (includeTasks) {
                    response.put("myOpen", Work.getActiveCount(myid));
                    response.put("myClosed", Work.getCount(myid)-Work.getActiveCount(myid));
                    JSONArray works = new JSONArray();
                    List<Work> l = Work.getWork(myid,true,0,100, 0);
                    for(Work x : l){
                        works.add(Work.toJsonWithStorage(x, -100, false));
                    }
                    response.put("myWorks", works);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        InetAddress externalAddress = UPnP.getExternalAddress();
        if (externalAddress != null) {
            response.put("upnpExternalAddress", externalAddress.getHostAddress());
        }
        if(includeLastTargets){
            JSONArray arr = new JSONArray();
            int counter = 0;
            Block bl = Nxt.getTemporaryComputationBlockchain().getLastBlock();
            for(counter=0; counter < 6; counter++){
                if(bl==null) break;
                JSONObject obj = new JSONObject();
                obj.put(bl.getHeight(), bl.getPowTarget());
                arr.add(obj);
                bl=Nxt.getTemporaryComputationBlockchain().getBlock(bl.getPreviousBlockId());
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
