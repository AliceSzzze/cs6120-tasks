package org.optim;

import lombok.Data;
import org.optim.Arg;

@Data
public class Bool implements Arg {

  private final boolean value;

  @Override
  public int compareTo(Object o) {
    return 0;
  }
}
