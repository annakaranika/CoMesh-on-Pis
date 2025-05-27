package routine.statement;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.stream.Collectors;

import deploy.DeviceState;

public class OrStatement extends RoutineStatement {
    List<RoutineStatement> innerStatements;

    public OrStatement(RoutineStatement statement) {
        innerStatements = new ArrayList<RoutineStatement>();
        innerStatements.add(statement);
    }

    public OrStatement(List<RoutineStatement> statements) {
        innerStatements = statements;
    }

    public void add(RoutineStatement statement) {
        innerStatements.add(statement);
    }

    public void addAll(List<RoutineStatement> statements) {
        innerStatements.addAll(statements);
    }

    @Override
    public RoutineStatement negate() {
        ListIterator<RoutineStatement> listIterator = innerStatements.listIterator();
        while (listIterator.hasNext()) {
            listIterator.set(listIterator.next().negate());
        }
        return new AndStatement(innerStatements);
    }

    public List<RoutineStatement> getInnerStatements() {
        return innerStatements;
    }

    public boolean isSatisfied(Map<String, DeviceState> devStates) {
        boolean success = false;
        for (RoutineStatement innerStatement: innerStatements) {
            if (innerStatement.isSatisfied(devStates)) {
                success = true;
                break;
            }
        }
        return success;
    }

    @Override
    public List<String> getTriggerDevIDs() {
        return innerStatements.stream()
                              .map(RoutineStatement::getTriggerDevIDs)
                              .flatMap(List::stream)
                              .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        String strRepr = "[";
        for (RoutineStatement innerStatement: innerStatements) {
            strRepr += innerStatement.toString() + " or ";
        }
        return strRepr.substring(0, strRepr.length() - 4).toString() + "]";
    }
}
