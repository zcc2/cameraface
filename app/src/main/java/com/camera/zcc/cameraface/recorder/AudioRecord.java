package com.camera.zcc.cameraface.recorder;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioFormat;
import android.media.MediaRecorder.AudioSource;
import android.os.Build;
import android.os.Process;
import android.util.Log;

import com.camera.zcc.cameraface.Constants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class AudioRecord {
  private static final boolean DEBUG = false;
  private static final boolean DUMP = false;

  private static final String TAG = "AudioRecord";

  // Default audio data format is PCM 16 bit per sample.
  // Guaranteed to be supported by all devices.
  private static final int BITS_PER_SAMPLE = 16;

  private static final int FRAMES_PER_BUFFER = 1024;

  // We ask for a native buffer size of BUFFER_SIZE_FACTOR * (minimum required
  // buffer size). The extra space is allocated to guard against glitches under
  // high load.
  private static final int BUFFER_SIZE_FACTOR = 4;

  // The AudioRecordJavaThread is allowed to wait for successful call to join()
  // but the wait times out afther this amount of time.
  private static final long AUDIO_RECORD_THREAD_JOIN_TIMEOUT_MS = 2000;
  private final Context context;

  private AudioEffects effects = null;
  private byte[] byteBuffer;

  private android.media.AudioRecord audioRecord = null;
  private AudioRecordThread audioThread = null;

  private static volatile boolean microphoneMute = false;
  private byte[] emptyBytes;

  // Audio recording data output callback interface
  public interface AudioRecordDataCallback {
    void writeAudio(byte[] buffer, int frames);
  }

  private AudioRecordDataCallback dataCallback = null;
  public void setDataCallback(AudioRecordDataCallback dataCallback) {
    Log.d(TAG, "Set data callback");
    this.dataCallback = dataCallback;
  }

  // Audio recording error handler functions.
  public interface AudioRecordErrorCallback {
    void onAudioRecordInitError(String errorMessage);
    void onAudioRecordStartError(String errorMessage);
    void onAudioRecordError(String errorMessage);
  }

  private AudioRecordErrorCallback errorCallback = null;
  public void setErrorCallback(AudioRecordErrorCallback errorCallback) {
    Log.d(TAG, "Set error callback");
    this.errorCallback = errorCallback;
  }

  /**
   * Audio thread which keeps calling ByteBuffer.read() waiting for audio
   * to be recorded. Feeds recorded data to the native counterpart as a
   * periodic sequence of callbacks using DataIsRecorded().
   * This thread uses a Process.THREAD_PRIORITY_URGENT_AUDIO priority.
   */
  private class AudioRecordThread extends Thread {
    private volatile boolean keepAlive = true;

    public AudioRecordThread(String name) {
      super(name);
    }

    @Override
    public void run() {
      Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
      Log.d(TAG, "AudioRecordThread" + AudioUtils.getThreadInfo());
      assertTrue(audioRecord.getRecordingState() ==
              android.media.AudioRecord.RECORDSTATE_RECORDING);

      FileOutputStream os = null;
      if (DUMP) {
        File file = new File(Constants.f2Path + "dump.pcm");
        if (file.exists()) {
          file.delete();
        }
        try {
          file.createNewFile();
          os = new FileOutputStream(file);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      long lastTime = System.nanoTime();
      while (keepAlive) {
        int bytesRead = audioRecord.read(
                byteBuffer, 0, byteBuffer.length);
        if (bytesRead == byteBuffer.length) {
          if (microphoneMute) {
            System.arraycopy(emptyBytes, 0,
                    byteBuffer, 0, byteBuffer.length);
          }
          if (dataCallback != null) {
            dataCallback.writeAudio(
                    byteBuffer, FRAMES_PER_BUFFER);
          } else {
            Log.w(TAG, "not set audio data callback");
          }
          if (os != null) {
            try {
              os.write(byteBuffer, 0, byteBuffer.length);
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        } else {
          String errorMessage = "AudioRecord.read failed: " + bytesRead;
          Log.e(TAG, errorMessage);
          if (bytesRead == android.media.AudioRecord.ERROR_INVALID_OPERATION) {
            keepAlive = false;
            reportAudioRecordError(errorMessage);
          }
        }
        if (DEBUG) {
          long nowTime = System.nanoTime();
          long durationInMs = TimeUnit.NANOSECONDS.toMillis((nowTime - lastTime));
          lastTime = nowTime;
          Log.d(TAG, "bytesRead[" + durationInMs + "] " + bytesRead);
        }
      }

      try {
        if (audioRecord != null) {
          audioRecord.stop();
        }
      } catch (IllegalStateException e) {
        Log.e(TAG, "AudioRecord.stop failed: " + e.getMessage());
      }

      if (os != null) {
        try {
          os.flush();
          os.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    // Stops the inner thread loop and also calls AudioRecord.stop().
    // Does not block the calling thread.
    public void stopThread() {
      Log.d(TAG, "stopThread");
      keepAlive = false;
    }
  }

  AudioRecord(Context context) {
    Log.d(TAG, "ctor" + AudioUtils.getThreadInfo());
    this.context  = context;
    if (DEBUG) {
      AudioUtils.logDeviceInfo(TAG);
    }
    effects = AudioEffects.create();
    if (enableBuiltInAEC(true))
    {
      Log.e(TAG, "Built-in AEC is enabled!");
    }else{
      Log.e(TAG, "Built-in AEC is not enabled!");
    }
  }

  public boolean enableBuiltInAEC(boolean enable) {
    Log.d(TAG, "enableBuiltInAEC(" + enable + ')');
    if (effects == null) {
      Log.e(TAG, "Built-in AEC is not supported on this platform");
      return false;
    }
    return effects.setAEC(enable);
  }

  public boolean enableBuiltInNS(boolean enable) {
    Log.d(TAG, "enableBuiltInNS(" + enable + ')');
    if (effects == null) {
      Log.e(TAG, "Built-in NS is not supported on this platform");
      return false;
    }
    return effects.setNS(enable);
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
  public int initRecording(int sampleRate, int channels) {
    Log.d(TAG, "initRecording(" +
            "sampleRate=" + sampleRate + ", channels=" + channels + ")");
    if (!AudioUtils.hasPermission(
            context, android.Manifest.permission.RECORD_AUDIO)) {
      reportAudioRecordInitError("RECORD_AUDIO permission is missing");
      return -1;
    }
    if (audioRecord != null) {
      reportAudioRecordInitError(
              "InitRecording called twice without StopRecording.");
      return -1;
    }
    final int bytesPerFrame = channels * (BITS_PER_SAMPLE / 8);
    final int framesPerBuffer = FRAMES_PER_BUFFER;
    byteBuffer = new byte[bytesPerFrame * framesPerBuffer];
    Log.d(TAG, "byteBuffer.capacity: " + byteBuffer.length);
    emptyBytes = new byte[byteBuffer.length];

    // Get the minimum buffer size required for the successful creation of
    // an AudioRecord object, in byte units.
    // Note that this size doesn't guarantee a smooth recording under load.
    final int channelConfig = channelCountToConfiguration(channels);
    int minBufferSize =
        android.media.AudioRecord.getMinBufferSize(
                sampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT);
    if (minBufferSize == android.media.AudioRecord.ERROR
            || minBufferSize == android.media.AudioRecord.ERROR_BAD_VALUE) {
      reportAudioRecordInitError(
              "AudioRecord.getMinBufferSize failed: " + minBufferSize);
      return -1;
    }
    Log.d(TAG, "AudioRecord.getMinBufferSize: " + minBufferSize);

    // Use a larger buffer size than the minimum required when creating the
    // AudioRecord instance to ensure smooth recording under load. It has been
    // verified that it does not increase the actual recording latency.
    int bufferSizeInBytes = Math.max(
            BUFFER_SIZE_FACTOR * minBufferSize, byteBuffer.length);
    Log.d(TAG, "bufferSizeInBytes: " + bufferSizeInBytes);
    try {
      audioRecord = new android.media.AudioRecord(AudioSource.MIC, sampleRate, channelConfig,
              AudioFormat.ENCODING_PCM_16BIT, bufferSizeInBytes);
      //audioRecord = new android.media.AudioRecord(AudioSource.VOICE_COMMUNICATION, sampleRate,
      //        channelConfig, AudioFormat.ENCODING_PCM_16BIT, bufferSizeInBytes);
    } catch (IllegalArgumentException e) {
      reportAudioRecordInitError("AudioRecord ctor error: " + e.getMessage());
      releaseAudioResources();
      return -1;
    }
    if (audioRecord == null ||
            audioRecord.getState() != android.media.AudioRecord.STATE_INITIALIZED) {
      reportAudioRecordInitError(
              "Failed to create a new AudioRecord instance");
      releaseAudioResources();
      return -1;
    }
    if (effects != null) {
      effects.enable(audioRecord.getAudioSessionId());
    }
    logMainParameters();
    logMainParametersExtended();
    return framesPerBuffer;
  }

  public int getAudioBufferSize() {
    return byteBuffer.length;
  }

  // Sets all recorded samples to zero if |mute| is true, i.e., ensures that
  // the microphone is muted.
  public static void setMicrophoneMute(boolean mute) {
    Log.w(TAG, "setMicrophoneMute(" + mute + ")");
    microphoneMute = mute;
  }

  public boolean startRecording() {
    Log.d(TAG, "startRecording");
    assertTrue(audioRecord != null);
    assertTrue(audioThread == null);
    try {
      audioRecord.startRecording();
    } catch (IllegalStateException e) {
      reportAudioRecordStartError(
              "AudioRecord.startRecording failed: " + e.getMessage());
      return false;
    }
    if (audioRecord.getRecordingState() != android.media.AudioRecord.RECORDSTATE_RECORDING) {
      reportAudioRecordStartError(
              "AudioRecord.startRecording failed - incorrect state :"
          + audioRecord.getRecordingState());
      return false;
    }
    audioThread = new AudioRecordThread("AudioRecordJavaThread");
    audioThread.start();
    return true;
  }

  public boolean stopRecording() {
    Log.d(TAG, "stopRecording");
    if(audioThread != null) {
      audioThread.stopThread();
      if (!ThreadUtils.joinUninterruptibly(
              audioThread, AUDIO_RECORD_THREAD_JOIN_TIMEOUT_MS)) {
        Log.e(TAG, "Join of AudioRecordJavaThread timed out");
      }
      audioThread = null;
    }
    if (effects != null) {
      effects.release();
    }
    releaseAudioResources();
    return true;
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
  private void logMainParameters() {
    Log.d(TAG, "AudioRecord: "
            + "session ID: " + audioRecord.getAudioSessionId() + ", "
            + "channels: " + audioRecord.getChannelCount() + ", "
            + "sample rate: " + audioRecord.getSampleRate());
  }

  @TargetApi(23)
  private void logMainParametersExtended() {
    if (AudioUtils.runningOnMarshmallowOrHigher()) {
      Log.d(TAG, "AudioRecord: "
              // The frame count of the native AudioRecord buffer.
              + "buffer size in frames: " + audioRecord.getBufferSizeInFrames());
    }
  }

  // Helper method which throws an exception  when an assertion has failed.
  private static void assertTrue(boolean condition) {
    if (!condition) {
      throw new AssertionError("Expected condition to be true");
    }
  }

  private int channelCountToConfiguration(int channels) {
    return (channels == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO);
  }

  // Releases the native AudioRecord resources.
  private void releaseAudioResources() {
    if (audioRecord != null) {
      audioRecord.release();
      audioRecord = null;
    }
  }

  private void reportAudioRecordInitError(String errorMessage) {
    Log.e(TAG, "Init recording error: " + errorMessage);
    if (errorCallback != null) {
      errorCallback.onAudioRecordInitError(errorMessage);
    }
  }

  private void reportAudioRecordStartError(String errorMessage) {
    Log.e(TAG, "Start recording error: " + errorMessage);
    if (errorCallback != null) {
      errorCallback.onAudioRecordStartError(errorMessage);
    }
  }

  private void reportAudioRecordError(String errorMessage) {
    Log.e(TAG, "Run-time recording error: " + errorMessage);
    if (errorCallback != null) {
      errorCallback.onAudioRecordError(errorMessage);
    }
  }
}
