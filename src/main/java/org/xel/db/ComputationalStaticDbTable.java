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

package org.xel.db;

import org.xel.Constants;
import org.xel.Db;
import org.xel.Nxt;
import org.xel.util.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class ComputationalStaticDbTable<T>  {
    protected final String table;

    protected final DbKey.Factory<T> dbKeyFactory;
    protected static final TransactionalDb db = Db.db;

    protected ComputationalStaticDbTable(String table, DbKey.Factory<T> dbKeyFactory) {
        this.dbKeyFactory = dbKeyFactory;
        this.table = table;
    }

    protected abstract T load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException;

    protected abstract void save(Connection con, T t) throws SQLException;


    public final T get(DbKey dbKey) {

        try (Connection con = db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table + dbKeyFactory.getPKClause())) {
            dbKey.setPK(pstmt);
            return get(con, pstmt);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    private T get(Connection con, PreparedStatement pstmt) throws SQLException {
        try (ResultSet rs = pstmt.executeQuery()) {
            if (!rs.next()) {
                return null;
            }
            T t = null;
            DbKey dbKey = null;
            if (t == null) {
                t = load(con, rs, dbKey);
            }
            if (rs.next()) {
                throw new RuntimeException("Multiple records found");
            }
            return t;
        }
    }

    public final void insert(T t) {
        if (!Db.db.isInTransaction()) {
            try {
                Db.db.beginTransaction();
                insert(t);
                Db.db.commitTransaction();
            } catch (Exception e) {
                Db.db.rollbackTransaction();
                throw e;
            } finally {
                Db.db.endTransaction();
            }
            return;
        }
        DbKey dbKey = dbKeyFactory.newKey(t);
        if (dbKey == null) {
            throw new RuntimeException("DbKey not set");
        }
        try (Connection con = db.getConnection()) {
            save(con, t);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

}
