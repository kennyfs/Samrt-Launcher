/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3.tflite;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** The main activity to provide interactions with users. */
public class NNAdapter{
  private static final String TAG = "OnDeviceRecommendationDemo";
  private static final String CONFIG_PATH = "nnconfig.json";  // Default config path in assets.

  private Config config;
  private Context context;
  private RecommendationClient client;

  private Handler handler;

  private Map<String, Integer> appIdDict = new HashMap<>();
  private Map<Integer, String> reversedAppIdDict = new HashMap<>();

  public NNAdapter(Context context) {
    Log.v(TAG, "init");;
    this.context = context;
    // Load config file.
    try {
      this.config = FileUtil.loadConfig(this.context.getAssets(), CONFIG_PATH);
    } catch (IOException ex) {
      Log.e(TAG, String.format("Error occurs when loading config %s: %s.", CONFIG_PATH, ex));
    }

    try {
      this.appIdDict = FileUtil.loadAppIdDict(this.context.getAssets(), this.config.appIdDictPath);
      for (Map.Entry<String, Integer> entry : this.appIdDict.entrySet()) {
        this.reversedAppIdDict.put(entry.getValue(), entry.getKey());
      }
      Log.v(TAG, "appIdDict and reversedAppIdDict loaded.");
    } catch (IOException ex) {
      Log.e(TAG, ex.getMessage());
    }

    this.client = new RecommendationClient(this.context, this.config, this.appIdDict);
    this.handler = new Handler();
    this.handler.post(() -> this.client.load());
  }

  public void stop() {
    Log.v(TAG, "stop");
    handler.post(() -> client.unload());
  }

  /** Sends selected movie list and get recommendations. */
  public String recommend() {
    // Run inference with TF Lite.
    Log.d(TAG, "Run inference with TFLite model.");
    float[] scores = client.recommend();

    // Show result on screen
    return getFiveHighest(scores);
  }

  /** Shows result on the screen. */
  private String getFiveHighest(final float[] scores) {
    // Run on UI thread as we'll updating our app UI
    List<Pair<Integer, Float>> pairList = new ArrayList<>();
    for (int i = 0; i < scores.length; i++) {
      pairList.add(new Pair<>(i, scores[i]));
    }
    pairList.sort((pair1, pair2) -> Float.compare(pair2.second, pair1.second));
    List<String> packageNames = pairList.stream()
      .limit(5)
      .map(pair -> reversedAppIdDict.get(pair.first))
      .collect(Collectors.toList());

    return String.join(",", packageNames);
  }
}