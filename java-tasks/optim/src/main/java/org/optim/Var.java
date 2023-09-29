package org.optim;

import lombok.Data;

@Data
public class Var implements Arg {
  private final String name;
  private final int version;

  @Override
  public int compareTo(Object o) {
    return 0;
  }

  @Override
  public String toString() {
    return name+"."+version;
  }
}
