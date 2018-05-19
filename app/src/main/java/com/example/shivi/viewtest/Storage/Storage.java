package com.example.shivi.viewtest.Storage;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by Shivi on 25-12-2017.
 */

public class Storage {

    SQLiteDatabase sqLiteDatabase;
    SharedPreferences sharedPreferences;


    public Storage(Context context){

        sqLiteDatabase = context.openOrCreateDatabase("Media",Context.MODE_PRIVATE,null);
        sharedPreferences = context.getSharedPreferences(context.getPackageName(),Context.MODE_PRIVATE);

        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS medialist ( mediaid INT(8), id INT PRIMARY KEY )");

    }


    public void insertmedialist(ArrayList<Long> medialist){

        ContentValues contentValues;

        sqLiteDatabase.delete("medialist",null,null);


        for (Long mediaid : medialist){

            contentValues = new ContentValues();

            contentValues.put("mediaid",mediaid.longValue());
            sqLiteDatabase.insert("medialist",null,contentValues);
        }

        Log.i("Insert media", medialist.toString());

    }

    public ArrayList<Long> getmedialist(){

        Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM medialist",null);
        Log.i("get list","searching");
        Log.i("cursor",cursor.toString());
        if ( cursor != null && cursor.getCount() > 0){
            ArrayList<Long> medialist = new ArrayList<>();


            Log.i("cursor",cursor.toString());

            while ( cursor.moveToNext() ){
                //Log.i("cursor",cursor.toString());

                medialist.add(cursor.getLong(cursor.getColumnIndex("mediaid")));
                //cursor.moveToNext();
            }
            cursor.close();
            Log.i("medialist got",medialist.toString());

            return medialist;
        }
        else {
            return null;
        }



    }

    public void setresumeposition(int resumeposition){

        sharedPreferences.edit().putInt("resume position",resumeposition).apply();
        Log.i("setting"," resume position");
    }

    public int getresumeposition(){

        int resumeposition = sharedPreferences.getInt("resume position",-1);

        return resumeposition;
    }

    public void setcurretnplayingsong(int playposition){

        sharedPreferences.edit().putInt("current song",playposition).apply();

        Log.i("setting"," play position");
    }

    public int getplayposition(){

        int playposition = sharedPreferences.getInt("current song",-1);
        return playposition;
    }

    public void setservicerunningstate(boolean runningstate){

        sharedPreferences.edit().putBoolean("runningstate",runningstate).apply();
    }

    public boolean getservicerunningstate(){

       return sharedPreferences.getBoolean("runningstate",false);
    }

    public void settaskstatus(boolean status){

        sharedPreferences.edit().putBoolean("task status",status).apply();
    }

    public boolean gettaskstatus(){

        return sharedPreferences.getBoolean("task status",true);
    }
}
