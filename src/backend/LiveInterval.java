package backend;

import util.Pair;

import java.util.LinkedList;

public class LiveInterval {
    private final LinkedList<Pair<Integer, Integer>> intervals;

    public LiveInterval() {
        this.intervals = new LinkedList<>();
    }

    public void addInterval(int start, int end) {
        intervals.add(new Pair<>(start, end));
    }

    public LinkedList<Pair<Integer, Integer>> getIntervals() {
        return intervals;
    }
}
