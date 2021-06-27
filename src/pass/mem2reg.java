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
import java.util.logging.Logger;
import pass.Pass.IRPass;
import util.IList;
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

    IList.INode<Function, MyModule> iterator;
    IList.INode<Function, MyModule> head = m.__functions.getEntry();
    for (iterator = head; iterator.getNext() != null; iterator = iterator.getNext()) {
      runMem2reg(iterator.getVal());
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
    IList.INode<BasicBlock, Function> bbIterator;
    IList.INode<BasicBlock, Function> bbHead = func.getList_().getEntry();
    for (bbIterator = bbHead; bbIterator.getNext() != null; bbIterator = bbIterator.getNext()) {
      BasicBlock bb = bbIterator.getVal();
      IList.INode<Instruction, BasicBlock> instIterator;
      IList.INode<Instruction, BasicBlock> instHead = bb.getList().getEntry();
      for (instIterator = instHead; instIterator.getNext() != null; instIterator = instIterator.getNext()) {
        Instruction inst = instIterator.getVal();
        if (inst instanceof AllocaInst) {
          AllocaInst allocaInst = (AllocaInst) inst;
          if (allocaInst.getAllocatedType().equals(factory.getI32Ty()))
            defs.put(allocaInst, new ArrayList<>());
        }
      }
    }




    // insert phi-instructions
    log.info("mem2reg: inserting phi-instructions");


    // variable renaming
    log.info("mem2reg: variable renaming");

  }
}
