package com.camera.zcc.cameraface.recorder;

import android.annotation.TargetApi;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AudioEffect;
import android.media.audiofx.AudioEffect.Descriptor;
import android.media.audiofx.NoiseSuppressor;
import android.os.Build;
import android.util.Log;

import java.util.List;
import java.util.UUID;

// This class wraps control of three different platform effects. Supported
// effects are: AcousticEchoCanceler (AEC) and NoiseSuppressor (NS).
// Calling enable() will active all effects that are
// supported by the device if the corresponding |shouldEnableXXX| member is set.
class AudioEffects {
  private static final boolean DEBUG = false;

  private static final String TAG = "AudioEffects";

  // UUIDs for Software Audio Effects that we want to avoid using.
  // The implementor field will be set to "The Android Open Source Project".
  private static final UUID AOSP_ACOUSTIC_ECHO_CANCELER =
      UUID.fromString("bb392ec0-8d4d-11e0-a896-0002a5d5c51b");
  private static final UUID AOSP_NOISE_SUPPRESSOR =
      UUID.fromString("c06c8400-8e06-11e0-9cb6-0002a5d5c51b");

  // Contains the available effect descriptors returned from the
  // AudioEffect.getEffects() call. This result is cached to avoid doing the
  // slow OS call multiple times.
  private static Descriptor[] cachedEffects = null;

  // Contains the audio effect objects. Created in enable() and destroyed
  // in release().
  private AcousticEchoCanceler aec = null;
  private NoiseSuppressor ns = null;

  // Affects the final state given to the setEnabled() method on each effect.
  // The default state is set to "disabled" but each effect can also be enabled
  // by calling setAEC() and setNS().
  // To enable an effect, both the shouldEnableXXX member and the static
  // canUseXXX() must be true.
  private boolean shouldEnableAec = false;
  private boolean shouldEnableNs = false;

  // Checks if the device implements Acoustic Echo Cancellation (AEC).
  // Returns true if the device implements AEC, false otherwise.
  public static boolean isAcousticEchoCancelerSupported() {
    // Note: we're using isAcousticEchoCancelerEffectAvailable() instead of
    // AcousticEchoCanceler.isAvailable() to avoid the expensive getEffects()
    // OS API call.
    return AudioUtils.runningOnJellyBeanOrHigher() && isAcousticEchoCancelerEffectAvailable();
  }

  // Checks if the device implements Noise Suppression (NS).
  // Returns true if the device implements NS, false otherwise.
  public static boolean isNoiseSuppressorSupported() {
    // Note: we're using isNoiseSuppressorEffectAvailable() instead of
    // NoiseSuppressor.isAvailable() to avoid the expensive getEffects()
    // OS API call.
    return AudioUtils.runningOnJellyBeanOrHigher() && isNoiseSuppressorEffectAvailable();
  }

  // Returns true if the device is blacklisted for HW AEC usage.
  public static boolean isAcousticEchoCancelerBlacklisted() {
    List<String> blackListedModels = AudioUtils.getBlackListedModelsForAecUsage();
    boolean isBlacklisted = blackListedModels.contains(Build.MODEL);
    if (isBlacklisted) {
      Log.w(TAG, Build.MODEL + " is blacklisted for HW AEC usage!");
    }
    return isBlacklisted;
  }

  // Returns true if the device is blacklisted for HW NS usage.
  public static boolean isNoiseSuppressorBlacklisted() {
    List<String> blackListedModels = AudioUtils.getBlackListedModelsForNsUsage();
    boolean isBlacklisted = blackListedModels.contains(Build.MODEL);
    if (isBlacklisted) {
      Log.w(TAG, Build.MODEL + " is blacklisted for HW NS usage!");
    }
    return isBlacklisted;
  }

  // Returns true if the platform AEC should be excluded based on its UUID.
  // AudioEffect.queryEffects() can throw IllegalStateException.
  @TargetApi(18)
  private static boolean isAcousticEchoCancelerExcludedByUUID() {
    for (Descriptor d : getAvailableEffects()) {
      if (d.type.equals(AudioEffect.EFFECT_TYPE_AEC)
          && d.uuid.equals(AOSP_ACOUSTIC_ECHO_CANCELER)) {
        return true;
      }
    }
    return false;
  }

  // Returns true if the platform NS should be excluded based on its UUID.
  // AudioEffect.queryEffects() can throw IllegalStateException.
  @TargetApi(18)
  private static boolean isNoiseSuppressorExcludedByUUID() {
    for (Descriptor d : getAvailableEffects()) {
      if (d.type.equals(AudioEffect.EFFECT_TYPE_NS) &&
              d.uuid.equals(AOSP_NOISE_SUPPRESSOR)) {
        return true;
      }
    }
    return false;
  }

  // Returns true if the device supports Acoustic Echo Cancellation (AEC).
  @TargetApi(18)
  private static boolean isAcousticEchoCancelerEffectAvailable() {
    return isEffectTypeAvailable(AudioEffect.EFFECT_TYPE_AEC);
  }

  // Returns true if the device supports Noise Suppression (NS).
  @TargetApi(18)
  private static boolean isNoiseSuppressorEffectAvailable() {
    return isEffectTypeAvailable(AudioEffect.EFFECT_TYPE_NS);
  }

  // Returns true if all conditions for supporting the HW AEC are fulfilled.
  // It will not be possible to enable the HW AEC if this method returns false.
  public static boolean canUseAcousticEchoCanceler() {
    boolean canUseAcousticEchoCanceler = isAcousticEchoCancelerSupported()
        && !AudioUtils.useWebRtcBasedAcousticEchoCanceler()
        && !isAcousticEchoCancelerBlacklisted() && !isAcousticEchoCancelerExcludedByUUID();
    Log.d(TAG, "canUseAcousticEchoCanceler: " + canUseAcousticEchoCanceler);
    return canUseAcousticEchoCanceler;
  }

  // Returns true if all conditions for supporting the HW NS are fulfilled.
  // It will not be possible to enable the HW NS if this method returns false.
  public static boolean canUseNoiseSuppressor() {
    boolean canUseNoiseSuppressor = isNoiseSuppressorSupported()
        && !AudioUtils.useWebRtcBasedNoiseSuppressor() && !isNoiseSuppressorBlacklisted()
        && !isNoiseSuppressorExcludedByUUID();
    Log.d(TAG, "canUseNoiseSuppressor: " + canUseNoiseSuppressor);
    return canUseNoiseSuppressor;
  }

  static AudioEffects create() {
    // Return null if VoIP effects (AEC, AGC and NS) are not supported.
    if (!AudioUtils.runningOnJellyBeanOrHigher()) {
      Log.w(TAG, "API level 16 or higher is required!");
      return null;
    }
    return new AudioEffects();
  }

  private AudioEffects() {
    Log.d(TAG, "ctor" + AudioUtils.getThreadInfo());
  }

  // Call this method to enable or disable the platform AEC. It modifies
  // |shouldEnableAec| which is used in enable() where the actual state
  // of the AEC effect is modified. Returns true if HW AEC is supported and
  // false otherwise.
  public boolean setAEC(boolean enable) {
    Log.d(TAG, "setAEC(" + enable + ")");
    if (!canUseAcousticEchoCanceler()) {
      Log.w(TAG, "Platform AEC is not supported");
      shouldEnableAec = false;
      return false;
    }
    if (aec != null && (enable != shouldEnableAec)) {
      Log.e(TAG, "Platform AEC state can't be modified while recording");
      return false;
    }
    shouldEnableAec = enable;
    return true;
  }

  // Call this method to enable or disable the platform NS. It modifies
  // |shouldEnableNs| which is used in enable() where the actual state
  // of the NS effect is modified. Returns true if HW NS is supported and
  // false otherwise.
  public boolean setNS(boolean enable) {
    Log.d(TAG, "setNS(" + enable + ")");
    if (!canUseNoiseSuppressor()) {
      Log.w(TAG, "Platform NS is not supported");
      shouldEnableNs = false;
      return false;
    }
    if (ns != null && (enable != shouldEnableNs)) {
      Log.e(TAG, "Platform NS state can't be modified while recording");
      return false;
    }
    shouldEnableNs = enable;
    return true;
  }

  public void enable(int audioSession) {
    Log.d(TAG, "enable(audioSession=" + audioSession + ")");
    assertTrue(aec == null);
    assertTrue(ns == null);

    if (DEBUG) {
      // Add logging of supported effects but filter out "VoIP effects", i.e.,
      // AEC, AEC and NS. Avoid calling AudioEffect.queryEffects() unless the
      // DEBUG flag is set since we have seen crashes in this API.
      for (Descriptor d : AudioEffect.queryEffects()) {
        if (effectTypeIsVoIP(d.type)) {
          Log.d(TAG, "name: " + d.name + ", "
                  + "mode: " + d.connectMode + ", "
                  + "implementor: " + d.implementor + ", "
                  + "UUID: " + d.uuid);
        }
      }
    }

    if (isAcousticEchoCancelerSupported()) {
      // Create an AcousticEchoCanceler and attach it to the AudioRecord on
      // the specified audio session.
      aec = AcousticEchoCanceler.create(audioSession);
      if (aec != null) {
        boolean enabled = aec.getEnabled();
        boolean enable = shouldEnableAec && canUseAcousticEchoCanceler();
        if (aec.setEnabled(enable) != AudioEffect.SUCCESS) {
          Log.e(TAG, "Failed to set the AcousticEchoCanceler state");
        }
        Log.d(TAG, "AcousticEchoCanceler: was "
                + (enabled ? "enabled" : "disabled") + ", enable: " + enable
                + ", is now: " + (aec.getEnabled() ? "enabled" : "disabled"));
      } else {
        Log.e(TAG, "Failed to create the AcousticEchoCanceler instance");
      }
    }

    if (isNoiseSuppressorSupported()) {
      // Create an NoiseSuppressor and attach it to the AudioRecord on the
      // specified audio session.
      ns = NoiseSuppressor.create(audioSession);
      if (ns != null) {
        boolean enabled = ns.getEnabled();
        boolean enable = shouldEnableNs && canUseNoiseSuppressor();
        if (ns.setEnabled(enable) != AudioEffect.SUCCESS) {
          Log.e(TAG, "Failed to set the NoiseSuppressor state");
        }
        Log.d(TAG, "NoiseSuppressor: was " + (enabled ?
                "enabled" : "disabled") + ", enable: " + enable + ", is now: "
                + (ns.getEnabled() ? "enabled" : "disabled"));
      } else {
        Log.e(TAG, "Failed to create the NoiseSuppressor instance");
      }
    }
  }

  // Releases all native audio effect resources. It is a good practice to
  // release the effect engine when not in use as control can be returned
  // to other applications or the native resources released.
  public void release() {
    Log.d(TAG, "release");
    if (aec != null) {
      aec.release();
      aec = null;
    }
    if (ns != null) {
      ns.release();
      ns = null;
    }
  }

  // Returns true for effect types in |type| that are of "VoIP" types:
  // Acoustic Echo Canceler (AEC) or Automatic Gain Control (AGC) or
  // Noise Suppressor (NS). Note that, an extra check for support is needed
  // in each comparison since some devices includes effects in the
  // AudioEffect.Descriptor array that are actually not available on the device.
  // As an example: Samsung Galaxy S6 includes an AGC in the descriptor but
  // AutomaticGainControl.isAvailable() returns false.
  @TargetApi(18)
  private boolean effectTypeIsVoIP(UUID type) {
    if (!AudioUtils.runningOnJellyBeanMR2OrHigher())
      return false;

    return (AudioEffect.EFFECT_TYPE_AEC.equals(type) && isAcousticEchoCancelerSupported())
        || (AudioEffect.EFFECT_TYPE_NS.equals(type) && isNoiseSuppressorSupported());
  }

  // Helper method which throws an exception when an assertion has failed.
  private static void assertTrue(boolean condition) {
    if (!condition) {
      throw new AssertionError("Expected condition to be true");
    }
  }

  // Returns the cached copy of the audio effects array, if available, or
  // queries the operating system for the list of effects.
  private static Descriptor[] getAvailableEffects() {
    if (cachedEffects != null) {
      return cachedEffects;
    }
    // The caching is best effort only - if this method is called from several
    // threads in parallel, they may end up doing the underlying OS call
    // multiple times. It's normally only called on one thread so there's no
    // real need to optimize for the multiple threads case.
    cachedEffects = AudioEffect.queryEffects();
    return cachedEffects;
  }

  // Returns true if an effect of the specified type is available. Functionally
  // equivalent to (NoiseSuppressor|AutomaticGainControl|...).isAvailable(), but
  // faster as it avoids the expensive OS call to enumerate effects.
  private static boolean isEffectTypeAvailable(UUID effectType) {
    Descriptor[] effects = getAvailableEffects();
    if (effects == null) {
      return false;
    }
    for (Descriptor d : effects) {
      if (d.type.equals(effectType)) {
        return true;
      }
    }
    return false;
  }
}
