package org.optim;

import org.json.JSONObject;
import org.optim.utils.Block;
import org.optim.utils.Cfg;

import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

import static org.optim.utils.Constants.ENTRY;

public class Dom {
  Map<String, Block> blocks;

  /**
   * Maps each block to its immediate dominator.
   */
  Map<Block, Block> idom;

  /**
   * Maps each block to its dominators.
   */
  Map<Block, Set<Block>> dom;

  Map<Block, Set<Block>> frontier;

  List<Block> revPostOrder = new ArrayList<>();

  private void dfs(Block cur, Set<Block> visited) {
    for (Block succ: cur.succs) {
      if (!visited.contains(succ)) {
        visited.add(succ);
        dfs(succ, visited);
      }
    }

    revPostOrder.add(cur);
  }

  public Map<Block, Set<Block>> dominators(JSONObject function) {
    if (this.blocks == null)
      this.blocks = Cfg.formBlocks(function);

    dom = new HashMap<>();

    // set of all nodes
    Set<Block> init = new HashSet<>(blocks.values());

    for (Block block : blocks.values()) {
      dom.put(block, init);
    }

    Block entry = blocks.get(ENTRY);
    dfs(entry, new HashSet<>());

    dom.put(entry, Set.of(blocks.get(entry.label)));

    boolean changed;
    do {
      changed = false;
      for (int i = 0; i < revPostOrder.size()-1; i++) {
        Block cur = revPostOrder.get(i);
        Set<Block> newDom = new HashSet<>();
        boolean initialized = false;

        for (Block pred : cur.preds) {
          if (initialized) {
            newDom.retainAll(dom.get(pred));
          } else {
            newDom.addAll(dom.get(pred));
            initialized = true;
          }
        }

        newDom.add(cur);

        if (!dom.get(cur).equals(newDom)) {
          dom.put(cur, newDom);
          changed = true;
        }
      }
    } while (changed);

    return dom;
  }

  public void domTree(JSONObject function) {
    if (dom == null) dominators(function);
    idom = new HashMap<>();

    final Block entry = blocks.get(ENTRY);

    Queue<Block> worklist = new LinkedList<>(entry.succs);

    while (!worklist.isEmpty()) {
      Block cur = worklist.poll();
      if (idom.containsKey(cur) || cur.equals(entry)) continue;

      if (cur.preds.size() == 1) {
        Block pred = cur.preds.stream().findAny().get();
        idom.put(cur, pred);
      } else {
        // pairwise comparison
        for (Block dom1 : dom.get(cur)) {
          if (dom1 == cur) continue;
          boolean isIDom = true;
          for (Block dom2 : dom.get(cur)) {
            // dom1 dominates dom2?
            if (dom1 != dom2 && dom2 != cur && dom.get(dom2).contains(dom1)) {
              isIDom = false;
              break;
            }
          }
          if (isIDom) {
            idom.put(cur, dom1);
            break;
          }
        }
      }

      worklist.addAll(cur.succs);
    }
  }

  public void findDomFrontier(JSONObject function) {
    domTree(function);

    frontier = new HashMap<>();

    // for each node, find dom of imm pred then remove all of the node's dom
    // from imm pred's dom

    for (Block block : blocks.values()) {
      frontier.put(block, new HashSet<>());
    }

    for (Block block : blocks.values()) {
      Set<Block> targ = new HashSet<>();

      for (Block pred : block.preds) {
        targ.addAll(dom.get(pred));
      }

      targ.removeAll(dom.get(block));

      for (Block d : targ) {
        frontier.get(d).add(block);
      }
    }
  }

  public void verify(JSONObject function) {
    findDomFrontier(function);

    // verify dominators
    Stack<List<Block>> dfs = new Stack<>();
    dfs.add(List.of(blocks.get(ENTRY)));

    Map<Block, Set<Block>> solution = new HashMap<>();

    while (!dfs.isEmpty()) {
      List<Block> path = dfs.pop();

      Block cur = path.get(path.size() - 1);

      solution.computeIfAbsent(cur, k -> new HashSet<>(path)).retainAll(path);

      for (Block succ : cur.succs) {
        if (!path.contains(succ)) {
          List<Block> newPath = new ArrayList<>(path);
          newPath.add(succ);
          dfs.add(newPath);
        }
      }
    }

    // verify dominator tree

    for (Block block : blocks.values()) {
      Set<Block> dominators = dom.get(block);
      Queue<Block> bfs = new LinkedList<>(block.preds);

      while (!bfs.isEmpty()) {
        Block cur = bfs.poll();

        if (dominators.contains(cur)) {
          if (!cur.equals(idom.get(block))) {
            throw new AssertionError(String.format("expected idom of %s to " +
                    "be %s, but found %s", block, cur, idom.get(block)));
          }
          break;
        } else {
          bfs.addAll(cur.preds);
        }
      }
    }

    // verify frontier
    for (Map.Entry<Block, Set<Block>> entry : frontier.entrySet()) {
      // check that nodes in the frontier are not dominated by key but their
      // at least one of the predecessor is

      for (Block node : entry.getValue()) {
        assert !dom.get(node).contains(entry.getKey());

        boolean found = false;

        for (Block pred : node.preds) {
          if (dom.get(pred).contains(entry.getKey())) {
            found = true;
            break;
          }
        }

        assert found;
      }
    }
    System.out.println("verified");
  }

  public void domTreeDot(JSONObject function) throws IOException {
    domTree(function);
    final String functionName = function.getString("name");
    FileWriter file = new FileWriter(functionName + "_domTree.txt");
    BufferedWriter output = new BufferedWriter(file);

    output.write(String.format("digraph %s {\n", functionName));

    for (String block : blocks.keySet()) {
      String name = block.replace('.', '_');
      output.write(String.format("   %s;\n", name));
    }

    for (Map.Entry<Block, Block> entry : idom.entrySet()) {
      String idom = entry.getValue().label.replace('.', '_');
      String node = entry.getKey().label.replace('.', '_');
      output.write(String.format("   %s -> %s;\n", idom, node));
    }

    output.write("}");
    output.close();

    Cfg.cfgDot(functionName, blocks);
  }
}
