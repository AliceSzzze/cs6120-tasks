package org.optim;

import org.json.JSONArray;
import org.json.JSONObject;

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
          Dom dom = new Dom();
          System.out.println("----------------------------------------------");
          System.out.println("function: " + functionArray.getJSONObject(i).getString("name"));
          var res = dom.dominators(functionArray.getJSONObject(i));
          System.out.println(res);
        }
        break;
      case "domTree":
        for (int i = 0; i < functionArray.length(); i++) {
          Dom dom = new Dom();
          System.out.println("----------------------------------------------");
          System.out.println("function: " + functionArray.getJSONObject(i).getString("name"));
          dom.domTree(functionArray.getJSONObject(i));
        }
        break;
      case "frontier":
        for (int i = 0; i < functionArray.length(); i++) {
          Dom dom = new Dom();
          System.out.println("----------------------------------------------");
          System.out.println("function: " + functionArray.getJSONObject(i).getString("name"));
          dom.findDomFrontier(functionArray.getJSONObject(i));
        }
        break;
      case "domVerify":
        for (int i = 0; i < functionArray.length(); i++) {
          Dom dom = new Dom();
          dom.verify(functionArray.getJSONObject(i));
        }
        break;
      case "domTreeGraph":
        for (int i = 0; i < functionArray.length(); i++) {
          Dom dom = new Dom();
          dom.domTreeDot(functionArray.getJSONObject(i));
        }
        break;
      case "lvn":
        break;
      case "tdce":
        break;
    }


  }
}
