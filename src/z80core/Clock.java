/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package z80core;

import java.util.ConcurrentModificationException;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 * @author jsanchez
 */
public class Clock {
    private static final Clock instance = new Clock();
    private long tstates;
    private long frames;
    private long timeout;
    private final CopyOnWriteArrayList<ClockTimeoutListener> clockListeners;

    // Clock class implements a Singleton pattern.
    private Clock() {
        this.clockListeners = new CopyOnWriteArrayList<>();
    }

    public static Clock getInstance() {
        return instance;
    }
    /**
     * Adds a new event listener to the list of event listeners.
     *
     * @param listener The new event listener.
     *
     * @throws NullPointerException Thrown if the listener argument is null.
     */
    public void addClockTimeoutListener(final ClockTimeoutListener listener) {

        if (listener == null) {
            throw new NullPointerException("Error: Listener can't be null");
        }

        // Avoid duplicates
        if (!clockListeners.contains(listener)) {
            clockListeners.add(listener);
        }
    }

    /**
     * Remove a new event listener from the list of event listeners.
     *
     * @param listener The event listener to remove.
     *
     * @throws NullPointerException Thrown if the listener argument is null.
     * @throws IllegalArgumentException Thrown if the listener wasn't registered.
     */
    public void removeClockTimeoutListener(final ClockTimeoutListener listener) {

        if (listener == null) {
            throw new NullPointerException("Internal Error: Listener can't be null");
        }

        if (!clockListeners.remove(listener)) {
            throw new IllegalArgumentException("Internal Error: Listener was not listening on object");
        }
        
        // When don't have listeners, disable any pending timeout
        if (clockListeners.isEmpty()) {
            timeout = 0;
        }
    }

    /**
     * @return the tstates
     */
    public long getTstates() {
        return tstates;
    }

    /**
     * @param states the tstates to set
     */
    public void setTstates(long states) {
        tstates = states;
        frames = timeout = 0;
    }

    public void addTstates(long states) {
        tstates += states;

        if (timeout > 0) {
            timeout -= states;

            if (timeout <= 0) {
                long res = timeout;
                for (final ClockTimeoutListener listener : clockListeners) {
                   listener.clockTimeout();
                }
                
                if (timeout > 0) {
//                    System.out.println("Timeout: " + timeout + " res: " + res);
                    timeout += res;
                }
            }
        }
    }

    public void reset() {
        frames = timeout = tstates = 0;
    }

    public void setTimeout(long ntstates) {
        if (timeout > 0) {
            throw new ConcurrentModificationException("A timeout is in progress. Can't set another timeout!");
        }

        timeout = ntstates > 10 ? ntstates : 10;
    }

    @Override
    public String toString() {
        return String.format("Frame: %d, t-states: %d", frames, tstates);
    }
}
