package org.xel;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.json.simple.JSONArray;
import org.xel.computation.CommandPowBty;

import org.xel.db.DbClause;
import org.xel.db.DbIterator;
import org.xel.db.DbKey;
import org.xel.db.DbUtils;
import org.xel.db.VersionedEntityDbTable;
import org.xel.util.Convert;
import org.xel.util.Listener;
import org.xel.util.Listeners;
import org.xel.util.Logger;

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

import static java.security.MessageDigest.getInstance;
public final class PowAndBounty{
    public static MessageDigest dig = null;
    public static int toInt(final byte[] bytes, final int offset) {
        int ret = 0;
        for (int i = 0; (i < 4) && ((i + offset) < bytes.length); i++) {
            ret <<= 8;
            ret |= bytes[i + offset] & 0xFF;
        }
        return ret;
    }
    static {
        try {
            dig = getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            // Should always work
            e.printStackTrace();
            System.exit(1);
        }
    }
    public static int[] personalizedIntStream(final byte[] publicKey, final long blockId, final byte[] multiplicator, final long workId) throws Exception {
        final int[] stream = new int[12];

        dig.reset();
        dig.update(multiplicator);
        dig.update(publicKey);

        System.out.println("Calculating Personalized Int Stream");
        System.out.println("Multiplicator: " + Convert.toHexString(multiplicator));
        System.out.println("PublicKey: " + Convert.toHexString(publicKey));

        final byte[] b1 = new byte[16];
        for (int i = 0; i < 8; ++i) b1[i] = (byte) (workId >> ((8 - i - 1) << 3));
        for (int i = 0; i < 8; ++i) b1[i + 8] = (byte) (blockId >> ((8 - i - 1) << 3));

        dig.update(b1);
        System.out.println("TotalBytes: " + (16+multiplicator.length+publicKey.length));

        System.out.println("b1: " + Convert.toHexString(b1));

        byte[] digest = dig.digest();

        System.out.println("Digest: " + Convert.toHexString(digest));

        int ln = digest.length;
        if (ln == 0) {
            throw new Exception("Bad digest calculation");
        }

        int[] multi32 = Convert.byte2int(multiplicator);

        for (int i = 0; i < 10; ++i) {
            int got = toInt(digest, (i * 4) % ln);
            if (i > 4) got = got ^ stream[i - 3];
            stream[i] = got;
            System.out.println(i + ": " + Integer.toHexString(stream[i]));

        }
        stream[10] = multi32[1];
        stream[11] = multi32[2];
        System.out.println("10" + ": " + Integer.toHexString(stream[10]));
        System.out.println("11" + ": " + Integer.toHexString(stream[11]));


        return stream;
    }
    //public static int[] personalizedIntStream(final byte[] publicKey, final long blockId, final byte[] multiplicator, final long workId) throws Exception {

    public JSONArray getJSONInts() {
        JSONArray arr = new JSONArray();
        byte[] pbkey = Account.getPublicKey(this.accountId);
        try {
            int[] ints = personalizedIntStream(pbkey, Work.getWork(this.work_id).getBlock_id(), this.multiplier, this.work_id);
            for(int x : ints){
                arr.add(x);
            }
        } catch (Exception e) {
        }
        return arr;
    }

    public enum Event {
        POW_SUBMITTED, BOUNTY_SUBMITTED
    }

    private static final Listeners<PowAndBounty, Event> listeners = new Listeners<>();

    private static final DbKey.LongKeyFactory<PowAndBounty> powAndBountyDbKeyFactory = new DbKey.LongKeyFactory<PowAndBounty>(
            "id") {

        @Override
        public DbKey newKey(final PowAndBounty participant) {
            return participant.dbKey;
        }

    };

    private static final VersionedEntityDbTable<PowAndBounty> powAndBountyTable = new VersionedEntityDbTable<PowAndBounty>(
            "pow_and_bounty", PowAndBounty.powAndBountyDbKeyFactory) {

        @Override
        protected PowAndBounty load(final Connection con, final ResultSet rs, final DbKey dbKey) throws SQLException {
            return new PowAndBounty(rs, dbKey);
        }

        @Override
        protected void save(final Connection con, final PowAndBounty participant) throws SQLException {
            participant.save(con);
        }
    };

    public static void addPowBty(final Transaction transaction, final CommandPowBty attachment) {
        PowAndBounty shuffling = new PowAndBounty(transaction, attachment);
        PowAndBounty.powAndBountyTable.insert(shuffling); // store immedeately!

        // Here check if it is counting or if it is "old"
        Work w = Work.getWork(attachment.getWork_id());
        if(w == null ||w.isClosed()){
            return; // just another line of defense
        }

        // Now the work itself has to be manipulated (and close if necessary)
        if(attachment.isIs_proof_of_work())
        {
            // logic for PoW
            w.setReceived_pows(w.getReceived_pows() + 1);
            //System.out.println("Work already got " + w.getReceived_pows() + " of " + w.getCap_number_pow() + " POWS!");

            // Close work if enough bounties were received
            int cap = w.getCap_number_pow();
            if(w.getReceived_pows() == cap){
                w.setClosed(true);
                w.setClosing_timestamp(transaction.getBlockTimestamp());
            }

            w.EmitPow();
            w.JustSave();
        }
        else
        {
            // logic for BTY
            w.setReceived_bounties(w.getReceived_bounties() + 1);


            // Close work if enough bounties were received
            int cap = w.getIterations() * w.getBounty_limit_per_iteration();
            if(w.getReceived_bounties() == cap){
                w.setClosed(true);
                w.setClosing_timestamp(transaction.getBlockTimestamp());
            }


            w.EmitBty();
            w.JustSave(); // we will have to save again when storage gets consolidated in the next step! Do it more elegant later on


            // In all cases (even after close case) make sure the combined storage is updated properly!
            if(w.getReceived_bounties()%w.getBounty_limit_per_iteration()==0){
                Logger.logDebugMessage("Consolidating storage for job " + w.getId() + " after " + w.getReceived_bounties() + " bounties behind the scenes; in fact, nothing has to be done due to the new method :-)");
            }



        }
        PowAndBounty.listeners.notify(shuffling, (shuffling.is_pow)?Event.POW_SUBMITTED:Event.BOUNTY_SUBMITTED);
    }


    public static boolean addListener(final Listener<PowAndBounty> listener, final Event eventType) {
        return PowAndBounty.listeners.addListener(listener, eventType);
    }


    public static DbIterator<PowAndBounty> getBounties(final long wid) {
        return PowAndBounty.powAndBountyTable.getManyBy(new DbClause.LongClause("work_id", wid)
                        .and(new DbClause.BooleanClause("is_pow", false)).and(new DbClause.BooleanClause("latest", true)), 0,
                -1, "");
    }

    public static DbIterator<PowAndBounty> getBounties(final long wid, final long aid) {
        return PowAndBounty.powAndBountyTable.getManyBy(new DbClause.LongClause("work_id", wid)
                .and(new DbClause.BooleanClause("is_pow", false)).and(new DbClause.LongClause("account_id", aid))
                .and(new DbClause.BooleanClause("latest", true)), 0, -1, "");
    }

    public static DbIterator<PowAndBounty> getLastBountiesRelevantForStorageGeneration(final long wid, int fullrounds, int skip, long index){
        return PowAndBounty.powAndBountyTable.getManyBy(new DbClause.LongClause("work_id", wid)
                        .and(new DbClause.BooleanClause("is_pow", false)).and(new DbClause.BooleanClause("latest", true)), skip+(int)index,
                skip+(int)index, " ORDER BY height DESC");
    }


    static int getBountyCount(final long wid) {
        return PowAndBounty.powAndBountyTable
                .getCount(new DbClause.LongClause("work_id", wid).and(new DbClause.BooleanClause("is_pow", false)));
    }

    static int getPowCount(final long wid) {
        return PowAndBounty.powAndBountyTable
                .getCount(new DbClause.LongClause("work_id", wid).and(new DbClause.BooleanClause("is_pow", true)));
    }

    public static PowAndBounty getPowOrBountyById(final long id) {
        return PowAndBounty.powAndBountyTable.get(PowAndBounty.powAndBountyDbKeyFactory.newKey(id));
    }

    public static DbIterator<PowAndBounty> getPows(final long wid) {
        return PowAndBounty.powAndBountyTable.getManyBy(new DbClause.LongClause("work_id", wid)
                        .and(new DbClause.BooleanClause("is_pow", true)).and(new DbClause.BooleanClause("latest", true)), 0, -1,
                "");
    }

    public static DbIterator<PowAndBounty> getPows(final long wid, final long aid) {
        return PowAndBounty.powAndBountyTable.getManyBy(new DbClause.LongClause("work_id", wid)
                .and(new DbClause.BooleanClause("is_pow", true)).and(new DbClause.LongClause("account_id", aid))
                .and(new DbClause.BooleanClause("latest", true)), 0, -1, "");
    }

    public static boolean hasHash(final byte[] hash) {
        return PowAndBounty.powAndBountyTable
                .getCount(new DbClause.BytesClause("hash", hash)) > 0;
    }


    // storage hash linked to wid only
    public static boolean hasVerificatorHash(long workId, byte[] hash) {
        return PowAndBounty.powAndBountyTable
                .getCount(new DbClause.BytesClause("verificator_hash", hash).and(new DbClause.LongClause("work_id",workId))) > 0;
    }

    public static boolean hasMultiplier(long workId, byte[] multiplier) {
        return PowAndBounty.powAndBountyTable
                .getCount(new DbClause.BytesClause("multiplier", multiplier).and(new DbClause.LongClause("work_id",workId))) > 0;
    }


    static void init() {
    }

    public static boolean removeListener(final Listener<PowAndBounty> listener, final Event eventType) {
        return PowAndBounty.listeners.removeListener(listener, eventType);
    }

    private final long id;
    private final boolean is_pow;
    private boolean too_late;
    private final long work_id;
    private final long accountId;
    private final DbKey dbKey;
    private final byte[] hash;
    private final byte[] verificator_hash;
    private final byte[] multiplier;
    private final byte[] pow_hash;
    private final int storage_bucket;
    private final byte[] submitted_storage;

    private int timestampReceived = 0;

    public long getWork_id() {
        return work_id;
    }

    public long getId() {
        return id;
    }

    private PowAndBounty(final ResultSet rs, final DbKey dbKey) throws SQLException {
        this.id = rs.getLong("id");
        this.work_id = rs.getLong("work_id");
        this.accountId = rs.getLong("account_id");
        this.is_pow = rs.getBoolean("is_pow");
        this.dbKey = dbKey;
        this.too_late = rs.getBoolean("too_late");
        this.hash = rs.getBytes("hash");
        this.verificator_hash = rs.getBytes("hash");
        this.multiplier = rs.getBytes("multiplier");
        this.pow_hash = rs.getBytes("pow_hash");
        this.submitted_storage = rs.getBytes("submitted_storage");
        this.storage_bucket = rs.getInt("storage_bucket");
        this.timestampReceived = rs.getInt("timestamp");
    }

    public byte[] getSubmitted_storage() {
        return submitted_storage;
    }

    private PowAndBounty(final Transaction transaction, final CommandPowBty attachment) {
        this.id = transaction.getId();
        this.work_id = attachment.getWork_id();
        this.accountId = transaction.getSenderId();
        this.dbKey = PowAndBounty.powAndBountyDbKeyFactory.newKey(this.id);
        this.is_pow = attachment.isIs_proof_of_work();
        this.hash = attachment.getHash();
        this.verificator_hash = attachment.getSubmittedStorageHash();
        this.multiplier = attachment.getMultiplier();
        this.pow_hash = attachment.getPowHash();
        this.submitted_storage = attachment.getSubmitted_storage();
        this.too_late = false;
        this.storage_bucket = attachment.getStorage_bucket();
        this.timestampReceived = transaction.getTimestamp();
    }

    public long getAccountId() {
        return this.accountId;
    }


    private void save(final Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement( /* removed storage between multiplier and submitted_storage in
        next line */
                "MERGE INTO pow_and_bounty (id, too_late, work_id, hash, multiplier, storage_bucket, submitted_storage, " +
                        "account_id, is_pow, verificator_hash, pow_hash, timestamp, "
                        + " height, latest) " + "KEY (id, height) " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?," +
                        " TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, this.id);
            pstmt.setBoolean(++i, this.too_late);
            pstmt.setLong(++i, this.work_id);
            DbUtils.setBytes(pstmt, ++i, this.hash);
            DbUtils.setBytes(pstmt, ++i, this.multiplier);
            pstmt.setInt(++i, this.storage_bucket);
            DbUtils.setBytes(pstmt, ++i, this.submitted_storage);
            pstmt.setLong(++i, this.accountId);
            pstmt.setBoolean(++i, this.is_pow);
            DbUtils.setBytes(pstmt, ++i, this.verificator_hash);
            DbUtils.setBytes(pstmt, ++i, this.pow_hash);
            pstmt.setInt(++i, this.timestampReceived);
            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
    }
}
