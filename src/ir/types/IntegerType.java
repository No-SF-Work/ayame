package ir.types;

/**
 * Int 类型，numBits 为所含位数
 */
public class IntegerType extends Type {

  //int 32
  public static final IntegerType i32 = new IntegerType(32);
  //bool
  public static final IntegerType i1 = new IntegerType(1);

  public static IntegerType getI32() {
    return i32;
  }

  public static IntegerType getI1() {
    return i1;
  }

  public IntegerType(int numBits) {
    this.numBits = numBits;
  }

  public int getNumBits() {
    return numBits;
  }

  private int numBits;

  @Override
  public String toString() {
    if (this.numBits == 32) {
      return "i32";
    } else {
      return "i1";
    }
  }
}
