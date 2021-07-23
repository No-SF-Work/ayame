package pass.mc;

import backend.CodeGenManager;
import backend.machinecodes.*;
import backend.reg.MachineOperand;
import pass.Pass;

import java.util.HashMap;

import static backend.machinecodes.ArmAddition.CondType.*;
import static backend.machinecodes.ArmAddition.ShiftType.*;

public class PeepholeOptimization implements Pass.MCPass {
    @Override
    public String getName() {
        return "Peephole";
    }

    private boolean isSameOperand(MachineOperand a, MachineOperand b) {
        return a.getState().equals(MachineOperand.state.imm) ?
                a.equals(b) :
                a.getState().equals(b.getState()) && a.getName().equals(b.getName());
    }

    private void trivialPeephole(CodeGenManager manager) {
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
                        boolean isSameDstLhs = isSameOperand(binInstr.getDst(), binInstr.getLhs());
                        boolean hasZeroOperand = isSameOperand(binInstr.getRhs(),MachineOperand.zeroImm);
                        boolean hasNoShift = binInstr.getShift().isNone();

                        if (isAddOrSub && isSameDstLhs && hasZeroOperand && hasNoShift) {
                            instrEntryIter.remove();
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
                            boolean isSameAddr = isSameOperand(preStore.getAddr(), curLoad.getAddr());
                            boolean isSameOffset = isSameOperand(preStore.getOffset(), curLoad.getOffset());
                            boolean isSameShift = preStore.getShift().equals(curLoad.getShift());

                            if (isSameAddr && isSameOffset && isSameShift) {
                                var moveInstr = new MCMove();
                                moveInstr.setDst(curLoad.getDst());
                                moveInstr.setRhs(preStore.getData());

                                moveInstr.insertAfterNode(preInstrEntry.getVal());
                                instrEntryIter.remove();
                            }
                        }
                    }

                    if (instr instanceof MCMove) {
                        MCMove curMove = (MCMove) instr;
                        boolean isSimple = curMove.getCond() == Any && curMove.getShift().getType() == None;

                        if (!isSimple) {
                            continue;
                        }

                        if (isSameOperand(curMove.getDst(), curMove.getRhs())) {
                            // move a a (to be remove)
                            instrEntryIter.remove();
                        } else {
                            if (nxtInstrEntry != null && nxtInstrEntry.getVal() instanceof MCMove) {
                                // move a b (cur, to be remove)
                                // move a c
                                // Warning: the following situation should not be optimized
                                // move a b
                                // move a a
                                var nxtMove = (MCMove) nxtInstrEntry.getVal();
                                boolean isSameDst = isSameOperand(nxtMove.getDst(), curMove.getDst());
                                boolean nxtInstrNotIdentity = !isSameOperand(nxtMove.getRhs(), nxtMove.getDst());
                                if (isSameDst && nxtInstrNotIdentity) {
                                    instrEntryIter.remove();
                                }
                            }

                            if (preInstrEntry != null && preInstrEntry.getVal() instanceof MCMove) {
                                // move a b
                                // move b a (cur, to be remove)
                                MCMove preMove = (MCMove) preInstrEntry.getVal();
                                boolean isSameA = isSameOperand(preMove.getDst(), curMove.getRhs());
                                boolean isSameB = isSameOperand(preMove.getRhs(), curMove.getDst());
                                if (isSameA && isSameB) {
                                    instrEntryIter.remove();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private HashMap<MachineCode, MachineCode> getLiveRange(MachineFunction func) {
        var lastDefMap = new HashMap<MachineOperand, MachineCode>();
        var lastNeedInstrMap = new HashMap<MachineCode, MachineCode>();
        for (var blockEntry : func.getmbList()) {
            var block = blockEntry.getVal();

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
        }
        return lastNeedInstrMap;
    }

    private void removeUnusedInstr(CodeGenManager manager) {
        for (var func : manager.getMachineFunctions()) {
            var liveRanges = getLiveRange(func);

            for (var blockEntry : func.getmbList()) {
                var block = blockEntry.getVal();

                for (var instrEntryIter = block.getmclist().iterator(); instrEntryIter.hasNext(); ) {
                    var instrEntry = instrEntryIter.next();
                    var instr = instrEntry.getVal();

                    if (liveRanges.get(instr) == null) {
                        instrEntryIter.remove();
                    }
                }
            }
        }
    }

    public void run(CodeGenManager manager) {
        trivialPeephole(manager);
        removeUnusedInstr(manager);
    }
}
