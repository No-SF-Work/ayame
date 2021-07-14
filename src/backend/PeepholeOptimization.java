package backend;

import backend.machinecodes.*;

public class PeepholeOptimization {
    public void peepholeOpt(CodeGenManager manager) {
        for (var func : manager.getMachineFunctions()) {
            for (var blockEntry : func.getmbList()) {
                var block = blockEntry.getVal();

                for (var instrEntry : block.getmclist()) {
                    var instr = instrEntry.getVal();

                    // add(sub) dst dst 0
                    if (instr instanceof MCBinary binInstr) {
                        boolean isAddSub = binInstr.getTag() == MachineCode.TAG.Add ||
                                binInstr.getTag() == MachineCode.TAG.Sub;
                        boolean isSameDstLhs = binInstr.getDst().equals(binInstr.getLhs());
                        boolean hasNoShift = binInstr.getShift().isNone();
                        if (isAddSub && isSameDstLhs && hasNoShift) {
                            // remove
                        }
                    }

                    // B1:
                    // jump target
                    // target:
                    if (instr instanceof MCJump jumpInstr) {
                        boolean isSameTargetNxtBB = jumpInstr.getTarget().equals(block.getTrueSucc());
                        if (isSameTargetNxtBB) {
                            // remove
                        }
                    }

                    // str a, [b, x]
                    // ldr c, [b, x]
                    // =>
                    // mov c, a
                    if (instr instanceof MCLoad loadInstr) {
                        var nxtInstrEntry = instrEntry.getNext();

                        if (nxtInstrEntry != null && nxtInstrEntry.getVal() instanceof MCStore storeInstr) {
                            if (storeInstr)
                        }

//                        && instrEntry.getNext() != null && instrEntry.getNext().getVal()
                    }
                }
            }
        }
    }
}
