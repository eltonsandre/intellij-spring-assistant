package in.oneton.idea.spring.assistant.plugin.initializr.step;

import com.intellij.openapi.Disposable;
import com.intellij.util.Alarm;

import static com.intellij.util.Alarm.ThreadToUse.SWING_THREAD;

// Based on https://intellij-support.jetbrains.com/hc/en-us/community/posts/115000133264/comments/115000165504
public class DocumentChangedThrottler implements Disposable {

    private static final long MODIFIED_DOCUMENT_TIMEOUT_MS = 200L;
    private Alarm myDocumentAlarm = new Alarm(SWING_THREAD, this);

    @Override
    public void dispose() {
        myDocumentAlarm.cancelAllRequests();
        myDocumentAlarm = null;
    }

    void throttle(Runnable runnable) {
        if (myDocumentAlarm.isDisposed()) {
            return;
        }

        myDocumentAlarm.cancelAllRequests();
        myDocumentAlarm.addRequest(runnable, MODIFIED_DOCUMENT_TIMEOUT_MS);
    }

}
