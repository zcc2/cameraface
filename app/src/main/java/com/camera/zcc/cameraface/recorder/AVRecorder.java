package com.camera.zcc.cameraface.recorder;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.shuzijiayuan.streamrecord.JavaAVRecord;

import java.util.LinkedList;

public class AVRecorder implements AudioRecord.AudioRecordDataCallback {
    private final static String TAG = AVRecorder.class.getName();
    private final static int AUDIO_THREAD_JOIN_TIMEOUT_MS = 500;
    private final static int AUDIO_FRAME_NUM = 3;
    private static AVRecorder instance  = null;

    private LinkedList<Frame> audioFrames = new LinkedList<Frame>();
    private AudioThread audioThread = null;
    private JavaAVRecord avRecord = null;
    private AudioRecord aRecord = null;
    private boolean started = false;

    public static AVRecorder getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new AVRecorder(context);
        }
        return instance;
    }

    public boolean startRecord(
            String out, String path, int width, int height, int rotation) {
        Log.w(TAG,  "startRecord");
        if (avRecord != null) {
            boolean ret = avRecord.startRecord(out, path, width, height, rotation);
            if (!ret) {
                Log.e(TAG, "start record failed!");
                return false;
            }
        } else {
            Log.e(TAG, "avRecord object is null");
            return false;
        }

        if (aRecord != null) {
            int framesPerBuffer = aRecord.initRecording(
                    44100, 1);
            if(framesPerBuffer < 0) {
                Log.w(TAG, "initRecording failed");
            } else {
                aRecord.setDataCallback(this);
                if (!aRecord.startRecording()) {
                    Log.w(TAG, "startRecording failed");
                }
                synchronized (audioFrames) {
                    for (int i = 0; i < AUDIO_FRAME_NUM; i++) {
                        int size = aRecord.getAudioBufferSize();
                        byte[] buffer = new byte[size];
                        Frame frame = new Frame(buffer, framesPerBuffer);
                        audioFrames.add(frame);
                    }
                }
                audioThread = new AudioThread("AudioThread");
                audioThread.start();
            }
        } else {
            Log.w(TAG, "aRecord object is null");
        }
        started = true;
        return true;
    }

    public void stopRecord() {
        Log.w(TAG, "stopRecord");
        started = false;
        if (aRecord != null) {
            aRecord.setDataCallback(null);
            if(audioThread != null) {
                audioThread.stopThread();
                synchronized (audioFrames) {
                    audioFrames.notify();
                }
                if (!ThreadUtils.joinUninterruptibly(
                        audioThread, AUDIO_THREAD_JOIN_TIMEOUT_MS)) {
                    Log.w(TAG,
                            "join of audio thread timed out");
                }
                audioThread = null;
            }
            if(!aRecord.stopRecording()) {
                Log.w(TAG, "stopRecording failed");
            }
        }
        if (avRecord != null) {
            avRecord.stopRecord();
        }
        audioFrames.clear();
    }

    @Override
    public void writeAudio(byte[] buffer, int frames) {
        if(!started) {
            return;
        }
        synchronized (audioFrames) {
            Frame find = null;
            for (Frame frame : audioFrames) {
                if(!frame.used) {
                    frame.used = true;
                    System.arraycopy(buffer, 0, frame.buffer,
                            0, frame.buffer.length);
                    audioFrames.notify();
                    find = frame;
                    break;
                }
            }
            if(find == null) {
                Log.w(TAG, "drop audio frame");
            }
        }
    }

    public void writeVideo(byte[] frame, int width, int height) {
        if(!started) {
            return;
        }
        if(avRecord != null) {
            if(!avRecord.writeVideo(frame, width, height)) {
                Log.w(TAG, "writeVideo failed");
            }
        } else {
            Log.w(TAG, "avRecord object is null");
        }
    }

    public void release() {
        if(avRecord != null) {
            avRecord.release();
        }
    }

    private AVRecorder(@NonNull Context context) {
        aRecord = new AudioRecord(context);
        if (aRecord != null) {
            aRecord.enableBuiltInNS(true);
        }
        avRecord = new JavaAVRecord();
        audioFrames.clear();
    }

    private AVRecorder() {
    }

    private class Frame {
        public byte[] buffer = null;
        public boolean used = false;
        public int samples  = 0;

        public Frame(byte[] buffer, int samples) {
            this.buffer = buffer;
            this.samples= samples;
            used = false;
        }
    }

    private class AudioThread extends Thread {
        private volatile boolean keepAlive = true;

        public AudioThread(String name) {
            super(name);
        }

        @Override
        public void run() {
            while (keepAlive) {
                Frame find = null;
                synchronized (audioFrames) {
                    for (Frame frame : audioFrames) {
                        if(frame.used) {
                            find = frame;
                            break;
                        }
                    }
                }

                if (find != null) {
                    if (avRecord != null) {
                        if (!avRecord.writeAudio(find.buffer,
                                find.buffer.length, find.samples)) {
                            Log.w(TAG, "writeAudio failed");
                        }
                    } else {
                        Log.w(TAG, "avRecord object is null");
                    }
                }

                synchronized (audioFrames) {
                    if (find == null) {
                        try {
                            if(keepAlive) {
                                audioFrames.wait();
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        find.used = false;
                    }
                }
            }
        }

        public void stopThread() {
            Log.d(TAG, "stopThread");
            keepAlive = false;
        }
    }
}
