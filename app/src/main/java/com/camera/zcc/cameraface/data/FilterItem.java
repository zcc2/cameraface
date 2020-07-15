package com.camera.zcc.cameraface.data;
/*

 * -----------------------------------------------------------------

 * Copyright (C) 2018-2021, by shuzijiayuan, All rights reserved.

 * -----------------------------------------------------------------

 *

 * @Author：  guoyuanzhen

 * @Version： V1

 * @Create：  18/7/18 下午4:28

 * @Changes： (from 18/7/18)

 * @Function：

 */

public class FilterItem {

    public String filterName;
    public int filterIcon;
    public boolean checked = false;

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }
}
