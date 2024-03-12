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
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.saggitt.omega.appusage.AppUsage;
import com.saggitt.omega.appusage.AppUsageDao;
import com.saggitt.omega.appusage.AppUsageDatabase;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.tensorflow.lite.Interpreter;
import com.saggitt.omega.appusage.UsageDataCollector;

/** Interface to load TfLite model and provide recommendations. */
public class RecommendationClient {
  private static final String TAG = "RecommendationClient";

  private final Context context;
  private final Config config;
  private Interpreter tflite;

  private Map<String, Integer> appIdDict;
  private AppUsageDao appUsageDao;

  private UsageDataCollector usageDataCollector;
  /** An immutable result returned by a RecommendationClient. */

  /** An immutable result returned by a RecommendationClient. */
  public static class Result {

    /** Predicted id. */
    public final int appId;

    /** A sortable score for how good the result is relative to others. Higher should be better. */
    public final float data;

    public Result(final int appId, final float data) {
      this.appId = appId;
      this.data = data;
    }

    @Override
    public String toString() {
      return String.format(Locale.getDefault(), "[%d] data: %.3f", appId, data);
    }
  }
  public RecommendationClient(Context context, Config config, Map<String, Integer> appIdDict) {
    this.context = context;
    this.config = config;
    this.appIdDict = appIdDict;
  }

  
  @WorkerThread
  public void load() {
    loadModel();
    loadDataBase();
  }

  @WorkerThread
  private void loadDataBase() {
    AppUsageDatabase appUsageDB = AppUsageDatabase.Companion.getDatabase(this.context);
    appUsageDao = appUsageDB.appUsageDao();
  }

  @WorkerThread
  private void loadModel() {
    try {
      ByteBuffer buffer = FileUtil.loadModelFile(this.context.getAssets(), config.model);
      tflite = new Interpreter(buffer);
      Log.v(TAG, "TFLite model loaded.");
    } catch (IOException ex) {
      Log.e(TAG, ex.getMessage());
    }
    usageDataCollector = new UsageDataCollector(this.context);
  }

  /** Free up resources as the client is no longer needed. */
  @WorkerThread
  public synchronized void unload() {
    tflite.close();
    appIdDict.clear();
  }

  int[] preprocessIds(List<String> Apps, int length) {
    int[] inputIds = new int[length];
    int i = 0;
    for (String App : Apps) {
      if (i >= inputIds.length) {
        break;
      }
      assert appIdDict.containsKey(App);
      inputIds[i] = appIdDict.get(App);
      ++i;
    }
    return inputIds;
  }

  /**
   * Given a list of selected items, preprocess to get tflite input.
   */
  @WorkerThread
  synchronized Object[] preprocess() {
    List<Object> inputs = new ArrayList<>();
    AtomicReference<List<AppUsage>> lastEntries = new AtomicReference<>();
    // Get the last 20 entries
    new Thread(() -> lastEntries.set(appUsageDao.getLastNRows(config.rnnLength))).start();
    // Extract a list of app_ids and a 2D list of other information
    List<Integer> pastIds = new ArrayList<>();
    List<List<Float>> pastData = new ArrayList<>();
    int i=0;
    for (AppUsage appUsage : lastEntries.get()) {
      if(!appIdDict.containsKey(appUsage.getPackageName())){
        continue;
      }
      pastIds.add(appIdDict.get(appUsage.getPackageName()));
      pastData.add(getList(appUsage));
    }
    AtomicReference<AppUsage> nowData = new AtomicReference<>();
    // Collect now data
    new Thread(() -> nowData.set(usageDataCollector.collectUsageData("a"))).start();
    List<Float> nowDataList = getList(nowData.get());

    List<List<Integer>> pastIds_inputs = new ArrayList<>();
    List<List<List<Float>>> pastData_inputs = new ArrayList<>();
    List<List<Float>> nowDataList_inputs = new ArrayList<>();
    List<Integer> targetAppId_inputs = new ArrayList<>();

    for (int targetAppId = 0; targetAppId < config.numApps; targetAppId++) {
      pastIds_inputs.add(pastIds);
      pastData_inputs.add(pastData);
      nowDataList_inputs.add(nowDataList);
      targetAppId_inputs.add(targetAppId);
    }

    inputs.add(pastIds_inputs);
    inputs.add(pastData_inputs);
    inputs.add(nowDataList_inputs);
    inputs.add(targetAppId_inputs);

    return inputs.toArray();
  }

  @NonNull
  private static List<Float> getList(AppUsage appUsage) {
    List<Float> info = new ArrayList<>();
    info.add((float) appUsage.getHourOfDay());
    info.add(appUsage.isAudioDeviceConnected()?1f:0f);
    info.add(appUsage.isCharging()?1f:0f);
    info.add(appUsage.isWifiConnected()?1f:0f);
    info.add(appUsage.isMobileDataConnected()?1f:0f);
    info.add(appUsage.isBluetoothConnected()?1f:0f);
    info.add((float) appUsage.getBrightness());
    return info;
  }

  /** Given a list of selected items, and returns the recommendation results. */
  @WorkerThread
  public synchronized float[] recommend() {
    Object[] inputs = preprocess();

    // Run inference.
    float[] outputData = new float[config.outputLength];
    Map<Integer, Object> outputs = new HashMap<>();
    outputs.put(0, outputData);
    tflite.runForMultipleInputsOutputs(inputs, outputs);

    return outputData;
  }

  Interpreter getTflite() {
    return this.tflite;
  }
}
