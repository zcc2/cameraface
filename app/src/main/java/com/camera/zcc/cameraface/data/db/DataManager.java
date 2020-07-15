package com.camera.zcc.cameraface.data.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.text.TextUtils;

import com.camera.zcc.cameraface.data.AvatarItem;
import com.faceunity.fup2a.misc.Constant;

import java.util.ArrayList;
import java.util.List;

import static com.faceunity.fup2a.misc.MiscUtil.Logger;

public class DataManager extends SQLiteOpenHelper {
    private final static String TAG = DataManager.class.getName();
    public final static int DATABASE_VERSION = 2;

    private final static String DATABASE_NAME = "history.db";
    private final static String HISTORY_UID = "uid";
    private final static String HISTORY_IMAGE = "img";
    private final static String HISTORY_IMAGE_ORIGIN = "img_origin";
    private final static String HISTORY_BUNDLE_URI = "bundle";
    private final static String HISTORY_Q_BUNDLE_URI = "q_bundle";
    private final static String HISTORY_NOBODY_BUNDLE_URI = "nobody_bundle";
    private final static String HISTORY_TIME_STAMP = "time";
    private final static String HISTORY_GENDER = "gender";
    private final static String HISTORY_HAIRIDS = "hairIDs";
    private final static String HISTORY_HAIRBUNDLES = "hairBundles";
    private final static String HISTORY_AVATAR_DIR = "avatarDir";
    private final static String HISTORY_PHOTO = "photo_origin";
    private final static boolean VERBOSE_LOG = true;
    private final static String[] bundleFiles = {
            "zhangxiaofan.bundle",
            "qiuxiaohao.bundle",
            "mieba.bundle",
            "xiong.bundle",
            "shengdanlaoren.bundle",
            "chaiquan.bundle",
            "mao.bundle",
            "gou.bundle",
            "tiger.bundle"

    };
    private final static String[] imageFiles = {
            "zhangxiaofan@2x.png",
            "icon_qiuxiaohao@2x.png",
            "mieba@2x.png",
            "xiong@2x.png",
            "shengdanlaoren@2x.png",
            "chaiquan@2x.png",
            "mao@2x.png",
            "gou@2x.png",
            "tiger@2x.png"
    };

    public DataManager(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Logger(TAG, "onCreate ", false);
        db.execSQL(
                "create table history " +
                        "(id integer primary key, uid integer, img text, img_origin text," +
                        "bundle text, q_bundle text, nobody_bundle text, time text, gender text,"+
                        "hairIDs text, hairBundles text, avatarDir text, photo_origin text)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS history");
        onCreate(db);
    }

    public boolean insertHistory(Long uid, String imagePath, String imageOriginUri,
        String bundleUri, String qBundleUri, String nobodyBundleUri, String time, String gender,
        String hairIDs, String hairBundles, String avatarItemDir, String originPhotoFilePath) {
        Logger(TAG, "insert history " + imagePath + " " + bundleUri + " "
                + qBundleUri + " " + time + " gender " + gender + " hairIDs "
                + hairIDs + " hariBundles " + hairBundles + " imageOriginUri "
                + imageOriginUri + " avatarItemDir " + avatarItemDir, true);
        //TODO query if already exist
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(HISTORY_UID, uid);
        contentValues.put(HISTORY_IMAGE, imagePath);
        contentValues.put(HISTORY_IMAGE_ORIGIN, imageOriginUri);
        contentValues.put(HISTORY_BUNDLE_URI, bundleUri);
        contentValues.put(HISTORY_Q_BUNDLE_URI, qBundleUri);
        contentValues.put(HISTORY_NOBODY_BUNDLE_URI, nobodyBundleUri);
        contentValues.put(HISTORY_TIME_STAMP, time);
        contentValues.put(HISTORY_GENDER, gender);
        contentValues.put(HISTORY_HAIRIDS, hairIDs);
        contentValues.put(HISTORY_HAIRBUNDLES, hairBundles);
        contentValues.put(HISTORY_AVATAR_DIR, avatarItemDir);
        contentValues.put(HISTORY_PHOTO, originPhotoFilePath);
        db.insert("history", null, contentValues);
        return true;
    }

    public boolean deleteHistory(String bundleSrcPath) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("history",
                HISTORY_BUNDLE_URI + "=?", new String[]{bundleSrcPath});
        return true;
    }

    private AvatarItem getPreloadAvatar(
            final String bundleUri, final String imageUri) {
        AvatarItem preloadAvatar = new AvatarItem();
        preloadAvatar.uID = 0;
        preloadAvatar.imagePath = imageUri;
        preloadAvatar.bundleUri = bundleUri;
        preloadAvatar.qBundleUri = "none";
        preloadAvatar.nobodyBundleUri = "none";
        preloadAvatar.gender = Constant.GENDER_NONE + "";
        preloadAvatar.hairIDs = null;
        preloadAvatar.hairBundles = null;
        preloadAvatar.imageOriginUri = imageUri;
        preloadAvatar.isLocalAvatar = true;
        return preloadAvatar;
    }

    private AvatarItem getEffectItem(
            final String bundleUri, final String imageUri) {
        AvatarItem preloadAvatar = new AvatarItem();
        preloadAvatar.uID = 0;
        preloadAvatar.imagePath = imageUri;
        preloadAvatar.bundleUri = bundleUri;
        preloadAvatar.qBundleUri = "none";
        preloadAvatar.nobodyBundleUri = "none";
        preloadAvatar.gender = Constant.GENDER_EFCT + "";
        preloadAvatar.hairIDs = null;
        preloadAvatar.hairBundles = null;
        preloadAvatar.imageOriginUri = imageUri;
        preloadAvatar.isLocalAvatar = true;
        return preloadAvatar;
    }

    public List<AvatarItem> getAllHotItems() {
        List<AvatarItem> items = new ArrayList<>();

        int bundles = bundleFiles.length;
        if (bundles > imageFiles.length) {
            Logger(TAG, "image files is not enough", true);
            bundles = imageFiles.length;
        }
        for (int i = 0; i < bundles; i++) {
            items.add(getPreloadAvatar(bundleFiles[i], imageFiles[i]));
        }
        Logger(TAG, "getAllHotItems", false);

        return items;
    }

    public List<AvatarItem> getAllEffectItems() {
        List<AvatarItem> items = new ArrayList<>();

        int bundles = bundleFiles.length;
        if (bundles > imageFiles.length) {
            Logger(TAG, "image files is not enough", true);
            bundles = imageFiles.length;
        }
        for (int i = 0; i < bundles; i++) {
            items.add(getEffectItem(bundleFiles[i], imageFiles[i]));
        }
        Logger(TAG, "getAllHotItems", false);

        return items;
    }

    public List<AvatarItem> getAllSelfItems(Long uid) {
        List<AvatarItem> items = new ArrayList<>();

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from history", null);
        res.moveToFirst();

        int uidIndex = res.getColumnIndex(HISTORY_UID);
        int imageIndex = res.getColumnIndex(HISTORY_IMAGE);
        int bundleIndex = res.getColumnIndex(HISTORY_BUNDLE_URI);
        int qBundleIndex = res.getColumnIndex(HISTORY_Q_BUNDLE_URI);
        int nobodyBundleIndex = res.getColumnIndex(HISTORY_NOBODY_BUNDLE_URI);
        int timeIndex = res.getColumnIndex(HISTORY_TIME_STAMP);
        int genderIndex = res.getColumnIndex(HISTORY_GENDER);
        int hairIDsIndex = res.getColumnIndex(HISTORY_HAIRIDS);
        int hairBundlesIndex = res.getColumnIndex(HISTORY_HAIRBUNDLES);
        int imageOriginIndex = res.getColumnIndex(HISTORY_IMAGE_ORIGIN);
        int originPhotoIndex = res.getColumnIndex(HISTORY_PHOTO);
        int avatarDirIndex = res.getColumnIndex(HISTORY_AVATAR_DIR);
        while (!res.isAfterLast()) {
            if(res.getInt(uidIndex) == uid) {
                AvatarItem historyItem = new AvatarItem();
                historyItem.uID = res.getInt(uidIndex);
                historyItem.imagePath = res.getString(imageIndex);
                historyItem.bundleUri = res.getString(bundleIndex);
                historyItem.qBundleUri = res.getString(qBundleIndex);
                historyItem.nobodyBundleUri = res.getString(nobodyBundleIndex);
                historyItem.time = res.getString(timeIndex);
                historyItem.gender = res.getString(genderIndex);
                historyItem.hairIDs = res.getString(hairIDsIndex);
                historyItem.hairBundles = res.getString(hairBundlesIndex);
                historyItem.imageOriginUri = res.getString(imageOriginIndex);
                historyItem.avatarDir = res.getString(avatarDirIndex);
                historyItem.photoOriginPath = res.getString(originPhotoIndex);
                historyItem.isLocalAvatar = false;
                items.add(historyItem);
                if (VERBOSE_LOG) {
                    Logger(TAG, historyItem.toString(), false);
                }
            }
            res.moveToNext();

        }
        Logger(TAG, "getAllSelfItems", false);

        return items;
    }

    public Bundle getBundleByAvatarItem(AvatarItem aitem) {
        AvatarItem item = aitem;
        Bundle bundle = new Bundle();
        bundle.putString(Constant.PHOTO_FILE_PATH_INDET_KEY, item.imagePath);
        bundle.putString(Constant.ORIGIN_PHOTO_FILE_PATH_INDET_KEY, item.photoOriginPath);
        bundle.putString(Constant.AVATAR_ITEM_DIR_KEY, item.avatarDir);
        bundle.putString(Constant.FINAL_BUNDLE_SRC_FILE_INTENT_KEY, item.bundleUri);
        bundle.putString(Constant.Q_FINAL_BUNDLE_SRC_FILE_INTENT_KEY, item.qBundleUri);
        bundle.putString(
                Constant.FINAL_NO_BODY_BUNDLE_SRC_FILE_INTENT_KEY, item.nobodyBundleUri);
        bundle.putBoolean("isHistoryItem", true);
        bundle.putBoolean(Constant.IS_LOCAL_PRELOAD_AVATAR, item.isLocalAvatar);
        bundle.putInt("gender", Integer.parseInt(item.gender));
        String hairsStr = item.hairIDs;
        if(hairsStr != null) {
            String[] hairsStrArray = hairsStr.split(",");
            int[] hairsIds = new int[hairsStrArray.length];
            for (int i = 0; i < hairsIds.length; i++) {
                if(!TextUtils.isEmpty(hairsStrArray[i])) {
                    hairsIds[i] = Integer.parseInt(hairsStrArray[i]);
                }
            }
            bundle.putIntArray("hairIds", hairsIds);
        } else {
            bundle.putIntArray("hairIds", null);
        }

        String hairBundles = item.hairBundles;
        if(hairBundles != null) {
            bundle.putStringArray("hairBundleSrcFileNames", hairBundles.split(","));
        } else {
            bundle.putStringArray("hairBundleSrcFileNames", null);
        }
        return bundle;
    }
}