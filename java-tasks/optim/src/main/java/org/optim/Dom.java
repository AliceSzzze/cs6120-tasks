package org.optim;

import lombok.Getter;
import org.json.JSONObject;
import org.optim.utils.Block;
import org.optim.utils.Cfg;

import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

@Getter
public class Dom {
  Block entry;

  Map<String, Block> blocks;

  String functionName;

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

  public Dom(JSONObject function) {
    final Cfg cfg = new Cfg(function);
    entry = cfg.getEntry();
    blocks = cfg.getBlocks();
    functionName = cfg.getFunctionName();
  }

  private void dfs(Block cur, Set<Block> visited) {
    for (Block succ : cur.succs) {
      if (!visited.contains(succ)) {
        visited.add(succ);
        dfs(succ, visited);
      }
    }

    revPostOrder.add(cur);
  }

  public Map<Block, Set<Block>> dominators() {
    dom = new HashMap<>();

    // set of all nodes
    Set<Block> init = new HashSet<>(blocks.values());

    for (Block block : blocks.values()) {
      dom.put(block, init);
    }

    dfs(entry, new HashSet<>());

    dom.put(entry, Set.of(blocks.get(entry.label)));

    boolean changed;
    do {
      changed = false;
      for (int i = 0; i < revPostOrder.size() - 1; i++) {
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

  public void domTree() {
    if (dom == null) dominators();
    idom = new HashMap<>();

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

  public void findDomFrontier() {
    domTree();

    frontier = new HashMap<>();

    // initialize
    for (Block block : blocks.values()) {
      frontier.put(block, new HashSet<>());
    }

    // for each node, union the dominators of its immediate predecessors then
    // remove all of the node's strict dominators from the union
    for (Block block : blocks.values()) {
      Set<Block> targ = new HashSet<>();

      for (Block pred : block.preds) {
        targ.addAll(dom.get(pred));
      }

      targ.removeIf(b -> dom.get(block).contains(b) && b != block);

      for (Block d : targ) {
        frontier.get(d).add(block);
      }
    }
  }

  public void verify() {
    findDomFrontier();

    // verify dominators
    Stack<List<Block>> dfs = new Stack<>();
    dfs.add(List.of(entry));

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
            throw new AssertionError(String.format("expected idom of %s in " +
                            "%s to " +
                            "be %s, but found %s", block, functionName,
                    cur,
                    idom.get(block)));
          }
          break;
        } else {
          bfs.addAll(cur.preds);
        }
      }
    }

    // verify frontier
    for (Map.Entry<Block, Set<Block>> e : frontier.entrySet()) {
      // check that nodes in the frontier are not dominated by key but their
      // at least one of the predecessor is

      // for every node in the frontier
      for (Block node : e.getValue()) {
        Block nodeInFrontier = e.getKey();
        // check that node does not strictly dominate the one in the frontier
        assert nodeInFrontier == node || !dom.get(node).contains(nodeInFrontier);

        boolean found = false;

        for (Block pred : node.preds) {
          if (dom.get(pred).contains(nodeInFrontier)) {
            found = true;
            break;
          }
        }

        assert found;
      }
    }
    System.out.println("verified");
  }

  public void domTreeDot() throws IOException {
    domTree();
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
