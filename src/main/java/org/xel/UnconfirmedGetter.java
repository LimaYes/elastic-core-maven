package org.xel;

import org.xel.computation.Pair;
import org.xel.db.DbIterator;

import java.util.HashMap;
import java.util.Map;

public class UnconfirmedGetter {

    public static Map<Long, Long> refreshMap() {
        Map<Long, Long> unMap = new HashMap<>();
        try (DbIterator<? extends Transaction> unconfirmedTransactions = Nxt.getTransactionProcessor().getAllUnconfirmedTransactions()) {
            while (unconfirmedTransactions.hasNext()) {
                Transaction t = unconfirmedTransactions.next();
                long bal = 0;
                if (unMap.containsKey(t.getSenderId())) {
                    bal = unMap.get(t.getSenderId());
                }
                bal -= t.getAmountNQT() - t.getFeeNQT();
                unMap.put(t.getSenderId(), bal);

                bal = 0;
                if (unMap.containsKey(t.getRecipientId())) {
                    bal = unMap.get(t.getRecipientId());
                }
                bal += t.getAmountNQT();
                unMap.put(t.getRecipientId(), bal);
            }
        }
        return unMap;
    }
}


