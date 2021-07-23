package pass.mc;

import backend.CodeGenManager;
import backend.machinecodes.*;
import backend.reg.MachineOperand;
import pass.Pass;

import java.util.function.Function;

import static backend.machinecodes.ArmAddition.CondType.*;
import static backend.machinecodes.ArmAddition.ShiftType.*;

public class PeepholeOptimization implements Pass.MCPass {
    @Override
    public String getName() {
        return "Peephole";
    }

    private void trivialPeephole(CodeGenManager manager) {
        // todo: use live interval to peephole
        for (var func : manager.getMachineFunctions()) {
            for (var blockEntry : func.getmbList()) {
                var block = blockEntry.getVal();

                for (var instrEntryIter = block.getmclist().iterator(); instrEntryIter.hasNext(); ) {
                    var instrEntry = instrEntryIter.next();
                    var preInstrEntry = instrEntry.getPrev();
                    var nxtInstrEntry = instrEntry.getNext();
                    var instr = instrEntry.getVal();

                    if (instr instanceof MCBinary) {
                        // todo: 0 is rhs?
                        // add(sub) dst dst 0 (to be remove)
                        MCBinary binInstr = (MCBinary) instr;
                        boolean isAddOrSub = binInstr.getTag() == MachineCode.TAG.Add ||
                                binInstr.getTag() == MachineCode.TAG.Sub;
                        boolean isSameDstLhs = binInstr.getDst().equals(binInstr.getLhs());
                        boolean hasZeroOperand = binInstr.getRhs().equals(MachineOperand.zeroImm);
                        boolean hasNoShift = binInstr.getShift().isNone();

                        if (isAddOrSub && isSameDstLhs && hasZeroOperand && hasNoShift) {
                            instrEntryIter.remove();
                        }
                    }

                    if (instr instanceof MCJump) {
                        // todo: is trueblock nxtblock?
                        // B1:
                        // jump target (to be remove)
                        // target:
                        MCJump jumpInstr = (MCJump) instr;
                        boolean isSameTargetNxtBB = jumpInstr.getTarget().equals(block.getFalseSucc());

                        if (isSameTargetNxtBB) {
                            instrEntryIter.remove();
                        }
                    }

                    if (instr instanceof MCLoad) {
                        // str a, [b, x]
                        // ldr c, [b, x] (cur, to be removed)
                        // =>
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
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void run(CodeGenManager manager) {
        trivialPeephole(manager);
    }
}
