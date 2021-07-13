package backend;

import backend.machinecodes.ArmAddition;
import backend.machinecodes.MCMove;
import backend.machinecodes.MachineBlock;
import backend.machinecodes.MachineFunction;
import backend.reg.MachineOperand;
import backend.reg.PhyReg;
import backend.reg.VirtualReg;
import util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// Graph-Coloring
public class RegAllocator {
    private final HashSet<PhyReg> allocatableMachineOperands = IntStream.range(1, 15).filter(i -> i != 13)
            .mapToObj(PhyReg::new).collect(Collectors.toCollection(HashSet::new));
    private final int INF = 0x3f3f3f3f;
    private final int K = 14;

    private class BlockLiveInfo {
        private final MachineBlock block;
        private HashSet<MachineOperand> liveUse = new HashSet<>();
        private HashSet<MachineOperand> liveDef = new HashSet<>();
        private HashSet<MachineOperand> liveIn = new HashSet<>();
        private HashSet<MachineOperand> liveOut = new HashSet<>();

        BlockLiveInfo(MachineBlock block) {
            this.block = block;
        }
    }

    private HashMap<MachineBlock, BlockLiveInfo> livenessAnalysis(MachineFunction func) {
        var liveInfoMap = new HashMap<MachineBlock, BlockLiveInfo>();
        for (var blockEntry : func.getmbList()) {
            var block = blockEntry.getVal();
            var blockLiveInfo = new BlockLiveInfo(block);
            liveInfoMap.put(block, blockLiveInfo);

            for (var instrEntry : block.getmclist()) {
                var instr = instrEntry.getVal();
                // fixme: not getVirtualDef
                instr.getVirtualDef().stream()
                        .filter(useMachineOperand -> useMachineOperand instanceof VirtualReg)
                        .filter(useMachineOperand -> !blockLiveInfo.liveDef.contains(useMachineOperand))
                        .forEach(blockLiveInfo.liveUse::add);
                instr.getVirtualUses().stream()
                        .filter(defMachineOperand -> defMachineOperand instanceof VirtualReg)
                        .filter(defMachineOperand -> !blockLiveInfo.liveUse.contains(defMachineOperand))
                        .forEach(blockLiveInfo.liveDef::add);
            }

            blockLiveInfo.liveIn.addAll(blockLiveInfo.liveUse);
        }

        boolean changed = true;
        while (changed) {
            changed = false;
            for (var blockEntry : func.getmbList()) {
                var block = blockEntry.getVal();
                var blockLiveInfo = liveInfoMap.get(block);
                var newLiveOut = new HashSet<MachineOperand>();

                if (block.getTrueSucc() != null) {
                    var succBlockInfo = liveInfoMap.get(block.getTrueSucc());
                    newLiveOut.addAll(succBlockInfo.liveIn);
                }

                if (block.getFalseSucc() != null) {
                    var succBlockInfo = liveInfoMap.get(block.getFalseSucc());
                    newLiveOut.addAll(succBlockInfo.liveIn);
                }

                if (!newLiveOut.equals(blockLiveInfo.liveOut)) {
                    changed = true;
                    blockLiveInfo.liveOut = newLiveOut;

                    blockLiveInfo.liveIn = new HashSet<>(blockLiveInfo.liveUse);
                    blockLiveInfo.liveOut.stream()
                            .filter(MachineOperand -> !blockLiveInfo.liveDef.contains(MachineOperand))
                            .forEach(blockLiveInfo.liveIn::add);
                }
            }
        }

        return liveInfoMap;
    }

    public void MachineOperandAlloc(CodeGenManager manager) {
        for (var func : manager.getMachineFunctions()) {
            boolean done = false;
            while(!done) {
                var liveInfoMap = livenessAnalysis(func);

                var adjList = new HashMap<MachineOperand, HashSet<MachineOperand>>();
                var adjSet = new HashSet<Pair<MachineOperand, MachineOperand>>();
                var alias = new HashMap<MachineOperand, MachineOperand>();
                var moveList = new HashMap<MachineOperand, HashSet<MCMove>>();
                var simplifyWorklist = new HashSet<MachineOperand>();
                var freezeWorklist = new HashSet<MachineOperand>();
                var spillWorklist = new HashSet<MachineOperand>();
                var spilledNodes = new HashSet<MachineOperand>();
                var coalescedNodes = new HashSet<MachineOperand>();
                var coloredNodes = new ArrayList<MachineOperand>();
                var selectStack = new Stack<MachineOperand>();
                var coalescedMoves = new HashSet<MCMove>();
                var constrainedMoves = new HashSet<MCMove>();
                var frozenMoves = new HashSet<MCMove>();
                var worklistMoves = new HashSet<MCMove>();
                var activeMoves = new HashSet<MCMove>();

                Map<MachineOperand, Integer> degree = allocatableMachineOperands.stream().collect(Collectors.toMap(MachineOperand -> MachineOperand, MachineOperand -> INF));

                BiConsumer<MachineOperand, MachineOperand> addEdge = (u, v) -> {
                    if (adjSet.contains(new Pair<>(u, v)) && u != v) {
                        adjSet.add(new Pair<>(u, v));
                        adjSet.add(new Pair<>(v, u));
                        if (u instanceof VirtualReg) {
                            adjList.putIfAbsent(u, new HashSet<>());
                            adjList.get(u).add(v);
                            degree.putIfAbsent(u, 0);
                            degree.compute(u, (key, value) -> value + 1);
                        }
                        if (v instanceof VirtualReg) {
                            adjList.putIfAbsent(v, new HashSet<>());
                            adjList.get(v).add(u);
                            degree.putIfAbsent(v, 0);
                            degree.compute(v, (key, value) -> value + 1);
                        }
                    }
                };

                Runnable build = () -> {
                    for (var blockEntry : func.getmbList()) {
                        var block = blockEntry.getVal();
                        var liveInfo = liveInfoMap.get(block);
                        var live = liveInfo.liveOut;

                        for (var instrEntry : block.getmclist()) {
                            var instr = instrEntry.getVal();
                            // fixme
                            var defs = instr.getPhyDef();
                            var uses = instr.getPhyUses();

                            if (instr instanceof MCMove mcInstr) {
                                var dst = mcInstr.getDst();
                                var rhs = mcInstr.getRhs();
                                if (dst instanceof VirtualReg && rhs instanceof VirtualReg &&
                                        mcInstr.getCond() == ArmAddition.CondType.Any &&
                                        mcInstr.getShift().getType() == ArmAddition.ShiftType.None) {
                                    live.remove(mcInstr.getRhs());
                                    moveList.putIfAbsent(rhs, new HashSet<>());
                                    moveList.get(rhs).add(mcInstr);
                                    moveList.putIfAbsent(dst, new HashSet<>());
                                    moveList.get(rhs).add(mcInstr);
                                    worklistMoves.add(mcInstr);
                                }

                                defs.stream().filter(d -> d instanceof PhyReg).forEach(live::add);
                                defs.stream().filter(d -> d instanceof PhyReg).forEach(d -> {
                                    live.forEach(l -> addEdge.accept(l, d));
                                });
                                // fixme: add loop cnt
                            }
                        }
                    }
                };

                Function<MachineOperand, Set<MachineOperand>> getAdjacent = n -> adjList.getOrDefault(n, new HashSet<>()).stream()
                        .filter(MachineOperand -> !(selectStack.contains(MachineOperand) || coalescedNodes.contains(MachineOperand)))
                        .collect(Collectors.toSet());

                Function<MachineOperand, Set<MCMove>> nodeMoves = n -> moveList.getOrDefault(n, new HashSet<>()).stream()
                        .filter(move -> activeMoves.contains(move) || worklistMoves.contains(move))
                        .collect(Collectors.toSet());

                Function<MachineOperand, Boolean> moveRelated = n -> !nodeMoves.apply(n).isEmpty();

                Runnable makeWorklist = () -> {
                    // fixme
                };

                Consumer<MachineOperand> enableMoves = n -> {
                    nodeMoves.apply(n).stream()
                            .filter(activeMoves::contains)
                            .forEach(m -> {
                                activeMoves.remove(m);
                                worklistMoves.add(m);
                            });

                    getAdjacent.apply(n).forEach(a -> {
                        nodeMoves.apply(a).stream()
                                .filter(activeMoves::contains)
                                .forEach(m -> {
                                    activeMoves.remove(m);
                                    worklistMoves.add(m);
                                });
                    });
                };

                Consumer<MachineOperand> decrementDegree = m -> {
                    assert degree.containsKey(m);
                    var d = degree.get(m);
                    degree.put(m, d - 1);
                    if (d == K) {
                        enableMoves.accept(m);
                        spillWorklist.add(m);
                        if (moveRelated.apply(m)) {
                            freezeWorklist.add(m);
                        } else {
                            simplifyWorklist.add(m);
                        }
                    }
                };

                Runnable simplify = () -> {
                    var n = simplifyWorklist.iterator().next();
                    simplifyWorklist.remove(n);
                    selectStack.add(n);
                    getAdjacent.apply(n).forEach(decrementDegree);
                };

                Function<MachineOperand, MachineOperand> getAlias = n -> coalescedNodes.contains(n) ? alias.get(n) : n;

                Consumer<MachineOperand> addWorklist = u -> {
                    if (u instanceof VirtualReg && !moveRelated.apply(u) && degree.get(u) < K) {
                        freezeWorklist.remove(u);
                        simplifyWorklist.add(u);
                    }
                };

                BiPredicate<MachineOperand, MachineOperand> ok = (t, r) ->
                        degree.get(t) < K || t instanceof PhyReg || adjSet.contains(new Pair<>(t, r));

                BiPredicate<MachineOperand, MachineOperand> adjOk = (v, u) -> getAdjacent.apply(v).stream().allMatch(t -> ok.test(t, u));

                BiConsumer<MachineOperand, MachineOperand> combine = (u, v) -> {
                    if (freezeWorklist.contains(v)) {
                        freezeWorklist.remove(v);
                    } else {
                        spillWorklist.remove(v);
                    }

                    coalescedNodes.add(v);
                    alias.put(v, u);
                    moveList.get(u).addAll(moveList.get(v));
                    enableMoves.accept(v);
                    getAdjacent.apply(v).forEach(t -> {
                        addEdge.accept(t, u);
                        decrementDegree.accept(t);
                    });

                    if (degree.get(u) >= K && freezeWorklist.contains(u)) {
                        freezeWorklist.remove(u);
                        spillWorklist.add(u);
                    }
                };

                BiPredicate<Set<MachineOperand>, Set<MachineOperand>> conservative = (u, v) -> {
                    u.addAll(v);
                    var cnt = u.stream().filter(n -> degree.get(n) >= K).count();
                    return cnt < K;
                };

                Runnable coalesce = () -> {
                    var m = worklistMoves.iterator().next();
                    var u = getAlias.apply(m.getDst());
                    var v = getAlias.apply(m.getRhs());
                    if (v instanceof PhyReg) {
                        var temp = u;
                        u = v;
                        v = temp;
                    }
                    worklistMoves.remove(m);
                    if (u == v) {
                        coalescedMoves.add(m);
                        addWorklist.accept(u);
                    } else if (v instanceof PhyReg || adjSet.contains(new Pair<>(u, v))) {
                        constrainedMoves.add(m);
                        addWorklist.accept(u);
                        addWorklist.accept(v);
                    } else if ((u instanceof PhyReg && adjOk.test(v, u)) ||
                            (!(u instanceof PhyReg) && conservative.test(getAdjacent.apply(u), getAdjacent.apply(v)))) {
                        coalescedMoves.add(m);
                        combine.accept(u, v);
                        addWorklist.accept(u);
                    } else {
                        activeMoves.add(m);
                    }
                };

                Consumer<MachineOperand> freeMoves = u -> nodeMoves.apply(u).forEach(m -> {
                    if (activeMoves.contains(m)) {
                        activeMoves.remove(m);
                    } else {
                        worklistMoves.remove(m);
                    }
                    frozenMoves.add(m);

                    var v = m.getDst() == u ? m.getRhs() : m.getDst();
                    if (!moveRelated.apply(v) && degree.get(v) < K) {
                        freezeWorklist.remove(v);
                        simplifyWorklist.add(v);
                    }
                });

                Runnable freeze = () -> {
                    var u = freezeWorklist.iterator().next();
                    freezeWorklist.remove(u);
                    simplifyWorklist.add(u);
                    freeMoves.accept(u);
                };

                Runnable select_spill = () -> {
                    // fixme
                };

                Runnable assignColors = () -> {
                    // fixme
                    var colored = new HashMap<MachineOperand, MachineOperand>();
                    while (!selectStack.isEmpty()) {
                        var n = selectStack.pop();

                    }
                };

                build.run();
                makeWorklist.run();
                do {
                    if (!simplifyWorklist.isEmpty()) {
                        simplify.run();
                    }
                    if (!worklistMoves.isEmpty()) {
                        coalesce.run();
                    }
                    if (!freezeWorklist.isEmpty()) {
                        freeze.run();
                    }
                    if (!spillWorklist.isEmpty()) {
                        select_spill.run();
                    }
                } while (!simplifyWorklist.isEmpty() || !worklistMoves.isEmpty() ||
                        !freezeWorklist.isEmpty() || !spillWorklist.isEmpty());

                assignColors.run();

                if (spilledNodes.isEmpty()) {
                    done = true;
                } else {
                    for (var n : spilledNodes) {
                        for (var blockEntry : func.getmbList()) {
                            var block = blockEntry.getVal();
                            // fixme
                        }
                    }
                }
            }
        }
    }
}
