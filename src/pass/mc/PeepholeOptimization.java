package pass.mc;

import backend.CodeGenManager;
import backend.machinecodes.*;
import backend.reg.MachineOperand;
import pass.Pass;
import util.Pair;

import java.util.HashMap;
import java.util.HashSet;

import static backend.machinecodes.ArmAddition.CondType.*;
import static backend.machinecodes.ArmAddition.ShiftType.*;
import static backend.reg.MachineOperand.state.*;

public class PeepholeOptimization implements Pass.MCPass {
    @Override
    public String getName() {
        return "Peephole";
    }

    private boolean trivialPeephole(CodeGenManager manager) {
        boolean done = true;
        for (var func : manager.getMachineFunctions()) {
            for (var blockEntry : func.getmbList()) {
                var block = blockEntry.getVal();

                for (var instrEntryIter = block.getmclist().iterator(); instrEntryIter.hasNext(); ) {
                    var instrEntry = instrEntryIter.next();
                    var preInstrEntry = instrEntry.getPrev();
                    var nxtInstrEntry = instrEntry.getNext();
                    var instr = instrEntry.getVal();

                    if (instr instanceof MCBinary) {
                        // add(sub) dst dst 0 (to be remove)
                        MCBinary binInstr = (MCBinary) instr;
                        boolean isAddOrSub = binInstr.getTag() == MachineCode.TAG.Add ||
                                binInstr.getTag() == MachineCode.TAG.Sub;
                        boolean isSameDstLhs = binInstr.getDst().equals(binInstr.getLhs());
                        boolean hasZeroOperand = binInstr.getRhs().equals(MachineOperand.zeroImm);
                        boolean hasNoShift = binInstr.getShift().isNone();

                        if (isAddOrSub && isSameDstLhs && hasZeroOperand && hasNoShift) {
                            instrEntryIter.remove();
                            done = false;
                        }
                    }

                    if (instr instanceof MCJump) {
                        // B1:
                        // jump target (to be remove)
                        // target:
                        MCJump jumpInstr = (MCJump) instr;
                        var nxtBB = blockEntry.getNext() == null ? null : blockEntry.getNext().getVal();
                        boolean isSameTargetNxtBB = jumpInstr.getTarget().equals(nxtBB);

                        if (isSameTargetNxtBB) {
                            instrEntryIter.remove();
                            done = false;
                        }
                    }

                    if (instr instanceof MCBranch) {
                        // B1:
                        // jump target (to be remove)
                        // target:
                        MCBranch brInstr = (MCBranch) instr;
                        var nxtBB = blockEntry.getNext() == null ? null : blockEntry.getNext().getVal();
                        boolean isSameTargetNxtBB = brInstr.getTarget().equals(nxtBB);

                        if (isSameTargetNxtBB) {
                            instrEntryIter.remove();
                            done = false;
                        }
                    }

                    if (instr instanceof MCLoad) {
                        // str a, [b, x]
                        // ldr c, [b, x] (cur, to be replaced)
                        // =>
                        // str a, [b, x]
                        // mov c, a
                        var curLoad = (MCLoad) instr;

                        if (preInstrEntry != null && preInstrEntry.getVal() instanceof MCStore) {
                            MCStore preStore = (MCStore) preInstrEntry.getVal();
                            boolean isSameAddr = preStore.getAddr().equals(curLoad.getAddr());
                            boolean isSameOffset = preStore.getOffset().equals(curLoad.getOffset());
                            boolean isSameShift = preStore.getShift().equals(curLoad.getShift());

                            if (isSameAddr && isSameOffset && isSameShift) {
                                var moveInstr = new MCMove();
                                moveInstr.setDst(curLoad.getDst());
                                moveInstr.setRhs(preStore.getData());

                                moveInstr.insertAfterNode(preInstrEntry.getVal());
                                instrEntryIter.remove();
                                done = false;
                            }
                        }
                    }

                    if (instr instanceof MCMove) {
                        MCMove curMove = (MCMove) instr;
                        boolean isSimple = curMove.getCond() == Any && curMove.getShift().getType() == None;

                        if (!isSimple) {
                            continue;
                        }

                        if (curMove.getDst().equals(curMove.getRhs())) {
                            // move a a (to be remove)
                            instrEntryIter.remove();
                            done = false;
                        } else {
                            if (nxtInstrEntry != null && nxtInstrEntry.getVal() instanceof MCMove) {
                                // move a b (cur, to be remove)
                                // move a c
                                // Warning: the following situation should not be optimized
                                // move a b
                                // move a a
                                var nxtMove = (MCMove) nxtInstrEntry.getVal();
                                boolean isSameDst = nxtMove.getDst().equals(curMove.getDst());
                                boolean nxtInstrNotIdentity = !nxtMove.getRhs().equals(nxtMove.getDst());
                                if (isSameDst && nxtInstrNotIdentity) {
                                    instrEntryIter.remove();
                                    done = false;
                                }
                            }

                            if (preInstrEntry != null && preInstrEntry.getVal() instanceof MCMove) {
                                // move a b
                                // move b a (cur, to be remove)
                                MCMove preMove = (MCMove) preInstrEntry.getVal();
                                boolean isSameA = preMove.getDst().equals(curMove.getRhs());
                                boolean isSameB = preMove.getRhs().equals(curMove.getDst());
                                if (isSameA && isSameB) {
                                    instrEntryIter.remove();
                                    done = false;
                                }
                            }
                        }
                    }
                }
            }
        }

        return done;
    }

    private Pair<HashMap<MachineOperand, MachineCode>, HashMap<MachineCode, MachineCode>> getLiveRangeInBlock(MachineBlock block) {
        var lastDefMap = new HashMap<MachineOperand, MachineCode>();
        var lastNeedInstrMap = new HashMap<MachineCode, MachineCode>();
        for (var instrEntry : block.getmclist()) {
            var instr = instrEntry.getVal();

            var defs = instr.getMCDef();
            var uses = instr.getMCUse();
            var hasSideEffect = instr instanceof MCBranch ||
                    instr instanceof MCCall ||
                    instr instanceof MCJump ||
                    instr instanceof MCStore ||
                    instr instanceof MCReturn ||
                    instr instanceof MCComment;

            uses.stream().filter(lastDefMap::containsKey).forEach(use -> lastNeedInstrMap.put(lastDefMap.get(use), instr));
            defs.forEach(def -> lastDefMap.put(def, instr));
            lastNeedInstrMap.put(instr, hasSideEffect ? instr : null);
        }
        return new Pair<>(lastDefMap, lastNeedInstrMap);
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
                        .filter(use -> !blockLiveInfo.liveDef.contains(use))
                        .forEach(blockLiveInfo.liveUse::add);
                instr.getDef().stream()
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

    private boolean peepholeWithDataFlow(CodeGenManager manager) {
        boolean done = true;
        for (var func : manager.getMachineFunctions()) {
            var liveInfoMap = livenessAnalysis(func);

            for (var blockEntry : func.getmbList()) {
                var block = blockEntry.getVal();
                var liveRangePair = getLiveRangeInBlock(block);
                var lastDefMap = liveRangePair.getFirst();
                var liveRangeInBlock = liveRangePair.getSecond();
                var liveout = liveInfoMap.get(block).liveOut;

                for (var instrEntryIter = block.getmclist().iterator(); instrEntryIter.hasNext(); ) {
                    var instrEntry = instrEntryIter.next();
                    var instr = instrEntry.getVal();

                    // Remove unused instr
                    var lastUseInstr = liveRangeInBlock.get(instr);
                    var isLastDefInstr = instr.getDef().stream().allMatch(def -> lastDefMap.get(def).equals(instr));
                    var defRegInLiveout = instr.getDef().stream().anyMatch(liveout::contains);
                    if (!isLastDefInstr && lastUseInstr == null) {
                        instrEntryIter.remove();
                        done = false;
                    } else {
                        var nxtInstrEntry = instrEntry.getNext();

                        // todo
                        // add a a #i
                        // ldr b [a, #0]
                        // =>
                        // ldr b [a, #i]

                    }
                }
            }
        }

        return done;
    }

    private boolean removeUselessBB(CodeGenManager manager) {
        boolean done = true;

        // todo

        return done;
    }

    public void run(CodeGenManager manager) {
        boolean done = false;

        while (!done) {
            done = trivialPeephole(manager) && peepholeWithDataFlow(manager); //&& removeUselessBB(manager);
        }
    }
}
