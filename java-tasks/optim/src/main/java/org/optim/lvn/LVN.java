package org.optim.lvn;

import org.json.JSONArray;
import org.json.JSONObject;
import org.optim.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class LVN {
  private final static String FUNCTIONS = "functions";
  private final static String INSTRS = "instrs";
  private final static String LABEL = "label";
  private final static String ARGS = "args";
  private final static String DEST = "dest";
  private final static String OP = "op";

  private final static List<String> acceptedOps = Arrays.asList("const", "id",
          "add", "mul", "sub", "div", "eq", "lt", "gt", "lt", "ge", "not",
          "and", "or");
// TODO: add float ops
//  "fadd", "fmul", "fsub", "fdiv", "feq", "flt", "fle",
//          "fgt", "fge"


  public static void main(String[] args) throws IOException {
    BufferedReader br =
            new BufferedReader(new InputStreamReader(System.in));
    StringBuilder content = new StringBuilder();
    for (String line = br.readLine(); line != null; line = br.readLine()) {
      content.append(line);
    }
    br.close();

    JSONObject jsonObject = new JSONObject(content.toString());

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
    System.out.println(jsonObject);
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
          varVersions.clear();
          continue;
        }

        String op = instr.get(OP).toString();
        if (!acceptedOps.contains(op)) {
          continue;
        }

        List<Arg> args = new ArrayList<>();
        if (instr.has(ARGS)) {
          JSONArray jargs = (JSONArray) instr.get(ARGS);

          for (int j = 0; j < jargs.length(); j++) {
            Object obj = jargs.get(j);

            // must be a var if it is not a number
            String name = obj.toString();
            Expr varExpr = vars2Exprs.get(name);
            Var var;
            if (varExpr != null && exprs2Vars.containsKey(varExpr)) {
              var = exprs2Vars.get(varExpr).peek();
              jargs.put(j, var.getName());
            } else {
              varVersions.putIfAbsent(name, 0);
              var = new Var(name, varVersions.get(name));
            }

            args.add(var);
          }
        }

        if (instr.has("value")) {
          Object val = instr.get("value");
          if (val instanceof Number n) {
            Num num = new Num(n.doubleValue());
            args.add(num);
          } else if (val instanceof Boolean b) {
            Bool bool = new Bool(b);
            args.add(bool);
          } else {
            throw new IllegalArgumentException("value is not a number");
          }

        }
//        TODO: sort args for commutative ops

        Var dest = null;
        if (instr.has(DEST)) {
          String destName = instr.get(DEST).toString();
          varVersions.put(destName,
                  varVersions.getOrDefault(destName, -1) + 1);
          dest = new Var(destName, varVersions.get(destName));
        }

        Expr expr = new Expr(op, args);
        if (exprs2Vars.containsKey(expr)) {
          // if this is an expression that we have already seen
          Queue<Var> varsWithExpr = exprs2Vars.get(expr);
          // rewrite the expression by using the canonical home for the expr
          instr.put(ARGS, List.of(varsWithExpr.peek().getName()));
          instr.put(OP, "id");
          instr.remove("value");
        } else if (dest != null) {
          Queue<Var> q = new LinkedList<>();
          if (expr.getOp().equals("id")) {
            Var idv = (Var) args.get(0);
            q.add(idv);
            vars2Exprs.putIfAbsent(idv.getName(), expr);
          }
          q.add(dest);
          exprs2Vars.put(expr, q);
        }

        if (dest != null) {
          if (dest.getVersion() > 0 &&
                  vars2Exprs.containsKey(dest.getName()) &&
                  !vars2Exprs.get(dest.getName()).equals(expr)) {
            // unmark dest as the canonical home for the old Expr if necessary

            Expr oldExpr = vars2Exprs.get(dest.getName());
            Queue<Var> homes = exprs2Vars.get(oldExpr);

            while (!homes.isEmpty() && (homes.peek().equals(dest) ||
                    varVersions.get(homes.peek().getName()) > homes.peek().getVersion())) {
              homes.poll();
            }

            if (homes.isEmpty()) {
              exprs2Vars.remove(oldExpr);
            }
          }

          // associate the variable with the expression
//          System.out.println("associated " + dest.getName() +" with " + expr);
          vars2Exprs.put(dest.getName(), expr);
        }
      }
    }
  }
}