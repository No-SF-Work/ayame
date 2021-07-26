package pass.mc;

import backend.CodeGenManager;
import backend.machinecodes.*;
import backend.reg.MachineOperand;
import pass.Pass;
import util.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

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
                    boolean hasNoCond = instr.getCond() == Any;
                    boolean hasNoShift = instr.getShift().getType() == None || instr.getShift().getImm() == 0;

                    if (instr instanceof MCBinary) {
                        // add(sub) dst dst 0 (to be remove)
                        MCBinary binInstr = (MCBinary) instr;
                        boolean isAddOrSub = binInstr.getTag() == MachineCode.TAG.Add ||
                                binInstr.getTag() == MachineCode.TAG.Sub;
                        boolean isSameDstLhs = binInstr.getDst().equals(binInstr.getLhs());
                        boolean hasZeroOperand = binInstr.getRhs().equals(MachineOperand.zeroImm);

                        if (isAddOrSub && isSameDstLhs && hasZeroOperand) {
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

                        if (isSameTargetNxtBB && hasNoCond) {
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

                        if (isSameTargetNxtBB && hasNoCond) {
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
                                moveInstr.setCond(curLoad.getCond());

                                moveInstr.insertAfterNode(preInstrEntry.getVal());
                                instrEntryIter.remove();
                                done = false;
                            }
                        }
                    }

                    if (instr instanceof MCMove) {
                        MCMove curMove = (MCMove) instr;

                        if (curMove.getDst().equals(curMove.getRhs()) && hasNoShift) {
                            // move a a (to be remove)
                            instrEntryIter.remove();
                            done = false;
                        } else {
                            if (nxtInstrEntry != null && nxtInstrEntry.getVal() instanceof MCMove && hasNoCond && hasNoShift) {
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

                            if (preInstrEntry != null && preInstrEntry.getVal() instanceof MCMove && hasNoShift) {
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
                    boolean hasNoCond = instr.getCond() == Any;
                    boolean hasNoShift = instr.getShift().getType() == None || instr.getShift().getImm() == 0;

                    // Remove unused instr
                    var lastUser = liveRangeInBlock.get(instr);
                    var isLastDefInstr = instr.getDef().stream().allMatch(def -> lastDefMap.get(def).equals(instr));
                    var defRegInLiveout = instr.getDef().stream().anyMatch(liveout::contains);

                    if (!(isLastDefInstr && defRegInLiveout) && hasNoCond && hasNoShift) { // is last instr and will be used in the future
                        if (lastUser == null) {
                            instrEntryIter.remove();
                            done = false;
                        } else {
                            // add/sub a a #i
                            // ldr b [a, #x]
                            // =>
                            // ldr b [a, #x+i]
                            // ---------------
                            // add/sub a a #i
                            // move b x
                            // str b [a, #x]
                            // =>
                            // move b x
                            // str b [a, #x+i]
                            var isCurAddSub = instr.getTag().equals(MachineCode.TAG.Add) || instr.getTag().equals(MachineCode.TAG.Sub);
                            if (!isCurAddSub) {
                                continue;
                            }

                            assert instr instanceof MCBinary;
                            var binInstr = (MCBinary) instr;
                            var isSameDstLhs = binInstr.getDst().equals(binInstr.getLhs());
                            var hasImmRhs = binInstr.getRhs().getState() == imm;

                            if (!(isSameDstLhs && hasImmRhs)) {
                                continue;
                            }

                            var isAdd = instr.getTag().equals(MachineCode.TAG.Add);
                            var imm = binInstr.getRhs().getImm();

                            var nxtInstrEntry = instrEntry.getNext();
                            if (nxtInstrEntry == null) {
                                continue;
                            }

                            var nxtInstr = nxtInstrEntry.getVal();

                            if (nxtInstr instanceof MCLoad && !Objects.equals(lastUser, nxtInstr)) {
                                // add/sub a a #i
                                // ldr b [a, #x]
                                // =>
                                // ldr b [a, #x+i]
                                var loadInstr = (MCLoad) nxtInstr;
                                var isSameLhsAddr = loadInstr.getAddr().equals(binInstr.getLhs());

                                if (isSameLhsAddr) {
                                    var addImm = new MachineOperand(loadInstr.getOffset().getImm() + imm);
                                    var subImm = new MachineOperand(loadInstr.getOffset().getImm() - imm);
                                    loadInstr.setOffset(isAdd ? addImm : subImm);
                                    instrEntryIter.remove();
                                }
                            } else if (nxtInstr instanceof MCMove) {
                                // add/sub a a #i
                                // move b y
                                // str b [a, #x]
                                // =>
                                // move b y
                                // str b [a, #x+i]
                                var nxt2InstrEntry = instrEntry.getNext();
                                if (nxt2InstrEntry == null) {
                                    continue;
                                }

                                var nxt2Instr = nxt2InstrEntry.getVal();

                                var moveInstr = (MCMove) nxtInstr;
                                if (nxt2Instr instanceof MCStore) {
                                    var storeInstr = (MCStore) nxt2Instr;
                                    var isSameData = moveInstr.getDst().equals(storeInstr.getData());
                                    var isSameLhsAddr = storeInstr.getAddr().equals(binInstr.getLhs());

                                    if (isSameData && isSameLhsAddr) {
                                        var addImm = new MachineOperand(storeInstr.getOffset().getImm() + imm);
                                        var subImm = new MachineOperand(storeInstr.getOffset().getImm() - imm);
                                        storeInstr.setOffset(isAdd ? addImm : subImm);
                                        instrEntryIter.remove();
                                    }
                                }
                            }
                        }
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
