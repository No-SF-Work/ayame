package pass.mc;

import backend.CodeGenManager;
import backend.machinecodes.*;
import backend.reg.*;
import pass.Pass;
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
        private final MachineBlock block;
        private TreeSet<MachineOperand> liveUse = new TreeSet<>();
        private TreeSet<MachineOperand> liveDef = new TreeSet<>();
        private TreeSet<MachineOperand> liveIn = new TreeSet<>();
        private TreeSet<MachineOperand> liveOut = new TreeSet<>();

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
                var newLiveOut = new TreeSet<MachineOperand>();

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

                    blockLiveInfo.liveIn = new TreeSet<>(blockLiveInfo.liveUse);
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
        } else if (instr instanceof MCBranch) {
        } else if (instr instanceof MCCall) {
        } else if (instr instanceof MCComment) {
        } else if (instr instanceof MCCompare) {
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
        } else if (instr instanceof MCGlobal) {
            var globalInstr = (MCGlobal) instr;
            if (globalInstr.getDst().equals(origin)) {
                globalInstr.setDst(target);
            }
        } else if (instr instanceof MCJump) {
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
            // fixme
//            var allocatable = IntStream.range(0, 15).filter(i -> i != 13)
//                    .mapToObj(func::getPhyReg).collect(Collectors.toCollection(TreeSet::new));

            var done = false;

            while (!done) {
                var liveInfoMap = livenessAnalysis(func);

                var adjList = new HashMap<MachineOperand, TreeSet<MachineOperand>>();
                var adjSet = new HashSet<Pair<MachineOperand, MachineOperand>>();
                var alias = new HashMap<MachineOperand, MachineOperand>();
                var moveList = new HashMap<MachineOperand, TreeSet<MCMove>>();
                var simplifyWorklist = new TreeSet<MachineOperand>();
                var freezeWorklist = new TreeSet<MachineOperand>();
                var spillWorklist = new TreeSet<MachineOperand>();
                var spilledNodes = new TreeSet<MachineOperand>();
                var coalescedNodes = new TreeSet<MachineOperand>();
                var selectStack = new Stack<MachineOperand>();
                var worklistMoves = new TreeSet<MCMove>();
                var activeMoves = new TreeSet<MCMove>();
                // maybe removed
                var coalescedMoves = new TreeSet<MCMove>();
                var constrainedMoves = new TreeSet<MCMove>();
                var frozenMoves = new TreeSet<MCMove>();
//                System.out.println(manager.genARM());

                Map<MachineOperand, Integer> degree = IntStream.range(0, 16)
                        .mapToObj(func::getPhyReg)
                        .collect(Collectors.toMap(MachineOperand -> MachineOperand, MachineOperand -> INF));

                BiConsumer<MachineOperand, MachineOperand> addEdge = (u, v) -> {
                    if (!adjSet.contains(new Pair<>(u, v)) && !u.equals(v)) {
                        adjSet.add(new Pair<>(u, v));
                        adjSet.add(new Pair<>(v, u));
                        if (!u.isPrecolored()) {
                            adjList.putIfAbsent(u, new TreeSet<>());
                            adjList.get(u).add(v);
                            degree.putIfAbsent(u, 0);
                            degree.compute(u, (key, value) -> value + 1);
                        }
                        if (!v.isPrecolored()) {
                            adjList.putIfAbsent(v, new TreeSet<>());
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
                        var live = new TreeSet<>(liveInfoMap.get(block).liveOut);

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

                                    moveList.putIfAbsent(rhs, new TreeSet<>());
                                    moveList.get(rhs).add(mcInstr);

                                    moveList.putIfAbsent(dst, new TreeSet<>());
                                    moveList.get(dst).add(mcInstr);

                                    worklistMoves.add(mcInstr);
                                }
                            }
                            defs.stream().filter(MachineOperand::needsColor).forEach(live::add);
                            defs.stream().filter(MachineOperand::needsColor).forEach(d -> live.forEach(l -> addEdge.accept(l, d)));
                            // todo: [heuristic] add loop cnt
                            defs.stream().filter(MachineOperand::needsColor).forEach(live::remove);

                            uses.stream().filter(MachineOperand::needsColor).forEach(live::add);
                        }
                    }
                };

                Function<MachineOperand, Set<MachineOperand>> getAdjacent = n -> adjList.getOrDefault(n, new TreeSet<>()).stream()
                        .filter(operand -> !(selectStack.contains(operand) || coalescedNodes.contains(operand)))
                        .collect(Collectors.toSet());

                Function<MachineOperand, Set<MCMove>> nodeMoves = n -> moveList.getOrDefault(n, new TreeSet<>()).stream()
                        .filter(move -> activeMoves.contains(move) || worklistMoves.contains(move))
                        .collect(Collectors.toSet());

                Function<MachineOperand, Boolean> moveRelated = n -> !nodeMoves.apply(n).isEmpty();

                Runnable makeWorklist = () ->
                        func.getVRegMap().values().forEach(vreg -> {
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
                    // todo: for debug
//                    var tree = new ArrayList<>(simplifyWorklist);
                    var n = simplifyWorklist.first();
//                    System.out.println(n.getName());
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
                    // todo: debug
                    var m = worklistMoves.first();
                    var u = getAlias.apply(m.getDst());
                    var v = getAlias.apply(m.getRhs());
                    if (v.isPrecolored()) {
                        var temp = u;
                        u = v;
                        v = temp;
                    }
//                    System.out.println("coalesce: " + u.getName() + ", " + v.getName());
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
                        if (!moveRelated.apply(v) && degree.getOrDefault(v, 0) < K) { // fixme: maybe a bug?
                            freezeWorklist.remove(v);
                            simplifyWorklist.add(v);
                        }
                    }
                };

                Runnable freeze = () -> {
                    // todo: debug
                    var u = freezeWorklist.first();
                    freezeWorklist.remove(u);
                    simplifyWorklist.add(u);
                    freezeMoves.accept(u);
                };

                Runnable selectSpill = () -> {
                    // todo: heuristic
                    // todo: debug
                    var m = spillWorklist.first();
                    simplifyWorklist.add(m);
                    freezeMoves.accept(m);
                    spillWorklist.remove(m);
                };

                Runnable assignColors = () -> {
                    var colored = new HashMap<MachineOperand, MachineOperand>();
                    while (!selectStack.isEmpty()) {
                        var n = selectStack.pop();
                        var okColors = IntStream.range(0, 15).filter(i -> i != 13).boxed() // 15
                                .collect(Collectors.toCollection(TreeSet::new));

                        for (MachineOperand w : adjList.getOrDefault(n, new TreeSet<>())) {
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
                            // todo: debug
                            var color = okColors.first();
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
                                var defs = new TreeSet<>(instr.getDef());
                                var uses = new TreeSet<>(instr.getUse());
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
