package ir.types;

import java.util.ArrayList;

/**
 * Array type :
 * <p>
 * 多维数组用Array的嵌套结构表示
 * <p>
 */
public class ArrayType extends Type {

  /**
   * @param contained    : 这个 ArrayType 的 element 的 Type
   * @param num_elements : 这个 ArrayType 的 element 的 num
   */
  public ArrayType(Type contained, int num_elements) {

    assert num_elements >= 0;
    this.contained = contained;
    this.num_elements = num_elements;
  }

  /**
   * 返回一个包含了各个维的长度的list
   */
  public ArrayList<Integer> getDims() {
    ArrayList<Integer> tmp = new ArrayList<>();
    Type arr = this;
    while (arr.isArrayTy()) {
      tmp.add(((ArrayType) arr).getNumEle());
      arr = ((ArrayType) arr).getELeType();
    }
    return tmp;
  }

  public Type getELeType() {
    return contained;
  }

  public int getNumEle() {
    return num_elements;
  }

  private Type contained;
  private int num_elements;
}
