package ir.types;
/**
 * Int 类型，numBits 为所含位数
 */
public class IntegerType extends Type {

  //int 32
  private static IntegerType i32 = new IntegerType(32);
  //bool
  private static IntegerType i1 = new IntegerType(1);

  public IntegerType(int numBits) {
    this.numBits = numBits;
  }

  public int getNumBits() {
    return numBits;
  }

  private int numBits;
}
