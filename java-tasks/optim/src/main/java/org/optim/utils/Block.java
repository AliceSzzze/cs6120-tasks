package org.optim.utils;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Block {
  public final String label;

  public final Set<Block> preds;

  public final Set<Block> succs;

  public List<JSONObject> instrs;


  public Block(String label) {
    this.label = label;
    this.preds = new HashSet<>();
    this.succs = new HashSet<>();
    this.instrs = new ArrayList<>();
  }

  public void addInstr(JSONObject instr) {
    instrs.add(instr);
  }
}
