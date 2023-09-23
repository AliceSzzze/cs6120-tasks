package org.optim;

import org.json.JSONObject;
import org.optim.utils.Block;
import org.optim.utils.Cfg;

import java.util.*;

import static org.optim.utils.Constants.ARGS;
import static org.optim.utils.Constants.DEST;

public class Dataflow {

  public enum Direction {
    FORWARD, BACKWARD;
  }
  final String type;
  final Direction direction;

  Map<String, Set<String>> defs;
  Map<String, Set<String>> uses;

  public Dataflow(String analysis) {
    this.type = analysis;

    switch (analysis) {
      case "live":
        this.direction = Direction.BACKWARD;
        break;

      default:
        this.direction = Direction.FORWARD;
    }
  }

  public void analyze(final JSONObject function) {
    System.out.println("----------------------------------------------");
    System.out.println("function: " + function.getString("name"));
    Cfg cfg = new Cfg(function);
    Map<String, Block> blocks = cfg.getBlocks();

    Queue<Block> worklist = new LinkedList<>(blocks.values());

    Map<String, Set<String>> in = new HashMap<>();
    Map<String, Set<String>> out = new HashMap<>();

    for (String block : blocks.keySet()) {
      // replace with init function
      in.put(block, new HashSet<>());
      out.put(block, new HashSet<>());
    }

    getUsesAndDefs(blocks);

    while (!worklist.isEmpty()) {
      Block cur = worklist.poll();

      Set<String> curIn = in.get(cur.label);
      Set<String> oldIn = new HashSet<>(curIn);
      Set<String> curOut = out.get(cur.label);

      curOut.clear();

      for (Block parent : getDependingBlocks(cur)) {
        // replace with merge function
        curOut.addAll(in.get(parent.label));
      }

      // replace with transfer function
      curIn.clear();
      curIn.addAll(curOut);
      curIn.removeAll(this.defs.get(cur.label));
      curIn.addAll(this.uses.get(cur.label));

      if (!oldIn.equals(curIn)) worklist.addAll(getDependentBlocks(cur));
    }

    for (String block : blocks.keySet()) {
      System.out.println("Block: " + block);
      System.out.println("Variables that are live on entry: " + in.get(block));
      System.out.println("Variables that are live on exit: " + out.get(block));
      System.out.println();
    }
  }

  private Set<Block> getDependentBlocks(Block cur) {
    if (this.direction == Direction.FORWARD) {
      return cur.succs;
    } else {
      return cur.preds;
    }
  }

  private Set<Block> getDependingBlocks(Block cur) {
    if (this.direction == Direction.FORWARD) {
      return cur.preds;
    } else {
      return cur.succs;
    }
  }

  private void getUsesAndDefs(final Map<String, Block> blocks) {
    this.defs = new HashMap<>();
    this.uses = new HashMap<>();

    for (Block block : blocks.values()) {
      Set<String> curUses = new HashSet<>();
      Set<String> curDefs = new HashSet<>();

      for (var instr : block.instrs) {
        if (instr.has(ARGS)) {
          var args = instr.getJSONArray(ARGS);
          for (var arg : args) {
            var name = arg.toString();
            if (!curDefs.contains(name)) curUses.add(name);
          }
        }

        if (instr.has(DEST)) curDefs.add(instr.getString(DEST));
      }

      this.defs.put(block.label, curDefs);
      this.uses.put(block.label, curUses);
    }
  }
}
