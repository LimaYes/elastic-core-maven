package org.xel;

import org.xel.db.ComputationalStaticDbTable;
import org.xel.db.DbKey;
import org.xel.db.DbUtils;
import org.xel.util.Convert;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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

public final class AlternativeChainPubkeys {

    private static final DbKey.LongKeyFactory<AlternativeChainPubkeys> acpDbKeyFactory = new DbKey.LongKeyFactory<AlternativeChainPubkeys>(
            "id") {

        @Override
        public DbKey newKey(final AlternativeChainPubkeys participant) {
            return participant.dbKey;
        }

    };

    private static final ComputationalStaticDbTable<AlternativeChainPubkeys> acpBountyTable = new ComputationalStaticDbTable<AlternativeChainPubkeys>(
            "acp", AlternativeChainPubkeys.acpDbKeyFactory) {

        @Override
        protected AlternativeChainPubkeys load(final Connection con, final ResultSet rs, final DbKey dbKey) throws SQLException {
            return new AlternativeChainPubkeys(rs, dbKey);
        }

        @Override
        protected void save(final Connection con, final AlternativeChainPubkeys participant) throws SQLException {
            participant.save(con);
        }
    };


    static void init() {
        byte[] todo = Convert.parseHexString("d47b2caa80370cbb3528d7b3635e0e43721bbd081424dd57ec062db2d6f5802b");
        long id = -1567302243468433412L;
        if(AlternativeChainPubkeys.getKnownIdentity(id)==null){
            AlternativeChainPubkeys.addKnownIdentity(id, todo);
        }

    }

    private final DbKey dbKey;
    private final long id;
    private final byte[] pubkey;

    public byte[] getPubkey() {
        return pubkey;
    }

    public long getId() {
        return id;
    }

    private AlternativeChainPubkeys(final ResultSet rs, final DbKey dbKey) throws SQLException {
        this.id = rs.getLong("id");
        this.pubkey = rs.getBytes("publickey");
        this.dbKey = dbKey;
    }

    private AlternativeChainPubkeys(final Transaction t)  {
        this.id = t.getSenderId();
        this.pubkey = t.getSenderPublicKeyComputational();
        this.dbKey = AlternativeChainPubkeys.acpDbKeyFactory.newKey(this.id);
    }
    private AlternativeChainPubkeys(final long id, byte[] pbk)  {
        this.id = id;
        this.pubkey = pbk;
        this.dbKey = AlternativeChainPubkeys.acpDbKeyFactory.newKey(this.id);
    }

    private AlternativeChainPubkeys(final Block t){
        this.id = t.getGeneratorId();
        this.pubkey = t.getGeneratorPubkeyComputational();
        this.dbKey = AlternativeChainPubkeys.acpDbKeyFactory.newKey(this.id);
    }

    public static void addKnownIdentity(final Transaction transaction) {
        AlternativeChainPubkeys p = new AlternativeChainPubkeys(transaction);
        AlternativeChainPubkeys.acpBountyTable.insert(p);
    }
    public static void addKnownIdentity(final Block b) {
        AlternativeChainPubkeys p = new AlternativeChainPubkeys(b);
        AlternativeChainPubkeys.acpBountyTable.insert(p);
    }
    public static void addKnownIdentity(final long id, final byte[] p) {
        AlternativeChainPubkeys px = new AlternativeChainPubkeys(id, p);
        AlternativeChainPubkeys.acpBountyTable.insert(px);
    }

    public static AlternativeChainPubkeys getKnownIdentity(final long id) {
        return AlternativeChainPubkeys.acpBountyTable.get(AlternativeChainPubkeys.acpDbKeyFactory.newKey(id));
    }
    private void save(final Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement( /* removed storage between multiplier and submitted_storage in
        next line */
                "MERGE INTO acp (id, publickey) " + "KEY (id) " + "VALUES (?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, this.id);
            DbUtils.setBytes(pstmt, ++i, this.pubkey);
            pstmt.executeUpdate();
        }
    }
}
