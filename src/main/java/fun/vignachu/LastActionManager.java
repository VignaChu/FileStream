package fun.vignachu;

import java.io.File;
import java.util.Stack;

public class LastActionManager {
    private final Stack<ActionRecord> actionHistory = new Stack<>();

    // 记录一步新的操作。

    public void recordAction(ActionType type, File original, File target) {
        actionHistory.push(new ActionRecord(type, original, target));
    }

    // 弹出并返回上一步操作记录。
    public ActionRecord popAction() {
        if (!actionHistory.isEmpty()) {
            return actionHistory.pop();
        }
        return null;
    }


    // 查看栈顶的操作记录
    public ActionRecord peekAction() {
        if (!actionHistory.isEmpty()) {
            return actionHistory.peek();
        }
        return null;
    }


    // 检查是否有可撤销的操作。
    public boolean hasAction() {
        return !actionHistory.isEmpty();
    }

    // 返回当前历史记录栈的大小。
    public int size() {
        return actionHistory.size();
    }
}