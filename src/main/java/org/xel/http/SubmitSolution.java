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

import org.json.simple.JSONObject;
import org.xel.NxtException;
import org.xel.Work;
import org.xel.computation.CommandCancelWork;
import org.xel.computation.CommandPowBty;
import org.xel.computation.MessageEncoder;
import org.xel.crypto.Crypto;
import org.xel.util.Convert;
import org.json.simple.JSONStreamAware;
import org.spongycastle.crypto.digests.SkeinEngine;
import org.xel.util.Logger;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public final class SubmitSolution extends CreateTransaction {

    static final SubmitSolution instance = new SubmitSolution();

    private SubmitSolution() {
        super(new APITag[] { APITag.CREATE_TRANSACTION }, "work_id", "data","multiplicator","storage_id","is_pow","hash");
    }

    @Override
    protected JSONStreamAware processRequest(final HttpServletRequest req) throws NxtException {

        final long workId = ParameterParser.getUnsignedLong(req, "work_id", true);
        int deadlineInt = ParameterParser.getInt(req, "deadline", 1, 3, false);
        if(deadlineInt<1 || deadlineInt>3) deadlineInt = 1;
        byte[] data = ParameterParser.getBytes(req, "data", false);
        final byte[] multiplicator = ParameterParser.getBytes(req, "multiplicator", true);
        int storageId = ParameterParser.getInt(req, "storage_id",0,Integer.MAX_VALUE, true);

        if(data==null||data.length==0) storageId = -1;
        final boolean is_pow = ParameterParser.getBooleanByString(req, "is_pow", true);

        if(is_pow) data=new byte[0];
        byte[] hash = ParameterParser.getBytes(req, "hash", false);

        Work w = Work.getWork(workId);
        if(w==null || w.isClosed()){
            return JSONResponses.ERROR_WORK_INCORRECT;
        }

        CommandPowBty work = new CommandPowBty(workId, is_pow, multiplicator, hash, data, storageId, w.getCurrentRound(), Crypto.getPublicKey(ParameterParser.getSecretPhrase(req, true)));

        try {
            MessageEncoder.push(work, ParameterParser.getSecretPhrase(req, true), deadlineInt);
            return JSONResponses.EVERYTHING_ALRIGHT;
        } catch (IOException e) {
            Logger.logInfoMessage("Work " + String.valueOf(w.getId()) + " submission failed. IO Exception");
            return JSONResponses.ERROR_INCORRECT_REQUEST;
        } catch (NxtException.ValidationException e) {
            if(e.getMessage().contains("limit")==false)
                Logger.logInfoMessage("Work " + String.valueOf(w.getId()) + " submission failed: " + e.getMessage());
            JSONObject response  = new JSONObject();
            response.put("errorCode", 6009);
            response.put("errorDescription", e.getMessage());
            return response;
        }
    }

}
