package org.xel.computation;

import org.xel.*;
import org.xel.crypto.Crypto;
import org.xel.http.JSONData;
import org.xel.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import static org.xel.computation.ComputationConstants.WORK_MESSAGE_RECEIVER_ACCOUNT;
import static org.xel.http.JSONResponses.*;
import static org.xel.http.JSONResponses.FEATURE_NOT_AVAILABLE;

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

public class CustomTransactionBuilder {

    public final static Pair<JSONStreamAware, String> createTransaction(Appendix.Message work_rel_message, String secretPhrase, int deadline) throws NxtException {
        return CustomTransactionBuilder.createTransaction(work_rel_message, secretPhrase, null, deadline);
    }

    public final static Pair<JSONStreamAware, String> createTransaction(Appendix.Message work_rel_message, String secretPhrase, String referencedTransactionFullHash, int deadline) throws NxtException {

        Appendix.Message prunablePlainMessage = work_rel_message;

        if (secretPhrase == null) {
            throw new NxtException.NotValidException("No passphrase given");
        }

        JSONObject response = new JSONObject();
        byte[] publicKey = Crypto.getPublicKey(secretPhrase);

        Transaction.Builder builder = Nxt.newTransactionBuilder(publicKey, 0, 0,
                (short)deadline, Attachment.ARBITRARY_MESSAGE).referencedTransactionFullHash(referencedTransactionFullHash).recipientId(WORK_MESSAGE_RECEIVER_ACCOUNT);

        builder.appendix(prunablePlainMessage);

        Transaction transaction = builder.buildComputation(secretPhrase, 0);

        JSONObject transactionJSON = JSONData.unconfirmedTransaction(transaction);
        response.put("transactionJSON", transactionJSON);
        response.put("unsignedTransactionBytes", Convert.toHexString(transaction.getUnsignedBytes()));
        response.put("transaction", transaction.getStringId());
        response.put("fullHash", transactionJSON.get("fullHash"));
        response.put("transactionBytes", Convert.toHexString(transaction.getBytes()));
        response.put("signatureHash", transactionJSON.get("signatureHash"));


        return new Pair<>(transactionJSON, transaction.getFullHash());
    }

    public final static Pair<JSONStreamAware, String> createTransactionPubkey(Appendix.PrunablePlainMessage work_rel_message, byte[] publicKey, int deadline) throws NxtException {
        return CustomTransactionBuilder.createTransactionPubkey(work_rel_message, publicKey, null, deadline);
    }

    public final static Pair<JSONStreamAware, String> createTransactionPubkey(Appendix.PrunablePlainMessage work_rel_message, byte[] publicKey, String referencedTransactionFullHash, int deadline) throws NxtException {

        Appendix.PrunablePlainMessage prunablePlainMessage = work_rel_message;

        if (publicKey == null) {
            throw new NxtException.NotValidException("No passphrase given");
        }



        Transaction.Builder builder = Nxt.newTransactionBuilder(publicKey, 0, 0,
                (short)deadline, Attachment.ARBITRARY_MESSAGE).referencedTransactionFullHash(referencedTransactionFullHash).recipientId(WORK_MESSAGE_RECEIVER_ACCOUNT);

        builder.appendix(prunablePlainMessage);

        Transaction transaction = builder.build();

        JSONObject transactionJSON = JSONData.unconfirmedTransaction(transaction);
        JSONObject response = new JSONObject();
        response.put("transactionJSON", transactionJSON);
        response.put("unsignedTransactionBytes", Convert.toHexString(transaction.getUnsignedBytes()));

        return new Pair<>(response, "");
    }

    public final static Pair<JSONStreamAware, String> createTransactionPubkeyComputation(Appendix.Message work_rel_message, byte[] publicKey, String referencedTransactionFullHash, int deadline) throws NxtException {

        Appendix.Message prunablePlainMessage = work_rel_message;

        if (publicKey == null) {
            throw new NxtException.NotValidException("No passphrase given");
        }



        Transaction.Builder builder = Nxt.newTransactionBuilder(publicKey, 0, 0,
                (short)deadline, Attachment.ARBITRARY_MESSAGE).referencedTransactionFullHash(referencedTransactionFullHash).recipientId(WORK_MESSAGE_RECEIVER_ACCOUNT);

        builder.appendix(prunablePlainMessage);

        Transaction transaction = builder.buildComputation(0);

        JSONObject transactionJSON = JSONData.unconfirmedTransaction(transaction);
        JSONObject response = new JSONObject();
        response.put("transactionJSON", transactionJSON);
        response.put("unsignedTransactionBytes", Convert.toHexString(transaction.getUnsignedBytes()));

        return new Pair<>(response, "");
    }



    // Only use this function if you know what you do!
    public final static Pair<JSONStreamAware, JSONStreamAware> createTransactionPubkey(Appendix.Message work_rel_message, byte[] publicKey, int deadline, long amount, long to) throws NxtException {

        Appendix.Message prunablePlainMessage = work_rel_message;

        if (publicKey == null) {
            throw new NxtException.NotValidException("No passphrase given");
        }



        Transaction.Builder builder = Nxt.newTransactionBuilder(publicKey, amount, 0,
                (short)deadline, Attachment.ORDINARY_PAYMENT).referencedTransactionFullHash(null).recipientId(to);

        builder.appendix(prunablePlainMessage);

        Transaction transaction = builder.build();

        JSONObject transactionJSON = JSONData.unconfirmedTransaction(transaction);
        JSONObject response = new JSONObject();
        response.put("transactionJSON", transactionJSON);
        response.put("unsignedTransactionBytes", Convert.toHexString(transaction.getUnsignedBytes()));


        return new Pair<>(response, work_rel_message.getJSONObject());
    }
}
