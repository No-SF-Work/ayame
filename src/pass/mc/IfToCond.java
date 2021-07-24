package pass.mc;

import backend.CodeGenManager;
import backend.machinecodes.*;
import pass.Pass;

import java.util.function.Function;

import static backend.machinecodes.ArmAddition.CondType.*;

public class IfToCond implements Pass.MCPass {
    @Override
    public String getName() {
        return "IfToCond";
    }

    @Override
    public void run(CodeGenManager manager) {
        for (var func : manager.getMachineFunctions()) {
            for (var blockEntry : func.getmbList()) {
                var block = blockEntry.getVal();
                var lastInstr = block.getmclist().getLast().getVal();

                if (lastInstr instanceof MCBranch) {
                    var brInstr = (MCBranch) lastInstr;
                    if (blockEntry.getNext() == null) {
                        continue;
                    }

                    var nxtBlock = blockEntry.getNext().getVal();
                    var target = brInstr.getTarget();

                    if (nxtBlock.equals(target)) {
                        boolean canBeOptimized = true;
                        int cntInstr = 0;

                        for (var instrEntry2 : nxtBlock.getmclist()) {
                            var instr2 = instrEntry2.getVal();

                            ++cntInstr;

                            boolean correctInstr = instr2 instanceof MCLoad || instr2 instanceof MCStore || instr2 instanceof MCFma;
                            boolean hasNoCond = instr2.getCond() == Any;
                            boolean tooMuchInstr = cntInstr > 4;

                            if (!(correctInstr && hasNoCond) || tooMuchInstr) {
                                canBeOptimized = false;
                                break;
                            }
                        }

                        if (canBeOptimized) {
                            lastInstr.getNode().removeSelf();

                            Function<ArmAddition.CondType, ArmAddition.CondType> getOppoCond = c -> switch (c) {
                                case Any -> Any;
                                case Eq -> Ne;
                                case Ne -> Eq;
                                case Ge -> Lt;
                                case Gt -> Le;
                                case Le -> Gt;
                                case Lt -> Ge;
                            };

                            for (var instrEntry2 : nxtBlock.getmclist()) {
                                var instr2 = instrEntry2.getVal();
                                if (instr2 instanceof MCLoad) {
                                    MCLoad loadInstr = (MCLoad) instr2;
                                    loadInstr.setCond(getOppoCond.apply(loadInstr.getCond()));
                                } else if (instr2 instanceof MCStore) {
                                    MCStore storeInstr = (MCStore) instr2;
                                    storeInstr.setCond(getOppoCond.apply(storeInstr.getCond()));
                                } else if (instr2 instanceof MCFma) {
                                    MCFma fmaInstr = (MCFma) instr2;
                                    fmaInstr.setCond(getOppoCond.apply(fmaInstr.getCond()));
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}