package pass.mc;

import backend.CodeGenManager;
import backend.machinecodes.ArmAddition;
import backend.machinecodes.MCBranch;
import backend.machinecodes.MCCall;
import backend.machinecodes.MCComment;
import backend.machinecodes.MCCompare;
import backend.machinecodes.MCJump;
import backend.machinecodes.MachineBlock;
import backend.machinecodes.MachineCode;
import java.util.ArrayList;
import pass.Pass;

/**
 * 如果基本块只有一个后继，且指令数量较少，没有cmp指令，
 * 那么可以将其复制一份放到其前驱的末尾中
 * 如果基本块有两个后继，但其前驱只有一个基本块，那将基本块复制一份
 * 放到前驱的末尾
 */

public class MergeMachineBlock implements Pass.MCPass {
    @Override
    public String getName() {
        return "MergeMachineBlock";
    }

    @Override
    public void run(CodeGenManager manager) {
        for (var mf : manager.getMachineFunctions()) {
            var mbEntry = mf.getmbList().getLast();
            while (mbEntry != null) {
                var mb = mbEntry.getVal();
                var currentMbEntry = mbEntry;
                mbEntry = mbEntry.getPrev();
                if (mb.getmclist().getLast() == null) {
                    continue;
                }
                boolean hasCompare = false;
                boolean hasCond = false;
                boolean hasCall = false;
                int branchNum = 0;
                int jumpNum = 0;
                int mcNum = 0;//非comment数量
                for (var mcEntry : mb.getmclist()) {
                    var mc = mcEntry.getVal();
                    if (!(mc instanceof MCComment)) {
                        mcNum++;
                        if (mc.getCond() != ArmAddition.CondType.Any) {
                            hasCond = true;
                        }
                        if (mc instanceof MCCompare) {
                            hasCompare = true;
                        } else if (mc instanceof MCCall) {
                            hasCall = true;
                        } else if (mc instanceof MCJump) {
                            jumpNum++;
                        } else if (mc instanceof MCBranch) {
                            branchNum++;
                        }
                    }
                }
                ArrayList<MachineBlock> predToRemove = new ArrayList<>();
                //有且只有一个后继
                if (mb.getFalseSucc() == null && mb.getTrueSucc() != null) {
                    for (var pred : mb.getPred()) {
                        //如果pred只有一个后继，且线性化后本块不是pred的下一个块
                        //或者如果本块是pred的false后继，且线性化后本块不是pred的下一个块
                        if ((pred.getFalseSucc() == null || pred.getFalseSucc() == mb )
                        && pred.getNode() != currentMbEntry.getPrev()) {
                            assert (pred.getmclist().getLast().getVal() instanceof MCJump);
                            var jump = (MCJump) (pred.getmclist().getLast().getVal());
                            var mcEntry = mb.getmclist().getEntry();
                            while (mcEntry != null) {
                                var mc = mcEntry.getVal();
                                mcEntry = mcEntry.getNext();
                                if (!(mc instanceof MCJump) && !(mc instanceof MCComment)) {
                                    mc.insertBeforeNode(jump);
                                }
                            }
                            if (pred.getNode().getNext() != null && mb.getTrueSucc() == pred.getNode().getNext().getVal()) {
                                jump.getNode().removeSelf();
                            } else {
                                jump.setTarget(mb.getTrueSucc());
                            }
                            predToRemove.add(pred);
                            if (pred.getFalseSucc() == null) {
                                pred.setTrueSucc(mb.getTrueSucc());
                            } else {
                                pred.setFalseSucc(mb.getTrueSucc());
                            }
                            mb.getTrueSucc().addPred(pred);
                        }
                        //如果pred有两个后继，且本块是pred的True后继
                        //此处的合并基本块有可能损失性能
//                        else if (false) {
                        if (pred.getTrueSucc() == mb && pred.getFalseSucc()!=null) {
                            if (hasCompare || hasCall || hasCond || (mcNum - branchNum - jumpNum) > 5) {
                                continue;
                            }
                            predToRemove.add(pred);
                            var lastEntry = pred.getmclist().getLast();
                            var branch = lastEntry.getVal();
                            while (lastEntry != null) {
                                var mc = lastEntry.getVal();
                                lastEntry = lastEntry.getPrev();
                                if (mc instanceof MCBranch && ((MCBranch) mc).getTarget() == mb) {
                                    branch = mc;
                                }
                            }
                            if(!(branch instanceof MCBranch) && pred.getFalseSucc()==pred.getTrueSucc()){
                                continue;
                            }
                            assert (branch instanceof MCBranch);
                            var mcEntry = mb.getmclist().getEntry();
                            while (mcEntry != null) {
                                var mc = mcEntry.getVal();
                                mcEntry = mcEntry.getNext();
                                if (!(mc instanceof MCJump) && !(mc instanceof MCComment)) {
                                    MachineCode m = (MachineCode) (mc.clone());
                                    m.genNewNode();
                                    m.setCond(branch.getCond());
                                    m.insertBeforeNode(branch);
                                }
                            }
                            ((MCBranch) branch).setTarget(mb.getTrueSucc());
                            pred.setTrueSucc(mb.getTrueSucc());
                            if (pred.getTrueSucc() == pred.getFalseSucc()) {
                                if (branch == pred.getmclist().getLast().getVal()) {
                                    branch.getNode().removeSelf();
                                }

                            }
                            mb.getTrueSucc().addPred(pred);
                        }
                    }
                }
                //如果有两个后继
                else if (mb.getFalseSucc() != null && mb.getTrueSucc() != null) {
                    if (!(mb.getmclist().getLast().getVal() instanceof MCJump)) {
                        continue;
                    }
                    for (var pred : mb.getPred()) {
                        //如果线性化后pred就是本块的上一个块
                        if (pred.getNode() == currentMbEntry.getPrev()) {
                            continue;
                        }
                        //如果pred只有一个后继，且线性化后本块不是pred的下一个块
                        //或者如果本块是pred的false后继，且线性化后本块不是pred的下一个块
                        else if (pred.getFalseSucc() == null || pred.getFalseSucc() == mb) {
                            assert (pred.getmclist().getLast().getVal() instanceof MCJump);
                            var jump = (MCJump) (pred.getmclist().getLast().getVal());
                            var mcEntry = mb.getmclist().getEntry();
                            while (mcEntry != null) {
                                var mc = mcEntry.getVal();
                                mcEntry = mcEntry.getNext();
                                if (!(mc instanceof MCComment)) {
                                    mc.insertBeforeNode(jump);
                                }
                            }
                            jump.getNode().removeSelf();
                            predToRemove.add(pred);
                            pred.setTrueSucc(mb.getTrueSucc());
                            pred.setFalseSucc(mb.getTrueSucc());
                            mb.getTrueSucc().addPred(pred);
                            mb.getFalseSucc().addPred(pred);
                        }
                    }
                }
                for (var b : predToRemove) {
                    if(b.getTrueSucc()!=mb&&b.getFalseSucc()!=mb){
                        mb.getPred().remove(b);
                    }
                }
                if (mb.getPred().isEmpty() && mb != mf.getmbList().getEntry().getVal()) {
                    mb.getNode().removeSelf();
                    mb.getTrueSucc().removePred(mb);
                    if (mb.getFalseSucc() != null) {
                        mb.getFalseSucc().removePred(mb);
                    }
                }

            }
        }
    }
}

