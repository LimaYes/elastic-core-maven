package org.xel.http;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import org.xel.*;
import org.xel.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

class ClearTask extends TimerTask {
	private HashMap<Integer, ExpiringListPointer> toClear = null;
	private ArrayList<String> events = null;

	public ClearTask(final HashMap<Integer, ExpiringListPointer> h, final ArrayList<String> e) {
		this.toClear = h;
		this.events = e;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public void run() {
		if (this.toClear != null) {
			int minimalIndex = Integer.MAX_VALUE;
			Iterator it = this.toClear.entrySet().iterator();
			while (it.hasNext()) {
				@SuppressWarnings("unchecked")
				final HashMap.Entry<Integer, ExpiringListPointer> ptr = (HashMap.Entry<Integer, ExpiringListPointer>) it
						.next();

				if (ptr.getValue().expired()) it.remove(); // avoids a ConcurrentModificationException
				else if (ExpiringListPointer.lastPosition < minimalIndex)
					minimalIndex = ExpiringListPointer.lastPosition;
			}

			// strip events below minimalIndex, if applicable
			if ((minimalIndex > 0) && (minimalIndex != Integer.MAX_VALUE)) this.events.subList(0, minimalIndex).clear();

			// run again through iterator and adjust minimal indized
			it = this.toClear.entrySet().iterator();
			while (it.hasNext()) {
				final HashMap.Entry<Integer, ExpiringListPointer> ptr = (HashMap.Entry<Integer, ExpiringListPointer>) it
						.next();
				if ((minimalIndex > 0) && (minimalIndex != Integer.MAX_VALUE))
					ptr.getValue().normalizeIndex(minimalIndex);
			}
		}

	}
}

final class ExpiringListPointer {
	static int lastPosition = 0;
	private static int expireTime = 0;
	private Date lastUpdated = null;

	public ExpiringListPointer(final int latestPosition, final int expireTimeLocal) {
		this.lastUpdated = new Date();
		ExpiringListPointer.lastPosition = latestPosition;
		ExpiringListPointer.expireTime = expireTimeLocal;
	}

	public boolean expired() {
		// ListPointers expire after 25 seconds
		final long seconds = ((new Date()).getTime() - this.lastUpdated.getTime()) / 1000;
		return seconds > (ExpiringListPointer.expireTime / 1000);
	}

	public void normalizeIndex(final int removed) {
		ExpiringListPointer.lastPosition = ExpiringListPointer.lastPosition - removed;
		if (ExpiringListPointer.lastPosition < 0) ExpiringListPointer.lastPosition = 0;
	}

	public void reuse(final int idx) {
		this.lastUpdated = new Date();
		ExpiringListPointer.lastPosition = idx;
	}
}

public final class Longpoll extends APIServlet.APIRequestHandler {
	static boolean allowLongpollRelaxed = Nxt.getBooleanProperty("nxt.longPollFromAnywhere");

	private static final int waitTimeValue = 5000;
	private static final int garbageTimeout = 10000;
	private static final int expireTime = 25000;
	static final Longpoll instance = new Longpoll();
	private static final HashMap<Integer, ExpiringListPointer> setListings = new HashMap<>();
	private static final ArrayList<String> eventQueue = new ArrayList<>();
	private static final ClearTask clearTask = new ClearTask(Longpoll.setListings, Longpoll.eventQueue);
	private static final Timer timer = new Timer();
	private static boolean timerInitialized = false;

	private Longpoll() {
		super(new APITag[] { APITag.AE }, "nil");
		TemporaryComputationBlockchainProcessorImpl.getInstance().blockListeners.addListener(block -> {
			final String event = "block " + block.getHeight();
			final ArrayList<String> list = new ArrayList<>();
			list.add(event);
			Longpoll.instance.addEvents(list);
		}, BlockchainProcessor.Event.BLOCK_SCANNED_COMPUTATION);

		TemporaryComputationBlockchainProcessorImpl.getInstance().blockListeners.addListener(block -> {
			final String event = "new block (" + block.getHeight() + ")";
			final ArrayList<String> list = new ArrayList<>();
			list.add(event);
			Longpoll.instance.addEvents(list);
		}, BlockchainProcessor.Event.BLOCK_PUSHED_COMPUTATION);


		TransactionProcessorImpl.getInstance().addListener(t -> {
			final String event = "broadcast transaction";
			final ArrayList<String> list = new ArrayList<>();
			list.add(event);
			Longpoll.instance.addEvents(list);
		}, TransactionProcessor.Event.ADDED_UNCONFIRMED_TRANSACTIONS_COMPUTATION);
	}

	private synchronized void addEvents(final List<String> l) {
		// System.out.println("Adding: " + x);
		Longpoll.eventQueue.addAll(l);

		synchronized (Longpoll.instance) {
			Longpoll.instance.notify();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected JSONStreamAware processRequest(final HttpServletRequest req) throws NxtException {

		final JSONObject response = new JSONObject();
		// Logger.logDebugMessage("Longpoll request from: " + req.getRemoteAddr() + " (" + req.getRemoteHost() + ")");
		if(!allowLongpollRelaxed && req.getRemoteAddr().equalsIgnoreCase("127.0.0.1")==false){
			// Do not allow long polling for remote addresses
			response.put("error", "permission denied, longpoll only from 127.0.0.1");
			return response;
		}

		final String randomIdStr = ParameterParser.getParameterMultipart(req, "randomId");
		int randomId;
		try {
			randomId = Integer.parseInt(randomIdStr);
		} catch (final NumberFormatException e) {
			response.put("error", "please provide a randomId (within the integer range)");
			return response;
		}

		ExpiringListPointer p;
		// System.out.println("Reusing Linstener: " + randomId);
		if (Longpoll.setListings.containsKey(randomId)) p = Longpoll.setListings.get(randomId);
		else synchronized (this) {
			p = new ExpiringListPointer(Longpoll.eventQueue.size(), Longpoll.expireTime);
			Longpoll.setListings.put(randomId, p);
		}

		// Schedule timer if not done yet
		// Schedule to run after every 3 second (3000 millisecond)
		if (!Longpoll.timerInitialized) try {
			Longpoll.timer.scheduleAtFixedRate(Longpoll.clearTask, 0, Longpoll.garbageTimeout);
			Longpoll.timerInitialized = true;
		} catch (final IllegalStateException e) {
			Longpoll.timerInitialized = true;
			// ITS ALREADY INITIALIZED)
		}

		synchronized (this) {
			try {
				if (ExpiringListPointer.lastPosition == Longpoll.eventQueue.size()) this.wait(Longpoll.waitTimeValue);

				final JSONArray arr;
				if (ExpiringListPointer.lastPosition >= Longpoll.eventQueue.size()) {
					// Timeout, nothing new, no notification
					response.put("event", "timeout");
					return response;
				}

				Stream<String> tmp = IntStream.range(ExpiringListPointer.lastPosition, Longpoll.eventQueue.size()).mapToObj(Longpoll.eventQueue::get);
				Collection<String> collate = tmp.collect(Collectors.toCollection(ArrayList::new));
				Iterator iterate = collate.iterator();
				JSONArray localarr = new JSONArray();

				while(iterate.hasNext()){
                    localarr.add(iterate.next());
                }
				p.reuse(Longpoll.eventQueue.size());

				response.put("event", localarr);
				return response;

			} catch (final InterruptedException e) {
				// Timeout, no notification
				response.put("event", "timeout");
				return response;
			}
		}
	}

}