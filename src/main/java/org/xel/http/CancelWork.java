package org.xel.http;

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

import javax.servlet.http.HttpServletRequest;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.xel.Work;
import org.xel.computation.CommandCancelWork;
import org.xel.computation.ComputationConstants;
import org.xel.computation.MessageEncoder;
import org.json.simple.JSONStreamAware;
import org.xel.NxtException;
import org.xel.util.Convert;

import java.io.IOException;

public final class CancelWork extends CreateTransaction {

    static final CancelWork instance = new CancelWork();

    private CancelWork() {
        super(new APITag[] { APITag.CREATE_TRANSACTION }, "work_id");
    }

    @Override
    protected JSONStreamAware processRequest(final HttpServletRequest req) throws NxtException {

        final long workId = ParameterParser.getUnsignedLong(req, "work_id", true);
        int deadlineInt = ParameterParser.getInt(req, "deadline", 1, ComputationConstants.WORK_TRANSACTION_DEADLINE_VALUE, false);
        if(deadlineInt<1 || deadlineInt>3) deadlineInt = ComputationConstants.WORK_TRANSACTION_DEADLINE_VALUE;
        Work w = Work.getWork(workId);
        if(w==null || w.isClosed())
            return JSONResponses.ERROR_WORK_UNKNOWN;
        final String secret = ParameterParser.getSecretPhrase(req, false);
        String publicKeyValue = Convert.emptyToNull(req.getParameter("publicKey"));
        byte pubKey[] = null;
        if(publicKeyValue!=null)
            pubKey = Convert.parseHexString(publicKeyValue);
        CommandCancelWork work = new CommandCancelWork(workId);

        try {
            if(secret!=null && secret.length()>0) {
                MessageEncoder.push(work, ParameterParser.getSecretPhrase(req, true), deadlineInt);
                return JSONResponses.EVERYTHING_ALRIGHT;
            }else{
                if(pubKey==null){
                    return JSONResponses.ERROR_INCORRECT_REQUEST;
                }else {
                    JSONArray ar = new JSONArray();
                    for(JSONStreamAware x : MessageEncoder.encodeOnly(work, pubKey, deadlineInt)){
                        ar.add(x);
                    }
                    JSONObject o = new JSONObject();
                    o.put("transactions", ar);
                    return o;
                }
            }

        } catch (IOException e) {
            return JSONResponses.ERROR_INCORRECT_REQUEST;
        }
    }

}
