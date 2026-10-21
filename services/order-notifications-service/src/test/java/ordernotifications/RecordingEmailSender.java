package ordernotifications;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

class RecordingEmailSender implements EmailSender {

    final List<EmailMessage> sent = new CopyOnWriteArrayList<>();
    volatile boolean failing;

    @Override
    public void send(EmailMessage message) {
        if (failing) {
            throw new IllegalStateException("gateway unavailable");
        }
        sent.add(message);
    }

    void reset() {
        sent.clear();
        failing = false;
    }
}
