package com.faceunity.fup2a;

import android.content.Context;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.faceunity.fup2a.gles.LandmarksPoints;
import com.faceunity.fup2a.misc.Constant;
import com.faceunity.wrapper.faceunity;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static com.faceunity.fup2a.misc.MiscUtil.Logger;

public class FaceUnity {
    private final static String TAG = FaceUnity.class.getName();
    private final static boolean SUPPORT_AVATAR = false;
    private final static int SET_MAX_FACES_NUMBER = 4;

    public final static float DEF_COLOR_LEVEL = 0.2f;
    public final static float DEF_BLUR_LEVEL = 6.0f;
    public final static float DEF_CHEECK_THIN = 1.0f;
    public final static float DEF_ENLARGE_EYE = 0.5f;
    public final static float DEF_RED_LEVEL = 0.5f;
    public final static float DEF_SHAPE_LEVEL = 0.5f;
    public final static int DEF_FACE_SHAPE = 3;

    // beautify parameters
    public static float colorLevel = DEF_COLOR_LEVEL;
    public static float blurLevel  = DEF_BLUR_LEVEL;
    public static float cheeckThin = DEF_CHEECK_THIN;
    public static float enlargeEye = DEF_ENLARGE_EYE;
    public static float redLevel   = DEF_RED_LEVEL;
    public static float shapeLevel = DEF_SHAPE_LEVEL;
    public static int faceShape = DEF_FACE_SHAPE;
    public static boolean isDefault = true;

    public static enum BUNDLE_ITEM {
        EFT3(0),    /* effects bundle(3D) */
        WMARK(1),   /* water mark bundle */
        BCKG(2),    /* background bunlde */
        CLOTH(3),   /* cloth bundle      */
        DCRTN(4),   /* decoration bundle */
        GLASS(5),   /* glass bundle      */
        HAIR(6),    /* hair bundle       */
        ANIM(7),    /* animation bundle  */
        QBODY(8),   /* q body bundle     */
        CTRL(9),    /* control bundle    */
        BODY(10),   /* body bundle       */
        BUTY(11),   /* beauty bundle     */
        EFT2(12),   /* effects bundle(2D)*/
        MOSC(13),   /* mosaic bundle     */
        COUNT(14);

        // Constructor function
        private BUNDLE_ITEM(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
        private int value;
    }
    private static volatile boolean isSetup = false;
    private static LandmarksPoints marksPoints = null;
    private static Context context = null;
    private static int[] bundleItems =
            new int[BUNDLE_ITEM.COUNT.value()];
    private static int[] updateItems =
            new int[BUNDLE_ITEM.COUNT.value()];
    /* Contract bundle item */
    private static int cnrtItem = 0;
    /* Body bundle item */
    private static int bodyItem = 0;
    /* Control bundle item */
    private static int ctrlItem = 0;

    private static float[] landmarksData = new float[150];
    private static float[] expressionData = new float[46];
    private static float[] rotationData = new float[4];
    private static float[] pupilPosData = new float[2];
    private static float[] rotationModeData = new float[1];

    private static boolean isChangeAvatar = false;
    private static boolean isChangeEffect = false;
    private static boolean isAvatarBundle = false;
    private static String[] lastBundleFiles =
            new String[BUNDLE_ITEM.COUNT.value()];
    private static String[] newBundleFiles =
            new String[BUNDLE_ITEM.COUNT.value()];

    /**
     * Setup face unity context environment
     *
     * @param context the context object
     */
    public static void setup(@NonNull Context context) {
        if(isSetup) {
            Logger(TAG, "face unity has created, return",
                    true);
            return;
        } else {
            Arrays.fill(lastBundleFiles, "none");
            Arrays.fill(newBundleFiles, "none");
            FaceUnity.context = context;
            // Setup authority
            fuSetupAuthority();
            // Load com items
            loadCommonItems();
            isSetup = true;
        }
    }

    /**
     * Set land marks points object
     *
     * @param marksPoints land marks points object
     */
    public static void setMarksPoints(LandmarksPoints marksPoints) {
        synchronized (FaceUnity.class) {
            FaceUnity.marksPoints = marksPoints;
        }
    }

    /**
     * Change face unity with input bundle parameter
     *
     * @param bundle bundle that include bundle items file name
     */
    public static void changeFUnity(@Nullable final Bundle bundle) {
        Logger(TAG, "changeAvatar: " + bundle, true);
        if (bundle != null) {
            int gender = bundle.getInt(Constant.GENDER_INTENT_KEY);
            if(gender != Constant.GENDER_EFCT) {
                loadAvatarItems(bundle, gender);
            } else {
                loadEffectItem(bundle);
            }
        } else {
            Logger(TAG, "input parameter bundle is null",
                    true);
        }
    }

    /**
     * Get change face unity under way flag
     *
     * @return change face unity flag, true: change face unity
     * is under way, false: not in change face unity
     */
    public static boolean isInChangeFaceUnity() {
        return (isChangeAvatar || isChangeEffect);
    }

    /**
     * Is avatar bundle, true: is avatar bundle, false: is
     * beautify bundle
     *
     * @return avatar bundle flag
     */
    public static boolean isAvatarBundle() {
        return isAvatarBundle;
    }

    /**
     * Create bundle item with file name
     *
     * @param fileName bundle file name
     * @param idx bundle item array index
     * @param assets file is assets flag
     * @return bundle item identifier
     */
    public static int createItem(
            String fileName, BUNDLE_ITEM idx, boolean assets) {
        Logger(TAG, "bundle file name: " + fileName,
                true);
        synchronized (FaceUnity.class) {
            newBundleFiles[idx.value()] = fileName;
        }
        return createItem(fileName, assets);
    }

    /**
     * Update bundle item identifier to bundle item array, it must
     * be called in the same OpenGL ES thread context as face unity
     * process
     *
     * @param id bundle item identifier
     * @param idx bundle item array index
     * @param bind bind body flag
     */

    public static void updateItem(
            int id, BUNDLE_ITEM idx, boolean bind) {
        if(idx == BUNDLE_ITEM.CTRL) {
            destroyItem(ctrlItem, true);
            bundleItems[idx.value()] = ctrlItem = 0;
        }
        updateItem(id, idx.value(), bind);
    }

    /**
     * Set body parameter value with its key, it must be called in
     * the same OpenGL ES thread context as face unity
     *
     * @param key
     * @param value
     */
    public static void setBodyParam(String key, double value) {
        int bodyItem = bundleItems[BUNDLE_ITEM.BODY.value()];
        if (bodyItem > 0) {
            faceunity.fuItemSetParam(bodyItem, key, value);
        }
    }

    /**
     * Set beautify parameter value with its key, it must be called in
     * the same OpenGL ES thread context as face unity process
     *
     * @param key beautify key
     * @param value beautify value
     */
    public static void setBeautifyParam(String key, double value) {
        int butyItem = bundleItems[BUNDLE_ITEM.BUTY.value()];
        if(butyItem > 0) {
            faceunity.fuItemSetParam(butyItem, key, value);
        }
    }

    /**
     * Beautify process, it need texture as input, and output texture
     * after beautify process, it must be called in the OpenGL ES
     * thread context
     *
     * @param texId texture identifier
     * @param flags face unity flags
     * @param cW preview picture width
     * @param cH preview picture height
     * @param rotation image rotation
     * @param frameId frame identifier
     *
     * @return texture identifier after beautify process
     */
    public static int fuBeautifyImage(int texId,
                                       int flags,
                                       int cW,
                                       int cH,
                                       int rotation,
                                       int frameId) {
        //long start = System.nanoTime();
        int[] items = changeFUnity(false, rotation, new int[]{});
        flags |= faceunity.FU_ADM_FLAG_EXTERNAL_OES_TEXTURE;
        /* Face unity process */
        int fuTex = faceunity.fuBeautifyImage(
                texId, flags, cW, cH, frameId, items);
        //long end = System.nanoTime();
        //Logger(TAG, "consumer time: " + (end - start)/1000000, true);
        return fuTex;
    }

    /**
     * Face unity process, it need image data and texture as input,
     * output texture after face unity process, it must be called
     * in the OpenGL ES thread context
     *
     * @param img input image data buffer
     * @param texId input texture identifier
     * @param flags face unity flags
     * @param cW  input image width
     * @param cH  input image height
     * @param rotation image rotation
     * @param frameId frame identifier
     * @return texture identifier processed
     */
    public static int fuDualInputToTexture(byte[] img,
                                           int texId,
                                           int flags,
                                           int cW,
                                           int cH,
                                           int rotation,
                                           int frameId, int[] filter) {
        //long start = System.nanoTime();
        int[] items = changeFUnity(false, rotation, filter);
        flags |= faceunity.FU_ADM_FLAG_EXTERNAL_OES_TEXTURE;
        /* Face unity process */
        int fuTex = faceunity.fuDualInputToTexture(
                img, texId, flags, cW, cH, frameId, items);
        //long end = System.nanoTime();
        //Logger(TAG, "consumer time: " + (end - start)/1000000, true);
        return fuTex;
    }

    /**
     * Face unity process, it need image data and texture as input,
     * output texture after face unity process, it must be called
     * in the OpenGL ES thread context
     *
     * @param img input image data buffer
     * @param texId input texture identifier
     * @param flags face unity flags
     * @param cW input image width
     * @param cH input image height
     * @param rotation image rotation
     * @param frameId frame identifier
     * @param oW output image width
     * @param oH output image height
     * @param oImg output image data buffer
     * @return texture identifier processed
     */
    public static int fuDualInputToTexture(byte[] img,
                                           int texId,
                                           int flags,
                                           int cW,
                                           int cH,
                                           int rotation,
                                           int frameId,
                                           int oW,
                                           int oH,
                                           byte[] oImg, int[] filter) {
        //long start = System.nanoTime();
        int[] items = changeFUnity(false, rotation, filter);
        flags |= faceunity.FU_ADM_FLAG_EXTERNAL_OES_TEXTURE;
        /* Face unity process */
        int fuTex = faceunity.fuDualInputToTexture(img,
                texId, flags, cW, cH, frameId, items, oW, oH, oImg);
        //long end = System.nanoTime();
        //Logger(TAG, "consumer time: " + (end - start)/1000000, true);
        return fuTex;
    }

    /**
     * Face unity process, it need nv21 image data as input, output
     * i420 image data and texture after face unity process, it
     * must be called in the OpenGL ES thread context
     *
     * @param img input image data buffer
     * @param flags face unity flags
     * @param cW  input image width
     * @param cH  input image height
     * @param rotation image rotation
     * @param face camera face value
     * @param frameId frame identifier
     * @return texture identifier processed
     */
    public static int fuRenderToNV21Image(byte[] img,
                                          int flags,
                                          int cW,
                                          int cH,
                                          int rotation,
                                          int face,
                                          int frameId) {
        //long start = System.nanoTime();
        int[] items = changeFUnity(false, rotation, new int[]{0});
        if(face != Camera.CameraInfo.CAMERA_FACING_FRONT) {
            flags |= faceunity.FU_ADM_FLAG_FLIP_X;
        }
        /* Face unity process */
        int fuTex = faceunity.fuRenderToNV21Image(img,
                cW,
                cH,
                frameId,
                items,
                flags);
        //long end = System.nanoTime();
        //Logger(TAG, "consumer time: " + (end - start)/1000000, true);
        return fuTex;
    }

    /**
     * Face unity process, it need image data as input, output image
     * data and texture after face unity process, it must be called
     * in the OpenGL ES thread context
     *
     * @param img input image data buffer
     * @param flags face unity flags
     * @param cW  input image width
     * @param cH  input image height
     * @param rotation image rotation
     * @param face camera face value
     * @param frameId frame identifier
     * @param oW  output image width
     * @param oH  output image height
     * @param oImg output image data buffer
     * @return texture identifier processed
     */
    public static int fuAvatarToImage(byte[] img,
                                        int flags,
                                        int cW,
                                        int cH,
                                        int rotation,
                                        int face,
                                        int frameId,
                                        int oW,
                                        int oH,
                                        byte[] oImg) {
        //long start = System.nanoTime();
        int[] items = changeFUnity(true, rotation,new int[]{0});
        /* Face unity process */
        int isTracking = fuTracking(img, cW, cH, rotation, face);
        int fuTex = faceunity.fuAvatarToImage(pupilPosData,
                expressionData,
                rotationData,
                rotationModeData,
                flags,
                cW,
                cH,
                frameId,
                items,
                isTracking,
                oW, oH, oImg);
        //long end = System.nanoTime();
        //Logger(TAG, "consumer time: " + (end - start)/1000000, true);
        return fuTex;
    }

    /**
     * Face unity process, it need image data as input, output and
     * texture after face unity process, it must be called in the
     * OpenGL ES thread context
     *
     * @param img input image data buffer
     * @param flags face unity flags
     * @param cW  input image width
     * @param cH  input image height
     * @param rotation image rotation
     * @param face camera face value
     * @param frameId frame identifier
     * @return texture identifier processed
     */
    public static int fuAvatarToTexture(byte[] img,
                                      int flags,
                                      int cW,
                                      int cH,
                                      int rotation,
                                      int face,
                                      int frameId) {
        //long start = System.nanoTime();
        int[] items = changeFUnity(true, rotation,new int[]{0});
        /* Face unity process */
        int isTracking = fuTracking(img, cW, cH, rotation, face);
        int fuTex = faceunity.fuAvatarToTexture(pupilPosData,
                expressionData,
                rotationData,
                rotationModeData,
                flags,
                cW,
                cH,
                frameId,
                items,
                isTracking);
        //long end = System.nanoTime();
        //Logger(TAG, "consumer time: " + (end - start)/1000000, true);
        return fuTex;
    }

    /**
     * Get track face number
     *
     * @return face number tracked
     */
    public static int fuIsTracking() {
        return faceunity.fuIsTracking();
    }

    /**
     * Reset beautify parameters to default value
     */
    public static void resetBeautifyParams() {
        colorLevel = DEF_COLOR_LEVEL;
        blurLevel  = DEF_BLUR_LEVEL;
        cheeckThin = DEF_CHEECK_THIN;
        enlargeEye = DEF_ENLARGE_EYE;
        redLevel   = DEF_RED_LEVEL;
        shapeLevel = DEF_SHAPE_LEVEL;
        faceShape = DEF_FACE_SHAPE;
        isDefault = false;
    }

    /**
     * Bind bundle items to body
     */
    public static void bindBody() {
        bindItem(bundleItems[BUNDLE_ITEM.CTRL.value()],
                true);
        for(int i = BUNDLE_ITEM.CLOTH.value();
            i < BUNDLE_ITEM.CTRL.value(); i++) {
            bindItem(bundleItems[i], true);
        }
    }

    /**
     * Unbind bundle items from body
     */
    public static void unbindBody() {
        for(int i = BUNDLE_ITEM.CLOTH.value();
            i < BUNDLE_ITEM.BODY.value(); i++) {
            unbindItem(bundleItems[i], true);
        }
    }

    /**
     * Clear readback related info
     */
    public static void fuClearReadbackRelated() {
        faceunity.fuClearReadbackRelated();
    }

    /**
     * Camera change
     */
    public static void fuOnCameraChange() {
        faceunity.fuOnCameraChange();
    }

    /**
     * Device lost
     */
    public static void fuOnDeviceLost() {
        faceunity.fuOnDeviceLost();
    }

    /**
     * Destroy body resources
     *
     */
    public static void destroyBody() {
        destroyBody(bundleItems);
    }

    /**
     * Destroy no body resources
     *
     */
    public static void destroyNoBody() {
        destroyNoBody(bundleItems);
    }

    /**
     * Destroy beautify resources
     *
     */
    public static void destroyBeautify() {
        destroyBeautify(bundleItems);
    }

    /**
     * Destroy all resources
     *
     */
    public static void destroy() {
        destroyNoBody(bundleItems);
        destroyBody(bundleItems);
        destroyBeautify(bundleItems);
        if(ctrlItem > 0) {
            faceunity.fuDestroyItem(
                    ctrlItem);
            ctrlItem = 0;
        }
        if(cnrtItem > 0) {
            faceunity.fuDestroyItem(
                    cnrtItem);
            cnrtItem = 0;
        }
        faceunity.fuDestroyAllItems();
        fuOnDeviceLost();
    }

    private static
        int[] changeFUnity(boolean isAvatar, int rotation, int[] filter) {
        int[] items = new int[BUNDLE_ITEM.COUNT.value()];
        Arrays.fill(items, 0);
        synchronized (FaceUnity.class) {
            if(isChangeAvatar) {
                updateItem(updateItems[BUNDLE_ITEM.BODY.value()],
                        BUNDLE_ITEM.BODY.value(), false);
                updateItem(updateItems[BUNDLE_ITEM.CTRL.value()],
                        BUNDLE_ITEM.CTRL.value(), true);
                setBodyParam("rot_delta",   0.0f);
                setBodyParam("scale_delta", 0.0f);
                for(int i = BUNDLE_ITEM.EFT3.value();
                    i < BUNDLE_ITEM.CLOTH.value(); i++) {
                    updateItem(updateItems[i], i, false);
                }
                for(int i = BUNDLE_ITEM.CLOTH.value();
                    i < BUNDLE_ITEM.CTRL.value(); i++) {
                    updateItem(updateItems[i], i, true);
                }
                Arrays.fill(updateItems, 0);
                isChangeAvatar = false;
                isAvatarBundle = true;
            }
            if(isChangeEffect) {
                updateItem(updateItems[BUNDLE_ITEM.EFT2.value()],
                        BUNDLE_ITEM.EFT2.value(), false);
                updateItem(updateItems[BUNDLE_ITEM.MOSC.value()],
                        BUNDLE_ITEM.MOSC.value(), false);
                Arrays.fill(updateItems, 0);
                isChangeEffect = false;
                isAvatarBundle = false;
            }
            if(isAvatar) {
                for(int i = BUNDLE_ITEM.EFT3.value();
                    i < BUNDLE_ITEM.CLOTH.value(); i++) {
                    items[i] = bundleItems[i];
                }
                items[BUNDLE_ITEM.BODY.value()] = bodyItem;
            } else {
                int eft2Item =
                    bundleItems[BUNDLE_ITEM.EFT2.value()];
                if(eft2Item > 0) {
                    faceunity.fuItemSetParam(eft2Item,
                            "isAndroid", 1.0);
                    faceunity.fuItemSetParam(eft2Item,
                            "rotationAngle",
                            360 - rotation);
                }
                items[BUNDLE_ITEM.EFT2.value()] = eft2Item;
                for(int i = BUNDLE_ITEM.BUTY.value();
                    i < BUNDLE_ITEM.COUNT.value(); i++) {
                    items[i] =  bundleItems[i];
                }
            }
        }

        items[BUNDLE_ITEM.GLASS.value()] = filter[0];
        return items;
    }

    private static void fuSetupAuthority() {
        String athority = "v3.mp3";
        InputStream is = null;
        try {
            is = context.getAssets().open(athority);
            if(is.available() > 0) {
                byte[] v3data = new byte[is.available()];
                if(v3data != null) {
                    int read = is.read(v3data);
                    if(read > 0) {
                        faceunity.fuSetup(v3data, null, authpack.A());
                        Logger(TAG, "fuSetup", true);
                    }
                } else {
                    Logger(TAG, "read stream failed", true);
                }
            } else {
                Logger(TAG, "stream data " +
                        "not available: " + is.available(), true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static int createItem(String fileName, boolean assets) {
        int newItem = 0;
        if (!fileName.equals("none")) {
            InputStream is = null;
            try {
                if (assets) {
                    is = context.getAssets().open(fileName);
                } else {
                    File file = new File(fileName);
                    is = new FileInputStream(file);
                }
                if (is.available() > 0) {
                    final byte[] itemData = new byte[is.available()];
                    if (itemData != null) {
                        int read = is.read(itemData);
                        if (read > 0) {
                            newItem = faceunity.fuCreateItemFromPackage(itemData);
                        }
                    } else {
                        Logger(TAG, "read stream failed", true);
                    }
                } else {
                    Logger(TAG, "stream data " +
                            "not available: " + is.available(), true);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if(is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return newItem;
    }

    private static void updateItem(int id, int i, boolean bind) {
        if(i == BUNDLE_ITEM.BODY.value()) {
            destroyBody(bundleItems);
            bundleItems[i] = bodyItem = id;
        } else {
            if(i == BUNDLE_ITEM.CTRL.value()) {
                unbindItem(bundleItems[i], bind);
            } else {
                destroyItem(bundleItems[i], bind);
                bundleItems[i] = 0;
            }

            if(bind && id > 0) {
                if(bodyItem > 0 && cnrtItem > 0) {
                    faceunity.fuAvatarBindItems(bodyItem,
                            new int[]{id}, new int[]{cnrtItem});
                } else {
                    Logger(TAG,"body or contract invalid: "
                            + bodyItem + ","+ cnrtItem,true);
                }
            }
            bundleItems[i] = id;
        }
    }

    private static void loadAvatarItems(Bundle bundle, int gender) {
        String bodyFileName =
                bundle.getString(Constant.FINAL_BUNDLE_SRC_FILE_INTENT_KEY);
        Logger(TAG, "body file name: " + bodyFileName, true);
        String qBodyFileName =
                bundle.getString(Constant.Q_FINAL_BUNDLE_SRC_FILE_INTENT_KEY);
        Logger(TAG, "q body file name: " + qBodyFileName, true);
        String[] airBundleSrcNames =
                bundle.getStringArray(Constant.HAIR_BUNDLE_SRC_FILE_INTENT_KEY);
        String hairFileName =
                (airBundleSrcNames != null)? airBundleSrcNames[0] : "none";
        Logger(TAG, "hair file name: " + hairFileName, true);

        boolean assets =
                bundle.getBoolean(Constant.IS_LOCAL_PRELOAD_AVATAR);
        Logger(TAG, "local avatar: " + assets, true);

        String backFileName = "none";
        // Q version p2a checking
        if(!qBodyFileName.equals("none")) {
            backFileName = "baibeijing.bundle";
            bodyFileName = qBodyFileName;
            qBodyFileName = (gender
                    == 0) ? "male_body.bundle" : "female_body.bundle";
        }
        String ctrlFileName = "none";
        if(gender != Constant.GENDER_NONE) {
            ctrlFileName = "controller.bundle";
        }
        synchronized (FaceUnity.class) {
            newBundleFiles[BUNDLE_ITEM.BODY.value()] = bodyFileName;
            newBundleFiles[BUNDLE_ITEM.CTRL.value()] = ctrlFileName;
            newBundleFiles[BUNDLE_ITEM.HAIR.value()] = hairFileName;
            newBundleFiles[BUNDLE_ITEM.QBODY.value()]= qBodyFileName;
            if(isSameAvatar()) {
                Logger(TAG, "same face unity, ignore it",
                        true);
                return;
            } else {
                updateAvatar();
            }
        }

        int bckgItem = createItem(backFileName,true);
        int bodyItem = createItem(bodyFileName, assets);
        int hairItem = createItem(hairFileName, assets);
        int qBodyItem= createItem(qBodyFileName,true);
        synchronized (FaceUnity.class) {
            Arrays.fill(updateItems, 0);
            updateItems[BUNDLE_ITEM.BCKG.value()] = bckgItem;
            updateItems[BUNDLE_ITEM.BODY.value()] = bodyItem;
            if(ctrlFileName.equals("none")) {
                updateItems[BUNDLE_ITEM.CTRL.value()] = 0;
            } else {
                updateItems[BUNDLE_ITEM.CTRL.value()] = ctrlItem;
            }
            updateItems[BUNDLE_ITEM.HAIR.value()]  = hairItem;
            updateItems[BUNDLE_ITEM.QBODY.value()] = qBodyItem;
            isChangeAvatar = true;
        }
    }

    private static void loadEffectItem(Bundle bundle) {
        String effectFileName =
                bundle.getString(Constant.FINAL_BUNDLE_SRC_FILE_INTENT_KEY);
        Logger(TAG,
                "body file name: " + effectFileName, true);
        String mosaicFileName = "none";//"mosaic.bundle";
        boolean assets =
                bundle.getBoolean(Constant.IS_LOCAL_PRELOAD_AVATAR);
        Logger(TAG, "local avatar: " + assets, true);

        synchronized (FaceUnity.class) {
            newBundleFiles[BUNDLE_ITEM.EFT2.value()] = effectFileName;
            newBundleFiles[BUNDLE_ITEM.MOSC.value()] = mosaicFileName;
            if(isSameBeautify()) {
                Logger(TAG, "same face unity, ignore it",
                        true);
                return;
            } else {
                updateBeautify();
            }
        }

        int effectItem = createItem(effectFileName, assets);
        int mosaicItem = createItem(mosaicFileName, assets);
        synchronized (FaceUnity.class) {
            Arrays.fill(updateItems, 0);
            updateItems[BUNDLE_ITEM.EFT2.value()] = effectItem;
            updateItems[BUNDLE_ITEM.MOSC.value()] = mosaicItem;
            isChangeEffect = true;
        }
    }

    private static void loadCommonItems() {
        String fileName = "";
        if(SUPPORT_AVATAR) {
            fileName = "contract_free_items.bundle";
            // Create contract bundle item
            cnrtItem = createItem(fileName, true);
            // Create control bundle item
            fileName = "controller.bundle";
            ctrlItem = createItem(fileName, true);
        }
        // Create beautify bundle item
        fileName = "face_beautification.mp3";
        int butyItem = FaceUnity.createItem(
                fileName, true);
        FaceUnity.updateItem(butyItem,
                BUNDLE_ITEM.BUTY, false);
        if(SET_MAX_FACES_NUMBER > 1) {
            // Set max faces
            faceunity.fuSetMaxFaces(SET_MAX_FACES_NUMBER);
        }
    }

    private static int fuTracking(
            byte[] img, int w, int h, int rotation, int type) {
        faceunity.fuTrackFace(img, 0, w, h);

        /**
         * landmarks
         */
        Arrays.fill(landmarksData, 0.0f);
        faceunity.fuGetFaceInfo(0, "landmarks", landmarksData);
        if (marksPoints != null) {
            marksPoints.refresh(landmarksData, w, h, rotation, type);
        }

        /**
         * rotation
         */
        Arrays.fill(rotationData, 0.0f);
        faceunity.fuGetFaceInfo(0, "rotation", rotationData);

        /**
         * expression
         */
        Arrays.fill(expressionData, 0.0f);
        faceunity.fuGetFaceInfo(0, "expression", expressionData);

        /**
         * pupil pos
         */
        Arrays.fill(pupilPosData, 0.0f);
        faceunity.fuGetFaceInfo(0, "pupil_pos", pupilPosData);

        /**
         * rotation mode
         */
        Arrays.fill(rotationModeData, 0.0f);
        //rotationModeData[0] = -1.0f;
        faceunity.fuGetFaceInfo(0, "rotation_mode", rotationModeData);

        int isTracking = faceunity.fuIsTracking();
        //rotation 是一个4元数，如果还没获取到，就使用1,0,0,0
        if (isTracking <= 0) {
            rotationData[3] = 1.0f;
        }

        /**
         * adjust rotation mode
         */
        if (isTracking <= 0) {
            rotationModeData[0] = (360 - rotation) / 90;
        }
        return isTracking;
    }

    private static boolean isSameAvatar() {
        for(int i = BUNDLE_ITEM.CLOTH.value();
            i < BUNDLE_ITEM.BUTY.value(); i++) {
            if(!lastBundleFiles[i].equals(newBundleFiles[i])) {
                return false;
            }
        }
        return true;
    }

    private static void updateAvatar() {
        for(int i = BUNDLE_ITEM.CLOTH.value();
            i < BUNDLE_ITEM.BUTY.value(); i++) {
            lastBundleFiles[i] = newBundleFiles[i];
        }
    }

    private static boolean isSameBeautify() {
        for(int i = BUNDLE_ITEM.EFT2.value();
            i < BUNDLE_ITEM.COUNT.value(); i++) {
            if(!lastBundleFiles[i].equals(newBundleFiles[i])) {
                return false;
            }
        }
        return true;
    }

    private static void updateBeautify() {
        for(int i = BUNDLE_ITEM.EFT2.value();
            i < BUNDLE_ITEM.COUNT.value(); i++) {
            lastBundleFiles[i] = newBundleFiles[i];
        }
    }

    private static void bindItem(int item, boolean bind) {
        if(bind && item > 0) {
            if(bodyItem > 0 && cnrtItem > 0) {
                faceunity.fuAvatarBindItems(bodyItem,
                        new int[]{item}, new int[]{cnrtItem});
            } else {
                Logger(TAG,"body or contract invalid: "
                        + bodyItem + ","+ cnrtItem,true);
            }
        }
    }

    private static void unbindItem(int item, boolean bind) {
        if(item > 0) {
            if(bind && bodyItem > 0) {
                faceunity.fuAvatarUnbindItems(
                        bodyItem, new int[]{item});
            }
        }
    }

    private static void destroyItem(int item, boolean bind) {
        unbindItem(item, bind);
        if(item > 0) {
            faceunity.fuDestroyItem(item);
        }
    }

    private static void destroyBody(int[] items) {
        for(int i = BUNDLE_ITEM.CLOTH.value();
            i < BUNDLE_ITEM.CTRL.value(); i++) {
            destroyItem(items[i], true);
            items[i] = 0;
        }
        unbindItem(items[BUNDLE_ITEM.CTRL.value()],
                true);
        items[BUNDLE_ITEM.CTRL.value()] = 0;
        if(bodyItem > 0) {
            faceunity.fuDestroyItem(bodyItem);
            items[BUNDLE_ITEM.BODY.value()] = 0;
            bodyItem = 0;
        }
    }

    private static void destroyNoBody(int[] items) {
        for(int i = BUNDLE_ITEM.EFT3.value();
            i < BUNDLE_ITEM.CLOTH.value(); i++) {
            destroyItem(items[i], false);
            items[i] = 0;
        }
    }

    private static void destroyBeautify(int[] items) {
        for (int i = BUNDLE_ITEM.BUTY.value();
             i < BUNDLE_ITEM.COUNT.value(); i++) {
            destroyItem(items[i], false);
            items[i] = 0;
        }
    }
}
