package org.xel.computation;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TimedCacheList {

    private static int REFRESH_INTERVAL = 5;
    // holds the last time the cache has been refreshed in millis
    private volatile long lastRefreshDate = (new Date()).getTime();

    // indicates that cache is currently refreshing entries
    private volatile boolean cacheCurrentlyRefreshing;

    private Map<Pair<Long, Long>, Long> map = new HashMap<>();

    public void put(Long key, Long value) {
        if (cacheNeedsRefresh()) {
            refresh();
        }
        Pair<Long, Long> elem = new Pair<>(key,value);
        map.put(elem, (new Date()).getTime());
    }

    public boolean has(Long key, Long value) {
        Pair<Long, Long> elem = new Pair<>(key,value);
        if (cacheNeedsRefresh()) {
            refresh();
        }
        if(map.containsKey(elem)) return true;
        return false;
    }

    private boolean cacheNeedsRefresh() {
        // make sure that cache is not currently being refreshed by some
        // other thread.
        if (cacheCurrentlyRefreshing) {
            return false;
        }
        return ((new Date()).getTime() - lastRefreshDate)/1000 >= REFRESH_INTERVAL;
    }

    private void refresh() {
        // make sure the cache did not start refreshing between cacheNeedsRefresh()
        // and refresh() by some other thread.
        if (cacheCurrentlyRefreshing) {
            return;
        }

        // signal to other threads that cache is currently being refreshed.
        cacheCurrentlyRefreshing = true;

        try {
            long currTm = ((new Date()).getTime());

            // refresh your cache contents here
            for(Iterator<Map.Entry<Pair<Long, Long> ,Long>> it = map.entrySet().iterator(); it.hasNext();){
                Map.Entry<Pair<Long, Long>, Long> entry = it.next();
                if ((currTm - entry.getValue())/1000 > 240) {
                    it.remove();
                }
            }
        } finally {
            // set the lastRefreshDate and signal that cache has finished
            // refreshing to other threads.
            lastRefreshDate = (new Date()).getTime();
            cacheCurrentlyRefreshing = false;
        }
    }

    public int itemcnt() {
        return map.size();
    }
}
