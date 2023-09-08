package org.optim;

import lombok.Data;

@Data
public class Num implements Arg {

  private final double value;

  @Override
  public int compareTo(Object o) {
    return 0;
  }
}
