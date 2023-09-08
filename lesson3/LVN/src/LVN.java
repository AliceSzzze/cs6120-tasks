import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class LVN {
  private final static String FUNCTIONS = "functions";
  private final static String INSTRS = "instrs";
  private final static String LABEL = "label";
  private final static String ARGS = "args";
  private final static String DEST = "dest";
  private final static String OP = "op";

  // TODO: add floating point stuff
  private final static List<String> acceptedOps = Arrays.asList("add", "mul", "sub",
          "div",
          "eq",
          "lt", "gt", "lt", "ge");


  public static void main(String[] args) throws IOException {
    BufferedReader bufferedReader =
            new BufferedReader(new InputStreamReader(System.in));
    final String jsonFileName = bufferedReader.readLine();
    final String content = Files.readString(Path.of(jsonFileName));

    JSONObject jsonObject = new JSONObject(content);

    if (jsonObject.has(FUNCTIONS)) {
      Object functions = jsonObject.get(FUNCTIONS);
      if (functions instanceof JSONArray jsonArray) {
        for (Object func : jsonArray) {
          if (func instanceof JSONObject jfunc) {
            lvnPass(jfunc);
          }
        }
      }
    }
  }

  private static void lvnPass(JSONObject jsonObject) {
    if (!jsonObject.has(INSTRS)) return;

    Object instrs = jsonObject.get(INSTRS);

    if (instrs instanceof JSONArray jInstrs) {
      Map<Expr, Queue<Var>> exprs2Vars = new HashMap<>();
      Map<String, Expr> vars2Exprs = new HashMap<>();
      Map<String, Integer> varVersions = new HashMap<>();

      for (int i = 0; i < jInstrs.length(); i++) {
        JSONObject instr = (JSONObject) jInstrs.get(i);

        if (instr.has(LABEL)) {
          exprs2Vars.clear();
          vars2Exprs.clear();
          continue;
        }

        String op = instr.get(OP).toString();
        if (!acceptedOps.contains(op)) {
          continue;
        }

        List<Arg> args = new ArrayList<>();
        if (instr.has(ARGS)) {
          JSONArray jargs = (JSONArray) instr.get(ARGS);

          // TODO: parse numbers as actual numbers
          for (Object obj : jargs) {
            if (obj instanceof Number n) {
              Num num = new Num((Double) n);
              args.add(num);
            } else {
              // must be a var
              String name = obj.toString();
              Var var = new Var(name, varVersions.getOrDefault(name, 0));
              args.add(var);
            }
          }
        }
//        TODO: sort args

        Var dest = null;
        if (instr.has(DEST)) {
          String destName = instr.get(DEST).toString();
          varVersions.put(destName,
                  varVersions.getOrDefault(destName, -1)+1);
          dest = new Var(destName, varVersions.get(destName));
        }

        // dest is updated, e.g. x = x + 1, we should not reuse this elsewhere
//        if (args.contains(dest)) continue;

        // TODO: replace args with what's in the map

        Expr expr = new Expr(op, args);
        if (exprs2Vars.containsKey(expr)) {
          Queue<Var> varsWithExpr = exprs2Vars.get(expr);
          instr.put(ARGS, List.of(varsWithExpr.peek().getName())); // REWRITE?
          instr.put(OP, "id");
          if (dest != null) varsWithExpr.add(dest);
        } else if (dest != null) {
          if (dest.getVersion() > 0) {
            // unmark dest as the canonical home for the old Expr if necessary
            Expr oldExpr = vars2Exprs.get(dest.getName());
            Queue<Var> homes = exprs2Vars.get(oldExpr);

            while (homes.peek().equals(dest) ||
                    varVersions.get(homes.peek().getName()) > homes.peek().getVersion()) {
              homes.poll();
            }

            if (homes.isEmpty()) {
              exprs2Vars.remove(oldExpr);
            }
          }
          Queue<Var> q = new LinkedList<>();
          q.add(dest);
          exprs2Vars.put(expr, q);
        }

        if (dest != null) {
          vars2Exprs.put(dest.getName(), expr);
        }

      }
      System.out.println(jInstrs);
    }
  }
}