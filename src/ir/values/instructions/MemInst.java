package ir.values.instructions;

import ir.types.Type;

public class MemInst {

  public static class AllocaInst {


    public AllocaInst(Type type) {

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

}
