package ir;

import ir.values.BasicBlock;
import ir.values.Function;
import ir.values.instructions.BinaryInst;
import ir.values.instructions.Instruction;
import ir.values.instructions.Instruction.TAG_;
import ir.values.instructions.TerminatorInst.BrInst;
import ir.values.instructions.TerminatorInst.RetInst;

/**
 *
 **/
public class IRBuilderFactory {

  private IRBuilderFactory() {
  }

  private static IRBuilderFactory ibf = new IRBuilderFactory();

  public static IRBuilderFactory getInstance() {
    return ibf;
  }


  public BinaryInst getBinary(TAG_ tag) {
    //todo
    return null;
  }

  public void BuildBinaryAfter(Instruction inst, TAG_ tag) {
    //todo
  }

  public void BuildBinary(BasicBlock bb, TAG_ tag) {
    //todo
  }

  public void BuildBinaryBefore(Instruction inst, TAG_ tag) {
    //todo
  }


  public BrInst getBr() {
    //todo
    return null;
  }

  public void buildBr(BasicBlock bb) {
    //todo
  }

  public RetInst getRet() {
    //todo
    return null;
  }


}
