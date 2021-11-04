package fr.uga.pddl4j.interpretation;

/////////////////////////////////////////////
// PLAN
////////////////////////////////////////////

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

class PlanLine {
    public final float time;
    public final String action;
    public final String[] parameters;
    public final float cost;

    public PlanLine(String line) {
        var parts = line.split(":");

        this.time = Float.parseFloat(parts[0]);

        parts = parts[1].split("\\[");

        var bodyString = parts[0];
        bodyString = bodyString.replace("(", "");
        bodyString = bodyString.replace(")", "");

        var body = Arrays.stream(bodyString.split(" ")).filter(s -> s.length() > 0).toArray(String[]::new);
        this.parameters = Arrays.stream(body).skip(1).map(b -> b.trim()).toArray(String[]::new);
        this.action = body[0].trim();

        parts[1] = parts[1].trim();
        this.cost = Float.parseFloat(parts[1].substring(0, parts[1].length() - 1));
    }

    static Binding[] empty = new Binding[0];

    public Binding[] matches(PlanAction[] lines, Binding[] bindings) throws Exception {
        for (var line: lines) {
            var match = this.matches(line, bindings);
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    public Binding[] matches(PlanAction line, Binding[] bindings) throws Exception {
        if (!this.action.equals(line.name)) {
            return null;
        }
        if (this.parameters.length != line.parameters.length) {
            return null;
        }

        // check all bound variables
        for (var i=0; i<line.bound.length; i++) {
            var b = line.bound[i];

            var binding = Arrays.stream(bindings).filter(bi -> bi.variable.equals(b)).findFirst();
            if (binding.isEmpty()) {
                throw new Exception("Expected bound variable " + b + " does not exist in bindings");
            }

            var index = getIndexOf(line.parameters, b);
            if (!this.parameters[index].equals(binding.get().value)) {
                return null;
            }
        }

        // add new bindings
        var newBindings = new ArrayList<Binding>();
        for (var b: line.bind) {
            var index = getIndexOf(line.parameters, b);
            newBindings.add(new Binding(b, this.parameters[index]));
        }

        // add existing bindings
        // browse the new bindings and only add it if does not exist previously
        for (var b: bindings) {
            var exists = newBindings.stream().anyMatch(s -> s.variable.equals(b.variable));
            if (!exists) {
                newBindings.add(new Binding(b.variable, b.value));
            }
        }
        return newBindings.isEmpty() ? empty : newBindings.toArray(Binding[]::new);

    }

    public static int getIndexOf(String[] strings, String item) {
        for (int i = 0; i < strings.length; i++) {
            if (item.equals(strings[i])) return i;
        }
        return -1;
    }
}

class PlanSource {
    public final PlanLine[] lines;

    public PlanSource(String definition) {
        this.lines = Arrays.stream(definition.trim().split("\n")).map(l -> new PlanLine(l)).toArray(PlanLine[]::new);
    }

    public float getCost() {
        return Arrays.stream(lines).map(l -> l.cost).reduce(0f, (subtotal , n) -> subtotal + n);
    }
}
