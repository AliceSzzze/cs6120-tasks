package org.optim;

import lombok.Data;

import java.util.List;

@Data
public class Expr {
  public final String op;
  public final List<Arg> args;
}
