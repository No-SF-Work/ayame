package ir.values.instructions;

import ir.types.Type;
import ir.values.BasicBlock;

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

    //todo
    public AllocaInst(TAG_ tag, Type type, int numOP, Type allocatedType_) {
      super(tag, type, numOP);
      this.allocatedType_ = allocatedType_;
    }

    public AllocaInst(TAG_ tag, Type type, int numOP, BasicBlock parent, Type allocatedType_) {
      super(tag, type, numOP, parent);
      this.allocatedType_ = allocatedType_;
    }

    public AllocaInst(TAG_ tag, Type type, int numOP, Instruction prev, Type allocatedType_) {
      super(tag, type, numOP, prev);
      this.allocatedType_ = allocatedType_;
    }

    public AllocaInst(Instruction next, TAG_ tag, Type type, int numOP, Type allocatedType_) {
      super(next, tag, type, numOP);
      this.allocatedType_ = allocatedType_;
    }

    public Type getAllocatedType() {
      return allocatedType_;
    }

    public void setInit(boolean init) {
      isInit = true;
    }

    public boolean isInit() {
      return isInit;
    }

    private Type allocatedType_;
    private boolean isInit = false;
  }

  public static class LoadInst extends MemInst {

    //todo
    public LoadInst(TAG_ tag, Type type, int numOP) {
      super(tag, type, numOP);
    }

    public LoadInst(TAG_ tag, Type type, int numOP, BasicBlock parent) {
      super(tag, type, numOP, parent);
    }

    public LoadInst(TAG_ tag, Type type, int numOP, Instruction prev) {
      super(tag, type, numOP, prev);
    }

    public LoadInst(Instruction next, TAG_ tag, Type type, int numOP) {
      super(next, tag, type, numOP);
    }
  }

  public static class StoreInst extends MemInst {

    //todo
    public StoreInst(TAG_ tag, Type type, int numOP) {
      super(tag, type, numOP);
    }

    public StoreInst(TAG_ tag, Type type, int numOP, BasicBlock parent) {
      super(tag, type, numOP, parent);
    }

    public StoreInst(TAG_ tag, Type type, int numOP, Instruction prev) {
      super(tag, type, numOP, prev);
    }

    public StoreInst(Instruction next, TAG_ tag, Type type, int numOP) {
      super(next, tag, type, numOP);
    }
  }

  public static class GEP extends MemInst {

    //todo
    public GEP(TAG_ tag, Type type, int numOP) {
      super(tag, type, numOP);
    }

    public GEP(TAG_ tag, Type type, int numOP, BasicBlock parent) {
      super(tag, type, numOP, parent);
    }

    public GEP(TAG_ tag, Type type, int numOP, Instruction prev) {
      super(tag, type, numOP, prev);
    }

    public GEP(Instruction next, TAG_ tag, Type type, int numOP) {
      super(next, tag, type, numOP);
    }
  }

  public static class Phi extends MemInst {

    //todo
    public Phi(TAG_ tag, Type type, int numOP) {
      super(tag, type, numOP);
    }

    public Phi(TAG_ tag, Type type, int numOP, BasicBlock parent) {
      super(tag, type, numOP, parent);
    }

    public Phi(TAG_ tag, Type type, int numOP, Instruction prev) {
      super(tag, type, numOP, prev);
    }

    public Phi(Instruction next, TAG_ tag, Type type, int numOP) {
      super(next, tag, type, numOP);
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
