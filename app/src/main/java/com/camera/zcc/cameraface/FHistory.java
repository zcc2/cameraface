package com.camera.zcc.cameraface;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Log;

public class FHistory {
    private final static String TAG = FHistory.class.getName();
    private static FHistory instance  = null;
    private SharedPreferences history = null;

    public static FHistory getInstance(@NonNull Context context) {
        if(instance == null) {
            instance = new FHistory(context);
        }
        return instance;
    }

    public int getAvatarType() {
        int avatarType = -1;
        if(history != null) {
            avatarType = history.getInt("AvatarType", -1);
        }
        return avatarType;
    }

    public int getmAvatarIndex() {
        int avatarIndex = 0;
        if(history != null) {
            avatarIndex = history.getInt("AvatarIndex", 0);
        }
        return avatarIndex;
    }

    public boolean setAvatarType(int type) {
        boolean retVal = false;
        if(history != null) {
            SharedPreferences.Editor editor = history.edit();
            if(editor != null) {
                editor.putInt("AvatarType", type);
                retVal = editor.commit();
            }
        }
        return retVal;
    }

    public boolean setAvatarIndex(int index) {
        boolean retVal = false;
        if(history != null) {
            SharedPreferences.Editor editor = history.edit();
            if(editor != null) {
                editor.putInt("AvatarIndex", index);
                retVal = editor.commit();
            }
        }
        return retVal;
    }

    private FHistory(Context context) {
        history = context.getSharedPreferences(
                "history", Context.MODE_PRIVATE);
        Log.i(TAG, "history: " + history);
    }

    public boolean setisFirstChat(boolean isFirstChat) {
        boolean retVal = false;
        if(history != null) {
            SharedPreferences.Editor editor = history.edit();
            if(editor != null) {
                editor.putBoolean("isFirstChat", isFirstChat);
                retVal = editor.commit();
            }
        }
        return retVal;
    }

    //默认是第一次聊天
    public boolean getisFirstChat() {
        boolean isFirstChat = false;
        if(history != null) {
            isFirstChat = history.getBoolean(
                    "isFirstChat", true);
        }
        return isFirstChat;
    }

    //自己发布的话题ID
    public String getSubjectId() {
        String subjectId = null;
        if(history != null) {
            subjectId = history.getString("SubjectId", null);
        }
        return subjectId;
    }

    public boolean setSubjectId(String subjectId) {
        boolean retVal = false;
        if(history != null) {
            SharedPreferences.Editor editor = history.edit();
            if(editor != null) {
                editor.putString("SubjectId", subjectId);
                retVal = editor.commit();
            }
        }
        return retVal;
    }
}
