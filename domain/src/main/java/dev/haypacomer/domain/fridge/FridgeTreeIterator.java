package dev.haypacomer.domain.fridge;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

public final class FridgeTreeIterator implements Iterator<FridgeNode> {

  private final Deque<FridgeNode> pending = new ArrayDeque<>();
  private final Traversal traversal;

  public FridgeTreeIterator(FridgeNode root, Traversal traversal) {
    this.traversal = Objects.requireNonNull(traversal, "traversal");
    pending.add(Objects.requireNonNull(root, "root"));
  }

  @Override
  public boolean hasNext() {
    return !pending.isEmpty();
  }

  @Override
  public FridgeNode next() {
    if (pending.isEmpty()) {
      throw new NoSuchElementException("No more fridge nodes");
    }
    FridgeNode current = pending.removeFirst();
    List<? extends FridgeNode> children = current.children();
    if (traversal == Traversal.DEPTH_FIRST) {
      for (int index = children.size() - 1; index >= 0; index--) {
        pending.addFirst(children.get(index));
      }
    } else {
      pending.addAll(children);
    }
    return current;
  }
}
