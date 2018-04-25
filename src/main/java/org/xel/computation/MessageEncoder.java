package org.xel.computation;

import org.xel.*;
import org.xel.db.DbIterator;
import org.xel.http.GetLastBlockId;
import org.xel.http.ParameterException;
import org.xel.http.ParameterParser;
import org.xel.peer.Peers;
import org.xel.util.Convert;
import org.xel.util.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.*;

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


public class MessageEncoder {

    /*
    the reason that both are present is, that messages can be split up in multiple chunks. Each chunk must be identifies as a NON message, but the last chunk only triggers the message parsing
     warning: both must be of same length */
    static byte[] MAGIC = {(byte)0xde, (byte)0xad, (byte)0xbe, (byte)0xef, (byte)0xde, (byte)0xad, (byte)0xbe, (byte)0xef};
    static byte[] MAGIC_INTERMEDIATE = {(byte)0xef, (byte)0xbe, (byte)0xad, (byte)0xde, (byte)0xde, (byte)0xad, (byte)0xbe, (byte)0xef};


    public static boolean useComputationEngine = Nxt.getBooleanProperty("nxt.enableComputationEngine");
    public static Map<Long, Integer> stupidLimiterBty = new HashMap<Long, Integer>();
    public static Map<Long, Integer> stupidLimiterPow = new HashMap<Long, Integer>();

    public static synchronized CommandPowBty hasPowAndBountyContent(Transaction t){
        Appendix.Message m = t.getMessage();
        if(m==null ) return null;

        try {
            Appendix.Message[] reconstructedChain = MessageEncoder.extractMessages(t);

            // Allow the decoding of the attachment
            IComputationAttachment att = MessageEncoder.decodeAttachment(reconstructedChain);
            if(att == null) return null;

            if(att instanceof CommandPowBty)
                return (CommandPowBty)att;

        } catch (Exception e) {
            // generous catch, do not allow anything to cripple the blockchain integrity
            return null;
        }
        return null;
    }

    public static synchronized boolean limitHit(Transaction transaction, CommandPowBty c){

        // basic simple test
        long wid = c.getWork_id();
        Work w = Work.getWork(wid);
        if (w == null) return false;
        if (w.isClosed() == true) return false;

        //Logger.logDebugMessage("Checking stupidLimiters for work " + c.getWork_id() + ": pow = " + ((stupidLimiterPow.containsKey(w.getId())==false)?0:stupidLimiterPow.get(w.getId())) + " (of " + w.getCap_number_pow() + "), bty = " + ((stupidLimiterBty.containsKey(w.getId())==false)?0:stupidLimiterBty.get(w.getId())) + " (of " + w.getBounty_limit_per_iteration() + ")");
        if(c.isIs_proof_of_work()){
            if(stupidLimiterPow.containsKey(w.getId())==false) return false;
            return stupidLimiterPow.get(w.getId())>=25;
        }else{
            if(stupidLimiterBty.containsKey(w.getId())==false) return false;
            return stupidLimiterBty.get(w.getId())>=w.getBounty_limit_per_iteration();
        }
    }

    public static synchronized void preValidate(Transaction transaction, CommandPowBty c) throws NxtException.NotCurrentlyValidException {


        boolean ret = c.validate(transaction, true); // use light mode
        if(!ret) throw new NxtException.NotCurrentlyValidException("The submitted " + ((c.isIs_proof_of_work())?"POW":"BOUNTY") + " was simply wrong");

        // looks good enough to keep it for now, real validation will be performed later
        if(c.isIs_proof_of_work()){
            if(stupidLimiterPow.containsKey(c.getWork_id()))
                stupidLimiterPow.put(c.getWork_id(), stupidLimiterPow.get(c.getWork_id())+1);
            else
                stupidLimiterPow.put(c.getWork_id(), 1);
        } else{
            if(stupidLimiterBty.containsKey(c.getWork_id()))
                stupidLimiterBty.put(c.getWork_id(), stupidLimiterBty.get(c.getWork_id())+1);
            else
                stupidLimiterBty.put(c.getWork_id(), 1);
        }

        Logger.logDebugMessage("(PREVALIDATION PASSED) for submitted " + ((c.isIs_proof_of_work())?"POW":"BOUNTY") + " (limiter bty=" + stupidLimiterBty.get(c.getWork_id()) + ", pow = " + stupidLimiterPow.get(c.getWork_id()));

    }


    static void paymentProcessor(Block block){
        for(Transaction t : block.getTransactions()) {
            Appendix.Message m2 = t.getMessage();

            if (m2 != null) {

                // Here process all payments
                if (m2.isText()) {
                    String str = Convert.toString(m2.getMessage(), true);
                    if (str.length() > 4 && str.length() < Constants.MAX_ARBITRARY_MESSAGE_LENGTH) {
                        if (str.startsWith("/!")) {
                            try {
                                if (str.indexOf(",") > 0) {
                                    String[] sp = str.split(",");
                                    long totalAttached = t.getAmountNQT();
                                    for (String x : sp) {
                                        if (x.startsWith("/!") && x.length() > 4) {
                                            x = x.substring(2);
                                            long p = 0;
                                            p = Long.parseLong(x);
                                            PowAndBounty bty = PowAndBounty.getPowOrBountyById(p);

                                            if (bty != null) {
                                                Work w = Work.getWorkById(bty.getWork_id());
                                                if ((bty.is_pow && totalAttached >= w.getXel_per_pow()) || (!bty.is_pow && totalAttached >= w.getXel_per_bounty())) {
                                                    bty.setWas_paid(true);
                                                    bty.JustSave();
                                                    totalAttached -= ((bty.is_pow) ? w.getXel_per_pow() : w.getXel_per_bounty());
                                                } else break;
                                            }
                                        }
                                    }
                                } else {
                                    str = str.substring(2);
                                    long p = 0;
                                    p = Long.parseLong(str);
                                    PowAndBounty bty = PowAndBounty.getPowOrBountyById(p);

                                    if (bty != null) {
                                        Work w = Work.getWorkById(bty.getWork_id());
                                        if ((bty.is_pow && t.getAmountNQT() >= w.getXel_per_pow()) || (!bty.is_pow && t.getAmountNQT() >= w.getXel_per_bounty())) {
                                            bty.setWas_paid(true);
                                            bty.JustSave();
                                        }
                                    }
                                }
                            } catch (Exception e) {
                            }
                        }
                    }

                }
            }
        }
    }
    static void processBlockInternal(Block block){
        // Check all TX for relevant stuff

        int powCounter = 0;
        int mintime = Integer.MAX_VALUE;
        int maxtime = 0;

        // first all pow and else
        // in second round the bounties

        for(Transaction t : block.getTransactions()){



            Appendix.Message m = t.getMessage();
            if(m==null) continue;


            if(MessageEncoder.checkMessageForPiggyback(m, true, false)){
                try {
                    Appendix.Message[] reconstructedChain = MessageEncoder.extractMessages(t);

                    // Allow the decoding of the attachment
                    IComputationAttachment att = MessageEncoder.decodeAttachment(reconstructedChain);
                    if(att == null) continue;

                    if(att instanceof CommandPowBty) {
                        if (((CommandPowBty) att).isIs_proof_of_work())
                            att.apply(t);
                        else
                            continue;
                        if (((CommandPowBty) att).isIs_proof_of_work() && ((CommandPowBty) att).isValid()) {
                            if (t.getTimestamp() > maxtime) maxtime = t.getTimestamp();
                            if (t.getTimestamp() < mintime) mintime = t.getTimestamp();
                            powCounter++;
                        }else{
                            ((CommandPowBty) att).setValidated(false);
                            att.validate(t);
                        }
                    }else{
                        att.apply(t);
                    }


                } catch (Exception e) {
                    // generous catch, do not allow anything to cripple the blockchain integrity
                    e.printStackTrace();
                    continue;
                }
            }
        }

        for(Transaction t : block.getTransactions()){


            Appendix.Message m = t.getMessage();
            if(m==null) continue;



            if(MessageEncoder.checkMessageForPiggyback(m, true, false)){
                try {
                    Appendix.Message[] reconstructedChain = MessageEncoder.extractMessages(t);

                    // Allow the decoding of the attachment
                    IComputationAttachment att = MessageEncoder.decodeAttachment(reconstructedChain);
                    if(att == null) continue;

                    if(att instanceof CommandPowBty) {
                        if (((CommandPowBty) att).isIs_proof_of_work()) continue;
                        att.apply(t);
                    }



                } catch (Exception e) {
                    // generous catch, do not allow anything to cripple the blockchain integrity
                    e.printStackTrace();
                    continue;
                }
            }
        }
        block.calculatePowTarget(powCounter, mintime, maxtime);
        block.setLocallyProcessed();
        // and clean the stupidLimiters
        stupidLimiterPow.clear();
        stupidLimiterBty.clear();

        // Now clear all jobs that have not seen enough payments in the past
        // Rule is, if more than 55 POW/BTY are open, we timeout this job immedeately
        try(DbIterator<Work> it = Work.getActiveWork()){
            while (it.hasNext()){
                Work w = it.next();
                int unpaid = PowAndBounty.getUnpaidSubmissionCount(w.getId());
                if(unpaid > 55){
                    w.CloseNoPayment(block);
                }
            }
        }
    }

    static {
        Nxt.getTemporaryComputationBlockchainProcessor().addListener(block -> {
            GetLastBlockId.lastBlockId = block.getId();
            paymentProcessor(block);
        }, TemporaryComputationBlockchainProcessorImpl.Event.AFTER_BLOCK_APPLY);
    }
    static {
        Nxt.getTemporaryComputationBlockchainProcessor().addListener(block -> {
            GetLastBlockId.lastBlockIdComp = block.getId();
            processBlockInternal(block);
        }, TemporaryComputationBlockchainProcessorImpl.Event.AFTER_BLOCK_APPLY_COMPUTATION);
    }

    public static void init(){
    }


    public static long push(IComputationAttachment work, String secretPhrase, int deadline) throws NxtException, IOException {
        Appendix.Message[] messages = MessageEncoder.encodeAttachment(work);
        JSONStreamAware[] individual_txs = MessageEncoder.encodeTransactions(messages, secretPhrase, deadline);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        for(int i=0;i<individual_txs.length;++i){
            individual_txs[i].writeJSONString(pw);
        }
        StringBuffer sb = sw.getBuffer();
        return MessageEncoder.pushThemAll(individual_txs);
    }

    public static JSONStreamAware[] encodeOnly(IComputationAttachment work, byte[] pubkey, int deadline) throws NxtException, IOException {
        Appendix.Message[] messages = MessageEncoder.encodeAttachment(work);
        JSONStreamAware[] individual_txs = MessageEncoder.encodeTransactions(messages, pubkey, deadline);
        for(int i=0; i<individual_txs.length;++i){
            JSONObject prunableMess = new JSONObject();
            messages[i].putMyJSON(prunableMess);
            ((JSONObject)individual_txs[i]).put("toAttach",prunableMess);
        }
        return individual_txs;
    }

    public static Appendix.Message[] extractMessages(Transaction _t) throws NxtException.ValidationException {

        Transaction t = _t;

        ArrayList<Appendix.Message> arl = new ArrayList<>();

        if(t == null) throw new NxtException.NotValidException("This transaction is not a valid work-encoder");
        Appendix.Message pm = t.getMessage();
        if(pm == null) throw new NxtException.NotValidException("This transaction is not a valid work-encoder");

        if(!checkMessageForPiggyback(pm, true, false)){
            throw new NxtException.NotValidException("This transaction is not a valid work-encoder");
        }

        arl.add(pm);

        int counter = 0;

        // now, that we have the original transaction we have to fetch (possible) referenced transactions
        while(t.getReferencedTransactionFullHash() != null){
            t = Nxt.getTemporaryComputationBlockchain().getTransactionByFullHash(t.getReferencedTransactionFullHash());

            if(t == null) throw new NxtException.NotValidException("This transaction is not a valid work-encoder");
            pm = t.getMessage();
            if(pm == null) throw new NxtException.NotValidException("This transaction is not a valid work-encoder");

            if(!checkMessageForPiggyback(pm, false, true)){
                throw new NxtException.NotValidException("This transaction is not a valid work-encoder");
            }

            arl.add(0, pm);

            counter = counter + 1;
            if(counter > ComputationConstants.MAX_CHAINED_TX_ACCEPTED)
                throw new NxtException.NotValidException("This transaction references a chain which is too long");
        }

        return arl.toArray(new Appendix.Message[arl.size()]);
    }


    public static JSONStreamAware[] encodeTransactions(Appendix.Message[] msgs, String passphraseOrPubkey, int deadline) throws NxtException {
        ArrayList<JSONStreamAware> array_tx = new ArrayList<>(msgs.length);

        // Transactions have to be created from "end to start" to get the "referenced tx hashes" chained up correctly
        String previousHash = "";
        for(int i=msgs.length-1; i>=0; --i){
            Pair<JSONStreamAware, String> t = null;
            if(previousHash.length()==0) {
                t = CustomTransactionBuilder.createTransaction(msgs[i], passphraseOrPubkey, deadline);
                previousHash = t.getElement1();
            }
            else
                t = CustomTransactionBuilder.createTransaction(msgs[i], passphraseOrPubkey, previousHash, deadline);
            array_tx.add(t.getElement0());
        }

        return array_tx.toArray(new JSONStreamAware[msgs.length]);
    }

    public static JSONStreamAware[] encodeTransactions(Appendix.Message[] msgs, byte[] passphraseOrPubkey, int deadline) throws NxtException {
        ArrayList<JSONStreamAware> array_tx = new ArrayList<>(msgs.length);

        // Transactions have to be created from "end to start" to get the "referenced tx hashes" chained up correctly
        String previousHash = "";
        for(int i=msgs.length-1; i>=0; --i){
            Pair<JSONStreamAware, String> t = null;
            if(previousHash.length()==0) {
                t = CustomTransactionBuilder.createTransactionPubkeyComputation(msgs[i], passphraseOrPubkey, null, deadline);
                previousHash = t.getElement1();
            }
            else
                t = CustomTransactionBuilder.createTransactionPubkeyComputation(msgs[i], passphraseOrPubkey, previousHash, deadline);
            array_tx.add(t.getElement0());
        }

        return array_tx.toArray(new JSONStreamAware[msgs.length]);
    }

    public static long pushThemAll(JSONStreamAware[] aw) throws NxtException.ValidationException, ParameterException {
        List<Transaction> toPush = new ArrayList<>();
        long lastPushed = 0;
        for(int i=0;i<aw.length;++i)
        {
            Transaction.Builder builder = ParameterParser.parseTransaction(aw[i].toString(), null, null);
            Transaction transaction = builder.buildComputation(0);

            // As a safeguard for ourselves, check if we should postpone that TX
            transaction.getType().postponeForNow(transaction);

            toPush.add(transaction);
        }

        for(Transaction t : toPush){
            Nxt.getTemporaryComputationTransactionProcessor().broadcast(t);
            lastPushed = t.getId();
        }
        return lastPushed;
    }

    public static Appendix.Message[] encodeAttachment(IComputationAttachment att){
        try {
            ArrayList<Appendix.Message> preparation = new ArrayList<>();
            byte[] to_encode = att.getByteArray();
            int pos_counter=0;

            while(pos_counter<to_encode.length){
                int maximum_read = Math.min(Constants.MAX_COMPU_MESSAGE_LENGTH - MAGIC.length, to_encode.length - pos_counter);
                byte[] msg = new byte[maximum_read + MAGIC.length];
                pos_counter += maximum_read;

                // now, depending on pos_counter decide whether MAGIC or MAGIC_INTERMEDIATE is appended
                if(pos_counter==to_encode.length)
                    System.arraycopy(MessageEncoder.MAGIC, 0, msg, 0, MessageEncoder.MAGIC.length);
                else
                    System.arraycopy(MessageEncoder.MAGIC_INTERMEDIATE, 0, msg, 0, MessageEncoder.MAGIC_INTERMEDIATE.length);

                System.arraycopy(to_encode, pos_counter-maximum_read, msg, MessageEncoder.MAGIC.length, maximum_read);
                Appendix.Message pl = new Appendix.Message(msg);
                preparation.add(pl);
            }

            return preparation.toArray(new Appendix.Message[preparation.size()]);
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public static IComputationAttachment decodeAttachment(Appendix.Message[] m){
        try {

            int total_length = 0;
            for(int i=0;i<m.length;++i) {
                if (!MessageEncoder.checkMessageForPiggyback(m[i])) return null;
                total_length = total_length + (m[i].getMessage().length - MessageEncoder.MAGIC.length);
            }

            byte[] work_package = new byte[total_length];
            int last_pos = 0;

            for(int i=0;i<m.length;++i) {
                byte[] msg = m[i].getMessage();
                System.arraycopy(msg, MessageEncoder.MAGIC.length, work_package, last_pos, msg.length-MessageEncoder.MAGIC.length);
                last_pos += msg.length;
            }

            if (work_package.length == 0) return null; // safe guard

            ByteBuffer wp_bb = ByteBuffer.wrap(work_package);

            byte messageType = wp_bb.get();
            if(messageType == CommandsEnum.CREATE_NEW_WORK.getCode()){
                return new CommandNewWork(wp_bb);
            }
            else if(messageType == CommandsEnum.CANCEL_WORK.getCode()){
                return new CommandCancelWork(wp_bb);
            }
            else if(messageType == CommandsEnum.POWBTY.getCode()){
                return new CommandPowBty(wp_bb);
            }
            else{
                return null;
            }
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public static boolean checkMessageForPiggyback(Appendix.Message plainMessage){
        return checkMessageForPiggyback(plainMessage, false, false);
    }

    public static boolean checkMessageForPiggyback(Appendix.Message plainMessage, boolean onlyFinalMessageOfChain, boolean onlyMidMessage){

        try {
            if (plainMessage.isText())
                return false;

            byte[] msg = plainMessage.getMessage();
            if (msg.length < MAGIC.length) return false;

            boolean returned = true;
            if(!onlyMidMessage) {
                for (int i = 0; i < MAGIC.length; ++i) {
                    if (msg[i] != MAGIC[i]) {
                        returned = false;
                        break;
                    }
                }
            }else{
                returned = false;
            }

            if(!returned && !onlyFinalMessageOfChain)
            {
                returned = true;
                for (int i = 0; i < MAGIC_INTERMEDIATE.length; ++i) {
                    if (msg[i] != MAGIC_INTERMEDIATE[i]){
                        returned = false;
                        break;
                    }
                }
            }
            return returned;
        }catch(Exception e){
            return false;
        }
    }
}
