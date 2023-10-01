package org.optim.ssa;

import org.json.JSONArray;
import org.json.JSONObject;
import org.optim.Dom;
import org.optim.Var;
import org.optim.utils.Block;

import java.util.*;

import static org.optim.utils.Constants.*;

public class SSA {

  Dom dom;

  // only keep track of the variables that have been written to at least once.
  // For variables that are only read, the same version is used.
  Map<String, Set<Block>> defs;

  Map<String, Object> types;

  // Maps a node to the node that it immediately dominates
  Map<Block, Set<Block>> revIdom;

  // set of phi instructions at the top of each basic block
  Map<Block, Set<Phi>> phisInBlock;

  Map<String, Stack<Integer>> varVersions;
  Map<String, Integer> nextVersion;

  JSONObject function;

  public SSA(JSONObject function) {
    this.function = function;
    this.dom = new Dom(function);
    this.dom.findDomFrontier();
    this.phisInBlock = new HashMap<>();
    this.varVersions = new HashMap<>();
    this.nextVersion = new HashMap<>();
    this.types = new HashMap<>();
    findDefs();
    getRevIdom();

  }

  public void toSSA() {
    renameArgs();
    insertPhi();
    rename(dom.getEntry());
    renderPhi();
  }

  public void fromSSA() {
    for (Block block: dom.getBlocks().values()) {
      for (int i = 0; i < block.instrs.size(); i++) {
        JSONObject instr = block.instrs.get(i);
        if (instr.has(OP) && instr.get(OP).equals(PHI)) {
          JSONArray labels = instr.getJSONArray(LABELS);
          JSONArray args = instr.getJSONArray(ARGS);

          for (int j = 0; j < args.length(); j++) {
            String label = labels.getString(j);
            String arg = args.getString(j);
            Block pred = dom.getBlocks().get(label);
            JSONObject newInstr = new JSONObject()
                    .put(DEST, instr.get(DEST))
                    .put(ARGS, List.of(arg))
                    .put(OP, "id");
            pred.instrs.add(newInstr);
          }
        }
      }

      List<JSONObject> philessInstrs = new ArrayList<>();
      for (JSONObject instr : block.instrs) {
        if (!instr.has(OP) || !instr.get(OP).equals(PHI)) {
          philessInstrs.add(instr);
        }
      }
      block.instrs = philessInstrs;
    }

    JSONArray origInstrs = function.getJSONArray(INSTRS);
    JSONArray newInstrs = new JSONArray();

    for (int i = 0; i < origInstrs.length(); i++) {
      while (i < origInstrs.length() && !origInstrs.getJSONObject(i).has(LABEL)) i++;
      if (i < origInstrs.length()) {
        newInstrs.put(origInstrs.getJSONObject(i));
        String label = origInstrs.getJSONObject(i).getString(LABEL);
        newInstrs.putAll(dom.getBlocks().get(label).instrs);
      }
    }

    function.put(INSTRS, newInstrs);
  }
  private void renameArgs() {
    if (!function.has(ARGS)) return;

    JSONArray args = function.getJSONArray(ARGS);
    for (int i = 0; i < args.length(); i++) {
      JSONObject arg = args.getJSONObject(i);
      arg.put("name", arg.getString("name") + ".0");
    }
  }

  private void insertPhi() {
    for (Map.Entry<String, Set<Block>> e : defs.entrySet()) {
      // Blocks where v is assigned.
      Queue<Block> varDefs = new LinkedList<>(e.getValue());

      while (!varDefs.isEmpty()) {
        Block cur = varDefs.poll();

        for (Block b : dom.getFrontier().get(cur)) {
          phisInBlock.computeIfAbsent(b, k -> new HashSet<>())
                  .add(new Phi(e.getKey()));
          Set<Block> defBlocks = e.getValue();
          if (!defBlocks.contains(b)) {
            defBlocks.add(b);
            varDefs.add(b);
          }
        }
      }
    }
  }

  private void rename(Block block) {
    Map<String, Integer> originalStack = new HashMap<>();

    if (phisInBlock.containsKey(block)) {
      // give the dest of the phi nodes a new copy
      for (Phi phi : phisInBlock.get(block)) {
        var varStack = varVersions.computeIfAbsent(phi.getDestVar(),
                k -> new Stack<>());
        int newVersion = nextVersion.getOrDefault(phi.getDestVar(), 0) + 1;
        if (!originalStack.containsKey(phi.getDestVar())) {
          originalStack.put(phi.getDestVar(), newVersion - 1);
        }
        nextVersion.put(phi.getDestVar(), newVersion);
        varStack.push(newVersion);

        phi.setSsaDest(new Var(phi.getDestVar(), newVersion));
      }
    }

    for (JSONObject instr : block.instrs) {
      if (instr.has(ARGS)) {
        JSONArray jargs = instr.getJSONArray(ARGS);
        for (int j = 0; j < jargs.length(); j++) {
          String arg = jargs.getString(j);
          var varStack = varVersions.get(arg);

          if (varStack != null && !varStack.isEmpty()) {
            jargs.put(j, arg + "." + varStack.peek().toString());
          } else {
            // TODO: findNext version?
//            int version = nextVersion.getOrDefault(arg, 0)
            jargs.put(j, arg + ".0");
          }
        }
      }

      if (instr.has(DEST)) {
        final String destVar = instr.getString(DEST);
        var varStack = varVersions.computeIfAbsent(destVar,
                k -> new Stack<>());
        int newVersion = nextVersion.getOrDefault(destVar, 0) + 1;
        if (!originalStack.containsKey(destVar)) {
          originalStack.put(destVar, newVersion - 1);
        }
        nextVersion.put(destVar, newVersion);
        instr.put(DEST, destVar + "." + newVersion);
        varStack.push(newVersion);
      }
    }

    for (Block succ : block.succs) {
      if (phisInBlock.containsKey(succ)) {
        Set<Phi> phis = phisInBlock.get(succ);

        for (Phi phi : phis) {
//          System.out.println(phi.getDestVar());
//          System.out.println(block);
//          System.out.println(succ);
          String v = phi.getDestVar();

          // TODO: maybe we shouldn't replace with 0
          int version =
                  varVersions.containsKey(v) && !varVersions.get(v).isEmpty() ?
                          varVersions.get(v).peek() : 0;
          phi.addVarWithLabel(new Var(v, version), block.label);
        }
      }
    }

    if (revIdom.containsKey(block)) {
      for (Block idommed : revIdom.get(block)) {
        rename(idommed);
      }
    }

    // restore stacks to original state
    for (Map.Entry<String, Integer> e : originalStack.entrySet()) {
      Stack<Integer> varStack = varVersions.get(e.getKey());
      int target = originalStack.get(e.getKey());
      while (!varStack.empty() && varStack.peek() != target) {
        varStack.pop();
      }
    }
  }

  private void renderPhi() {
    JSONArray instrs = function.getJSONArray(INSTRS);

    // phi instructions to be inserted
    Map<String, List<JSONObject>> phiInstrs = new HashMap<>();

    for (Map.Entry<Block, Set<Phi>> e : phisInBlock.entrySet()) {
      for (Phi p : e.getValue()) {
        // create new phi instruction
        JSONObject phiInstr = new JSONObject();
        phiInstr.put(OP, PHI);
        phiInstr.put(TYPE, types.get(p.getDestVar()));
        phiInstr.put(DEST, p.getSsaDest());

        JSONArray args = new JSONArray();
        JSONArray labels = new JSONArray();

        // get all the incoming values for this phi instruction
        for (Map.Entry<String, Var> option : p.getIncoming().entrySet()) {
          args.put(option.getValue().toString());
          labels.put(option.getKey());
        }

        phiInstr.put(ARGS, args);
        phiInstr.put(LABELS, labels);

        phiInstrs.computeIfAbsent(e.getKey().label, k -> new ArrayList<>()).add(phiInstr);
//        System.out.println(e.getKey().label);
      }
    }
    JSONArray newInstrs = new JSONArray();

    if (dom.getEntry().label.equals(ENTRY)) {
      newInstrs.put(new JSONObject().put(LABEL, ENTRY));
    }
    for (int i = 0; i < instrs.length(); i++) {
      JSONObject instr = instrs.getJSONObject(i);

      newInstrs.put(instr);

      if (instr.has(LABEL) && phiInstrs.containsKey(instr.getString(LABEL))) {
        newInstrs.putAll(phiInstrs.get(instr.getString(LABEL)));
      }
    }

    // replace old instrs with new instrs
    function.put(INSTRS, newInstrs);
  }

  private void getRevIdom() {
    revIdom = new HashMap<>();

    for (Map.Entry<Block, Block> e : dom.getIdom().entrySet()) {
      revIdom.computeIfAbsent(e.getValue(), k -> new HashSet<>()).add(e.getKey());
    }
  }

  private void findDefs() {
    defs = new HashMap<>();

    for (Block block : dom.getBlocks().values()) {
      for (var instr : block.instrs) {
        if (instr.has(DEST)) {
          String var = instr.getString(DEST);
          defs.computeIfAbsent(var, k -> new HashSet<>()).add(block);
//          System.out.println(instr);
          types.putIfAbsent(var, instr.get("type"));
        }
      }
    }
  }
}
