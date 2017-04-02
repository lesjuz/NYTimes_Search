package com.lesjuz.nytimes.nytimes.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Lesjuz on 3/19/2017.
 */

public class Article {
    String webUrl;
    String thumbNail;
    String headLines;
    String snippet;
    String new_desk;

    public String getHeadLines() {
        return headLines;
    }

    public String getThumbNail() {
        return thumbNail;
    }

    public String getWebUrl() {
        return webUrl;
    }

    public String getNew_desk() {
        return new_desk;
    }

    public String getSnippet() {
        return snippet;
    }

    public static Article fromJson(JSONObject jsonObject) throws JSONException {
        Article article = new Article();
        // Deserialize json into object fields
       article.webUrl=jsonObject.getString("web_url");
        article.new_desk=jsonObject.getString("news_desk");
        article.snippet=jsonObject.getString("snippet");
       article.headLines=jsonObject.getJSONObject("headline").getString("main");
        JSONArray multimedia =jsonObject.getJSONArray("multimedia");
        if (multimedia.length()>0){
            JSONObject jsonObject1=multimedia.getJSONObject(0);
            article.thumbNail="http://www.nytimes.com/"+jsonObject1.getString("url");
        }
        else {
            article.thumbNail="";
        }

        // Return new object 
        return article;
    }
    public static ArrayList<Article> fromJson(JSONArray jsonArray) throws JSONException {
        JSONObject articleJson;
        ArrayList<Article> articlesArrayList = new ArrayList<Article>(jsonArray.length());
        // Process each result in json array, decode and convert to business object
        for (int i=0; i < jsonArray.length(); i++) {
            try {

                articleJson= jsonArray.getJSONObject(i);
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }

            Article article = Article.fromJson(articleJson);
            if (article != null) {
                articlesArrayList.add(article);
            }
        }

        return articlesArrayList;
    }
}
