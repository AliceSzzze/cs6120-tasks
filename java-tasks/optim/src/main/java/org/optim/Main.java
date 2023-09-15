package org.optim;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static org.optim.utils.JSONConstants.FUNCTIONS;

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
      case "lvn":
        break;
      case "tdce":
        break;
    }
  }
}
