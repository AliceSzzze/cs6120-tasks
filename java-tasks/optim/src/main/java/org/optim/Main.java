package org.optim;

import org.json.JSONArray;
import org.json.JSONObject;
import org.optim.ssa.SSA;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static org.optim.utils.Constants.FUNCTIONS;

public class Main {

  public static void main(String[] args) throws IOException {
    BufferedReader br =
            new BufferedReader(new InputStreamReader(System.in));
    StringBuilder content = new StringBuilder();
    for (String line = br.readLine(); line != null; line = br.readLine()) {
      content.append(line);
    }
    br.close();

    JSONObject program = new JSONObject(content.toString());

    JSONArray functionArray = program.getJSONArray(FUNCTIONS);

    switch (args[0]) {
      case "dataflow":
        Dataflow dataflow = new Dataflow(args[1]);

        for (int i = 0; i < functionArray.length(); i++) {
          dataflow.analyze(functionArray.getJSONObject(i));
        }
        break;
      case "dominators":
        for (int i = 0; i < functionArray.length(); i++) {
          Dom dom = new Dom(functionArray.getJSONObject(i));
          System.out.println("----------------------------------------------");
          System.out.println("function: " + dom.functionName);
          var res = dom.dominators();
          System.out.println(res);
        }
        break;
      case "domTree":
        for (int i = 0; i < functionArray.length(); i++) {
          Dom dom = new Dom(functionArray.getJSONObject(i));
          System.out.println("----------------------------------------------");
          System.out.println("function: " + functionArray.getJSONObject(i).getString("name"));
          dom.domTree();
        }
        break;
      case "frontier":
        for (int i = 0; i < functionArray.length(); i++) {
          Dom dom = new Dom(functionArray.getJSONObject(i));
          System.out.println("----------------------------------------------");
          System.out.println("function: " + functionArray.getJSONObject(i).getString("name"));
          dom.findDomFrontier();
          System.out.println(dom.frontier);
        }
        break;
      case "domVerify":
        for (int i = 0; i < functionArray.length(); i++) {
          Dom dom = new Dom(functionArray.getJSONObject(i));
          dom.verify();
        }
        break;
      case "domTreeGraph":
        for (int i = 0; i < functionArray.length(); i++) {
          Dom dom = new Dom(functionArray.getJSONObject(i));
          dom.domTreeDot();
        }
        break;

      case "fromSSA":
        for (int i = 0; i < functionArray.length(); i++) {

        }
        break;
      case "toSSA":
        for (int i = 0; i < functionArray.length(); i++) {
          SSA ssa = new SSA(functionArray.getJSONObject(i));
          ssa.toSSA();
        }
        System.out.println(program);
        break;

      case "lvn":
        break;
      case "tdce":
        break;
    }


  }
}
