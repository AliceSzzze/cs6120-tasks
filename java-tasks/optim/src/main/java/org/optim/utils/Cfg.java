package org.optim.utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.optim.utils.Constants.*;

public class Cfg {
  public static Map<String, Block> formBlocks(JSONObject function) {
    JSONArray instrs = function.getJSONArray(INSTRS);

    Map<String, Block> blocks = new HashMap<>();

    int i = 0;
    Block curBlock;

    // can the first block have a label?
//    if (instrs.getJSONObject(0).has(LABEL)) {
//      final String label = instrs.getJSONObject(0).getString(LABEL);
//      curBlock = new Block(label);
//      blocks.put(label, curBlock);
//      i++;
//    } else {
    curBlock = new Block(ENTRY);
    blocks.put(ENTRY, curBlock);
//    }

    while (i < instrs.length()) {
      JSONObject instr = instrs.getJSONObject(i);
      i++;

      if (instr.has(LABEL)) {
        final String label = instr.getString(LABEL);
        curBlock = blocks.computeIfAbsent(label, k -> new Block(label));
        if (curBlock == null) System.out.println(label);
      } else {
        curBlock.addInstr(instr);

        final String op = instr.getString(OP);

        if (op.equals("jmp") || op.equals("br")) {
          for (Object succ : instr.getJSONArray(LABELS)) {
            Block next = blocks.computeIfAbsent(succ.toString(),
                    k -> new Block(succ.toString()));

            curBlock.succs.add(next);
            next.preds.add(curBlock);
          }
        }

        if (op.equals("jmp") || op.equals("br") || op.equals("ret")) {
          // ignore unreachable code?
          while (i < instrs.length() && !instrs.getJSONObject(i).has(LABEL))
            i++;
          continue;
        }
      }

      if (i < instrs.length() && instrs.getJSONObject(i).has(LABEL)) {
        // if last ordinary instr in block, fall through
        final String nextLabel = instrs.getJSONObject(i).getString(LABEL);
        Block next = blocks.computeIfAbsent(nextLabel,
                k -> new Block(nextLabel));
        curBlock.succs.add(next);
        next.preds.add(curBlock);
      }
    }

    return blocks;
  }

  public static void cfgDot(final String function,
                            final Map<String, Block> blocks) throws IOException {
    FileWriter file = new FileWriter(function + "_cfg.txt");
    BufferedWriter output = new BufferedWriter(file);
    output.write(String.format("digraph %s {\n", function));

    for (String block : blocks.keySet()) {
      String name = block.replace('.', '_');
      output.write(String.format("   %s;\n", name));
    }

    for (Block block : blocks.values()) {
      String node = block.label.replace('.', '_');
      for (Block succ : block.succs) {
        String succName = succ.label.replace('.', '_');
        output.write(String.format("   %s -> %s;\n", node, succName));
      }
    }

    output.write("}");
    output.close();
  }
}
