package com.camera.zcc.cameraface;

import android.content.Context;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.camera.zcc.cameraface.data.AvatarItem;
import com.camera.zcc.cameraface.listener.IItemClickListener;
import com.camera.zcc.cameraface.widget.CircleImageView;
import com.faceunity.fup2a.misc.MiscUtil;

import java.util.List;



/**
 * Created by gc on 2017/9/12.
 */

public class ImageAdapter extends RecyclerView.Adapter {

    private static final int ITEM_HEAD = 0;

    private static final int ITEM_CONTENT = 1;

    private LayoutInflater mInflater;

    private IItemClickListener mListener;

    private List<AvatarItem> mIds;

    private Context mContext;

    private int mColorYellow;

    private int mColorTransparent;

    public ImageAdapter(Context context, List<AvatarItem> infos, IItemClickListener listener) {
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.mListener = listener;
        this.mIds = infos;
        this.mContext = context;
        mColorYellow = context.getResources().getColor(R.color.coolPurpleColor);
        mColorTransparent = context.getResources().getColor(R.color.transparent);
    }
    public void setItems(List<AvatarItem> infos) {
        this.mIds = infos;
    }
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        /*if (viewType == ITEM_HEAD) {
            return new HeadHolder(mInflater.inflate(R.layout.layout_model_item, parent, false));
        }*/
        return new ImageHolder(mInflater.inflate(
                R.layout.layout_model_item, parent, false));
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
            final AvatarItem info = mIds.get(position);
            imageHolder.mIvHead.setImageBitmap(info.isLocalAvatar
                    ? MiscUtil.getBitmapFromAssets(mContext, info.imageOriginUri)
                    : MiscUtil.getBitmapFromUri(mContext, Uri.parse(info.imageOriginUri)));

            if(info.isLocalAvatar){
                imageHolder.mIvDeleteAvater.setVisibility(View.GONE);
            }

            if (info.checked) {
                imageHolder.mIvHead.setBorderColor(mColorYellow);
            } else {
                imageHolder.mIvHead.setBorderColor(mColorTransparent);
            }
        imageHolder.mIvHead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean flag = mListener.onItemClick(position);
                if(flag) {
                    for (int i = 0; i < mIds.size(); i++) {
                        mIds.get(i).setChecked(false);
                    }

                    //mIds.get(position - 1).setChecked(true);
                    mIds.get(position).setChecked(true);
                    notifyDataSetChanged();
                }
            }
        });
            imageHolder.mIvHead.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if(position > 0 && !info.isLocalAvatar){
                        imageHolder.mIvDeleteAvater.setVisibility(View.VISIBLE);
                    }
                    return true;
                }
            });

            imageHolder.mIvDeleteAvater.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(mListener != null){
                        if(position > 0 ){
                            //boolean isDelete = mListener.onLongClick(position - 1);
                            boolean isDelete = mListener.onLongClick(position);
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
        return mIds.size();
    }

    @Override
    public int getItemViewType(int position) {
        /*if (position == 0) {
            return ITEM_HEAD;
        }*/
        return ITEM_CONTENT;
    }

    private class ImageHolder extends RecyclerView.ViewHolder {

        public CircleImageView mIvHead;

        public ImageView mIvDeleteAvater;

        public ImageHolder(View itemView) {
            super(itemView);
            mIvHead = (CircleImageView) itemView.findViewById(R.id.iv_head);
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