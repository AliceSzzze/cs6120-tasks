import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

public class DCEPass {

  private final static String FUNCTIONS = "functions";
  private final static String INSTRS = "instrs";
  private final static String LABEL = "label";
  private final static String ARGS = "args";
  private final static String DEST = "dest";

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
            trivialDCE(jfunc);
          }
        }
      }
    }
  }

  private static void trivialDCE(JSONObject jsonObject) {
    if (!jsonObject.has(INSTRS)) return;

    Object instrs = jsonObject.get(INSTRS);

    if (instrs instanceof JSONArray jInstrs) {
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
              vars.remove(arg);
            }
          }

          // defs
          if (instr.has(DEST)) {
            final String dest = instr.get(DEST).toString();
            if (vars.containsKey(dest)) {
              toBeDeleted.add(i);
            }
            vars.put(dest, i);
          }
        }

        int offset = 0;
        for (int i = 0; i < toBeDeleted.size(); i++) {
            jInstrs.remove(toBeDeleted.get(i)-offset);
            offset++;
        }

        if (offset == 0) {
          break;
        }
      }

      System.out.println(jInstrs);
    }
  }
}