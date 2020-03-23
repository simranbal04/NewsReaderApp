package com.simrankaurbal.newsreaderapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> titles = new ArrayList<>();
    ArrayList<String> content = new ArrayList<>();

    ArrayAdapter arrayAdapter;
    SQLiteDatabase articleDB;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView listView = (ListView) findViewById(R.id.listview);
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1,titles);
        listView.setAdapter(arrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent(getApplicationContext(),ArticleActivity.class);
                i.putExtra("content",content.get(position));
                startActivity(i);

            }
        });

        articleDB = this.openOrCreateDatabase("Articles",MODE_PRIVATE,null);
        articleDB.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY, articleid INTEGER, title VARCHAR, content VARCHAR)");

        updateListview();

        DownloadTask downloadTask = new DownloadTask();
        try {
            //downloadTask.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");

        } catch (Exception e)
        {
            e.printStackTrace();
        }


    }

    public void updateListview(){
        Cursor cursor = articleDB.rawQuery("SELECT * FROM articles",null);
        int contentIndex = cursor.getColumnIndex("content");
        int titleIndex = cursor.getColumnIndex("title");
        if (cursor.moveToNext()){
            titles.clear();
            content.clear();

            do {
                titles.add(cursor.getString(titleIndex));
                content.add(cursor.getString(contentIndex));
            } while (cursor.moveToNext());

            arrayAdapter.notifyDataSetChanged();
        }
    }

    // content of url download

    public class DownloadTask extends AsyncTask<String,Void,String>{

        @Override
        protected String doInBackground(String... strings) {
            String results = "";
            URL url;
            HttpURLConnection httpURLConnection = null;
            try {
                url = new URL(strings[0]);
                httpURLConnection = (HttpURLConnection) url.openConnection();

                InputStream inputStream = httpURLConnection.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);

                int data = inputStreamReader.read();
                while (data != -1){
                    char current = (char) data;
                    results += current;
                    data = inputStreamReader.read();
                }
                //Log.i("UrlConnection",results);

                // extracting numbers in json array
                JSONArray jsonArray = new JSONArray(results);

                // setting limit
                int numberofitems = 20;
                if (jsonArray.length() < 20) {
                    numberofitems = jsonArray.length();
                }

                // instructions
                articleDB.execSQL("DELETE FROM articles");
                for (int i = 0; i < numberofitems; i++)
                {
                    String articleId = jsonArray.getString(i);

                    url = new URL("https://hacker-news.firebaseio.com/v0/item/"+articleId+".json?print=pretty");
                   //url = new URL("https://hacker-news.firebaseio.com/v0/item/8863.json?print=pretty");
                    httpURLConnection = (HttpURLConnection) url.openConnection();
                    inputStream = httpURLConnection.getInputStream();
                    inputStreamReader = new InputStreamReader(inputStream);
                    data = inputStreamReader.read();
                    String articleInfo = "";
                    while (data != -1){
                        char current = (char) data;
                        articleInfo += current;
                        data = inputStreamReader.read();

                    }

                    JSONObject jsonObject = new JSONObject(articleInfo);
                    Log.i("info",jsonObject.toString());

                    if (!jsonObject.isNull("title") && !jsonObject.isNull("url") )
                    {
                        String articleTitle = jsonObject.getString("title");

                        String articleUrl = jsonObject.getString("url");

                        url = new URL(articleUrl);
                        //url = new URL("https://hacker-news.firebaseio.com/v0/item/8863.json?print=pretty");
                        httpURLConnection = (HttpURLConnection) url.openConnection();
                        inputStream = httpURLConnection.getInputStream();
                        inputStreamReader = new InputStreamReader(inputStream);
                        data = inputStreamReader.read();
                        String articleContent = "";
                        while (data != -1){
                            char current = (char) data;
                            articleInfo += current;
                            data = inputStreamReader.read();

                        }
                        Log.i("atriclecontent", articleContent);


                        String sql = "INSERT INTO articles (articleid,title,content) VALUES(? , ? , ?)";
                        SQLiteStatement sqLiteStatement = articleDB.compileStatement(sql);
                        sqLiteStatement.bindString(1,articleId);
                        sqLiteStatement.bindString(2,articleTitle);
                        sqLiteStatement.bindString(3,articleContent);

                        sqLiteStatement.execute();


//                        Log.i("info",articleTitle + articleUrl);


                    }

                   // Log.i("ArticleInfo", articleInfo);

                  //  Log.i("JSONItem",jsonArray.getString(i));

                }



            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            updateListview();
        }
    }



}
