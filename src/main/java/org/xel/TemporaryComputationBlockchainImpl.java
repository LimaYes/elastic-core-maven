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

package org.xel;

import org.xel.computation.ComputationConstants;
import org.xel.db.DbIterator;
import org.xel.db.DbUtils;
import org.xel.util.Convert;
import org.xel.util.Filter;
import org.xel.util.ReadWriteUpdateLock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

final class TemporaryComputationBlockchainImpl implements Blockchain {

    private static final TemporaryComputationBlockchainImpl instance = new TemporaryComputationBlockchainImpl();

    static TemporaryComputationBlockchainImpl getInstance() {
        return instance;
    }

    private TemporaryComputationBlockchainImpl() {}

    private final ReadWriteUpdateLock lock = new ReadWriteUpdateLock();
    private final AtomicReference<BlockImpl> lastBlock = new AtomicReference<>();

    @Override
    public void readLock() {
        lock.readLock().lock();
    }

    @Override
    public void readUnlock() {
        lock.readLock().unlock();
    }

    @Override
    public void updateLock() {
        lock.updateLock().lock();
    }

    @Override
    public void updateUnlock() {
        lock.updateLock().unlock();
    }

    void writeLock() {
        lock.writeLock().lock();
    }

    void writeUnlock() {
        lock.writeLock().unlock();
    }

    @Override
    public BlockImpl getLastBlock() {
        return lastBlock.get();
    }

    void setLastBlock(BlockImpl block) {
        lastBlock.set(block);
    }

    @Override
    public int getHeight() {
        BlockImpl last = lastBlock.get();
        return last == null ? 0 : last.getHeight();
    }


    @Override
    public int getLastBlockTimestamp() {
        BlockImpl last = lastBlock.get();
        return last == null ? 0 : last.getTimestamp();
    }

    @Override
    public BlockImpl getLastBlock(int timestamp) {
        BlockImpl block = lastBlock.get();
        if (timestamp >= block.getTimestamp()) {
            return block;
        }
        return TemporaryComputationBlockDb.findLastBlock(timestamp);
    }

    @Override
    public BlockImpl getBlock(long blockId) {
        BlockImpl block = lastBlock.get();
        if (block.getId() == blockId) {
            return block;
        }
        return TemporaryComputationBlockDb.findBlock(blockId);
    }

    @Override
    public boolean hasBlock(long blockId) {
        return lastBlock.get().getId() == blockId || TemporaryComputationBlockDb.hasBlock(blockId);
    }

    @Override
    public DbIterator<BlockImpl> getAllBlocks() {
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block_comp ORDER BY db_id ASC");
            return getBlocks(con, pstmt);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public DbIterator<BlockImpl> getBlocks(int from, int to) {
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block_comp WHERE height <= ? AND height >= ? ORDER BY height DESC");
            int blockchainHeight = getHeight();
            pstmt.setInt(1, blockchainHeight - from);
            pstmt.setInt(2, blockchainHeight - to);
            return getBlocks(con, pstmt);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public DbIterator<BlockImpl> getBlocks(long accountId, int timestamp) {
        return getBlocks(accountId, timestamp, 0, -1);
    }

    @Override
    public DbIterator<BlockImpl> getBlocks(long accountId, int timestamp, int from, int to) {
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block_comp WHERE generator_id = ? "
                    + (timestamp > 0 ? " AND timestamp >= ? " : " ") + "ORDER BY height DESC"
                    + DbUtils.limitsClause(from, to));
            int i = 0;
            pstmt.setLong(++i, accountId);
            if (timestamp > 0) {
                pstmt.setInt(++i, timestamp);
            }
            DbUtils.setLimits(++i, pstmt, from, to);
            return getBlocks(con, pstmt);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public int getBlockCount(long accountId) {
        try (Connection con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT COUNT(*) FROM block_comp WHERE generator_id = ?")) {
            pstmt.setLong(1, accountId);
            try (ResultSet rs = pstmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public DbIterator<BlockImpl> getBlocks(Connection con, PreparedStatement pstmt) {
        return new DbIterator<>(con, pstmt, TemporaryComputationBlockDb::loadBlock);
    }

    @Override
    public List<Long> getBlockIdsAfter(long blockId, int limit) {
        // Check the block cache
        List<Long> result = new ArrayList<>(TemporaryComputationBlockDb.BLOCK_CACHE_SIZE);
        synchronized(TemporaryComputationBlockDb.blockCache) {
            BlockImpl block = TemporaryComputationBlockDb.blockCache.get(blockId);
            if (block != null) {
                Collection<BlockImpl> cacheMap = TemporaryComputationBlockDb.heightMap.tailMap(block.getHeight() + 1).values();
                for (BlockImpl cacheBlock : cacheMap) {
                    if (result.size() >= limit) {
                        break;
                    }
                    result.add(cacheBlock.getId());
                }
                return result;
            }
        }
        // Search the database
        try (Connection con = Db.db.getConnection();
                PreparedStatement pstmt = con.prepareStatement("SELECT id FROM block_comp "
                            + "WHERE db_id > IFNULL ((SELECT db_id FROM block_comp WHERE id = ?), " + Long.MAX_VALUE + ") "
                            + "ORDER BY db_id ASC LIMIT ?")) {
            pstmt.setLong(1, blockId);
            pstmt.setInt(2, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getLong("id"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        return result;
    }

    @Override
    public List<BlockImpl> getBlocksAfter(long blockId, int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }
        // Check the block cache
        List<BlockImpl> result = new ArrayList<>(TemporaryComputationBlockDb.BLOCK_CACHE_SIZE);
        synchronized(TemporaryComputationBlockDb.blockCache) {
            BlockImpl block = TemporaryComputationBlockDb.blockCache.get(blockId);
            if (block != null) {
                Collection<BlockImpl> cacheMap = TemporaryComputationBlockDb.heightMap.tailMap(block.getHeight() + 1).values();
                for (BlockImpl cacheBlock : cacheMap) {
                    if (result.size() >= limit) {
                        break;
                    }
                    result.add(cacheBlock);
                }
                return result;
            }
        }
        // Search the database
        try (Connection con = Db.db.getConnection();
                PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block_comp "
                        + "WHERE db_id > IFNULL ((SELECT db_id FROM block_comp WHERE id = ?), " + Long.MAX_VALUE + ") "
                        + "ORDER BY db_id ASC LIMIT ?")) {
            pstmt.setLong(1, blockId);
            pstmt.setInt(2, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    result.add(TemporaryComputationBlockDb.loadBlock(con, rs, true));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        return result;
    }

    @Override
    public List<BlockImpl> getBlocksAfter(long blockId, List<Long> blockList) {
        if (blockList.isEmpty()) {
            return Collections.emptyList();
        }
        // Check the block cache
        List<BlockImpl> result = new ArrayList<>(TemporaryComputationBlockDb.BLOCK_CACHE_SIZE);
        synchronized(TemporaryComputationBlockDb.blockCache) {
            BlockImpl block = TemporaryComputationBlockDb.blockCache.get(blockId);
            if (block != null) {
                Collection<BlockImpl> cacheMap = TemporaryComputationBlockDb.heightMap.tailMap(block.getHeight() + 1).values();
                int index = 0;
                for (BlockImpl cacheBlock : cacheMap) {
                    if (result.size() >= blockList.size() || cacheBlock.getId() != blockList.get(index++)) {
                        break;
                    }
                    result.add(cacheBlock);
                }
                return result;
            }
        }
        // Search the database
        try (Connection con = Db.db.getConnection();
                PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block_comp "
                        + "WHERE db_id > IFNULL ((SELECT db_id FROM block_comp WHERE id = ?), " + Long.MAX_VALUE + ") "
                        + "ORDER BY db_id ASC LIMIT ?")) {
            pstmt.setLong(1, blockId);
            pstmt.setInt(2, blockList.size());
            try (ResultSet rs = pstmt.executeQuery()) {
                int index = 0;
                while (rs.next()) {
                    BlockImpl block = TemporaryComputationBlockDb.loadBlock(con, rs, true);
                    if (block.getId() != blockList.get(index++)) {
                        break;
                    }
                    result.add(block);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        return result;
    }

    @Override
    public long getBlockIdAtHeight(int height) {
        Block block = lastBlock.get();
        if (height > block.getHeight()) {
            throw new IllegalArgumentException("Invalid height " + height + ", current comp-blockchain is at " + block.getHeight());
        }
        if (height == block.getHeight()) {
            return block.getId();
        }
        return TemporaryComputationBlockDb.findBlockIdAtHeight(height);
    }

    @Override
    public BlockImpl getBlockAtHeight(int height) {
        BlockImpl block = lastBlock.get();
        if (height > block.getHeight()) {
            throw new IllegalArgumentException("Invalid height " + height + ", current comp-blockchain is at " + block.getHeight());
        }
        if (height == block.getHeight()) {
            return block;
        }
        return TemporaryComputationBlockDb.findBlockAtHeight(height);
    }

    @Override
    public BlockImpl getECBlock(int timestamp) {
        Block block = getLastBlock(timestamp);
        if (block == null) {
            return getBlockAtHeight(0);
        }
        return TemporaryComputationBlockDb.findBlockAtHeight(Math.max(block.getHeight() - 720, 0));
    }

    @Override
    public TransactionImpl getTransaction(long transactionId) {
        return TemporaryComputationTransactionDb.findTransaction(transactionId);
    }

    @Override
    public TransactionImpl getTransactionByFullHash(String fullHash) {
        return TemporaryComputationTransactionDb.findTransactionByFullHash(Convert.parseHexString(fullHash));
    }

    @Override
    public boolean hasTransaction(long transactionId) {
        return TemporaryComputationTransactionDb.hasTransaction(transactionId);
    }

    @Override
    public boolean hasTransactionByFullHash(String fullHash) {
        return TemporaryComputationTransactionDb.hasTransactionByFullHash(Convert.parseHexString(fullHash));
    }

    @Override
    public int getTransactionCount() {
        try (Connection con = Db.db.getConnection(); PreparedStatement pstmt = con.prepareStatement("SELECT COUNT(*) FROM transaction_comp");
             ResultSet rs = pstmt.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public DbIterator<TransactionImpl> getAllTransactions() {
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM transaction_comp ORDER BY db_id ASC");
            return getTransactions(con, pstmt);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public DbIterator<TransactionImpl> getTransactions(long accountId, byte type, byte subtype, int blockTimestamp,
                                                       boolean includeExpiredPrunable) {
        return getTransactions(accountId, 0, type, subtype, blockTimestamp, false, false, false, 0, -1, includeExpiredPrunable, false);
    }

    @Override
    public DbIterator<TransactionImpl> getTransactions(long accountId, int numberOfConfirmations, byte type, byte subtype,
                                                       int blockTimestamp, boolean withMessage, boolean phasedOnly, boolean nonPhasedOnly,
                                                       int from, int to, boolean includeExpiredPrunable, boolean executedOnly) {
        if (phasedOnly && nonPhasedOnly) {
            throw new IllegalArgumentException("At least one of phasedOnly or nonPhasedOnly must be false");
        }
        int height = numberOfConfirmations > 0 ? getHeight() - numberOfConfirmations : Integer.MAX_VALUE;
        if (height < 0) {
            throw new IllegalArgumentException("Number of confirmations required " + numberOfConfirmations
                    + " exceeds current blockchain height " + getHeight());
        }
        Connection con = null;
        try {
            StringBuilder buf = new StringBuilder();
            buf.append("SELECT transaction.* FROM transaction ");
            if (executedOnly && !nonPhasedOnly) {
                buf.append(" LEFT JOIN phasing_poll_result ON transaction.id = phasing_poll_result.id ");
            }
            buf.append("WHERE recipient_id = ? AND sender_id <> ? ");
            if (blockTimestamp > 0) {
                buf.append("AND block_timestamp >= ? ");
            }
            if (type >= 0) {
                buf.append("AND type = ? ");
                if (subtype >= 0) {
                    buf.append("AND subtype = ? ");
                }
            }
            if (height < Integer.MAX_VALUE) {
                buf.append("AND transaction.height <= ? ");
            }
            if (withMessage) {
                buf.append("AND (has_message = TRUE OR has_encrypted_message = TRUE ");
                buf.append("OR ((has_prunable_message = TRUE OR has_prunable_encrypted_message = TRUE) AND timestamp > ?)) ");
            }
            if (phasedOnly) {
                buf.append("AND phased = TRUE ");
            } else if (nonPhasedOnly) {
                buf.append("AND phased = FALSE ");
            }
            if (executedOnly && !nonPhasedOnly) {
                buf.append("AND (phased = FALSE OR approved = TRUE) ");
            }
            buf.append("UNION ALL SELECT transaction.* FROM transaction ");
            if (executedOnly && !nonPhasedOnly) {
                buf.append(" LEFT JOIN phasing_poll_result ON transaction.id = phasing_poll_result.id ");
            }
            buf.append("WHERE sender_id = ? ");
            if (blockTimestamp > 0) {
                buf.append("AND block_timestamp >= ? ");
            }
            if (type >= 0) {
                buf.append("AND type = ? ");
                if (subtype >= 0) {
                    buf.append("AND subtype = ? ");
                }
            }
            if (height < Integer.MAX_VALUE) {
                buf.append("AND transaction.height <= ? ");
            }
            if (withMessage) {
                buf.append("AND (has_message = TRUE OR has_encrypted_message = TRUE OR has_encrypttoself_message = TRUE ");
                buf.append("OR ((has_prunable_message = TRUE OR has_prunable_encrypted_message = TRUE) AND timestamp > ?)) ");
            }
            if (phasedOnly) {
                buf.append("AND phased = TRUE ");
            } else if (nonPhasedOnly) {
                buf.append("AND phased = FALSE ");
            }
            if (executedOnly && !nonPhasedOnly) {
                buf.append("AND (phased = FALSE OR approved = TRUE) ");
            }

            buf.append("ORDER BY block_timestamp DESC, transaction_index DESC");
            buf.append(DbUtils.limitsClause(from, to));
            con = Db.db.getConnection();
            PreparedStatement pstmt;
            int i = 0;
            pstmt = con.prepareStatement(buf.toString());
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, accountId);
            if (blockTimestamp > 0) {
                pstmt.setInt(++i, blockTimestamp);
            }
            if (type >= 0) {
                pstmt.setByte(++i, type);
                if (subtype >= 0) {
                    pstmt.setByte(++i, subtype);
                }
            }
            if (height < Integer.MAX_VALUE) {
                pstmt.setInt(++i, height);
            }
            int prunableExpiration = Math.max(0, Constants.INCLUDE_EXPIRED_PRUNABLE && includeExpiredPrunable ?
                                        Nxt.getEpochTime() - Constants.MAX_PRUNABLE_LIFETIME :
                                        Nxt.getEpochTime() - Constants.MIN_PRUNABLE_LIFETIME);
            if (withMessage) {
                pstmt.setInt(++i, prunableExpiration);
            }
            pstmt.setLong(++i, accountId);
            if (blockTimestamp > 0) {
                pstmt.setInt(++i, blockTimestamp);
            }
            if (type >= 0) {
                pstmt.setByte(++i, type);
                if (subtype >= 0) {
                    pstmt.setByte(++i, subtype);
                }
            }
            if (height < Integer.MAX_VALUE) {
                pstmt.setInt(++i, height);
            }
            if (withMessage) {
                pstmt.setInt(++i, prunableExpiration);
            }
            DbUtils.setLimits(++i, pstmt, from, to);
            return getTransactions(con, pstmt);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public DbIterator<TransactionImpl> getReferencingTransactions(long transactionId, int from, int to) {
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT transaction_comp.* FROM transaction_comp, referenced_transaction_comp "
                    + "WHERE referenced_transaction_comp.referenced_transaction_id = ? "
                    + "AND referenced_transaction_comp.transaction_id = transaction.id "
                    + "ORDER BY transaction_comp.block_timestamp DESC, transaction_comp.transaction_index DESC "
                    + DbUtils.limitsClause(from, to));
            int i = 0;
            pstmt.setLong(++i, transactionId);
            DbUtils.setLimits(++i, pstmt, from, to);
            return getTransactions(con, pstmt);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public DbIterator<TransactionImpl> getTransactions(Connection con, PreparedStatement pstmt) {
        return new DbIterator<>(con, pstmt, TemporaryComputationTransactionDb::loadTransaction);
    }

    @Override
    public List<TransactionImpl> getExpectedTransactions(Filter<Transaction> filter) {
        Map<TransactionType, Map<String, Integer>> duplicates = new HashMap<>();
        BlockchainProcessorImpl blockchainProcessor = BlockchainProcessorImpl.getInstance();
        List<TransactionImpl> result = new ArrayList<>();
        readLock();
        try {
            if (getHeight() >= Constants.PHASING_BLOCK) {
                try (DbIterator<TransactionImpl> phasedTransactions = PhasingPoll.getFinishingTransactions(getHeight() + 1)) {
                    for (TransactionImpl phasedTransaction : phasedTransactions) {
                        try {
                            phasedTransaction.validate();
                            if (!phasedTransaction.attachmentIsDuplicate(duplicates, false) && filter.ok(phasedTransaction)) {
                                result.add(phasedTransaction);
                            }
                        } catch (NxtException.ValidationException ignore) {
                        }
                    }
                }
            }
            blockchainProcessor.selectUnconfirmedTransactions(duplicates, getLastBlock(), -1).forEach(
                    unconfirmedTransaction -> {
                        TransactionImpl transaction = unconfirmedTransaction.getTransaction();
                        if (transaction.getPhasing() == null && filter.ok(transaction)) {
                            result.add(transaction);
                        }
                    }
            );
        } finally {
            readUnlock();
        }
        return result;
    }
}
