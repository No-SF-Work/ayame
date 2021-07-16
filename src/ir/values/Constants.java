package ir.values;

import ir.types.ArrayType;
import ir.types.IntegerType;
import ir.types.Type;
import java.util.ArrayList;

public class Constants {

  public static class ConstantInt extends Constant {

    private static final ConstantInt c0_ = new ConstantInt(IntegerType.getI32(), 0);

    public static ConstantInt newOne(Type type, int val) {
      return new ConstantInt(type, val);
    }

    private ConstantInt(Type type, int val) {
      super(type);
      this.val = val;
    }

    public static ConstantInt CONST0() {
      return c0_;//太常用了
    }

    public int getVal() {
      return val;
    }

    public void setVal(int val) {
      this.val = val;
    }

    @Override
    public String getName() {
      return String.valueOf(this.getVal());
    }

    private int val;
  }


  public static class ConstantArray extends Constant {

    public ConstantArray(Type type, ArrayList<Constant> arr) {
      super(type, arr.size());
      for (int i = 0; i < arr.size(); i++) {
        this.CoSetOperand(i, arr.get(i));
      }
      const_arr_ = new ArrayList<>(arr);
    }

    public ArrayList<Constant> getConst_arr_() {
      return const_arr_;
    }

    private ArrayList<Constant> const_arr_;

    @Override
    public String toString() {
      return ArrayType.buildConstInitStr((ArrayType) this.getType(), this);
    }

    public ArrayList<Integer> getDims() {
      ArrayList<Integer> tmp = new ArrayList<>();
      ConstantArray arr = this;
      while (true) {
        tmp.add(arr.const_arr_.size());
        if (arr.const_arr_.get(0) instanceof ConstantInt) {
          break;
        }
        arr = (ConstantArray) const_arr_.get(0);
      }
      return tmp;
    }
  }

}
