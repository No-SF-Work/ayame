package util;

import java.util.Objects;

public class ComparablePair<A extends Comparable<A>, B extends Comparable<B>>
        implements Comparable<ComparablePair<A, B>> {
    private A first;
    private B second;

    public ComparablePair(A first, B second) {
        this.first = first;
        this.second = second;
    }

    public String toString() {
        return "(" + first + ", " + second + ")";
    }

    public A getFirst() {
        return first;
    }

    public void setFirst(A first) {
        this.first = first;
    }

    public B getSecond() {
        return second;
    }

    public void setSecond(B second) {
        this.second = second;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComparablePair<?, ?> pair = (ComparablePair<?, ?>) o;
        return Objects.equals(first, pair.first) && Objects.equals(second, pair.second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }

    @Override
    public int compareTo(ComparablePair<A, B> o) {
        if (first.compareTo(o.first) == 0) {
            return second.compareTo(o.second);
        } else {
            return first.compareTo(o.first);
        }
    }
}
