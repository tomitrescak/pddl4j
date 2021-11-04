package fr.uga.pddl4j.interpretation;

import fr.uga.pddl4j.util.FilesUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class InterpreterTests {


    @Test
    void loadInterpretationFromJson() throws IOException {
        var definition = FilesUtil.readTextFile("pddl/l2/interpretation.json");

        var planInterpreter = new PlanInterpreter(definition);

        assertEquals(planInterpreter.interpretations.length, 6);

        // check individual items
        var i1 = planInterpreter.interpretations[0];
        assertEquals(i1.id, "truck-transport-from-to");
        assertEquals(i1.parameters.length, 4);
        assertArrayEquals(i1.parameters, new String[] { "packageId", "truckId", "from", "to"});
        assertEquals(i1.description, "Truck delivery from ?from to ?to");


        assertEquals(i1.plan.items.length, 3);
        assertEquals(i1.plan.type, ItemStructure.Sequence);

        assertEquals(i1.plan.items[0].action.name, "load-truck");
        assertArrayEquals(i1.plan.items[0].action.parameters, new String[] { "packageId", "truckId", "from" });

        // check more complex structure

        var i3 = planInterpreter.interpretations[3];
        assertEquals(1, i3.views.length);

        var view = i3.views[0];
        assertEquals("load-truck", view.start[0].name);
        assertEquals("unload-truck", view.goals[0].name);
        assertEquals(GoalStrategy.Final, view.goalStrategy);
        assertEquals("package-delivery-from-to", view.view.name);

        var i5 = planInterpreter.interpretations[5];
        assertEquals(1, i5.views.length);

        view = i5.views[0];
        assertEquals(4, view.start.length);
        assertEquals("load-airplane", view.start[0].name);
    }

    @Test
    void checkInterpretation() throws Exception {
        var definition = FilesUtil.readTextFile("pddl/l2/plan.txt");
        var plan = new PlanSource(definition);
        definition = FilesUtil.readTextFile("pddl/l2/interpretation.json");
        var planInterpreter = new PlanInterpreter(definition);

        var ip = planInterpreter.interpret(plan);

        assertEquals(2, ip.length);
    }

    @Test
    void loadTest() throws IOException {
        var definition = FilesUtil.readTextFile("pddl/l2/plan.txt");

        var plan = new PlanSource(definition);
        var plaLines = plan.lines;

        assertEquals(plaLines.length, 19);

        assertEquals(1,plaLines[1].time);
        assertEquals(1, plaLines[1].cost);
        assertEquals("drive-truck", plaLines[1].action);
        assertEquals(4, plaLines[1].parameters.length);
        assertEquals("trub", plaLines[1].parameters[0]);
        assertEquals("b1", plaLines[1].parameters[1]);
        assertEquals("b2", plaLines[1].parameters[2]);
        assertEquals("b", plaLines[1].parameters[3]);

        assertEquals(plan.getCost(), 1269);
    }
}
