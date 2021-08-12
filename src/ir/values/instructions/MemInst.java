package ir.values.instructions;

import ir.types.ArrayType;
import ir.types.PointerType;
import ir.types.Type;
import ir.types.Type.VoidType;
import ir.values.BasicBlock;
import ir.values.Constant;
import ir.values.Constants.ConstantInt;
import ir.values.GlobalVariable;
import ir.values.UndefValue;
import ir.values.Value;
import java.util.ArrayList;
import java.util.stream.Collectors;

public abstract class MemInst extends Instruction {

  public MemInst(TAG_ tag, Type type, int numOP) {
    super(tag, type, numOP);
  }

  public MemInst(TAG_ tag, Type type, int numOP, BasicBlock parent) {
    super(tag, type, numOP, parent);
  }

  public MemInst(TAG_ tag, Type type, int numOP, Instruction prev) {
    super(tag, type, numOP, prev);
  }

  public MemInst(Instruction next, TAG_ tag, Type type, int numOP) {
    super(next, tag, type, numOP);
  }

  public static class AllocaInst extends MemInst {

    public AllocaInst(Type allocatedType_) {
      super(TAG_.Alloca, new PointerType(allocatedType_), 0);
      this.allocatedType_ = allocatedType_;
    }

    public AllocaInst(BasicBlock parent, Type allocatedType_) {
      super(TAG_.Alloca, new PointerType(allocatedType_), 0, parent);
      this.allocatedType_ = allocatedType_;
    }

    public AllocaInst(Instruction prev, Type allocatedType_) {
      super(TAG_.Alloca, new PointerType(allocatedType_), 0, prev);
      this.allocatedType_ = allocatedType_;
    }

    public AllocaInst(Type allocatedType_, Instruction next) {
      super(next, TAG_.Alloca, new PointerType(allocatedType_), 0);
      this.allocatedType_ = allocatedType_;
    }

    public Type getAllocatedType() {
      return allocatedType_;
    }

    public void setInit() {
      isInit = true;
    }

    public boolean isInit() {
      return isInit;
    }

    private final Type allocatedType_;
    private boolean isInit = false;

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(this.getName())
          .append(" = ")
          .append("alloca")
          .append(" ")
          .append(allocatedType_);
      return sb.toString();
    }
  }

  public static class LoadInst extends MemInst {

    //todo typecheck
    public LoadInst(Type type, Value v) {/**指针*/
      super(TAG_.Load, type, 1);
      CoSetOperand(0, v);
    }

    public LoadInst(Type type, Value v, BasicBlock parent) {
      super(TAG_.Load, type, 1, parent);
      CoSetOperand(0, v);
    }

    public LoadInst(Type type, Value v, Instruction prev) {
      super(TAG_.Load, type, 1, prev);
      CoSetOperand(0, v);
    }

    public LoadInst(Instruction next, Value v, Type type) {
      super(next, TAG_.Load, type, 1);
      CoSetOperand(0, v);
    }

    public void setUseStore(Value store) {
      if (this.numOP == 1) {
        if (this.operands.size() == 1) {
          this.COaddOperand(store);
        } else {
          this.numOP++;
          this.CoSetOperand(1, store);
        }
      } else {
        this.CoSetOperand(1, store);
      }
    }

    public Value getUseStore() {
      return this.getOperands().get(1);
    }

    public void removeUseStore() {
      if (this.numOP == 1) {
        return;
      } else if (this.getUseStore() == null) {
        this.numOP--;
        return;
      }
      this.getUseStore().removeUseByUser(this);
      CoSetOperand(1, null);
      this.operands.remove(1);
      this.numOP--;
    }

    public Value getPointer() {
      return this.getOperands().get(0);
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(this.getName())
          .append(" = ")
          .append(" load ")
          .append(this.getType())
          .append(",")
          .append(operands.get(0).getType())
          .append(" ")
          .append(operands.get(0).getName());
      return sb.toString();
    }
  }

  public static class StoreInst extends MemInst {

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      var lhs = operands.get(0);
      var rhs = operands.get(1);
      sb.append("store " + lhs.getType().toString() + " " + lhs.getName() + ", ");
      sb.append(rhs.getType().toString() + " " + rhs.getName());
      return sb.toString();
    }

    //todo typecheck
    public StoreInst(Value val, Value pointer) {
      super(TAG_.Store, VoidType.getType(), 2);
      CoSetOperand(0, val);
      CoSetOperand(1, pointer);
      needname = false;
    }

    public StoreInst(Value val, Value pointer, BasicBlock parent) {
      super(TAG_.Store, VoidType.getType(), 2, parent);
      CoSetOperand(0, val);
      CoSetOperand(1, pointer);
      needname = false;
    }

    public StoreInst(Value val, Value pointer, Instruction prev) {
      super(TAG_.Store, VoidType.getType(), 2, prev);
      CoSetOperand(0, val);
      CoSetOperand(1, pointer);
      needname = false;
    }

    public StoreInst(Instruction next, Value val, Value pointer) {
      super(next, TAG_.Store, VoidType.getType(), 2);
      CoSetOperand(0, val);
      CoSetOperand(1, pointer);
      needname = false;
    }

    public Value getVal() {
      return this.getOperands().get(0);
    }

    public Value getPointer() {
      return this.getOperands().get(1);
    }

    public boolean hasAlias;
  }

  public static class GEPInst extends MemInst {

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(this.getName() + "= getelementptr " + ((PointerType) operands.get(0).getType())
          .getContained() + "," + operands.get(0)
          .getType() + " " + operands.get(0).getName() + " ");
      for (var i = 1; i < operands.size(); i++) {
        sb.append(", " + operands.get(i).getType() + " " + operands.get(i).getName());
      }
      return sb.toString();
    }

    /**
     * 拿到这个pointer指向的数组/值的指针的type
     * <p>
     */
    private static Type getElementType(Value ptr, ArrayList<Value> indices) {
      assert ptr.getType().isPointerTy();
      Type type = ((PointerType) ptr.getType()).getContained();
      if (type.isIntegerTy()) {
        return type;
      }
      if (type.isArrayTy()) {
        for (int i = 1; i < indices.size(); i++) {
          type = ((ArrayType) type).getELeType();
          assert (i < indices.size()) || !type.isArrayTy();
        }
        return type;
      } else {
        //todo typecheck
        return null;
      }
    }

    public GEPInst(Value pointer, ArrayList<Value> indices) {
      super(TAG_.GEP, new PointerType(getElementType(pointer, indices)), indices.size() + 1);
      aimTo= ConstantInt.CONST0();
      if (pointer instanceof GEPInst) {
        aimTo = ((GEPInst) pointer).aimTo;
      }
      if (pointer instanceof AllocaInst) {
        aimTo = pointer;
      }
      if (pointer instanceof GlobalVariable) {
        aimTo = pointer;
      }
      CoSetOperand(0, pointer);
      for (int i = 0; i < indices.size(); i++) {
        CoSetOperand(i + 1, indices.get(i));
      }
      elementType_ = getElementType(pointer, indices);
    }

    public GEPInst(Value pointer, ArrayList<Value> indices, BasicBlock parent) {
      super(TAG_.GEP, new PointerType(getElementType(pointer, indices)), indices.size() + 1,
          parent);
      aimTo= ConstantInt.CONST0();
      if (pointer instanceof GEPInst) {
        aimTo = ((GEPInst) pointer).aimTo;
      }
      if (pointer instanceof AllocaInst) {
        aimTo = pointer;
      }
      if (pointer instanceof GlobalVariable) {
        aimTo = pointer;
      }
      CoSetOperand(0, pointer);
      for (int i = 0; i < indices.size(); i++) {
        CoSetOperand(i + 1, indices.get(i));
      }
      elementType_ = getElementType(pointer, indices);
    }

    public GEPInst(Instruction prev, Value pointer, ArrayList<Value> indices) {
      super(TAG_.GEP, new PointerType(getElementType(pointer, indices)), indices.size() + 1, prev);
      aimTo= ConstantInt.CONST0();
      if (pointer instanceof GEPInst) {
        aimTo = ((GEPInst) pointer).aimTo;
      }
      if (pointer instanceof AllocaInst) {
        aimTo = pointer;
      }
      if (pointer instanceof GlobalVariable) {
        aimTo = pointer;
      }
      CoSetOperand(0, pointer);
      for (int i = 0; i < indices.size(); i++) {
        CoSetOperand(i + 1, indices.get(i));
      }
      elementType_ = getElementType(pointer, indices);
    }


    public GEPInst(Value pointer, ArrayList<Value> indices, Instruction next) {
      super(next, TAG_.GEP, new PointerType(getElementType(pointer, indices)), indices.size() + 1);
      aimTo= ConstantInt.CONST0();
      if (pointer instanceof GEPInst) {
        aimTo = ((GEPInst) pointer).aimTo;
      }
      if (pointer instanceof AllocaInst) {
        aimTo = pointer;
      }
      if (pointer instanceof GlobalVariable) {
        aimTo = pointer;
      }
      CoSetOperand(0, pointer);
      for (int i = 0; i < indices.size(); i++) {
        CoSetOperand(i + 1, indices.get(i));
      }
      elementType_ = getElementType(pointer, indices);
    }

    public Type getElementType_() {
      return elementType_;
    }

    public Value getAimTo() {
      return this.aimTo;
    }

    private Value aimTo;
    private final Type elementType_;
  }

  public static class Phi extends MemInst {

    public Phi(TAG_ tag, Type type, int numOP) {
      super(tag, type, numOP);
    }

    // Phi 指令需要放在基本块最前面
    public Phi(TAG_ tag, Type type, int numOP, BasicBlock parent) {
      super(tag, type, numOP);
      this.node.insertAtEntry(parent.getList());
      this.numOP = parent.getPredecessor_().size();
    }

    public Phi(TAG_ tag, Type type, int numOP, Instruction prev) {
      super(tag, type, numOP, prev);
    }

    public Phi(Instruction next, TAG_ tag, Type type, int numOP) {
      super(next, tag, type, numOP);
    }

    public Phi(TAG_ tag, Type type, int numOP, ArrayList<Value> incomingVals) {
      super(tag, type, numOP);
      for (int i = 0; i < incomingVals.size(); i++) {
        this.CoSetOperand(i, incomingVals.get(i));
      }
    }

    public ArrayList<Value> getIncomingVals() {
      return operands;
    }

    public void setIncomingVals(int index, Value val) {
      CoSetOperand(index, val);
    }

    public void removeIncomingVals(int index) {
      int[] indexArr = {index};
      this.CORemoveNOperand(indexArr);
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(this.getName() + " = phi i32 ");
      for (int i = 0; i < operands.size(); i++) {
        sb.append("[ ")
            .append(operands.get(i).getName())
            .append(", %")
            .append(this.node.getParent().getVal().getPredecessor_().get(i).getName())
            .append(" ],");
      }
      sb.deleteCharAt(sb.length() - 1);
      return sb.toString();
    }
  }


  public static class MemPhi extends MemInst {

    public MemPhi(TAG_ tag, Type type, int numOP) {
      super(tag, type, numOP);
    }

    // MemPhi 指令需要放在基本块最前面
    public MemPhi(TAG_ tag, Type type, int numOP, Value array, BasicBlock parent) {
      super(tag, type, numOP);
      CoSetOperand(0, array); // operands[0]: array
      for (int i = 1; i < this.numOP; i++) {
        this.getOperands().add(new UndefValue());
      }
      this.node.insertAtEntry(parent.getList());
    }

    public void setIncomingVals(int index, Value val) {
      // operands[0] is array
      CoSetOperand(index + 1, val);
    }

    public ArrayList<Value> getIncomingVals() {
      return this.operands.stream().skip(1).collect(Collectors.toCollection(ArrayList::new));
    }
  }

  public static class ZextInst extends MemInst {

    private Type destTy;// only i32

    public ZextInst(Value val, Type dest) {
      super(TAG_.Zext, dest, 1);
      destTy = dest;
      this.CoSetOperand(0, val);
    }

    public Type getDest() {
      return this.destTy;
    }

    public ZextInst(Value val, Type dest, BasicBlock parent) {
      super(TAG_.Zext, dest, 1, parent);
      destTy = dest;
      this.CoSetOperand(0, val);
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(this.getName())
          .append(" = ")
          .append("zext i1 ")
          .append(operands.get(0).getName())
          .append(" to i32");
      return sb.toString();
    }
  }

  public static class LoadDepInst extends MemInst {

    public LoadDepInst(Instruction next, TAG_ tag, Type type, int numOP) {
      super(next, tag, type, numOP);
    }

    public void setLoadDep(Value val) {
      this.numOP = 1;
      this.CoSetOperand(0, val);
    }

    public void removeLoadDep() {
      this.CORemoveAllOperand();
      this.COReplaceAllUseWith(null);
    }
  }

  public boolean isGEP() {
    return this.tag == TAG_.GEP;
  }

  public boolean isLoad() {
    return this.tag == TAG_.Load;
  }

  public boolean isStore() {
    return this.tag == TAG_.Store;
  }

  public boolean isPhi() {
    return this.tag == TAG_.Phi;
  }
}
