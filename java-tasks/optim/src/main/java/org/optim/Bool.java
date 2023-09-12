package org.optim;

import lombok.Data;

@Data
public class Bool implements Arg {

  private final boolean value;

  @Override
  public int compareTo(Object o) {
    return 0;
  }
}
