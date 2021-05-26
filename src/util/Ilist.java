package util;

import ir.values.instructions.Instruction;

/**
 * LinkedList,猫猫都不用
 */
public interface Ilist<T> {

  public void insertBefore(T t);

  public void insertAfter(T t);

  public void removeSelf();

  public T getPrev();

  public T getNext();
}
