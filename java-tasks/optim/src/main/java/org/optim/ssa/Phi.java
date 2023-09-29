package org.optim.ssa;

import lombok.Data;
import org.optim.Var;

import java.util.HashMap;
import java.util.Map;

@Data
public class Phi {
  // Maps label -> var
  private Map<String, Var> incoming;
  private String destVar;
  private Var ssaDest;

  public Phi(final String destVar) {
    incoming = new HashMap<>();
    this.destVar = destVar;
  }

  public void addVarWithLabel(final Var v, final String label) {
    incoming.put(label, v);
  }

}
