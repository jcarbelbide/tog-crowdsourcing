/*
 * Copyright (c) 2019, Weird Gloop <admin@weirdgloop.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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
//    private static final String CROWDSOURCING_BASE = "https://togcrowdsourcing.com/worldinfo";
    private static final String CROWDSOURCING_BASE = "http://127.0.0.1:8080/worldinfo";           // For debug
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    @Inject
    private OkHttpClient okHttpClient;

    @Inject
    private Gson gson;

    protected void submitToAPI(WorldData worldData, WorldHopper worldHopper)
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
                makeGetRequest(worldHopper);
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
                    .build();
            okHttpClient.newCall(r).enqueue(new Callback()
            {
                @Override
                public void onFailure(Call call, IOException e)
                {
                    log.debug("Error retrieving tog crowdsourcing data", e);

                    worldHopper.setGetError(true);

                    worldHopper.updateList();
                }

                @Override
                public void onResponse(Call call, Response response)
                {
                    if (response.isSuccessful())
                    {
                        try
                        {
                            worldHopper.setGetError(false);
                            JsonArray j = gson.newBuilder().create().fromJson(response.body().string(), JsonArray.class);
                            worldHopper.setWorldData(parseData(j));
                            worldHopper.updateList();
                        }
                        catch (IOException | JsonSyntaxException e)
                        {
                            log.error(e.getMessage());
                        }
                    }
                    else
                    {
                        log.error("Get request unsuccessful");
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
