package ir.values.instructions;

import ir.types.Type;
import ir.types.VectorType;
import ir.values.BasicBlock;
import ir.values.Value;

public class VectorInstruction extends Instruction {

  public VectorInstruction(TAG_ tag, Type type, int numOP) {
    super(tag, type, numOP);
  }

  public static class InsertEleInst extends VectorInstruction {

    public InsertEleInst(Value pointer, Value value, Value idx) {
      super(TAG_.InsertEle, pointer.getType(), 3);
      assert pointer.getType() instanceof VectorType;
      this.CoSetOperand(0, pointer);
      this.CoSetOperand(1, value);
      this.CoSetOperand(2, idx);
    }
  }

  public static class ExtractEleInst extends VectorInstruction {

    public ExtractEleInst(Value val, Value idx) {
      super(TAG_.ExtractEle, ((VectorType) val.getType()).getEleType(), 2);
      this.CoSetOperand(0, val);
      this.CoSetOperand(1, idx);
    }
  }


}
