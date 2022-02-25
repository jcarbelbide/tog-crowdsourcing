package net.runelite.client.plugins.togcrowdsourcing.src.main.java.com.togcrowdsourcing;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;

@Slf4j
@Singleton
public class CrowdsourcingManager
{
    private static final String CROWDSOURCING_BASE = "https://togcrowdsourcing.com/worldinfo";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    @Inject
    private OkHttpClient okHttpClient;

    @Inject
    private Gson gson;

    protected void submitToAPI(WorldData worldData)
    {
        Request r = new Request.Builder()
                .url(CROWDSOURCING_BASE)
                .post(RequestBody.create(JSON, gson.toJson(worldData)))
                .build();

        okHttpClient.newCall(r).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                System.out.println("Failure sending to crowdsourcing server.");
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                System.out.println(response.body());
                response.close();
            }

        });
    }

}
