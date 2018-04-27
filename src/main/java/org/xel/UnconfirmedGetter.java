package org.xel;

import org.xel.computation.MessageEncoder;
import org.xel.computation.Pair;
import org.xel.db.DbIterator;

import java.util.*;


public class UnconfirmedGetter {
    public static Map<Long, Long> unMap = new HashMap<>();
    public static List<Long> unMapPaidBty = new ArrayList<>();
    public static long lastTimeUnmapGenerated = 0;

    public static synchronized Map<Long, Long> refreshMap() {
        // Only recreate that map once every 5 seconds tops!
        long curTime = (new Date()).getTime();
        if((curTime - lastTimeUnmapGenerated) / 1000 < 5) return unMap;
        unMap.clear();
        unMapPaidBty.clear();
        lastTimeUnmapGenerated = curTime;
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

                // Check for BTY
                List<Pair<Long,Long>> containsPm = MessageEncoder.extractPaymentsFromTX(t);
                for(Pair<Long, Long> p : containsPm){
                    unMapPaidBty.add(p.getElement0());
                }
            }
        }
        return unMap;
    }

    public static List<Long> getUnMapPaidBty() {
        return unMapPaidBty;
    }
}


