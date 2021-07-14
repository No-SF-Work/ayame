package backend;

import backend.machinecodes.*;
import backend.reg.*;
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
    private final int INF = 0x3f3f3f3f;
    private final int K = 14;

    private static class BlockLiveInfo {
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
                instr.getDef().stream()
                        .filter(useMachineOperand -> useMachineOperand instanceof VirtualReg)
                        .filter(useMachineOperand -> !blockLiveInfo.liveDef.contains(useMachineOperand))
                        .forEach(blockLiveInfo.liveUse::add);
                instr.getUse().stream()
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

    private void replaceReg(MachineCode instr, MachineOperand origin, MachineOperand target) {
        if (instr instanceof MCBinary binaryInstr) {
            if (binaryInstr.getDst().equals(origin)) {
                binaryInstr.setDst(target);
            } else if (binaryInstr.getLhs().equals(origin)) {
                binaryInstr.setLhs(target);
            } else if (binaryInstr.getRhs().equals(origin)) {
                binaryInstr.setRhs(target);
            }
        } else if (instr instanceof MCBranch ) {
            return;
        } else if (instr instanceof MCCall callInstr) {
            return;
        } else if (instr instanceof MCComment commentInstr) {
            return;
        } else if (instr instanceof MCCompare compareInstr) {
            return;
        } else if (instr instanceof MCFma fmaInstr) {
            if (fmaInstr.getDst().equals(origin)) {
                fmaInstr.setDst(target);
            } else if (fmaInstr.getLhs().equals(origin)) {
                fmaInstr.setLhs(target);
            } else if (fmaInstr.getRhs().equals(origin)) {
                fmaInstr.setRhs(target);
            } else if (fmaInstr.getAcc().equals(origin)) {
                fmaInstr.setAcc(target);
            }
        } else if (instr instanceof MCGlobal globalInstr) {
            if (globalInstr.getDst().equals(origin)) {
                globalInstr.setDst(target);
            }
        } else if (instr instanceof MCJump jumpInstr) {
            return;
        } else if (instr instanceof MCLoad loadInstr) {
            if (loadInstr.getAddr().equals(origin)) {
                loadInstr.setAddr(target);
            } else if (loadInstr.getOffset().equals(origin)) {
                loadInstr.setOffset(target);
            } else if (loadInstr.getDst().equals(origin)) {
                loadInstr.setDst(target);
            }
        } else if (instr instanceof MCLongMul longMulInstr) {
            if (longMulInstr.getDst().equals(origin)) {
                longMulInstr.setDst(target);
            } else if (longMulInstr.getLhs().equals(origin)) {
                longMulInstr.setLhs(target);
            } else if (longMulInstr.getRhs().equals(origin)) {
                longMulInstr.setRhs(target);
            }
        } else if (instr instanceof MCMove moveInstr) {
            if (moveInstr.getDst().equals(origin)) {
                moveInstr.setDst(target);
            }
            if (moveInstr.getRhs().equals(origin)) {
                moveInstr.setRhs(target);
            }
        } else if (instr instanceof MCStore storeInstr) {
            if (storeInstr.getAddr().equals(origin)) {
                storeInstr.setAddr(target);
            } else if (storeInstr.getOffset().equals(origin)) {
                storeInstr.setOffset(target);
            } else if (storeInstr.getData().equals(origin)) {
                storeInstr.setData(target);
            }
        }
    }

    public void RegisterAllocation(CodeGenManager manager) {
        for (var func : manager.getMachineFunctions()) {
            var allocatable = IntStream.range(0, 14).filter(i -> i != 13)
                    .mapToObj(func::getPhyReg).collect(Collectors.toCollection(HashSet::new));

            var done = false;

            while (!done) {
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
                var worklistMoves = new HashSet<MCMove>();
                var activeMoves = new HashSet<MCMove>();
                // maybe removed
                var allocated = new HashSet<MachineOperand>();
                var coalescedMoves = new HashSet<MCMove>();
                var constrainedMoves = new HashSet<MCMove>();
                var frozenMoves = new HashSet<MCMove>();

                Function<MachineOperand, Boolean> needsColor = n -> n.getState() != MachineOperand.state.imm && !allocated.contains(n);

                Map<MachineOperand, Integer> degree = allocatable.stream()
                        .collect(Collectors.toMap(MachineOperand -> MachineOperand, MachineOperand -> INF));

                BiConsumer<MachineOperand, MachineOperand> addEdge = (u, v) -> {
                    if (!adjSet.contains(new Pair<>(u, v)) && !u.equals(v)) {
                        adjSet.add(new Pair<>(u, v));
                        adjSet.add(new Pair<>(v, u));
                        if (!u.isPrecolored()) {
                            adjList.putIfAbsent(u, new HashSet<>());
                            adjList.get(u).add(v);
                            degree.putIfAbsent(u, 0);
                            degree.compute(u, (key, value) -> value + 1);
                        }
                        if (!v.isPrecolored()) {
                            adjList.putIfAbsent(v, new HashSet<>());
                            adjList.get(v).add(u);
                            degree.putIfAbsent(v, 0);
                            degree.compute(v, (key, value) -> value + 1);
                        }
                    }
                };

                Runnable build = () -> {
                    for (var blockEntry = func.getmbList().getLast();
                         blockEntry != null;
                         blockEntry = blockEntry.getPrev()) {
                        var block = blockEntry.getVal();
                        var liveInfo = liveInfoMap.get(block);
                        var live = liveInfo.liveOut;

                        for (var instrEntry = block.getmclist().getLast();
                             instrEntry != null;
                             instrEntry = instrEntry.getPrev()) {
                            var instr = instrEntry.getVal();
                            var defs = instr.getDef();
                            var uses = instr.getUse();

                            if (instr instanceof MCMove mcInstr) {
                                var dst = mcInstr.getDst();
                                var rhs = mcInstr.getRhs();
                                if (needsColor.apply(dst) && needsColor.apply(rhs) &&
                                        mcInstr.getCond() == ArmAddition.CondType.Any &&
                                        mcInstr.getShift().getType() == ArmAddition.ShiftType.None) {
                                    live.remove(mcInstr.getRhs());

                                    moveList.putIfAbsent(rhs, new HashSet<>());
                                    moveList.get(rhs).add(mcInstr);

                                    moveList.putIfAbsent(dst, new HashSet<>());
                                    moveList.get(dst).add(mcInstr);

                                    worklistMoves.add(mcInstr);
                                }

                                defs.stream().filter(needsColor::apply).forEach(live::add);
                                defs.stream().filter(needsColor::apply).forEach(d -> live.forEach(l -> addEdge.accept(l, d)));
                                // todo: [heuristic] add loop cnt
                            }
                        }
                    }
                };

                Function<MachineOperand, Set<MachineOperand>> getAdjacent = n -> adjList.getOrDefault(n, new HashSet<>()).stream()
                        .filter(operand -> !(selectStack.contains(operand) || coalescedNodes.contains(operand)))
                        .collect(Collectors.toSet());

                Function<MachineOperand, Set<MCMove>> nodeMoves = n -> moveList.getOrDefault(n, new HashSet<>()).stream()
                        .filter(move -> activeMoves.contains(move) || worklistMoves.contains(move))
                        .collect(Collectors.toSet());

                Function<MachineOperand, Boolean> moveRelated = n -> !nodeMoves.apply(n).isEmpty();

                Runnable makeWorklist = () -> func.getVRegMap().values().forEach(vreg -> {
                    if (degree.get(vreg) >= K) {
                        spillWorklist.add(vreg);
                    } else if (moveRelated.apply(vreg)) {
                        freezeWorklist.add(vreg);
                    } else {
                        simplifyWorklist.add(vreg);
                    }
                });

                Consumer<MachineOperand> enableMoves = n -> {
                    nodeMoves.apply(n).stream()
                            .filter(activeMoves::contains)
                            .forEach(m -> {
                                activeMoves.remove(m);
                                worklistMoves.add(m);
                            });

                    getAdjacent.apply(n).forEach(a -> nodeMoves.apply(a).stream()
                            .filter(activeMoves::contains)
                            .forEach(m -> {
                                activeMoves.remove(m);
                                worklistMoves.add(m);
                            }));
                };

                Consumer<MachineOperand> decrementDegree = m -> {
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
                    selectStack.push(n);
                    getAdjacent.apply(n).forEach(decrementDegree);
                };

                Function<MachineOperand, MachineOperand> getAlias = n -> {
                    while (coalescedNodes.contains(n)) {
                        n = alias.get(n);
                    }
                    return n;
                };

                Consumer<MachineOperand> addWorklist = u -> {
                    if (!u.isPrecolored() && !moveRelated.apply(u) && degree.get(u) < K) {
                        freezeWorklist.remove(u);
                        simplifyWorklist.add(u);
                    }
                };

                BiPredicate<MachineOperand, MachineOperand> ok = (t, r) ->
                        degree.get(t) < K || t.isPrecolored() || adjSet.contains(new Pair<>(t, r));

                BiPredicate<MachineOperand, MachineOperand> adjOk = (v, u) ->
                        getAdjacent.apply(v).stream().allMatch(t -> ok.test(t, u));

                BiConsumer<MachineOperand, MachineOperand> combine = (u, v) -> {
                    if (freezeWorklist.contains(v)) {
                        freezeWorklist.remove(v);
                    } else {
                        spillWorklist.remove(v);
                    }

                    coalescedNodes.add(v);
                    alias.put(v, u);
                    moveList.get(u).addAll(moveList.get(v));
                    // enableMoves.accept(v);
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
                    if (v.isPrecolored()) {
                        var temp = u;
                        u = v;
                        v = temp;
                    }
                    worklistMoves.remove(m);
                    if (u.equals(v)) {
                        coalescedMoves.add(m);
                        addWorklist.accept(u);
                    } else if (v.isPrecolored() || adjSet.contains(new Pair<>(u, v))) {
                        constrainedMoves.add(m);
                        addWorklist.accept(u);
                        addWorklist.accept(v);
                    } else if ((u.isPrecolored() && adjOk.test(v, u)) ||
                            (!u.isPrecolored() && conservative.test(getAdjacent.apply(u), getAdjacent.apply(v)))) {
                        coalescedMoves.add(m);
                        combine.accept(u, v);
                        addWorklist.accept(u);
                    } else {
                        activeMoves.add(m);
                    }
                };

                Consumer<MachineOperand> freeMoves = u ->
                        nodeMoves.apply(u).forEach(m -> {
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

                Runnable selectSpill = () -> {
                    // todo: heuristic
                    var m = spillWorklist.iterator().next();
                    simplifyWorklist.add(m);
                    freeMoves.accept(m);
                    spillWorklist.remove(m);
                };

                Runnable assignColors = () -> {
                    var colored = new HashMap<MachineOperand, MachineOperand>();
                    while (!selectStack.isEmpty()) {
                        var n = selectStack.pop();
                        var okColors = new HashSet<>(allocatable);

                        adjList.get(n).forEach(w -> {
                            var a = getAlias.apply(w);
                            if (allocated.contains(a) || a.isPrecolored()) {
                                assert a instanceof PhyReg;
                                okColors.remove(a);
                            } else if (a instanceof VirtualReg) {
                                colored.remove(a);
                            }
                        });

                        if (okColors.isEmpty()) {
                            spilledNodes.add(n);
                        } else {
                            var color = okColors.iterator().next();
                            colored.put(n, color);
                        }
                    }

                    if (!spilledNodes.isEmpty()) {
                        return;
                    }

                    coalescedNodes.forEach(n -> {
                        var a = getAlias.apply(n);
                        if (a.isPrecolored()) {
                            colored.put(n, a);
                        } else {
                            colored.put(n, colored.get(a));
                        }
                    });

                    for (var blockEntry : func.getmbList()) {
                        var block = blockEntry.getVal();

                        for (var instrEntry : block.getmclist()) {
                            var instr = instrEntry.getVal();
                            instr.getDef().stream().filter(colored::containsKey)
                                    .forEach(origin -> replaceReg(instr, origin, colored.get(origin)));
                            instr.getUse().stream().filter(colored::containsKey)
                                    .forEach(origin -> replaceReg(instr, origin, colored.get(origin)));
                        }
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
                        selectSpill.run();
                    }
                } while (!(simplifyWorklist.isEmpty() && worklistMoves.isEmpty() &&
                        freezeWorklist.isEmpty() && spillWorklist.isEmpty()));

                assignColors.run();

                if (spilledNodes.isEmpty()) {
                    done = true;
                } else {
                    for (var n : spilledNodes) {
                        for (var blockEntry : func.getmbList()) {
                            var block = blockEntry.getVal();
                            var offset = func.getStackSize();
                            var offsetOperand = new MachineOperand(offset);

                            Consumer<MachineCode> fixOffset = inst -> {
                                if (offset < (1 << 12)) {
                                    if (inst instanceof MCLoad loadInstr) {
                                        loadInstr.setOffset(offsetOperand);
                                    } else if (inst instanceof MCStore storeInstr) {
                                        storeInstr.setOffset(offsetOperand);
                                    }
                                } else {
                                    var moveInstr = new MCMove();
                                    moveInstr.insertBeforeNode(inst);
                                    moveInstr.setRhs(offsetOperand);

                                    var newVReg = new VirtualReg();
                                    moveInstr.setDst(newVReg);

                                    if (inst instanceof MCLoad loadInstr) {
                                        loadInstr.setOffset(newVReg);
                                    } else if (inst instanceof MCStore storeInstr) {
                                        storeInstr.setOffset(newVReg);
                                    }
                                }
                            };

                            var ref = new Object() {
                                VirtualReg vreg = null;
                                MachineCode firstUse = null;
                                MachineCode lastDef = null;
                            };

                            Runnable checkPoint = () -> {
                                if (ref.firstUse != null) {
                                    var loadInstr = new MCLoad();
                                    loadInstr.insertBeforeNode(ref.firstUse);

                                    loadInstr.setAddr(func.getPhyReg("sp"));
                                    loadInstr.setDst(ref.vreg);
                                    loadInstr.setShift(ArmAddition.ShiftType.None, 0);
                                    fixOffset.accept(loadInstr);
                                    ref.firstUse = null;
                                }

                                if (ref.lastDef != null) {
                                    var storeInstr = new MCStore();
                                    storeInstr.insertAfterNode(ref.lastDef);

                                    storeInstr.setAddr(func.getPhyReg("sp"));
                                    storeInstr.setShift(ArmAddition.ShiftType.None, 0);
                                    fixOffset.accept(storeInstr);
                                    storeInstr.setData(ref.vreg);
                                    ref.lastDef = null;
                                }
                            };

                            int cntInstr = 0;
                            for (var instrEntry : block.getmclist()) {
                                var instr = instrEntry.getVal();

                                instr.getDef().stream().filter(def -> def.equals(n)).forEach(def -> {
                                    if (ref.vreg == null) {
                                        ref.vreg = new VirtualReg();
                                        func.addVirtualReg(ref.vreg);
                                    }

                                    replaceReg(instr, def, ref.vreg);
                                    ref.lastDef = instr;
                                });

                                instr.getUse().stream().filter(use -> use.equals(n)).forEach(use -> {
                                    if (ref.vreg == null) {
                                        ref.vreg = new VirtualReg();
                                        func.addVirtualReg(ref.vreg);
                                    }

                                    replaceReg(instr, use, ref.vreg);
                                    ref.firstUse = instr;
                                });

                                if (cntInstr > 30) {
                                    checkPoint.run();
                                }

                                ++cntInstr;
                            }

                            checkPoint.run();
                        }
                        func.addStackSize(4);
                    }
                    done = false;
                }
            }
        }
    }
}
