package backend;

import backend.machinecodes.*;

public class PeepholeOptimization {
    public void peepholeOpt(CodeGenManager manager) {
        for (var func : manager.getMachineFunctions()) {
            for (var blockEntry : func.getmbList()) {
                var block = blockEntry.getVal();

                for (var instrEntry : block.getmclist()) {
                    var instr = instrEntry.getVal();

                    if (instr instanceof MCBinary binInstr) {
                        // add(sub) dst dst 0 (to be remove)
                        boolean isAddSub = binInstr.getTag() == MachineCode.TAG.Add ||
                                binInstr.getTag() == MachineCode.TAG.Sub;
                        boolean isSameDstLhs = binInstr.getDst().equals(binInstr.getLhs());
                        boolean hasNoShift = binInstr.getShift().isNone();

                        if (isAddSub && isSameDstLhs && hasNoShift) {
                            instr.getNode().removeSelf();
                        }
                    }

                    if (instr instanceof MCJump jumpInstr) {
                        // B1:
                        // jump target (to be remove)
                        // target:
                        boolean isSameTargetNxtBB = jumpInstr.getTarget().equals(block.getTrueSucc());

                        if (isSameTargetNxtBB) {
                            instr.getNode().removeSelf();
                        }
                    }

                    if (instr instanceof MCStore storeInstr) {
                        // str a, [b, x]
                        // ldr c, [b, x]
                        // =>
                        // mov c, a
                        var nxtInstrEntry = instrEntry.getNext();

                        if (nxtInstrEntry != null && nxtInstrEntry.getVal() instanceof MCLoad loadInstr) {
                            boolean isSameAddr = loadInstr.getAddr().equals(storeInstr.getAddr());
                            boolean isSameOffset = loadInstr.getOffset().equals(storeInstr.getOffset());
                            boolean isSameShift = loadInstr.getShift().equals(storeInstr.getShift());
                            // fixme: Postfix/Prefix/etc: boolean isSameMode;

                            if (isSameAddr && isSameOffset && isSameShift) {
                                var moveInstr = new MCMove();
                                moveInstr.setDst(loadInstr.getDst());
                                moveInstr.setRhs(storeInstr.getData());

                                moveInstr.insertAfterNode(instr);
                                nxtInstrEntry.removeSelf();
                            }
                        }
                    }

                    if (instr instanceof MCMove moveInstr) {
                        var nxtInstrEntry = instrEntry.getNext();

                        if (moveInstr.getDst().equals(moveInstr.getRhs())) {
                            // move a a (to be remove)
                            instrEntry.removeSelf();
                        } else if (nxtInstrEntry.getVal() instanceof MCMove nxtMove) {
                            // move a b (to be remove)
                            // move a c
                            // Warning: the following situation should not be optimized
                            // move a b
                            // move a a
                            boolean isSameDst = nxtMove.getDst().equals(moveInstr.getDst());
                            boolean nxtInstrNotIdentity = nxtMove.getRhs().equals(nxtMove.getDst());
                            if (isSameDst && nxtInstrNotIdentity) {
                                instrEntry.removeSelf();
                            }
                        }
                    }
                }
            }
        }
    }
}
