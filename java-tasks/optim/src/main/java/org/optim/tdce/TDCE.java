package org.optim.tdce;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import org.json.JSONArray;
import org.json.JSONObject;

public class TDCE {

  private final static String FUNCTIONS = "functions";
  private final static String INSTRS = "instrs";
  private final static String LABEL = "label";
  private final static String ARGS = "args";
  private final static String DEST = "dest";

  private final Set<String> usedVars;

  public TDCE() {
    usedVars = new HashSet<>();
  }

  public static void main(String[] args) throws IOException {
    BufferedReader br =
            new BufferedReader(new InputStreamReader(System.in));
    StringBuilder content = new StringBuilder();
    for (String line = br.readLine(); line != null; line = br.readLine()) {
      content.append(line);
    }
    br.close();

    JSONObject jsonObject = new JSONObject(content.toString());
    TDCE tdce = new TDCE();

    JSONArray functions = jsonObject.getJSONArray(FUNCTIONS);
    for (Object func : functions) {
      if (func instanceof JSONObject jfunc) {
        tdce.firstDCEPass(jfunc);
        tdce.secondDCEPAss(jfunc);
      }
    }
    System.out.println(jsonObject);
  }

  private void firstDCEPass(JSONObject function) {
    if (!function.has(INSTRS)) return;

    JSONArray jInstrs = function.getJSONArray(INSTRS);

    while (true) {
      List<Integer> toBeDeleted = new ArrayList<>();
      Map<String, Integer> vars = new HashMap<>();

      for (int i = 0; i < jInstrs.length(); i++) {
        JSONObject instr = (JSONObject) jInstrs.get(i);

        if (instr.has(LABEL)) {
          // new basic block
          vars.clear();
        }

        // uses
        if (instr.has(ARGS)) {
          JSONArray args = (JSONArray) instr.get(ARGS);

          for (var obj : args) {
            final String arg = obj.toString();
            if (obj instanceof String var) {
              usedVars.add(var);
            }
            vars.remove(arg);
          }
        }

        // defs
        if (instr.has(DEST)) {
          final String dest = instr.get(DEST).toString();
          if (vars.containsKey(dest)) {
            toBeDeleted.add(vars.get(dest));
          }
          vars.put(dest, i);
        }
      }

      int offset = 0;
      for (Integer idx : toBeDeleted) {
        jInstrs.remove(idx - offset);
        offset++;
      }

      // we haven't deleted any of the instructions, stop
      if (offset == 0) {
        break;
      }
    }
  }

  private void secondDCEPAss(JSONObject jsonObject) {
    if (!jsonObject.has(INSTRS)) return;

    JSONArray jInstrs = jsonObject.getJSONArray(INSTRS);

    while (true) {
      List<Integer> toBeDeleted = new ArrayList<>();

      for (int i = 0; i < jInstrs.length(); i++) {
        JSONObject instr = jInstrs.getJSONObject(i);

        // defs
        if (instr.has(DEST)) {
          final String dest = instr.get(DEST).toString();
          if (!usedVars.contains(dest)) {
            toBeDeleted.add(i);
          }
        }
      }

      int offset = 0;
      for (Integer idx : toBeDeleted) {
        jInstrs.remove(idx - offset);
        offset++;
      }

      // we haven't deleted any of the instructions, stop
      if (offset == 0) {
        break;
      }
    }
  }
}
