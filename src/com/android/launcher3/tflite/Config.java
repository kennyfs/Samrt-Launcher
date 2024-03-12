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

import android.util.Log;
import java.util.ArrayList;
import java.util.List;

/** Config for recommendation app. */
public final class Config {
  private static final String TAG = "Config";
  private static final String DEFAULT_MODEL_PATH = "kennyfs_model.tflite";
  private static final String DEFAULT_APP_DICT_PATH = "app_id_dict.json";
  private static final int DEFAULT_OUTPUT_LENGTH = 100;
  private static final int DEFAULT_RNN_LENGTH = 20;
  private static final int NUM_APPS = 76;

  /** TF Lite model path. */
  public String model = DEFAULT_MODEL_PATH;

  /** Path to the app id dictionary. */
  public String appIdDictPath = DEFAULT_APP_DICT_PATH;

  /** Default RNN length. */
  public int rnnLength = DEFAULT_RNN_LENGTH;
  public int outputLength = DEFAULT_OUTPUT_LENGTH;
  /** Number of apps supported by the model. */
  public int numApps = NUM_APPS;
  
  public Config() {}
}
