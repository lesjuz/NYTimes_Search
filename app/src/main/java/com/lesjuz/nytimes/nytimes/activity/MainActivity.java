package com.lesjuz.nytimes.nytimes.activity;


import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.lesjuz.nytimes.R;
import com.lesjuz.nytimes.nytimes.adapters.ArticleGridAdapter;
import com.lesjuz.nytimes.nytimes.adapters.EndlessRecyclerViewScrollListener;
import com.lesjuz.nytimes.nytimes.models.Article;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity implements DatePickerDialog.OnDateSetListener {

    private EndlessRecyclerViewScrollListener scrollListener;
    private ArrayList<Article> articleArrayList;
    private ArticleGridAdapter adapter;
    private EditText etDateRange;

    private View positiveAction;
    private  String lastQuery;
    private int lastPageRequest;
    private Bitmap bitmap;

    private SharedPreferences filterPreferences;
    private SharedPreferences.Editor editor;

    private boolean isArtsChecked = false;
    private boolean isSportsChecked = false;
    private boolean isFashionChecked = false;
    private int sortOrder = 0;
    private String begin_date = "";
    private String end_date = "";
    private static Integer PAGE_NUMBER = 0;
    private static String QUERY = "";
    private static String TEMP_QUERY = "";

    static final String STATE_QUERY = "query";
    public static final String BASE_URL = "http://api.nytimes.com/svc/search/v2/articlesearch.json";
    private boolean isFirst;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(savedInstanceState!=null){
            lastQuery=savedInstanceState.getString(STATE_QUERY);
        }
        RecyclerView rvArticle = (RecyclerView) findViewById(R.id.rcyArticles);

        // Initialize default movies array
        articleArrayList = new ArrayList<>();

        // Create adapter passing in the sample user data
        adapter = new ArticleGridAdapter(this,articleArrayList, new ArticleGridAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Article item) {
                // Use a CustomTabsIntent.Builder to configure CustomTabsIntent.
                CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();

                // set toolbar color and/or setting custom actions before invoking build()
                // Once ready, call CustomTabsIntent.Builder.build() to create a CustomTabsIntent
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, item.getWebUrl());

                int requestCode = 100;
                PendingIntent pendingIntent = PendingIntent.getActivity(MainActivity.this,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT);

                builder.setActionButton(bitmap, "Share Link", pendingIntent, true);

                builder.setToolbarColor(ContextCompat.getColor(MainActivity.this, R.color.colorAccent));
                CustomTabsIntent customTabsIntent = builder.build();

                customTabsIntent.launchUrl(MainActivity.this, Uri.parse(item.getWebUrl()));
            }
        });
        rvArticle.setAdapter(adapter);
        StaggeredGridLayoutManager gridLayoutManager =
                new StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL);
        // Attach the layout manager to the recycler view
        rvArticle.setLayoutManager(gridLayoutManager);
        scrollListener = new EndlessRecyclerViewScrollListener(gridLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                // Triggered only when new data needs to be appended to the list
                // Add whatever code is needed to append new items to the bottom of the list
                PAGE_NUMBER++;
                loadArticles(lastQuery);
            }
        };
        // Adds the scroll listener to RecyclerView
        rvArticle.addOnScrollListener(scrollListener);

        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_action_share);

        filterPreferences = getSharedPreferences("filter_settings", Context.MODE_PRIVATE);
        editor = filterPreferences.edit();

        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            QUERY = query;
            TEMP_QUERY = query;
            isFirst = true;
            loadArticles(query);
        } else {
            editor.clear().apply();
        }

    }

    private void notifyArrayAdapterDataSetChangedOnUIThread(final ArticleGridAdapter adapter) {
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
            }
        });
    }

    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current game state
        savedInstanceState.putString(STATE_QUERY, lastQuery);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // perform query here
                PAGE_NUMBER = 0;
                QUERY = query;
                TEMP_QUERY = query;
                articleArrayList.clear();
                adapter.notifyDataSetChanged();
                loadArticles(query);

                // workaround to avoid issues with some emulators and keyboard devices firing twice if a keyboard enter is used
                // see https://code.google.com/p/android/issues/detail?id=24599
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {

                TEMP_QUERY = newText;
                return false;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_filter) {
            showCustomView();
        }

        return super.onOptionsItemSelected(item);
    }



    public void loadArticles(final String query) {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No Internet connection", Toast.LENGTH_LONG).show();
            return;
        }
        if (PAGE_NUMBER == 0) {
           articleArrayList.clear();
           adapter.notifyDataSetChanged();
        }
        int sort = filterPreferences.getInt("sortOrder", 0);
        String begin_date = filterPreferences.getString("begin_date", "");
        String end_date = filterPreferences.getString("end_date", "");

        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        if (sort != 0) {
            String sortString = "newest";
            if (sort == 2) {
                sortString = "oldest";
            }
            params.put("sort", sortString);
        }
        if (!TextUtils.isEmpty(begin_date)) {
            params.put("begin_date", begin_date);
        }
        if (!TextUtils.isEmpty(end_date)) {
            params.put("end_date", end_date);
        }
        if (isArtsChecked || isSportsChecked || isFashionChecked) {
            String temp = "news_desk:(";
            if (isArtsChecked) {
                temp += " Arts";
            }
            if (isSportsChecked) {
                temp += " Sports";
            }
            if (isFashionChecked) {
                temp += " Fashion%20%26%20Style";
            }
            temp += ")";
            Log.d("temp","fetchArticles: " + temp);
            params.put("fq", temp);
        }
        params.put("api-key", "8de7cb907a8041a3b5b84f4ad27a4c69");
        params.put("q", query);
        params.put("page",String.valueOf(PAGE_NUMBER));
        client.get(BASE_URL, params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                JSONArray ArticleJson;

                try {
                    ArticleJson = response.getJSONObject("response").getJSONArray("docs");
                    articleArrayList.addAll(Article.fromJson(ArticleJson)); // add new items
                    notifyArrayAdapterDataSetChangedOnUIThread(adapter);
                    Log.d("aricles", articleArrayList.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }


        });
    }
    private Boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }


    public void showCustomView() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.filter_dialog, null);
        dialogBuilder.setView(dialogView);

        final CheckBox checkBoxArts = (CheckBox) dialogView.findViewById(R.id.ckArt);
        final CheckBox checkBoxFashion = (CheckBox) dialogView.findViewById(R.id.ckFashion);
        final CheckBox checkBoxSports = (CheckBox) dialogView.findViewById(R.id.ckSports);
        final Spinner oldNewSpinner = (Spinner) dialogView.findViewById(R.id.mySpinner);
        Button save= (Button) dialogView.findViewById(R.id.filter_apply);
        etDateRange = (EditText) dialogView.findViewById(R.id.etbegin_date);

        checkBoxArts.setChecked(filterPreferences.getBoolean("isArtsChecked", false));
        checkBoxFashion.setChecked(filterPreferences.getBoolean("isFashionChecked", false));
        checkBoxSports.setChecked(filterPreferences.getBoolean("isSportsChecked", false));
        oldNewSpinner.setSelection(filterPreferences.getInt("sortOrder", 0));
        etDateRange.setText(filterPreferences.getString("begin_date", ""));

        dialogBuilder.setTitle("Filter");
        final AlertDialog b = dialogBuilder.create();
       save.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
                isArtsChecked = checkBoxArts.isChecked();
                editor.putBoolean("isArtsChecked", isArtsChecked);

                isFashionChecked = checkBoxFashion.isChecked();
                editor.putBoolean("isFashionChecked", isFashionChecked);

                isSportsChecked = checkBoxSports.isChecked();
                System.out.println("isSportsChecked " + isSportsChecked);
                editor.putBoolean("isSportsChecked", isSportsChecked);

                sortOrder = oldNewSpinner.getSelectedItemPosition();
                editor.putInt("sortOrder", sortOrder);

                if (!TextUtils.isEmpty(etDateRange.getText().toString())) {
                    String temp_date[] = etDateRange.getText().toString().split("-");
                    if (temp_date.length == 2) {
                        begin_date = temp_date[0];
                        end_date = temp_date[1];
                    }
                } else {
                    begin_date = "";
                    end_date = "";
                }
                editor.putString("begin_date", begin_date);
                editor.putString("end_date", end_date);

                editor.apply();

                if (!TextUtils.isEmpty(TEMP_QUERY) || !TextUtils.isEmpty(QUERY)) {
                    PAGE_NUMBER = 0;
                    if (!TextUtils.isEmpty(TEMP_QUERY)) {
                        QUERY = TEMP_QUERY;
                    }
                    loadArticles(QUERY);
                }
            b.dismiss();}
        });


            b.show();

        etDateRange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar now=Calendar.getInstance();
                DatePickerDialog dpd=DatePickerDialog.newInstance(
                        MainActivity.this,
                        now.get(Calendar.YEAR),
                        now.get(Calendar.MONTH),
                        now.get(Calendar.DAY_OF_MONTH)
                );
                dpd.show(getFragmentManager(),"Datepickerdialog");

            }
        });

    }


    @Override
    public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
        String date =dayOfMonth+"/"+(monthOfYear+1)+"/"+year;
        etDateRange.setText(date);
    }
}
