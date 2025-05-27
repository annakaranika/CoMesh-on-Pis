package routine.command;

public class LengthyCommand extends RoutineCommand {
    private long duration;
    
    public LengthyCommand(String devID, String newState, long duration) {
        super(devID, newState);
        this.duration = duration;
    }

    @Override
    public long getLength() {
        return duration;
    }
}
