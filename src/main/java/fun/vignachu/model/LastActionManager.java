package fun.vignachu.model;
import java.io.File;
import java.util.Stack;
public class LastActionManager {

    // 使用栈存储用户的操作
    private final Stack<ActionRecord> history = new Stack<>();
    public void record(ActionType type, File orig, File target) { history.push(new ActionRecord(type, orig, target)); }
    public ActionRecord pop() { return history.isEmpty() ? null : history.pop(); }
    public boolean hasAction() { return !history.isEmpty(); }
    public int size() { return history.size(); }
    public ActionRecord peek() { return history.isEmpty() ? null : history.peek(); }
}