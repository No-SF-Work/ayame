package util;

import ir.values.instructions.Instruction;

/**
 * LinkedList,猫猫都不用
 */
public interface Ilist<T> {

  void insertBefore(T t);

  void insertAfter(T t);

  void removeSelf();

  T getPrev();

  T getNext();
}
