package backend;

import backend.reg.VirtualReg;
import util.Pair;

import java.util.HashMap;
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

    public static void buildLiveInterval(CodeGenManager manager) {
        for (var func : manager.getMachineFunctions()) {
            HashMap<VirtualReg, Integer> defMap = new HashMap<>();
            HashMap<VirtualReg, Integer> lastUseMap = new HashMap<>();

            for (var blockEntry : func.getmbList()) {
                var block = blockEntry.getVal();

                for (var instrEntry: block.getmclist()) {
                    var instr = instrEntry.getVal();
                    var defSet = instr.getVirtualDef();
                    var useSet = instr.getVirtualUses();

                    defSet.stream().filter(defMap::containsKey).forEach(vr ->
                            vr.getLiveInterval().addInterval(defMap.get(vr), lastUseMap.get(vr))
                    );

                    defSet.forEach(vr -> defMap.put(vr, instr.getSlotIndex()));

                    useSet.forEach(vr -> lastUseMap.put(vr, instr.getSlotIndex()));
                }
            }

            lastUseMap.forEach((vr, lastUseIndex) -> vr.getLiveInterval().addInterval(defMap.get(vr), lastUseIndex));
        }
    }
}
