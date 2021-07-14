package ir.values.instructions;

import ir.Use;
import ir.types.ArrayType;
import ir.types.PointerType;
import ir.types.Type;
import ir.types.Type.VoidType;
import ir.values.BasicBlock;
import ir.values.GlobalVariable;
import ir.values.Value;
import java.util.ArrayList;
import java.util.Set;

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

  public static class ZextInst extends MemInst {

    private Type destTy;

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


  }


  public static class AllocaInst extends MemInst {

    //todo typecheck
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

    private Type allocatedType_;
    private boolean isInit = false;
  }

  public static class LoadInst extends MemInst {

    public Use useStore;

    //todo typecheck
    public LoadInst(Type type, Value v/**指针*/) {
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
  }

  public static class StoreInst extends MemInst {

    //todo typecheck
    public StoreInst(Value val, Value pointer) {
      super(TAG_.Store, VoidType.getType(), 2);
      CoSetOperand(0, val);
      CoSetOperand(1, pointer);
    }

    public StoreInst(Value val, Value pointer, BasicBlock parent) {
      super(TAG_.Store, VoidType.getType(), 2, parent);
      CoSetOperand(0, val);
      CoSetOperand(1, pointer);
    }

    public StoreInst(Value val, Value pointer, Instruction prev) {
      super(TAG_.Store, VoidType.getType(), 2, prev);
      CoSetOperand(0, val);
      CoSetOperand(1, pointer);
    }

    public StoreInst(Instruction next, Value val, Value pointer) {
      super(next, TAG_.Store, VoidType.getType(), 2);
      CoSetOperand(0, val);
      CoSetOperand(1, pointer);
    }
  }

  public static class GEPInst extends MemInst {

    //todo

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
        for (int i = 0; i < indices.size(); i++) {
          type = ((ArrayType) type).getELeType();
          assert (i < indices.size() - 1) || !type.isArrayTy();
        }
        return type;
      } else {
        //todo typecheck
        return null;
      }
    }

    public GEPInst(Value pointer, ArrayList<Value> indices) {
      super(TAG_.GEP, new PointerType(getElementType(pointer, indices)), indices.size() + 1);
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
    private Type elementType_;
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

    public ArrayList<Value> getIncomingVals() {
      return operands;
    }

    public void setIncomingVals(int index, Value val) {
      CoSetOperand(index, val);
    }
  }

  public static class MemPhi extends MemInst {

    public MemPhi(TAG_ tag, Type type, int numOP) {
      super(tag, type, numOP);
    }

    // MemPhi 指令需要放在基本块最前面
    public MemPhi(TAG_ tag, Type type, int numOP, Value array, BasicBlock parent) {
      super(tag, type, numOP);
      this.numOP = parent.getPredecessor_().size() + 1;
      CoSetOperand(0, array); // operands[0]: array
      this.node.insertAtEntry(parent.getList());
    }

    public void setIncomingVals(int index, Value val) {
      // operands[0] is array
      CoSetOperand(index + 1, val);
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
