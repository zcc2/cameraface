package com.camera.zcc.cameraface;
/*

 * -----------------------------------------------------------------

 * Copyright (C) 2018-2021, by shuzijiayuan, All rights reserved.

 * -----------------------------------------------------------------

 *

 * @Author：  guoyuanzhen

 * @Version： V1

 * @Create：  18/7/18 下午4:38

 * @Changes： (from 18/7/18)

 * @Function：

 */


import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.camera.zcc.cameraface.data.FilterItem;
import com.camera.zcc.cameraface.listener.IFilterItemClickListener;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;


public class FilterAdapter extends RecyclerView.Adapter {

    private static final int ITEM_HEAD = 0;

    private static final int ITEM_CONTENT = 1;

    private LayoutInflater mInflater;

    private IFilterItemClickListener mListener;

    private List<FilterItem> mData;

    private Context mContext;

    private int mColorYellow;

    private int mColorTransparent;

    public FilterAdapter(Context context, List<FilterItem> mData, IFilterItemClickListener listener) {
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.mListener = listener;
        this.mData = mData;
        this.mContext = context;
        mColorYellow = context.getResources().getColor(R.color.white);
        mColorTransparent = context.getResources().getColor(R.color.transparent);
    }
    public void setItems(List<FilterItem> infos) {
        this.mData = infos;
    }
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        /*if (viewType == ITEM_HEAD) {
            return new HeadHolder(mInflater.inflate(R.layout.layout_model_item, parent, false));
        }*/
        return new ImageHolder(mInflater.inflate(
                R.layout.layout_model_item_av, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        /*if (holder instanceof HeadHolder) {
            HeadHolder imageHolder = (HeadHolder) holder;
            imageHolder.mIvHead.setImageResource(R.mipmap.ic_3d);
            imageHolder.mIvHead.setOnClickListener(view -> mListener.onItemClick(-1));
        } else {*/
        final ImageHolder imageHolder = (ImageHolder) holder;
        //AvatarItem info = mIds.get(position - 1);
        FilterItem info = mData.get(position);
//        imageHolder.mIvHead.setImageBitmap(mContext.getResources().getD);
        Glide.with(mContext).load(info.filterIcon).into(imageHolder.mIvHead);

        imageHolder.mIvDeleteAvater.setVisibility(View.GONE);

        if (info.checked) {
            imageHolder.mIvHead.setBackgroundColor(mContext.getResources().getColor(R.color.coolPurpleColor));
        } else {
            imageHolder.mIvHead.setBackgroundColor(mContext.getResources().getColor(R.color.transparent));
        }
        imageHolder.mIvHead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean flag = mListener.onFilterItemClick(position);
                if(flag) {
                    for (int i = 0; i < mData.size(); i++) {
                        mData.get(i).setChecked(false);
                    }

                    //mIds.get(position - 1).setChecked(true);
                    mData.get(position).setChecked(true);
                    notifyDataSetChanged();
                }
            }
        });
        imageHolder.mIvHead.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
//                if(position > 0 && !info.isLocalAvatar){
//                    imageHolder.mIvDeleteAvater.setVisibility(View.VISIBLE);
//                }
                return true;
            }
        });

        imageHolder.mIvDeleteAvater.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mListener != null){
                    if(position > 0 ){
                        //boolean isDelete = mListener.onLongClick(position - 1);
                        boolean isDelete = mListener.onFilterLongClick(position);
                        if(isDelete){
                            notifyDataSetChanged();
                        }else {
                            if(imageHolder.mIvDeleteAvater.getVisibility() == View.VISIBLE){
                                imageHolder.mIvDeleteAvater.setVisibility(View.GONE);
                            }
                        }
                    }
                }
            }
        });
        //}
    }

    @Override
    public int getItemCount() {
        //return mIds.size() + 1;
        return mData.size();
    }

    @Override
    public int getItemViewType(int position) {
        /*if (position == 0) {
            return ITEM_HEAD;
        }*/
        return ITEM_CONTENT;
    }

    private class ImageHolder extends RecyclerView.ViewHolder {

        public ImageView mIvHead;

        public ImageView mIvDeleteAvater;

        public ImageHolder(View itemView) {
            super(itemView);
            mIvHead = (ImageView) itemView.findViewById(R.id.iv_head);
            mIvDeleteAvater = (ImageView) itemView.findViewById(R.id.iv_delete_avater);
        }
    }

    private class HeadHolder extends RecyclerView.ViewHolder {

        public CircleImageView mIvHead;

        public ImageView mIvDeleteAvater;

        public HeadHolder(View itemView) {
            super(itemView);
            mIvHead = (CircleImageView) itemView.findViewById(R.id.iv_head);
            mIvDeleteAvater = (ImageView) itemView.findViewById(R.id.iv_delete_avater);
        }
    }
}
