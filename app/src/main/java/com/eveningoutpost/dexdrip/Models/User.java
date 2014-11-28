package com.eveningoutpost.dexdrip.Models;

import android.provider.BaseColumns;
import android.util.Log;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.eveningoutpost.dexdrip.Interfaces.UserInterface;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import com.google.gson.internal.bind.DateTypeAdapter;

import java.util.Date;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;

/**
 * Created by stephenblack on 11/7/14.
 */
@Table(name = "User", id = BaseColumns._ID)
public class User extends Model {
    private static final String baseUrl = "http://10.0.2.2:3000";

    public static Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .registerTypeAdapter(Date.class, new DateTypeAdapter())
            .create();

    @Expose
    @Column(name = "email")
    public String email;

    @Expose
    @Column(name = "password")
    public String password;

    @Expose
    @Column(name = "token")
    public String token;

    @Expose
    @Column(name = "token_expiration")
    public double token_expiration;

    @Expose
    @Column(name = "uuid", index = true)
    public String uuid;


    public static User currentUser() {
        User user = new Select()
                .from(User.class)
                .orderBy("_ID desc")
                .limit(1)
                .executeSingle();
        return user;
    }

    //TODO: Add refresh token attempt instance method!!

    public static void authenticate() {
        final User user = User.currentUser();
        userInterface().authenticate(user, new Callback<String>() {
                    @Override
                    public void success(String gsonResponse, Response response) {
                        JsonObject jobj = new Gson().fromJson(gsonResponse, JsonObject.class);
                        user.token = jobj.get("token").getAsString();
                        user.token_expiration = jobj.get("expiration").getAsDouble();
                        user.save();
                    }
                    @Override
                    public void failure(RetrofitError error) {
                        Response response = error.getResponse();
                        Log.w("REST CALL ERROR:", "****************");
                        Log.w("REST CALL STATUS:", "" + response.getStatus());
                        Log.w("REST CALL REASON:", response.getReason());
                    }
                }
        );
    }

    public static UserInterface userInterface() {
        RestAdapter adapter = adapterBuilder().build();
        UserInterface userInterface =
                adapter.create(UserInterface.class);
        return userInterface();
    }

    public static RestAdapter.Builder adapterBuilder() {
        RestAdapter.Builder adapterBuilder = new RestAdapter.Builder();
        adapterBuilder
                .setEndpoint(baseUrl)
                .setConverter(new GsonConverter(gson));
        return adapterBuilder;
    }


}

