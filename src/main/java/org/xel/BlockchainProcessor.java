/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 * Copyright © 2017 The XEL Core Developers
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

import org.xel.db.ComputationalDerivedDbTable;
import org.xel.db.DerivedDbTable;
import org.xel.peer.Peer;
import org.xel.util.Observable;
import org.json.simple.JSONObject;

import java.util.List;

public interface BlockchainProcessor extends Observable<Block,BlockchainProcessor.Event> {

    enum Event {
        BLOCK_PUSHED, BLOCK_POPPED, BLOCK_GENERATED, BLOCK_SCANNED,
        RESCAN_BEGIN, RESCAN_END,
        BEFORE_BLOCK_ACCEPT, AFTER_BLOCK_ACCEPT,
        BEFORE_BLOCK_APPLY, AFTER_BLOCK_APPLY,
        BLOCK_PUSHED_COMPUTATION, BLOCK_POPPED_COMPUTATION, BLOCK_GENERATED_COMPUTATION, BLOCK_SCANNED_COMPUTATION,
        RESCAN_BEGIN_COMPUTATION, RESCAN_END_COMPUTATION,
        BEFORE_BLOCK_ACCEPT_COMPUTATION, AFTER_BLOCK_ACCEPT_COMPUTATION,
        BEFORE_BLOCK_APPLY_COMPUTATION, AFTER_BLOCK_APPLY_COMPUTATION
    }
    void pushBlock(final BlockImpl block, boolean pushAnyway) throws BlockNotAcceptedException;
    void pushBlock(final BlockImpl block) throws BlockNotAcceptedException;

    Peer getLastBlockchainFeeder();

    int getLastBlockchainFeederHeight();

    boolean isScanning();

    boolean isDownloading();

    boolean isProcessingBlock();

    int getMinRollbackHeight();

    int getInitialScanHeight();

    void generateBlock(String secretPhrase, int blockTimestamp) throws BlockNotAcceptedException;

    void processPeerBlock(JSONObject request) throws NxtException;

    void fullReset();

    void scan(int height, boolean validate);

    void fullScanWithShutdown();

    void setGetMoreBlocks(boolean getMoreBlocks);

    List<? extends Block> popOffTo(int height);

    void registerDerivedTable(DerivedDbTable table);

    void registerComputationalDerivedTable(ComputationalDerivedDbTable table);

    void trimDerivedTables();

    int restorePrunedData();

    Transaction restorePrunedTransaction(long transactionId);

    class BlockNotAcceptedException extends NxtException {

        private final BlockImpl block;

        BlockNotAcceptedException(String message, BlockImpl block) {
            super(message);
            this.block = block;
        }

        BlockNotAcceptedException(Throwable cause, BlockImpl block) {
            super(cause);
            this.block = block;
        }

        @Override
        public String getMessage() {
            return block == null ? super.getMessage() : super.getMessage() + ", block " + block.getStringId() + " " + block.getJSONObject().toJSONString();
        }

    }

    class TransactionNotAcceptedException extends BlockNotAcceptedException {

        private final TransactionImpl transaction;

        TransactionNotAcceptedException(String message, TransactionImpl transaction) {
            super(message, transaction.getBlock());
            this.transaction = transaction;
        }

        TransactionNotAcceptedException(Throwable cause, TransactionImpl transaction) {
            super(cause, transaction.getBlock());
            this.transaction = transaction;
        }

        public TransactionImpl getTransaction() {
            return transaction;
        }

        @Override
        public String getMessage() {
            return super.getMessage() + ", transaction " + transaction.getStringId() + " " + transaction.getJSONObject().toJSONString();
        }
    }

    class BlockOutOfOrderException extends BlockNotAcceptedException {

        BlockOutOfOrderException(String message, BlockImpl block) {
            super(message, block);
        }

	}

}
