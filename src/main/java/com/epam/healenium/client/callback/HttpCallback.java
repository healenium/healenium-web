package com.epam.healenium.client.callback;

import com.epam.healenium.message.MessageAction;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;


@Deprecated
@Data
@Slf4j(topic = "healenium")
public class HttpCallback implements Callback {

    protected int messageCount = 0;
    protected CountDownLatch countDownLatch;

    @Deprecated
    @Override
    public void onFailure(@NotNull Call call, @NotNull IOException e) {
        updateActiveMessageAmount(MessageAction.DELETE);
        log.warn("[Queue] Error during request: {}. Message: {}. Exception: {}", call.request(), e.getMessage(), e);
    }

    @Deprecated
    @Override
    public void onResponse(@NotNull Call call, @NotNull Response response) {
        updateActiveMessageAmount(MessageAction.DELETE);
        response.close();
    }

    @Deprecated
    public HttpCallback asyncCall() {
        updateActiveMessageAmount(MessageAction.ADD);
        return this;
    }

    public synchronized void updateActiveMessageAmount(MessageAction messageAction) {
        switch (messageAction) {
            case ADD:
                messageCount++;
                break;
            case DELETE:
                messageCount--;
                if (countDownLatch != null) {
                    countDownLatch.countDown();
                }
                break;
            case PUSH:
                countDownLatch = new CountDownLatch(messageCount);
                break;
        }
    }

}
