package util;

import java.util.Iterator;

import util.IList.INode;

/**
 * LinkedList,猫猫都不用
 */
public class IList<T, P> implements Iterable<INode<T, P>> {

  private INode<T, P> entry;
  private INode<T, P> last;
  private P val;
  private int numNode;

  public void setVal(P val) {
    this.val = val;
  }

  public int getNumNode() {
    return numNode;
  }

  public IList(P val) {
    this.val = val;
    numNode = 0;
    entry = last = null;
  }

  public P getVal() {
    return val;
  }

  public void clear() {
    this.entry = null;
    this.last = null;
    this.numNode = 0;
  }

  public INode<T, P> getEntry() {
    return entry;
  }

  public INode<T, P> getLast() {
    return last;
  }

  protected void setEntry(INode<T, P> entry) {
    this.entry = entry;
  }

  protected void setLast(INode<T, P> last) {
    this.last = last;
  }

  @Override
  public Iterator<INode<T, P>> iterator() {
    return new IIterator(entry);
  }

  class IIterator implements Iterator<INode<T, P>> {

    INode<T, P> tmp = new INode<>(null);

    IIterator(INode<T, P> head) {
      tmp.next = head;
    }

    @Override
    public boolean hasNext() {
      return tmp.next != null;
    }

    @Override
    public INode<T, P> next() {
      tmp = tmp.next;
      return tmp;
    }

    @Override
    public void remove() {
      var prev = tmp.prev;
      var next = tmp.next;
      var parent = tmp.getParent();
      if (prev != null) {
        prev.next = next;
      } else {
        parent.setEntry(next);
      }

      if (next != null) {
        next.prev = prev;
      } else {
        parent.setLast(prev);
      }
      --parent.numNode;

      tmp.next = tmp.prev = null;
      tmp.val = null;
    }
  }

  public static class INode<T, P> {

    private T val;
    private INode<T, P> prev = null;//前驱
    private INode<T, P> next = null;//后继
    private IList<T, P> parent;

    public INode(T t) {
      this.val = t;
    }

    public T getVal() {
      return val;
    }

    public void setParent(IList<T, P> parent) {
      this.parent = parent;
    }

    public IList<T, P> getParent() {
      return parent;
    }


    public void insertAtEntry(IList<T, P> father) {
      this.setParent(father);
      if (father.getEntry() == null && father.getLast() == null) {
        father.numNode++;
        father.setEntry(this);
        father.setLast(this);
      } else {
        insertBefore(father.getEntry());
      }
    }

    public void insertAtEnd(IList<T, P> father) {
      this.setParent(father);
      if (father.getEntry() == null && father.getLast() == null) {
        father.numNode++;
        father.setEntry(this);
        father.setLast(this);
      } else {
        insertAfter(father.getLast());
      }
    }


    //将自己从链表中移除
    public INode<T, P> removeSelf() {
      this.parent.numNode--;
      if (parent.getEntry() == this) {
        parent.setEntry(this.next);
      }
      if (parent.getLast() == this) {
        parent.setLast(this.prev);
      }
      if (this.prev != null && this.next != null) {
        this.prev.next = this.next;
        this.next.prev = this.prev;

      } else if (this.prev == null) {
        this.next.prev = null;
      } else {
        this.prev.next = null;
      }
      this.prev = null;
      this.next = null;
      this.parent = null;
      return this;
    }

    //insert my self before next node
    public void insertBefore(INode<T, P> next) {
      this.parent = next.parent;
      this.parent.numNode++;
      if (next.getParent().getEntry() == next) {
        next.getParent().setEntry(this);
      }
      this.prev = next.prev;
      this.next = next;
      next.prev = this;

    }

    //insert my self after prev node
    public void insertAfter(INode<T, P> prev) {
      this.parent = prev.parent;
      this.parent.numNode++;
      if (prev.getParent().getLast() == prev) {
        prev.getParent().setLast(this);
      }
      this.prev = prev;
      this.next = prev.next;
      prev.next = this;
    }

    public INode<T, P> getPrev() {
      return prev;
    }

    public INode<T, P> getNext() {
      return next;
    }

  }

}
