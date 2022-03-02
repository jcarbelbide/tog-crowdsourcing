// TODO add license
package com.togcrowdsourcing;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.gson.*;
import com.togcrowdsourcing.ui.WorldHopper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;

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
                log.debug("Failure sending to crowdsourcing server.");
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                System.out.println(response.body());
                response.close();
            }

        });
    }

    public void makeGetRequest(WorldHopper worldHopper)
    {
        try
        {
            Request r = new Request.Builder()
                    .url(CROWDSOURCING_BASE)
//                    .addHeader("Authorization", plugin.getShootingStarsSharedKey())
                    .build();
            okHttpClient.newCall(r).enqueue(new Callback()
            {
                @Override
                public void onFailure(Call call, IOException e)
                {
                    log.debug("Error retrieving shooting star data", e);
                    worldHopper.setGetError(true);
                }

                @Override
                public void onResponse(Call call, Response response)
                {
                    if (response.isSuccessful())
                    {
                        try
                        {
                            JsonArray j = new Gson().fromJson(response.body().string(), JsonArray.class);
                            worldHopper.setWorldData(parseData(j));
                            worldHopper.setGetError(false);
                            System.out.println(worldHopper.getWorldData().toString());
                            worldHopper.updateList();
                        }
                        catch (IOException | JsonSyntaxException e)
                        {
                            worldHopper.setGetError(true);
                            log.error(e.getMessage());
                        }
                    }
                    else
                    {
                        log.error("Get request unsuccessful");
                        worldHopper.setGetError(true);
                    }
                }
            });
        }
        catch (IllegalArgumentException e)
        {
            log.error("Bad URL given: " + e.getLocalizedMessage());
        }
    }

    private ArrayList<WorldData> parseData(JsonArray j)
    {
        ArrayList<WorldData> l = new ArrayList<>();

        if (j == null) {return l;}

        for (JsonElement jsonElement : j)
        {
            JsonObject jObj = jsonElement.getAsJsonObject();
            WorldData d = new WorldData(
                    jObj.get("world_number").getAsInt(),
                    jObj.get("stream_order").getAsString(),
                    jObj.get("hits").getAsInt());
            l.add(d);
        }
        return l;
    }

}
