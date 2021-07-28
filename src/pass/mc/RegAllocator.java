package pass.mc;

import backend.CodeGenManager;
import backend.machinecodes.*;
import backend.reg.*;
import util.Pair;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import pass.Pass.MCPass;

// Graph-Coloring
public class RegAllocator implements MCPass {
    private final int INF = 0x3f3f3f3f;
    private final int K = 14;

    @Override
    public String getName() {
        return "RegAlloc";
    }

    private static class BlockLiveInfo {
        private final HashSet<MachineOperand> liveUse = new HashSet<>();
        private final HashSet<MachineOperand> liveDef = new HashSet<>();
        private HashSet<MachineOperand> liveIn = new HashSet<>();
        private HashSet<MachineOperand> liveOut = new HashSet<>();

        BlockLiveInfo(MachineBlock block) {
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
                instr.getUse().stream()
                        .filter(MachineOperand::needsColor)
                        .filter(use -> !blockLiveInfo.liveDef.contains(use))
                        .forEach(blockLiveInfo.liveUse::add);
                instr.getDef().stream()
                        .filter(MachineOperand::needsColor)
                        .filter(def -> !blockLiveInfo.liveUse.contains(def))
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
        if (instr instanceof MCBinary) {
            var binaryInstr = (MCBinary) instr;
            if (binaryInstr.getDst().equals(origin)) {
                binaryInstr.setDst(target);
            }
            if (binaryInstr.getLhs().equals(origin)) {
                binaryInstr.setLhs(target);
            }
            if (binaryInstr.getRhs().equals(origin)) {
                binaryInstr.setRhs(target);
            }
        } else if (instr instanceof MCCompare) {
            var compareInstr = (MCCompare) instr;
            if (compareInstr.getLhs().equals(origin)) {
                compareInstr.setLhs(target);
            }
            if (compareInstr.getRhs().equals(origin)) {
                compareInstr.setRhs(target);
            }
        } else if (instr instanceof MCFma) {
            var fmaInstr = (MCFma) instr;
            if (fmaInstr.getDst().equals(origin)) {
                fmaInstr.setDst(target);
            }
            if (fmaInstr.getLhs().equals(origin)) {
                fmaInstr.setLhs(target);
            }
            if (fmaInstr.getRhs().equals(origin)) {
                fmaInstr.setRhs(target);
            }
            if (fmaInstr.getAcc().equals(origin)) {
                fmaInstr.setAcc(target);
            }
        } else if (instr instanceof MCLoad) {
            var loadInstr = (MCLoad) instr;
            if (loadInstr.getAddr().equals(origin)) {
                loadInstr.setAddr(target);
            }
            if (loadInstr.getOffset().equals(origin)) {
                loadInstr.setOffset(target);
            }
            if (loadInstr.getDst().equals(origin)) {
                loadInstr.setDst(target);
            }
        } else if (instr instanceof MCLongMul) {
            var longMulInstr = (MCLongMul) instr;
            if (longMulInstr.getDst().equals(origin)) {
                longMulInstr.setDst(target);
            }
            if (longMulInstr.getLhs().equals(origin)) {
                longMulInstr.setLhs(target);
            }
            if (longMulInstr.getRhs().equals(origin)) {
                longMulInstr.setRhs(target);
            }
        } else if (instr instanceof MCMove) {
            var moveInstr = (MCMove) instr;
            if (moveInstr.getDst().equals(origin)) {
                moveInstr.setDst(target);
            }
            if (moveInstr.getRhs().equals(origin)) {
                moveInstr.setRhs(target);
            }
        } else if (instr instanceof MCStore) {
            var storeInstr = (MCStore) instr;
            if (storeInstr.getAddr().equals(origin)) {
                storeInstr.setAddr(target);
            }
            if (storeInstr.getOffset().equals(origin)) {
                storeInstr.setOffset(target);
            }
            if (storeInstr.getData().equals(origin)) {
                storeInstr.setData(target);
            }
        }
    }

    public void run(CodeGenManager manager) {
        for (var func : manager.getMachineFunctions()) {
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
                var selectStack = new Stack<MachineOperand>();
                var worklistMoves = new HashSet<MCMove>();
                var activeMoves = new HashSet<MCMove>();
                var loopDepth = new HashMap<MachineOperand, Integer>();
                // maybe removed
                var coalescedMoves = new HashSet<MCMove>();
                var constrainedMoves = new HashSet<MCMove>();
                var frozenMoves = new HashSet<MCMove>();

                Map<MachineOperand, Integer> degree = IntStream.range(0, 17)
                        .mapToObj(func::getPhyReg)
                        .collect(Collectors.toMap(MachineOperand -> MachineOperand, MachineOperand -> INF));

                BiConsumer<MachineOperand, MachineOperand> addEdge = (u, v) -> {
                    if (!adjSet.contains(new Pair<>(u, v)) && !u.equals(v)) {
                        adjSet.add(new Pair<>(u, v));
                        adjSet.add(new Pair<>(v, u));
                        if (!u.isPrecolored()) {
                            adjList.putIfAbsent(u, new HashSet<>());
                            adjList.get(u).add(v);
                            // degree.putIfAbsent(u, 0);
                            degree.compute(u, (key, value) -> value == null ? 0 : value + 1);
                        }
                        if (!v.isPrecolored()) {
                            adjList.putIfAbsent(v, new HashSet<>());
                            adjList.get(v).add(u);
                            // degree.putIfAbsent(v, 0);
                            degree.compute(v, (key, value) -> value == null ? 0 : value + 1);
                        }
                    }
                };

                Runnable build = () -> {
                    for (var blockEntry = func.getmbList().getLast();
                         blockEntry != null;
                         blockEntry = blockEntry.getPrev()) {
                        var block = blockEntry.getVal();
                        var live = new HashSet<>(liveInfoMap.get(block).liveOut);

                        for (var instrEntry = block.getmclist().getLast();
                             instrEntry != null;
                             instrEntry = instrEntry.getPrev()) {
                            var instr = instrEntry.getVal();
                            var defs = instr.getDef();
                            var uses = instr.getUse();

                            if (instr instanceof MCMove) {
                                var mcInstr = (MCMove) instr;
                                var dst = mcInstr.getDst();
                                var rhs = mcInstr.getRhs();
                                if (dst.needsColor() && rhs.needsColor() &&
                                        mcInstr.getCond() == ArmAddition.CondType.Any &&
                                        mcInstr.getShift().getType() == ArmAddition.ShiftType.None) {
                                    live.remove(mcInstr.getRhs());

                                    moveList.putIfAbsent(rhs, new HashSet<>());
                                    moveList.get(rhs).add(mcInstr);

                                    moveList.putIfAbsent(dst, new HashSet<>());
                                    moveList.get(dst).add(mcInstr);

                                    worklistMoves.add(mcInstr);
                                }
                            }
                            defs.stream().filter(MachineOperand::needsColor).forEach(live::add);
                            defs.stream().filter(MachineOperand::needsColor).forEach(d -> live.forEach(l -> addEdge.accept(l, d)));

                            // heuristic
                            defs.stream().filter(MachineOperand::needsColor).forEach(d -> {
                                // loopDepth.putIfAbsent(d, 0);
                                loopDepth.compute(d, (key, value) -> value == null ? 0 : value + block.getLoopDepth());
                            });

                            uses.stream().filter(MachineOperand::needsColor).forEach(u -> {
                                // loopDepth.putIfAbsent(u, 0);
                                loopDepth.compute(u, (key, value) -> value == null ? 0 : value + block.getLoopDepth());
                            });

                            defs.stream().filter(MachineOperand::needsColor).forEach(live::remove);

                            uses.stream().filter(MachineOperand::needsColor).forEach(live::add);
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

                Runnable makeWorklist = () ->
                        func.getVRegMap().values()
                                .stream().filter(vreg -> !vreg.isGlobal())
                                .forEach(vreg -> {
                                    if (degree.getOrDefault(vreg, 0) >= K) {
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
                    if (!u.isPrecolored() && !moveRelated.apply(u) && degree.getOrDefault(u, 0) < K) {
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

                    if (degree.getOrDefault(u, 0) >= K && freezeWorklist.contains(u)) {
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

                Consumer<MachineOperand> freezeMoves = u ->
                {
                    for (MCMove m : nodeMoves.apply(u)) {
                        if (activeMoves.contains(m)) {
                            activeMoves.remove(m);
                        } else {
                            worklistMoves.remove(m);
                        }
                        frozenMoves.add(m);

                        var v = m.getDst().equals(u) ? m.getRhs() : m.getDst();
                        if (!moveRelated.apply(v) && degree.getOrDefault(v, 0) < K) {
                            freezeWorklist.remove(v);
                            simplifyWorklist.add(v);
                        }
                    }
                };

                Runnable freeze = () -> {
                    var u = freezeWorklist.iterator().next();
                    freezeWorklist.remove(u);
                    simplifyWorklist.add(u);
                    freezeMoves.accept(u);
                };

                Runnable selectSpill = () -> {
                    // heuristic
                    // var m = spillWorklist.iterator().next();
                    var m = spillWorklist.stream().max((l, r) -> {
                        var value1 = degree.getOrDefault(l, 0).doubleValue() / Math.pow(1.4, loopDepth.getOrDefault(l, 0));
                        var value2 = degree.getOrDefault(r, 0).doubleValue() / Math.pow(1.4, loopDepth.getOrDefault(r, 0));

                        return Double.compare(value1, value2);
                    }).get();
                    simplifyWorklist.add(m);
                    freezeMoves.accept(m);
                    spillWorklist.remove(m);
                };

                Runnable assignColors = () -> {
                    var colored = new HashMap<MachineOperand, MachineOperand>();
                    selectStack.removeIf(n -> n instanceof VirtualReg && ((VirtualReg) n).isGlobal());
                    while (!selectStack.isEmpty()) {
                        var n = selectStack.pop();
                        var okColors = IntStream.range(0, 15).filter(i -> i != 13).boxed() // 15
                                .collect(Collectors.toCollection(HashSet::new));

                        for (MachineOperand w : adjList.getOrDefault(n, new HashSet<>())) {
                            var a = getAlias.apply(w);
                            if (a.isAllocated() || a.isPrecolored()) {
                                assert a instanceof PhyReg;
                                okColors.remove(((PhyReg) a).getIdx());
                            } else if (a instanceof VirtualReg) {
                                if (colored.containsKey(a)) {
                                    var color = colored.get(a);
                                    assert color instanceof PhyReg;
                                    okColors.remove(((PhyReg) color).getIdx());
                                }
                            }
                        }

                        if (okColors.isEmpty()) {
                            spilledNodes.add(n);
                        } else {
                            var color = okColors.iterator().next();
                            colored.put(n, func.getAllocatedReg(color));
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
                            var defs = new ArrayList<>(instr.getDef());
                            var uses = new ArrayList<>(instr.getUse());

                            defs.stream().filter(colored::containsKey)
                                    .forEach(def -> replaceReg(instr, def, colored.get(def)));
                            uses.stream().filter(colored::containsKey)
                                    .forEach(use -> replaceReg(instr, use, colored.get(use)));
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
                                    if (inst instanceof MCLoad) {
                                        var loadInstr = (MCLoad) inst;
                                        loadInstr.setOffset(offsetOperand);
                                    } else if (inst instanceof MCStore) {
                                        var storeInstr = (MCStore) inst;
                                        storeInstr.setOffset(offsetOperand);
                                    }
                                } else {
                                    var moveInstr = new MCMove();
                                    moveInstr.insertBeforeNode(inst);
                                    moveInstr.setRhs(offsetOperand);

                                    var newVReg = new VirtualReg();
                                    func.addVirtualReg(newVReg);
                                    moveInstr.setDst(newVReg);

                                    if (inst instanceof MCLoad) {
                                        var loadInstr = (MCLoad) inst;
                                        loadInstr.setOffset(newVReg);
                                    } else if (inst instanceof MCStore) {
                                        var storeInstr = (MCStore) inst;
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

                                ref.vreg = null;
                            };

                            int cntInstr = 0;
                            for (var instrEntry : block.getmclist()) {
                                var instr = instrEntry.getVal();
                                var defs = new HashSet<>(instr.getDef());
                                var uses = new HashSet<>(instr.getUse());
                                defs.stream().filter(def -> def.equals(n)).forEach(def -> {
                                    if (ref.vreg == null) {
                                        ref.vreg = new VirtualReg();
                                        func.addVirtualReg(ref.vreg);
                                    }

                                    replaceReg(instr, def, ref.vreg);
                                    ref.lastDef = instr;
                                });

                                uses.stream().filter(use -> use.equals(n)).forEach(use -> {
                                    if (ref.vreg == null) {
                                        ref.vreg = new VirtualReg();
                                        func.addVirtualReg(ref.vreg);
                                    }

                                    replaceReg(instr, use, ref.vreg);
                                    if (ref.firstUse == null && ref.lastDef == null) {
                                        ref.firstUse = instr;
                                    }
                                });

                                if (cntInstr > 40) {
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

        // todo: [to be refactor] fix allocator equality
        for (var func : manager.getMachineFunctions()) {
            for (var blockEntry : func.getmbList()) {
                var block = blockEntry.getVal();

                for (var instrEntry : block.getmclist()) {
                    var instr = instrEntry.getVal();

                    instr.getDef().stream().filter(PhyReg.class::isInstance)
                            .map(PhyReg.class::cast).forEach(phyReg -> phyReg.isAllocated = false);
                    instr.getUse().stream().filter(PhyReg.class::isInstance)
                            .map(PhyReg.class::cast).forEach(phyReg -> phyReg.isAllocated = false);
                }
            }
        }
    }
}
