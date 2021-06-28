package pass;

import ir.CFGInfo;
import ir.MyFactoryBuilder;
import ir.MyModule;
import ir.types.IntegerType;
import ir.values.BasicBlock;
import ir.values.Function;
import ir.values.instructions.Instruction;
import ir.values.instructions.MemInst.AllocaInst;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;
import pass.Pass.IRPass;
import util.IList;
import util.IList.INode;
import util.Mylogger;

public class mem2reg implements IRPass {

  Logger log = Mylogger.getLogger(IRPass.class);
  MyFactoryBuilder factory = MyFactoryBuilder.getInstance();

  @Override
  public String getName() {
    return "mem2reg";
  }

  public void run(MyModule m) {
    log.info("Running pass : mem2reg");

    for (INode<Function, MyModule> funcNode : m.__functions) {
      runMem2reg(funcNode.getVal());
    }
  }

  public void runMem2reg(Function func) {
    // prepare
    log.info("Running compute dominance info");
    CFGInfo.computeDominanceInfo(func);
    log.info("Running compute dominance frontier");
    CFGInfo.computeDominanceFrontier(func);

    HashMap<AllocaInst, ArrayList<BasicBlock>> defs = new HashMap<AllocaInst, ArrayList<BasicBlock>>();

    // initialize `defs`
    for (INode<BasicBlock, Function> bbNode : func.getList_()) {
      BasicBlock bb = bbNode.getVal();
      for (INode<Instruction, BasicBlock> instNode : bb.getList()) {
        Instruction inst = instNode.getVal();
        if (inst instanceof AllocaInst) {
          AllocaInst allocaInst = (AllocaInst) inst;
          if (allocaInst.getAllocatedType().equals(factory.getI32Ty())) {
            defs.put(allocaInst, new ArrayList<>());
            // TODO: only int
          }
        }
      }
    }

    // insert phi-instructions
    log.info("mem2reg: inserting phi-instructions");
    Queue<BasicBlock> W = new LinkedList<>();
    for (AllocaInst allocaInst : defs.keySet()) {
      for (BasicBlock bb: defs.get(allocaInst)) {
        W.add(bb);
      }

      while (!W.isEmpty()) {
        BasicBlock bb = W.remove();
        for (BasicBlock y: bb.getDominanceFrontier()) {
          // TODO: if y not visited, insert phi
          W.add(y);
        }
      }
    }


    // variable renaming
    log.info("mem2reg: variable renaming");

  }
}
