package com.camera.zcc.cameraface.data;

/**
 * Created by gc on 2017/9/12.
 */

public class AvatarItem {
    public int uID;
    public String imagePath;
    public String imageOriginUri;
    public String bundleUri;
    public String qBundleUri;
    public String nobodyBundleUri;
    public String time = "";
    public String gender;
    public String hairIDs;
    public String hairBundles;
    public String photoOriginPath;
    public String avatarDir;
    public boolean isLocalAvatar = false;
    public boolean checked = false;

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    @Override
    public String toString() {
        return imagePath + " " + bundleUri + " " + time + " " + nobodyBundleUri;
    }
}
