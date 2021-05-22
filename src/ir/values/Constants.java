package ir.values;

import ir.types.Type;
import java.util.ArrayList;

public class Constants {

  public static class ConstantInt extends Constant {

    public static ConstantInt newOne(Type type, int val) {
      return new ConstantInt(type, val);
    }

    private ConstantInt(Type type, int val) {
      super(type);
      this.val = val;
    }

    public int getVal() {
      return val;
    }

    public void setVal(int val) {
      this.val = val;
    }

    private int val;
  }


  public static class ConstantArray extends Constant {

    public ConstantArray(Type type, ArrayList<Constant> arr) {
      super(type, arr.size());
      for (int i = 0; i < arr.size(); i++) {
        this.CoSetOperand(i, arr.get(i));
      }
    }

  }
}
