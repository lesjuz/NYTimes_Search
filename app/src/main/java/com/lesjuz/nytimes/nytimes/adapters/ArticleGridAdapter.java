package com.lesjuz.nytimes.nytimes.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.lesjuz.nytimes.R;
import com.lesjuz.nytimes.nytimes.models.Article;


import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Lesjuz on 3/19/2017.
 */

public class ArticleGridAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Article item);


    }

    private List<Article> articles;
    private Context mContext;
    private final OnItemClickListener listener;

    private final static int ThumbArticle = 0;
    private final static int NoThumbArticle    = 1;

    class ThumbArticleViewHolder extends RecyclerView.ViewHolder {


        @BindView(R.id.tvHeadline) TextView headline;
        @BindView(R.id.ivThumbnail)
        ImageView thumbnail;
        @BindView(R.id.tvCategory) TextView new_desk;
        @BindView(R.id.tvSnippet) TextView snippet;

        ThumbArticleViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }


    class NoThumbMovieViewHolder extends RecyclerView.ViewHolder {



        @BindView(R.id.tvHeadline2)
        TextView tvTitle;


        NoThumbMovieViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);

        }


    }
    private class RecyclerViewSimpleTextViewHolder extends RecyclerView.ViewHolder {
        RecyclerViewSimpleTextViewHolder(View v) {
            super(v);
        }}

    public ArticleGridAdapter(Context context, List<Article> articles, OnItemClickListener listener){
        this.articles = articles;
        this.mContext=context;
        this.listener = listener;
    }

    private Context getContext() {
        return mContext;
    }

    @Override
    public int getItemViewType(int position) {
        Article mv=articles.get(position);
        if (mv.getThumbNail() != null) {
            return ThumbArticle;
        }
        return NoThumbArticle;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        RecyclerView.ViewHolder viewHolder;
        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());

        switch (viewType) {
            case ThumbArticle:
                View v1 = inflater.inflate(R.layout.article_item_with_thumbnail, viewGroup, false);
                viewHolder = new ThumbArticleViewHolder(v1);
                break;
            case NoThumbArticle:
                View v2 = inflater.inflate(R.layout.article_item, viewGroup, false);
                viewHolder = new NoThumbMovieViewHolder(v2);
                break;
            default:
                View v = inflater.inflate(android.R.layout.simple_list_item_1, viewGroup, false);
                viewHolder = new RecyclerViewSimpleTextViewHolder(v);
                break;
        }
        return viewHolder;
    }
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        switch (viewHolder.getItemViewType()) {
            case ThumbArticle:
                ThumbArticleViewHolder vh1 = (ThumbArticleViewHolder) viewHolder;
                configureViewHolder1(vh1, position,listener);
                break;
            case NoThumbArticle:
                NoThumbMovieViewHolder vh2 = (NoThumbMovieViewHolder) viewHolder;
                configureViewHolder2(vh2, position,listener);
                break;
            default:
                break;
        }

    }

    private void configureViewHolder2(NoThumbMovieViewHolder vh2, int position, final OnItemClickListener listener) {
        final Article mv=articles.get(position);

        TextView tv1 = vh2.tvTitle;
        tv1.setText(mv.getHeadLines());

        vh2.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onItemClick(mv);
            }
        });
    }

    private void configureViewHolder1(ThumbArticleViewHolder vh1, int position, final OnItemClickListener listener) {
        final Article mv=articles.get(position);

        TextView newDesk=vh1.new_desk;
        TextView headline = vh1.headline;
        ImageView img=vh1.thumbnail;
        TextView snp=vh1.snippet;

        headline.setText(mv.getHeadLines());

        img.setImageResource(0);
        Glide.with(getContext())
                .load(mv.getThumbNail())
                .into(img);
        if (mv.getNew_desk()!="null" && mv.getNew_desk()!="None" && mv.getNew_desk()!=""){
           newDesk.setVisibility(View.VISIBLE);
            newDesk.setText(mv.getNew_desk());
        }

        snp.setText(mv.getSnippet());

        vh1.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onItemClick(mv);
            }
        });
    }


    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public int getItemCount() {
        return articles.size();
    }
}
