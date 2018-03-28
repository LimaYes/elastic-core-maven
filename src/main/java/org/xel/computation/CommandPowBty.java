package org.xel.computation;


import org.xel.*;
import org.xel.crypto.Crypto;
import org.xel.util.Convert;
import org.xel.util.Logger;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;


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

public class CommandPowBty extends IComputationAttachment {

    private long work_id;
    private boolean is_proof_of_work;
    private byte[] multiplier;
    private byte[] hash;
    private byte[] submitted_storage;
    private boolean validated = false;
    private boolean isValid = false;
    private int storage_bucket;
    private int current_round;

    public CommandPowBty(long work_id, boolean is_proof_of_work, byte[] multiplier, byte[] hash,
                         byte[] submitted_storage, int storage_bucket, int current_round) {
        super();
        this.work_id = work_id;
        this.is_proof_of_work = is_proof_of_work;
        this.multiplier = multiplier;
        this.hash = hash;
        this.storage_bucket = storage_bucket;
        this.submitted_storage = submitted_storage;
        if(this.submitted_storage==null)this.submitted_storage=new byte[0];
        this.current_round = current_round;
    }

    CommandPowBty(ByteBuffer buffer) {
        super(buffer);
        try {

            /* you will notice, that here only "upper bounds" are checked, not the exact correct storate/m/submit length.
            The reason for this is, that we not yet know the work id nor do we want any db access for Work retrieval here.
            The correct lengths will later be checked in verify, for now just stop some memory exhaustion attacks using simple upper bounds checks
             */

            this.work_id = buffer.getLong();


            this.is_proof_of_work = (buffer.get() == (byte) 0x01) ? true : false;

            // First read in the multiplicator
            short readsize = buffer.getShort();
            if (readsize != ComputationConstants.MULTIPLIER_LENGTH) {
                throw new NxtException.NotValidException("Wrong Parameters, your multiplier was " + readsize + " but " +
                        "should be " + ComputationConstants.MULTIPLIER_LENGTH);
            }
            multiplier = new byte[readsize];
            buffer.get(multiplier);


            // Then, read the pow_hash, must be MD5LEN
            readsize = buffer.getShort();
            if (readsize != ComputationConstants.MD5LEN) {
                throw new NxtException.NotValidException("Wrong Parameters: pow_hash must be MD5LEN size, but was just " + readsize);
            }
            hash = new byte[readsize];
            buffer.get(hash);


           this.storage_bucket = buffer.getInt();

            // And finally, read the submitted_storage
            readsize = buffer.getShort();
            if (readsize > ComputationConstants.MAX_STORAGE_SIZE * 4) {
                throw new NxtException.NotValidException("Wrong Parameters: submitted_storage/data length is too large");
            }

            submitted_storage = new byte[readsize];
            buffer.get(submitted_storage);
            current_round = buffer.getInt();
            //System.out.println("POWBTY - About to decode " + this.storage_bucket + ", round " + current_round);
        } catch (Exception e) {
            // pass through any error
            this.work_id = 0;
            this.is_proof_of_work = false;
            this.multiplier = new byte[0];
            this.submitted_storage = new byte[0];
            this.hash = new byte[0];
            this.storage_bucket = 0;
            this.current_round = 0;
        }
    }


    public long getWork_id() {
        return work_id;
    }

    public boolean isIs_proof_of_work() {
        return is_proof_of_work;
    }

    @Override
    String getAppendixName() {
        return "CommandPowBty";
    }

    @Override
    int getMySize() {
        return 8 + 1 + 2 + 2 + 2 + this.multiplier.length + this.submitted_storage.length  + this.hash.length  + 4 /*storage bucket in t */ + 4 /* current round */;
    }

    public int getCurrent_round() {
        return current_round;
    }

    @Override
    byte getMyMessageIdentifier() {
        return CommandsEnum.POWBTY.getCode();
    }

    @Override
    public String toString() {
        return "Pow-or-bty for id " + Long.toUnsignedString(this.work_id);
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(this.work_id);
        buffer.put((this.is_proof_of_work == true) ? (byte) 0x01 : (byte) 0x00);
        // Now put the "triade"
        buffer.putShort((short)this.multiplier.length);
        buffer.put(this.multiplier);
        buffer.putShort((short)this.hash.length);
        buffer.put(this.hash);
        buffer.putInt(this.storage_bucket);
        buffer.putShort((short)this.submitted_storage.length);
        buffer.put(this.submitted_storage);
        buffer.putInt(this.current_round);

    }

    public byte[] getMultiplier() {
        return multiplier;
    }

    public int getStorage_bucket() {
        return storage_bucket;
    }

    public byte[] getSubmitted_storage() {
        return submitted_storage;
    }

    private boolean validatePow(byte[] pubkey, long blockid, long workId, byte[] target){
        byte[] hash_array = this.getPowHash();
        byte[] multiplier_array = this.getMultiplier();

        Work w = Work.getWorkById(workId);
        ExecutionEngine e = new ExecutionEngine();

        try {
            ComputationResult r = e.compute(target, pubkey, blockid, multiplier_array, workId, storage_bucket, false);
            return r.isPow;
        } catch (Exception e1) {
            return false;
        }
    }
    private boolean validateBty(byte[] pubkey, long blockid, long workId, byte[] target){
        byte[] hash_array = this.getPowHash();
        byte[] multiplier_array = this.getMultiplier();

        Work w = Work.getWorkById(workId);
        ExecutionEngine e = new ExecutionEngine();

        try {
            ComputationResult r = e.compute(target, pubkey, blockid, multiplier_array, workId, storage_bucket, false);
            return r.isBty;
        } catch (Exception e1) {
            return false;
        }
    }

    byte[] integersToBytes(int[] values) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        for(int i=0; i < values.length; ++i)
        {
            dos.writeInt(values[i]);
        }

        return baos.toByteArray();
    }

    @Override
    boolean validate(Transaction transaction) {

        // This construction avoids multiple code-evaluations which are not really required
        if(validated) return isValid;
        validated = true;

        if (this.work_id == 0) return false;
        Work w = Work.getWork(this.work_id);
        if (w == null) return false;
        if (w.isClosed() == true) return false;
        if (w.getCurrentRound() != this.getCurrent_round()) return false;

        // TODO WARNING: Badly programmed jobs that allow the same output (POW/BTY) for a different multiplicator can suffer big time here! Be careful coding!
        byte[] myMultiplier = this.getMultiplier();
        if(PowAndBounty.hasMultiplier(w.getId(), myMultiplier)) {
            Logger.logInfoMessage("Work " + String.valueOf(w.getId()) + " verification failed: multiplier already in database.");
            return false;
        }

        // checking multiplicator length requirements
        if (multiplier.length != ComputationConstants.MULTIPLIER_LENGTH) {
            Logger.logInfoMessage("Work " + String.valueOf(w.getId()) + " verification failed: multiplier length is incorrect.");
            return false;
        }

        // checking pow_hash length requirements once again
        if (hash.length != ComputationConstants.MD5LEN) {
            Logger.logInfoMessage("Work " + String.valueOf(w.getId()) + " verification failed: pow_hash length is incorrect");
            return false;
        }

        // !! if storage size is larger than 0 this indicates the presence of a storage. Therefore, storage bucket must be in a valid range
        if((w.getStorage_size()>0) && (this.storage_bucket >= w.getBounty_limit_per_iteration() || this.storage_bucket < 0)) {
            Logger.logInfoMessage("Work " + String.valueOf(w.getId()) + " verification failed: storage_bucket index exceeds bounds: got " + this.storage_bucket + " but limits were [0, " + w.getBounty_limit_per_iteration() + "].");
            return false;
        }

        // !! otherwise, if storage_size == 0, then no storage is there and storage_bucket must be -1
        if(w.getStorage_size()==0 && this.storage_bucket != -1) {
            Logger.logInfoMessage("Work " + String.valueOf(w.getId()) + " verification failed: storage_bucket index must be -1 because there simply is no storage.");
            return false;
        }


        if (this.isIs_proof_of_work()==false && (submitted_storage.length/4 != w.getStorage_size())) {
            Logger.logInfoMessage("Work " + String.valueOf(w.getId()) + " verification failed: the submitted_storage does not match the works original storage size (" + String.valueOf(submitted_storage.length/4) + " != " + String.valueOf(w.getStorage_size()) + ").");
            return false;
        }
        if (this.isIs_proof_of_work()==true && (submitted_storage.length!=0)) {
            Logger.logInfoMessage("Work " + String.valueOf(w.getId()) + " verification failed: the submitted_storage must be empty for POW.");
            return false;
        }

        long lastBlocksTarget = transaction.getBlock().getPreviousBlockPowTarget();
        if(lastBlocksTarget==0){
            lastBlocksTarget = 1;
            Logger.logErrorMessage("Fatal error came up: previous block target seems to be 0! Block ID of parent " +
                    "block: " + transaction.getBlock().getStringId());
        }

        BigInteger myTarget = ComputationConstants.MAXIMAL_WORK_TARGET;
        myTarget = myTarget.divide(BigInteger.valueOf(Long.MAX_VALUE/100)); // Note, our target in compact form is in range 1..LONG_MAX/100
        myTarget = myTarget.multiply(BigInteger.valueOf(lastBlocksTarget));
        if(myTarget.compareTo(ComputationConstants.MAXIMAL_WORK_TARGET) == 1)
            myTarget = ComputationConstants.MAXIMAL_WORK_TARGET;
        if(myTarget.compareTo(BigInteger.ONE) == 2)
            myTarget = BigInteger.ONE;
        int[] target = Convert.bigintToInts(myTarget,4);
        // safeguard
        if(target.length!=4) target = new int[]{0,0,0,0};
        byte[] tgt;
        try {
            tgt = integersToBytes(target);
        } catch (IOException e) {
            Logger.logInfoMessage("Work " + String.valueOf(w.getId()) + " due to unhandled exception. You are probably hard-forked on this job, don't worry about it ... it won't affect other jobs.");
            return false;
        }

        // Validate code-level
        if (this.is_proof_of_work && !validatePow(transaction.getSenderPublicKey(), w.getBlock_id(),
                work_id, tgt)) {
            Logger.logInfoMessage("Work " + String.valueOf(w.getId()) + " verification failed: proof of work checks in code execution failed.");
            return false;
        }
        if (!this.is_proof_of_work && !validateBty(transaction.getSenderPublicKey(), w.getBlock_id(),
                work_id, tgt)) {
            Logger.logInfoMessage("Work " + String.valueOf(w.getId()) + " verification failed: bounty checks in code execution failed.");
            return false;
        }

        if(this.is_proof_of_work) {
            transaction.itWasAPow();
            Logger.logInfoMessage("Work " + String.valueOf(w.getId()) + " verification succeeded: pow submission passed all checks.");
        }
        else
            Logger.logInfoMessage("Work " + String.valueOf(w.getId()) + " verification succeeded: bty submission passed all checks.");


        isValid = true;
        return true;
    }

    boolean validateLight(Transaction transaction) {

        // This construction avoids multiple code-evaluations which are not really required
        if(validated) return isValid;
        validated = true;

        if (this.work_id == 0) return false;
        Work w = Work.getWork(this.work_id);
        if (w == null) return false;
        if (w.isClosed() == true) return false;
        if (w.getCurrentRound() != this.getCurrent_round()) return false;

        // TODO WARNING: Badly programmed jobs that allow the same output (POW/BTY) for a different multiplicator can suffer big time here! Be careful coding!
        byte[] myMultiplier = this.getMultiplier();
        if(PowAndBounty.hasMultiplier(w.getId(), myMultiplier)) {
            return false;
        }

        // checking multiplicator length requirements
        if (multiplier.length != ComputationConstants.MULTIPLIER_LENGTH) {
            return false;
        }

        // checking pow_hash length requirements once again
        if (hash.length != ComputationConstants.MD5LEN) {
            return false;
        }

        // !! if storage size is larger than 0 this indicates the presence of a storage. Therefore, storage bucket must be in a valid range
        if((w.getStorage_size()>0) && (this.storage_bucket >= w.getBounty_limit_per_iteration() || this.storage_bucket < 0)) {
            return false;
        }

        // !! otherwise, if storage_size == 0, then no storage is there and storage_bucket must be -1
        if(w.getStorage_size()==0 && this.storage_bucket != -1) {
            return false;
        }


        if (this.isIs_proof_of_work()==false && (submitted_storage.length/4 != w.getStorage_size())) {
            Logger.logInfoMessage("Work " + String.valueOf(w.getId()) + " verification failed: the submitted_storage does not match the works original storage size (" + String.valueOf(submitted_storage.length/4) + " != " + String.valueOf(w.getStorage_size()) + ").");
            return false;
        }
        if (this.isIs_proof_of_work()==true && (submitted_storage.length!=0)) {
            Logger.logInfoMessage("Work " + String.valueOf(w.getId()) + " verification failed: the submitted_storage must be empty for POW.");
            return false;
        }


        long lastBlocksTarget = Nxt.getBlockchain().getLastBlock().getCurrentBlockPowTarget(); // TODO: Observe, we are assuming this will go into next block with this
        if(lastBlocksTarget==0){
            lastBlocksTarget = 1;
        }

        BigInteger myTarget = ComputationConstants.MAXIMAL_WORK_TARGET;
        myTarget = myTarget.divide(BigInteger.valueOf(Long.MAX_VALUE/100)); // Note, our target in compact form is in range 1..LONG_MAX/100
        myTarget = myTarget.multiply(BigInteger.valueOf(lastBlocksTarget));
        if(myTarget.compareTo(ComputationConstants.MAXIMAL_WORK_TARGET) == 1)
            myTarget = ComputationConstants.MAXIMAL_WORK_TARGET;
        if(myTarget.compareTo(BigInteger.ONE) == 2)
            myTarget = BigInteger.ONE;
        int[] target = Convert.bigintToInts(myTarget,4);
        // safeguard
        if(target.length!=4) target = new int[]{0,0,0,0};
        byte[] tgt;
        try {
            tgt = integersToBytes(target);
        } catch (IOException e) {
            return false;
        }

        // Validate code-level
        if (this.is_proof_of_work && !validatePow(transaction.getSenderPublicKey(), w.getBlock_id(),
                work_id, tgt)) {
            return false;
        }
        if (!this.is_proof_of_work && !validateBty(transaction.getSenderPublicKey(), w.getBlock_id(),
                work_id, tgt)) {
            return false;
        }

        return true;
    }

    @Override
    void apply(Transaction transaction) {
        if (!validate(transaction))
            return;
        // Here, apply the actual package
        Logger.logInfoMessage("processing pow-or-bty for work: id=" + Long.toUnsignedString(this.work_id));
        PowAndBounty.addPowBty(transaction, this);
    }

    public boolean prevalidate(Transaction transaction) { // TODO This must become more "fluid", what about DOS?
        if (!validateLight(transaction))
            return false;
        return true;
    }

    public byte[] getSubmittedStorageHash() {
        final MessageDigest dig = Crypto.sha256();
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final DataOutputStream dos = new DataOutputStream(baos);
        try {
            dos.write(this.submitted_storage);
            dos.close();
        } catch (final IOException ignored) {

        }
        byte[] longBytes = baos.toByteArray();
        if (longBytes == null) longBytes = new byte[0];
        dig.update(longBytes);
        return dig.digest();
    }

    public byte[] getPowHash() {
        return this.hash;
    }
    public byte[] getHash() {
        final MessageDigest dig = Crypto.sha256();
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final DataOutputStream dos = new DataOutputStream(baos);
        try {
            dos.writeLong(this.work_id);
            dos.writeBoolean(this.is_proof_of_work); // distinguish between pow and bounty
            dos.write(getSubmittedStorageHash());
            dos.close();
        } catch (final IOException ignored) {

        }
        byte[] longBytes = baos.toByteArray();
        if (longBytes == null) longBytes = new byte[0];
        dig.update(longBytes);
        return dig.digest();
    }

}
