package fr.uga.pddl4j.interpretation;

import java.util.ArrayList;
import java.util.Arrays;

import fr.uga.pddl4j.encoding.Encoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;


enum ItemStructure {
    Sequence,
    AllOf,
    AnyOf,
    OneOf,
    Item
}

enum GoalStrategy {
    Final,
    First
}

class PlanAction {
    public final String name;
    public final String[] parameters;
    public final String[] bound;
    public final String[] bind;

    public PlanAction(String stringRecord) {
        var parts = stringRecord.split(" ");
        this.name = parts[0];
        this.parameters = Arrays.stream(parts)
                .skip(1).map(e -> e.charAt(0) == '?' || e.charAt(0) == '!' ? e.substring(1) : e)
                .toArray(String[]::new);

        this.bound = Arrays.stream(this.parameters).filter(p -> stringRecord.indexOf("!" + p) >= 0).toArray(String[]::new);
        this.bind = Arrays.stream(this.parameters).filter(p -> stringRecord.indexOf("?" + p) >= 0).toArray(String[]::new);
    }

}

class Item {
    public final Item[] items;
    public final ItemStructure type;
    public final PlanAction action;

    public Item(Object record) {

        // this may be just a string line
        if (record instanceof String) {
            var stringRecord = (String) record;
            this.action = new PlanAction(stringRecord);
            this.items = null;
            this.type = ItemStructure.Item;
            return;
        }

        // this is a complex record, either sequence, oneOf, anyOf, allOf

        this.action = null;

        var json = (JSONObject) record;

        this.type = json.containsKey("sequence")
                ? ItemStructure.Sequence
                : json.containsKey("anyOf")
                ? ItemStructure.AnyOf
                : json.containsKey("allOf")
                ? ItemStructure.AllOf
                : ItemStructure.OneOf;

        JSONArray items = (JSONArray) json.get(
                this.type == ItemStructure.Sequence ? "sequence"
                        : this.type == ItemStructure.AnyOf ? "anyOf"
                        : this.type == ItemStructure.OneOf ? "oneOf" : "allOf");

        this.items = new Item[items.size()];
        for (var i = 0; i < items.size(); i++) {
            this.items[i] = new Item(items.get(i));
        }
    }
}

class View {
    public final PlanAction[] start;
    public final PlanAction[] goals;
    public final GoalStrategy goalStrategy;
    public final PlanAction view;
    public final String[] boundVariables;


    public View(JSONObject definition) {

        var start = definition.get("start");
        if (start instanceof String) {
            this.start = new PlanAction[]{new PlanAction((String) start)};
        } else {
            this.start = (PlanAction[]) ((JSONArray) start).stream().map(
                    a -> new PlanAction((String) a)).toArray(PlanAction[]::new);
        }

        var goal = definition.get("goal");
        if (goal != null) {
            if (goal instanceof String) {
                this.goals = new PlanAction[]{new PlanAction((String) goal)};
            } else {
                this.goals = (PlanAction[]) ((JSONArray) goal).stream().map(
                        a -> new PlanAction((String) a)).toArray(PlanAction[]::new);
            }
        } else {
            this.goals = null;
        }

        this.view = definition.containsKey("view") ? new PlanAction((String) definition.get("view")) : null;

        var strategyString = definition.containsKey("goalStrategy") ? (String) definition.get("goalStrategy") : null;
        this.goalStrategy = strategyString.equals("final") ? GoalStrategy.Final : GoalStrategy.First;

        this.boundVariables = definition.containsKey("bind")
                ? (String[]) ((JSONArray) definition.get("bind")).stream().toArray(String[]::new)
                : null;
    }
}

class Scenario {
    public final String id;
    public final String description;
    public final String[] parameters;
    public final View[] views;
    public final Item plan;

    public Scenario(JSONObject definition) {
        this.id = (String) definition.get("id");
        this.description = definition.containsKey("description") ? ((String) definition.get("description")) : null;
        this.plan = definition.containsKey("plan") ? new Item(definition.get("plan")) : null;
        this.parameters = definition.containsKey("parameters")
                ? (String[]) ((JSONArray) definition.get("parameters")).stream().toArray(String[]::new)
                : null;
        this.views = definition.containsKey("views")
                ? (View[]) ((JSONArray) definition.get("views")).stream().map(d -> new View((JSONObject) d)).toArray(View[]::new)
                : new View[0];
    }
}

class Binding {
    public final String variable;
    public final String value;

    public Binding(String variable, String value) {
        this.variable = variable;
        this.value = value;
    }

    @Override
    public String toString() {
        return this.variable + ": " + this.value;
    }
}

class InterpretedScenario {
   // private Scenario scenario;
    private String text;
    private ArrayList<PlanLine> lines;
    private  ArrayList<InterpretedScenario> scenarios;

    private static final Logger LOGGER = LogManager.getLogger(InterpretedScenario.class);

    private void checkAddPlanLine(Scenario scenario, PlanLine line, Binding[] bindings) {
        // currently support only anyOf
        if (Arrays.stream(scenario.plan.items).anyMatch(m -> {
            try {
                return line.matches(m.action, bindings) != null;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        })) {
            this.lines.add(line);
        }
    }

    private Binding[] checkStartMatch(View view, PlanLine line, Binding[] bindings) throws Exception {
        Binding[] startBind = null;

        // find a starting line
        for (var start : view.start) {
            startBind = line.matches(start, bindings);
            if (startBind != null) {
                return startBind;
            }
        }
        return null;
    }

    private Binding[] findLinesTillGoal(PlanLine[] lines, int i, View view, Binding[] startBind, ArrayList lineSet, int goalLineStart) throws Exception {
        Binding[] goalBind = null;
        for (var j = i + 1; j < lines.length; j++) {
            final PlanLine currentLine = lines[j];

            Binding[] matchedGoal = null;

            // match any of the goals
            matchedGoal = lines[j].matches(view.goals, startBind);

            // if we are ok with first matching goal we stopt the execution, otherwise we will try to match the last possible goal
            if (matchedGoal != null) {
                // remember this goal
                goalBind = matchedGoal;

                // add all lines until current goal
                for (var l = goalLineStart; l <= j; l++) {
                    lineSet.add(lines[l]);
                    goalLineStart = j + 1;
                }

                if (view.goalStrategy.equals(GoalStrategy.First)) {
                    break;
                }
            }
        }
        return goalBind;
    }

    private void expandScenario(PlanInterpreter interpreter, View view, ArrayList<PlanLine> lineSet, Binding[] goalBind) throws Exception {
        var referenceScenario = Arrays.stream(interpreter.interpretations).filter(k -> k.id.equals(view.view.name)).findFirst();

        if (referenceScenario.isEmpty()) {
            throw new Exception("View does not exist: " + view.view.name);
        }
        scenarios.add(
                new InterpretedScenario(
                        interpreter,
                        lineSet.toArray(PlanLine[]::new),
                        referenceScenario.get(), goalBind));
    }

    public InterpretedScenario(PlanInterpreter interpreter, PlanLine[] lines, Scenario scenario, Binding[] bindings) throws Exception {
        this.scenarios = new ArrayList<InterpretedScenario>();
        this.lines = new ArrayList<>();

        var finished = new boolean[scenario.views.length];

        // create the text
        this.text = scenario.description;
        if (this.text != null) {
            for (var b: bindings) {
                this.text = this.text.replace("!" + b.variable, b.value);
            }
        }

        // process the views
        for (var i = 0; i < lines.length; i++) {

            var line = lines[i];

            ////////////////////////////////////////////////////
            // if this is a final plan view, add all matching plan lines
            ////////////////////////////////////////////////////

            if (scenario.plan != null) {
                checkAddPlanLine(scenario, line, bindings);
            }

            /////////////////////////////////////////////////////
            // process view find all plan subsets in all views
            ////////////////////////////////////////////////////

            for (var s = 0; s<scenario.views.length; s++) {

                // we may have finished processing the plan
                if (finished[s]) {
                    continue;
                }

                var view = scenario.views[s];
                var startBind = checkStartMatch(view, line, bindings);

                // we did not find the starting line
                if (startBind == null) {
                    continue;
                }

                // if we found a startLine we find the finish

                var lineSet = new ArrayList<PlanLine>();

                Binding[] goalBind = null;

                // we mark that we are taking everything from this line forward  in the list of lines from start to goal
                var goalLineStart = i;

                ///////////////////////////////////////////////////////////////////////////////////////////////////
                // !IMPORTANT - If we have no goal, we take only the first line and continue execution
                //            - Otherwise we wil try to find the final goal
                ///////////////////////////////////////////////////////////////////////////////////////////////////

                if (view.goals == null) {
                    // we that the existing binding from start as goal does not exist
                    goalBind = startBind.clone();

                    // we only add one line and that is a start line, since we have no goal
                    lineSet.add(lines[i]);
                }
                 else {
                    // copy all lines from start to goal to the output and create a new view from it
                    goalBind = findLinesTillGoal(lines, i, view, startBind, lineSet, goalLineStart);
                }

                // if we found a goal, we can now

                if (view.view != null) {
                    if (goalBind != null) {
                        expandScenario(interpreter, view, lineSet, goalBind);
                    } else {
                        throw new Exception("Could not find the goal state");
                    }
                }

                // we mark this as finished so no more processing is done on this view
                if (view.goalStrategy.equals(GoalStrategy.Final)) {
                    finished[s] = true;
                }
            }
        }
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}

class PlanInterpreter {
    public final Scenario[] interpretations;

    public PlanInterpreter(String definition) {
        JSONObject inter = (JSONObject) JSONValue.parse(definition);
        JSONArray def = (JSONArray) inter.get("views");

        this.interpretations = (Scenario[]) def.stream().map(i -> new Scenario((JSONObject) i)).toArray(Scenario[]::new);
    }

    public InterpretedScenario[] interpret(PlanSource plan) throws Exception {
        var topLevelInterpretations = Arrays.stream(this.interpretations).filter(p -> p.parameters == null).toArray(Scenario[]::new);
        var scenarios = new ArrayList<InterpretedScenario>();

        for (var i: topLevelInterpretations) {
            scenarios.add(this.interpretScenario(plan.lines, i));
        }
        return scenarios.toArray(InterpretedScenario[]::new);
    }

    private InterpretedScenario interpretScenario(PlanLine[] lines, Scenario scenario) throws Exception {
        InterpretedScenario ip = null;
        return new InterpretedScenario(this, lines, scenario, new Binding[0]);

    }
}

public class Loader {
////    public PlanInterpreter loadDefinition(String path) throws IOException {
//        BufferedReader br = new BufferedReader(new FileReader("D:\\sampleJson.txt"));
//
//        StringBuilder sb = new StringBuilder();
//        String line = br.readLine();
//
//        while (line != null) {
//            sb.append(line);
//            sb.append(System.lineSeparator());
//            line = br.readLine();
//        }
//        br.close();
//        String jsonInput = sb.toString();
//
//        JSONArray definition = (JSONArray) JSONValue.parse(jsonInput);
//
//
//        return null;
//    }
}
