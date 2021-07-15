package pass.mc;

import backend.CodeGenManager;
import backend.machinecodes.*;
import util.IList;

import java.util.Iterator;
import java.util.function.Function;

import static backend.machinecodes.ArmAddition.CondType.*;

public class PeepholeOptimization {
    public enum optType {
        trivial,
        ifToCond
    }

    private void ifToCond(CodeGenManager manager) {
        for (var func : manager.getMachineFunctions()) {
            for (var blockEntry : func.getmbList()) {
                var block = blockEntry.getVal();
                var lastInstr = block.getmclist().getLast().getVal();

                if (lastInstr instanceof MCBranch brInstr) {
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
                                if (instr2 instanceof MCLoad loadInstr) {
                                    loadInstr.setCond(getOppoCond.apply(loadInstr.getCond()));
                                } else if (instr2 instanceof MCStore storeInstr) {
                                    storeInstr.setCond(getOppoCond.apply(storeInstr.getCond()));
                                } else if (instr2 instanceof MCFma fmaInstr) {
                                    fmaInstr.setCond(getOppoCond.apply(fmaInstr.getCond()));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void trivialPeephole(CodeGenManager manager) {
        for (var func : manager.getMachineFunctions()) {
            for (var blockEntry : func.getmbList()) {
                var block = blockEntry.getVal();

                for (var instrEntryIter = block.getmclist().iterator(); instrEntryIter.hasNext(); ) {
                    var instrEntry = instrEntryIter.next();
                    var instr = instrEntry.getVal();

                    if (instr instanceof MCBinary binInstr) {
                        // add(sub) dst dst 0 (to be remove)
                        boolean isAddSub = binInstr.getTag() == MachineCode.TAG.Add ||
                                binInstr.getTag() == MachineCode.TAG.Sub;
                        boolean isSameDstLhs = binInstr.getDst().equals(binInstr.getLhs());
                        boolean hasNoShift = binInstr.getShift().isNone();

                        if (isAddSub && isSameDstLhs && hasNoShift) {
                            instrEntryIter.remove();
                        }
                    }

                    if (instr instanceof MCJump jumpInstr) {
                        // B1:
                        // jump target (to be remove)
                        // target:
                        boolean isSameTargetNxtBB = jumpInstr.getTarget().equals(block.getTrueSucc());

                        if (isSameTargetNxtBB) {
                            instrEntryIter.remove();
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
                                instrEntryIter.remove();
                            }
                        }
                    }

                    if (instr instanceof MCMove moveInstr) {
                        var preInstrEntry = instrEntry.getPrev();

                        // fixme
                        if (moveInstr.getDst().equals(moveInstr.getRhs())) {
                            // move a a (to be remove)
                            instrEntryIter.remove();
                        } else if (preInstrEntry.getVal() instanceof MCMove preMove) {
                            // move a b (to be remove)
                            // move a c
                            // Warning: the following situation should not be optimized
                            // move a b
                            // move a a
                            boolean isSameDst = preMove.getDst().equals(moveInstr.getDst());
                            boolean preInstrNotIdentity = preMove.getRhs().equals(preMove.getDst());
                            if (isSameDst && preInstrNotIdentity) {
                                instrEntryIter.remove();
                            }

                            // move a b
                            // move b a
                            // =>
                            // move a b
                            boolean isSameA = moveInstr.getDst().equals(preMove.getRhs());
                            boolean isSameB = moveInstr.getRhs().equals(preMove.getDst());
                            if (isSameA && isSameB) {
                                preInstrEntry.removeSelf();
                            }
                        }
                    }
                }
            }
        }
    }

    public void peepholeOpt(CodeGenManager manager, optType type) {
        if (type.equals(optType.trivial)) {
            trivialPeephole(manager);
        } else if (type.equals(optType.ifToCond)) {
            ifToCond(manager);
        }
    }
}
