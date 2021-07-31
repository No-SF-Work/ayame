package pass.mc;

import backend.CodeGenManager;
import backend.machinecodes.*;
import backend.reg.MachineOperand;
import pass.Pass;
import util.Pair;

import java.util.*;
import java.util.function.Supplier;

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
                        boolean hasZeroOperand = binInstr.getRhs().getState() == imm && binInstr.getRhs().getImm() == 0;

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
                        // br target (to be remove)
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

                            var preNoCond = preStore.getCond() == Any;

                            if (isSameAddr && isSameOffset && isSameShift && preNoCond) {
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

                                var nxtNoCond = nxtMove.getCond() == Any;

                                if (isSameDst && nxtInstrNotIdentity && nxtNoCond) {
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

                                var preNoCond = preMove.getCond() == Any;
                                var preNoShift = preMove.getShift().getType() == None || preMove.getShift().getImm() == 0;

                                if (isSameA && isSameB && preNoShift && preNoCond) {
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


    private void replaceUseReg(MachineCode instr, MachineOperand origin, MachineOperand target) {
        if (instr instanceof MCBinary) {
            var binaryInstr = (MCBinary) instr;
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
        } else if (instr instanceof MCLongMul) {
            var longMulInstr = (MCLongMul) instr;
            if (longMulInstr.getLhs().equals(origin)) {
                longMulInstr.setLhs(target);
            }
            if (longMulInstr.getRhs().equals(origin)) {
                longMulInstr.setRhs(target);
            }
        } else if (instr instanceof MCMove) {
            var moveInstr = (MCMove) instr;
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

                    if (!(isLastDefInstr && defRegInLiveout) && hasNoCond) { // is last instr and will be used in the future
                        if (lastUser == null && hasNoShift) {
                            instrEntryIter.remove();
                            done = false;
                        } else {
                            Supplier<Boolean> addSubLdrStr = () -> {
                                // add/sub a c #i
                                // ldr b [a, #x]
                                // =>
                                // ldr b [c, #x+i]
                                // ---------------
                                // add/sub a c #i
                                // move b x
                                // str b [a, #x]
                                // =>
                                // move b x
                                // str b [c, #x+i]
                                if (!hasNoShift) {
                                    return true;
                                }

                                var isCurAddSub = instr.getTag().equals(MachineCode.TAG.Add) || instr.getTag().equals(MachineCode.TAG.Sub);
                                if (!isCurAddSub) {
                                    return true;
                                }

                                assert instr instanceof MCBinary;
                                var binInstr = (MCBinary) instr;
                                var hasImmRhs = binInstr.getRhs().getState() == imm;

                                if (!hasImmRhs) {
                                    return true;
                                }

                                var isAdd = instr.getTag().equals(MachineCode.TAG.Add);
                                var imm = binInstr.getRhs().getImm();

                                var nxtInstrEntry = instrEntry.getNext();
                                if (nxtInstrEntry == null) {
                                    return true;
                                }
                                var nxtInstr = nxtInstrEntry.getVal();
                                if (!Objects.equals(lastUser, nxtInstr)) {
                                    return true;
                                }

                                if (nxtInstr instanceof MCLoad) {
                                    // add/sub a c #i
                                    // ldr b [a, #x]
                                    // =>
                                    // ldr b [c, #x+i]
                                    var loadInstr = (MCLoad) nxtInstr;
                                    var isSameDstAddr = loadInstr.getAddr().equals(binInstr.getDst());
                                    var isOffsetImm = loadInstr.getOffset().getState() == MachineOperand.state.imm;

                                    if (isSameDstAddr && isOffsetImm) {
                                        assert nxtInstr.getShift().getType() == None || nxtInstr.getShift().getImm() == 0;
                                        var addImm = new MachineOperand(loadInstr.getOffset().getImm() + imm);
                                        var subImm = new MachineOperand(loadInstr.getOffset().getImm() - imm);
                                        loadInstr.setAddr(binInstr.getLhs());
                                        loadInstr.setOffset(isAdd ? addImm : subImm);
                                        instrEntryIter.remove();
                                        return false;
                                    }
                                } else if (nxtInstr instanceof MCMove) {
                                    // add/sub a c #i
                                    // move b y
                                    // str b [a, #x]
                                    // =>
                                    // move b y
                                    // str b [c, #x+i]
                                    // this situation should be avoided:
                                    // add/sub a c #i
                                    // move c y
                                    // str c [a, #x]
                                    var nxt2InstrEntry = nxtInstrEntry.getNext();
                                    if (nxt2InstrEntry == null) {
                                        return true;
                                    }
                                    var nxt2Instr = nxt2InstrEntry.getVal();
                                    if (!Objects.equals(lastUser, nxt2Instr)) {
                                        return true;
                                    }

                                    var moveInstr = (MCMove) nxtInstr;
                                    if (nxt2Instr instanceof MCStore) {
                                        var storeInstr = (MCStore) nxt2Instr;
                                        var isSameData = moveInstr.getDst().equals(storeInstr.getData());
                                        var isSameDstAddr = storeInstr.getAddr().equals(binInstr.getDst());
                                        var notSameLhsData = !binInstr.getLhs().equals(storeInstr.getData()); // attention
                                        var isOffsetImm = storeInstr.getOffset().getState() == MachineOperand.state.imm;

                                        if (isSameData && isSameDstAddr && notSameLhsData && isOffsetImm) {
                                            assert nxt2Instr.getShift().getType() == None || nxt2Instr.getShift().getImm() == 0;
                                            var addImm = new MachineOperand(storeInstr.getOffset().getImm() + imm);
                                            var subImm = new MachineOperand(storeInstr.getOffset().getImm() - imm);
                                            storeInstr.setAddr(binInstr.getLhs());
                                            storeInstr.setOffset(isAdd ? addImm : subImm);
                                            instrEntryIter.remove();
                                            return false;
                                        }
                                    }
                                }
                                return true;
                            };

                            Supplier<Boolean> movReplace = () -> {
                                // mov a, b
                                // anything
                                // =>
                                // anything (replaced)
                                if (!hasNoShift) {
                                    return true;
                                }

                                if (!(instr instanceof MCMove)) {
                                    return true;
                                }
                                var movInstr = (MCMove) instr;

                                if (movInstr.getRhs().getState() == imm) { // dont replace imm
                                    return true;
                                }

                                var nxtInstrEntry = instrEntry.getNext();
                                if (nxtInstrEntry == null) {
                                    return true;
                                }
                                var nxtInstr = nxtInstrEntry.getVal();
                                var nxtHasSideEffect = nxtInstr instanceof MCCall || nxtInstr instanceof MCReturn;
                                if (!Objects.equals(lastUser, nxtInstr) || nxtHasSideEffect) {
                                    return true;
                                }

                                replaceUseReg(nxtInstr, movInstr.getDst(), movInstr.getRhs());

                                instrEntryIter.remove();
                                return false;
                            };

                            Supplier<Boolean> addLdrShift = () -> {
                                // add a, b, c, shift
                                // ldr/str x, [a, #0]
                                // =>
                                // ldr/str x, [b, c, shift]
                                if (hasNoShift) { // shold have shift
                                    return true;
                                }

                                if (!(instr.getTag() == MachineCode.TAG.Add)) {
                                    return true;
                                }
                                assert instr instanceof MCBinary;
                                var addInstr = (MCBinary) instr;

                                assert addInstr.getRhs().getState() != imm;

                                var nxtInstrEntry = instrEntry.getNext();
                                if (nxtInstrEntry == null) {
                                    return true;
                                }
                                var nxtInstr = nxtInstrEntry.getVal();
                                if (!Objects.equals(lastUser, nxtInstr)) {
                                    return true;
                                }

                                if (nxtInstr instanceof MCLoad) {
                                    var loadInstr = (MCLoad) nxtInstr;

                                    var isSameDstAddr = addInstr.getDst().equals(loadInstr.getAddr());
                                    var isOffsetZero = loadInstr.getOffset().getState() == imm && loadInstr.getOffset().getImm() == 0;

                                    if (isSameDstAddr && isOffsetZero) {
                                        loadInstr.setAddr(addInstr.getLhs());
                                        loadInstr.setOffset(addInstr.getRhs());
                                        loadInstr.setShift(addInstr.getShift().getType(), addInstr.getShift().getImm());
                                        instrEntryIter.remove();
                                        return false;
                                    }
                                } else if (nxtInstr instanceof MCStore) {
                                    var storeInstr = (MCStore) nxtInstr;

                                    var isSameDstAddr = addInstr.getDst().equals(storeInstr.getAddr());
                                    var isOffsetZero = storeInstr.getOffset().getState() == imm && storeInstr.getOffset().getImm() == 0;
                                    var notSameDstData = !storeInstr.getData().equals(addInstr.getDst());

                                    if (isSameDstAddr && isOffsetZero && notSameDstData) {
                                        storeInstr.setAddr(addInstr.getLhs());
                                        storeInstr.setOffset(addInstr.getRhs());
                                        storeInstr.setShift(addInstr.getShift().getType(), addInstr.getShift().getImm());
                                        instrEntryIter.remove();
                                        return false;
                                    }
                                }

                                return true;
                            };

                            Supplier<Boolean> movCmp = () -> {
                                // mov a imm
                                // cmp b a
                                // =>
                                // cmp b imm
                                if (!hasNoShift) {
                                    return true;
                                }

                                if (!(instr instanceof MCMove)) {
                                    return true;
                                }
                                var movInstr = (MCMove) instr;
                                if (movInstr.getRhs().getState() != imm) { // just replace imm
                                    return true;
                                }

                                var nxtInstrEntry = instrEntry.getNext();
                                if (nxtInstrEntry == null) {
                                    return true;
                                }
                                var nxtInstr = nxtInstrEntry.getVal();
                                if (!Objects.equals(lastUser, nxtInstr)) {
                                    return true;
                                }

                                if (!(nxtInstr instanceof MCCompare)) {
                                    return true;
                                }
                                var cmpInstr = (MCCompare) nxtInstr;

                                var isSameDstRhs = movInstr.getDst().equals(cmpInstr.getRhs());
                                var notSameDstLhs = movInstr.getDst().equals(cmpInstr.getLhs());
                                if (!(isSameDstRhs && notSameDstLhs)) {
                                    return true;
                                }

                                var nxtNoShift = nxtInstr.getShift().getType() == None || nxtInstr.getShift().getImm() == 0;
                                if (!nxtNoShift) {
                                    return true;
                                }

                                var imm = movInstr.getRhs().getImm();
                                if (CodeGenManager.canEncodeImm(imm)) {
                                    cmpInstr.setRhs(new MachineOperand(imm));
                                } else if (CodeGenManager.canEncodeImm(-imm)) {
                                    cmpInstr.setRhs(new MachineOperand(-imm));
                                    cmpInstr.setCmn();
                                } else {
                                    return true;
                                }

                                instrEntryIter.remove();
                                return false;
                            };

                            Supplier<Boolean> mulAddSub = () -> {
                                // mul a, b, c
                                // add/sub d, x, a (add d, a, x)
                                // =>
                                // mla/mls d, b, c, x
                                if (!hasNoShift) {
                                    return true;
                                }

                                if (!(instr.getTag() == MachineCode.TAG.Mul)) {
                                    return true;
                                }
                                assert instr instanceof MCBinary;
                                var mulInstr = (MCBinary) instr;

                                var nxtInstrEntry = instrEntry.getNext();
                                if (nxtInstrEntry == null) {
                                    return true;
                                }
                                var nxtInstr = nxtInstrEntry.getVal();
                                if (!Objects.equals(lastUser, nxtInstr)) {
                                    return true;
                                }

                                var nxtNoShift = nxtInstr.getShift().getType() == None || nxtInstr.getShift().getImm() == 0;
                                if (!nxtNoShift) {
                                    return true;
                                }

                                if (!(nxtInstr.getTag() == MachineCode.TAG.Add || nxtInstr.getTag() == MachineCode.TAG.Sub)) {
                                    return true;
                                }

                                assert nxtInstr instanceof MCBinary;
                                var binInstr = (MCBinary) nxtInstr;

                                var fmaInstr = new MCFma();
                                fmaInstr.setCond(binInstr.getCond());
                                fmaInstr.setDst(binInstr.getDst());
                                fmaInstr.setLhs(mulInstr.getLhs());
                                fmaInstr.setRhs(mulInstr.getRhs());
                                fmaInstr.setSign(false);

                                if (binInstr.getTag() == MachineCode.TAG.Add) {
                                    fmaInstr.setAdd(true);
                                    if (binInstr.getRhs().equals(mulInstr.getDst())) {
                                        fmaInstr.setAcc(binInstr.getLhs());
                                        if (binInstr.getLhs().equals(mulInstr.getDst())) {
                                            return true;
                                        }
                                    } else if (binInstr.getLhs().equals(mulInstr.getDst())) {
                                        fmaInstr.setAcc(binInstr.getRhs());
                                        if (binInstr.getRhs().equals(mulInstr.getDst())) {
                                            return true;
                                        }
                                    } else {
                                        return true;
                                    }
                                } else if (binInstr.getTag() == MachineCode.TAG.Sub) {
                                    if (binInstr.getRhs().equals(mulInstr.getDst())) {
                                        fmaInstr.setAdd(false);
                                        fmaInstr.setAcc(binInstr.getLhs());
                                        if (binInstr.getLhs().equals(mulInstr.getDst())) {
                                            return true;
                                        }
                                    } else {
                                        return true;
                                    }
                                } else {
                                    return true;
                                }

                                var nxtInstrNode = nxtInstr.getNode();
                                nxtInstrNode.setVal(fmaInstr);
                                fmaInstr.setNode(nxtInstrNode);
                                fmaInstr.mb = block;
                                fmaInstr.mf = func;

                                // maintain data flow info
                                if (liveRangeInBlock.containsKey(nxtInstr)) {
                                    liveRangeInBlock.put(fmaInstr, liveRangeInBlock.get(nxtInstr));
                                    liveRangeInBlock.remove(nxtInstr);
                                }

                                if (lastDefMap.containsValue(nxtInstr)) {
                                    var key = fmaInstr.getDef().get(0);
                                    assert key != null;
                                    lastDefMap.put(key, fmaInstr);
                                }

                                instrEntryIter.remove();
                                return false;
                            };

                            Supplier<Boolean> movShift = () -> {
                                // mov a b shift
                                // instr c a
                                // =>
                                // instr c b shift
                                if (!(instr instanceof MCMove)) {
                                    return true;
                                }
                                var movInstr = (MCMove) instr;

                                assert hasNoShift || movInstr.getRhs().getState() != imm;

                                var nxtInstrEntry = instrEntry.getNext();
                                if (nxtInstrEntry == null) {
                                    return true;
                                }
                                var nxtInstr = nxtInstrEntry.getVal();
                                if (!Objects.equals(lastUser, nxtInstr)) {
                                    return true;
                                }

                                if (nxtInstr instanceof MCBinary) {
                                    // mov a b shift
                                    // add c d a
                                    // =>
                                    // add c d b shift
                                    var binInstr = (MCBinary) nxtInstr;
                                    if (binInstr.getTag() == MachineCode.TAG.Div || binInstr.getTag() == MachineCode.TAG.Mul) {
                                        return true;
                                    }

                                    var isSameDstRhs = movInstr.getDst().equals(binInstr.getRhs());
                                    var nxtNoShift = nxtInstr.getShift().getType() == None || nxtInstr.getShift().getImm() == 0;
                                    var notSameDstLhs = !movInstr.getDst().equals(binInstr.getLhs());

                                    if (isSameDstRhs && nxtNoShift && notSameDstLhs) {
                                        binInstr.setRhs(movInstr.getRhs());
                                        binInstr.setShift(movInstr.getShift().getType(), movInstr.getShift().getImm());
                                        instrEntryIter.remove();
                                        return false;
                                    }
                                } else if (nxtInstr instanceof MCLoad) {
                                    // mov a b shift
                                    // ldr c, [d a]
                                    // =>
                                    // ldr c, [d b shift]
                                    var loadInstr = (MCLoad) nxtInstr;
                                    var isSameDstOffset = movInstr.getDst().equals(loadInstr.getOffset());
                                    var nxtNoShift = nxtInstr.getShift().getType() == None || nxtInstr.getShift().getImm() == 0;
                                    var notSameDstAddr = !movInstr.getDst().equals(loadInstr.getAddr());

                                    if (isSameDstOffset && nxtNoShift && notSameDstAddr) {
                                        loadInstr.setOffset(movInstr.getRhs());
                                        loadInstr.setShift(movInstr.getShift().getType(), movInstr.getShift().getImm());
                                        instrEntryIter.remove();
                                        return false;
                                    }
                                } else if (nxtInstr instanceof MCStore) {
                                    // mov a b shift
                                    // str c, [d a]
                                    // =>
                                    // str c, [d b shift]
                                    var storeInstr = (MCStore) nxtInstr;
                                    var isSameDstOffset = movInstr.getDst().equals(storeInstr.getOffset());
                                    var nxtNoShift = nxtInstr.getShift().getType() == None || nxtInstr.getShift().getImm() == 0;
                                    var notSameDstAddr = !movInstr.getDst().equals(storeInstr.getAddr());
                                    var notSameDstData = !movInstr.getDst().equals(storeInstr.getData());

                                    if (isSameDstOffset && nxtNoShift && notSameDstData && notSameDstAddr) {
                                        storeInstr.setOffset(movInstr.getRhs());
                                        storeInstr.setShift(movInstr.getShift().getType(), movInstr.getShift().getImm());
                                        instrEntryIter.remove();
                                        return false;
                                    }
                                }

                                return true;
                            };

                            Supplier<Boolean> subSub = () -> {
                                // sub a, b, a
                                // sub b, b, a
                                if (!hasNoShift) {
                                    return true;
                                }

                                if (instr.getTag() != MachineCode.TAG.Sub) {
                                    return true;
                                }
                                var subInstr = (MCBinary) instr;

                                var nxtInstrEntry = instrEntry.getNext();
                                if (nxtInstrEntry == null) {
                                    return true;
                                }
                                var nxtInstr = nxtInstrEntry.getVal();
                                if (!Objects.equals(lastUser, nxtInstr)) {
                                    return true;
                                }

                                if (nxtInstr.getTag() != MachineCode.TAG.Sub) {
                                    return true;
                                }
                                var subNxtInstr = (MCBinary) nxtInstr;

                                var nxtNoShift = nxtInstr.getShift().getType() == None || nxtInstr.getShift().getImm() == 0;
                                if (!nxtNoShift) {
                                    return true;
                                }

                                var a = subInstr.getDst();
                                var b = subNxtInstr.getDst();
                                if (a.equals(b)) {
                                    return true;
                                }

                                var matched1 = subInstr.getDst().equals(a) && subInstr.getLhs().equals(b) && subInstr.getRhs().equals(a);
                                var matched2 = subNxtInstr.getDst().equals(b) && subNxtInstr.getLhs().equals(b) && subNxtInstr.getRhs().equals(a);
                                if (matched1 && matched2) {
                                    var moveInstr = new MCMove();
                                    moveInstr.setDst(b);
                                    moveInstr.setRhs(a);
                                    moveInstr.setCond(subNxtInstr.getCond());

                                    var nxtInstrNode = nxtInstr.getNode();
                                    nxtInstrNode.setVal(moveInstr);
                                    moveInstr.setNode(nxtInstrNode);
                                    moveInstr.mb = block;
                                    moveInstr.mf = func;

                                    // maintain data flow info
                                    if (liveRangeInBlock.containsKey(nxtInstr)) {
                                        liveRangeInBlock.put(moveInstr, liveRangeInBlock.get(nxtInstr));
                                        liveRangeInBlock.remove(nxtInstr);
                                    }

                                    if (lastDefMap.containsValue(nxtInstr)) {
                                        var key = moveInstr.getDef().get(0);
                                        assert key != null;
                                        lastDefMap.put(key, moveInstr);
                                    }
                                    instrEntryIter.remove();
                                    return false;
                                } else {
                                    return true;
                                }
                            };

                            if (instr instanceof MCMove) {
                                var imm = ((MCMove) instr).getRhs().getImm();
                                if (!CodeGenManager.canEncodeImm(imm)) {
                                    continue;
                                }
                            }

                            done &= addSubLdrStr.get();
                            done &= addLdrShift.get();
                            done &= mulAddSub.get();
                            done &= subSub.get();

//                            if (instr instanceof MCMove && func.getArgMoves().contains(instr)) {
//                                continue;
//                            }

                            done &= movReplace.get();
                            done &= movCmp.get();
                            done &= movShift.get();
                        }
                    }
                }
            }
        }
        return done;
    }

    private HashMap<MachineBlock, MachineBlock> getReplaceableBB(CodeGenManager manager) {
        var replaceableBB = new HashMap<MachineBlock, MachineBlock>();

        for (var func : manager.getMachineFunctions()) {
            for (var blockEntry : func.getmbList()) {
                var block = blockEntry.getVal();
                var mcCount = block.getmclist().getNumNode();

                if (mcCount == 1) {
                    var instrEntry = block.getmclist().getEntry();
                    var instr = instrEntry.getVal();

                    if (instr.getCond() == Any && (instr instanceof MCJump || instr instanceof MCBranch)) {
                        MachineBlock target;

                        if (instr instanceof MCJump) {
                            target = ((MCJump) instr).getTarget();
                        } else {
                            target = ((MCBranch) instr).getTarget();
                        }

                        replaceableBB.put(block, target);
                        instrEntry.removeSelf();
                    }
                }
            }
        }
        // make closure
        for (boolean isClosure = false; !isClosure; ) {
            isClosure = true;
            for (var entry : replaceableBB.entrySet()) {
                var key = entry.getKey();
                var value = entry.getValue();
                if (replaceableBB.containsKey(value)) {
                    replaceableBB.put(key, replaceableBB.get(value));
                    isClosure = false;
                }
            }
        }
        return replaceableBB;
    }

    private HashMap<MachineBlock, MachineBlock> getEmptyReplaceableBB(CodeGenManager manager) {
        var replaceableBB = new HashMap<MachineBlock, MachineBlock>();

        for (var func : manager.getMachineFunctions()) {
            for (var blockEntry : func.getmbList()) {
                var block = blockEntry.getVal();
                var mcCount = block.getmclist().getNumNode();

                if (mcCount == 0) {
                    if (blockEntry.getNext() != null) {
                        replaceableBB.put(block, blockEntry.getNext().getVal());
                    } else {
                        replaceableBB.put(block, null);
                    }
                }
            }
        }
        return replaceableBB;
    }

    private void replaceBB(CodeGenManager manager, HashMap<MachineBlock, MachineBlock> replaceableBB) {
        for (var func : manager.getMachineFunctions()) {
            for (var blockEntry : func.getmbList()) {
                var block = blockEntry.getVal();

                for (var instrEntry : block.getmclist()) {
                    var instr = instrEntry.getVal();

                    if (instr instanceof MCJump) {
                        var jumpInstr = (MCJump) instr;
                        var target = jumpInstr.getTarget();

                        if (replaceableBB.containsKey(target)) {
                            jumpInstr.setTarget(replaceableBB.get(target));
                        }
                    } else if (instr instanceof MCBranch) {
                        var brInstr = (MCBranch) instr;
                        var target = brInstr.getTarget();

                        if (replaceableBB.containsKey(target)) {
                            brInstr.setTarget(replaceableBB.get(target));
                        }
                    }
                }
            }
        }
    }

    private void removeEmptyBB(CodeGenManager manager) {
        for (var func : manager.getMachineFunctions()) {
            for (var blockEntryIter = func.getmbList().iterator(); blockEntryIter.hasNext(); ) {
                var blockEntry = blockEntryIter.next();
                var block = blockEntry.getVal();

                if (block.getmclist().getNumNode() == 0) {
                    blockEntryIter.remove();
                }
            }
        }
    }

    private boolean removeUselessBB(CodeGenManager manager) {
        // todo
        boolean done;

        var replaceableBB = getReplaceableBB(manager);
        done = replaceableBB.isEmpty();
        replaceBB(manager, replaceableBB);

        return done;
    }

    public void run(CodeGenManager manager) {
        boolean done = false;

        while (!done) {
            done = trivialPeephole(manager);
            done &= peepholeWithDataFlow(manager);
//            done &= removeUselessBB(manager);
        }
    }
}
