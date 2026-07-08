package com.eveningoutpost.dexdrip.utils.framework;

import static com.eveningoutpost.dexdrip.models.JoH.emptyString;

import com.eveningoutpost.dexdrip.utilitymodels.OkHttpWrapper;

import com.eveningoutpost.dexdrip.cgm.nsfollow.GzipRequestInterceptor;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.store.FastStore;
import com.eveningoutpost.dexdrip.tidepool.InfoInterceptor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.bind.TypeAdapters;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * General Retrofit Instance
 *
 * Holds a cache of Retrofit Instances and can both remove one instance or clear all.
 *
 * @author Asbjørn Aarrestad
 */
public class RetrofitService {
    private static Map<String, Retrofit> RETROFIT_CACHE = new HashMap<>();

    public static Retrofit getRetrofitInstance(String url, String tag, boolean debugLogging) throws IllegalArgumentException {
        // Check cache before creating new
        String key = getKey(url, tag, debugLogging);
        if(RETROFIT_CACHE.containsKey(key)) {
            return RETROFIT_CACHE.get(key);
        }


        if (emptyString(url)) {
            UserError.Log.d(tag, "Empty url - cannot create instance");
            return null;
        }
        final HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
        if (debugLogging) {
            httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        }
        final OkHttpClient client = OkHttpWrapper.getClient().newBuilder()
                .addInterceptor(httpLoggingInterceptor)
                .addInterceptor(new InfoInterceptor(tag))
                .addInterceptor(new GzipRequestInterceptor())
                .build();

        final Gson gson = new GsonBuilder()
                .registerTypeAdapterFactory(UNRELIABLE_INTEGER_FACTORY)
                .registerTypeAdapterFactory(EMPTY_OBJECT_AS_EMPTY_LIST)
                .create();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        RETROFIT_CACHE.put(key, retrofit);

        return retrofit;
    }

    public static void clear() {
        RETROFIT_CACHE.clear();
    }

    public static void remove(String url, String tag, boolean debugLogging) {
        RETROFIT_CACHE.remove(getKey(url, tag, debugLogging));
    }

    public static int size() {
        return RETROFIT_CACHE.size();
    }

    // ===== Boilerplate classes ===================================================================
    // Callback template to reduce boiler plate

    @RequiredArgsConstructor
    public static  class BaseCallback<T> implements Callback<T> {

        protected final String TAG = this.getClass().getSimpleName();

        //  final Session session;
        protected final String name;
        private Runnable onSuccess;
        private Runnable onFailure;

        public BaseCallback<T> setOnSuccess(final Runnable runnable) {
            this.onSuccess = runnable;
            return this;
        }

        public BaseCallback<T> setOnFailure(final Runnable runnable) {
            this.onFailure = runnable;
            return this;
        }

        @Override
        public void onResponse(Call<T> call, Response<T> response) {
            if (response.isSuccessful() && response.body() != null) {
                UserError.Log.d(TAG, name + " success");
                if (onSuccess != null) {
                    onSuccess.run();
                }
            } else {
                final String msg = name + " was not successful: " + response.code() + " " + response.message();
                UserError.Log.e(TAG, msg);
                status(msg);
                if (onFailure != null) {
                    onFailure.run();
                }
            }
        }

        @Override
        public void onFailure(Call<T> call, Throwable t) {
            final String msg = name + " Failed: " + t;
            UserError.Log.e(TAG, msg);
            status(msg);
            if (onFailure != null) {
                onFailure.run();
            }
        }

        public String getStatus() {
            return FastStore.getInstance().getS(TAG + ".STATUS_KEY");
        }

        private void status(final String status) {
            FastStore.getInstance().putS(TAG + ".STATUS_KEY", status);
        }
    }

    // ===== Private helpers =======================================================================
    private static String getKey(String url, String tag, boolean debugLogging) {
        return url + "|" + tag + "|" + debugLogging;
    }

    private static final TypeAdapter<Number> UNRELIABLE_INTEGER = new TypeAdapter<Number>() {
        @Override
        public Number read(JsonReader in) throws IOException {
            JsonToken jsonToken = in.peek();
            switch (jsonToken) {
                case NUMBER:
                case STRING:
                    String s = in.nextString();
                    try {
                        return Integer.parseInt(s);
                    } catch (NumberFormatException ignored) {
                    }
                    try {
                        return (int)Double.parseDouble(s);
                    } catch (NumberFormatException ignored) {
                    }
                    return null;
                case NULL:
                    in.nextNull();
                    return null;
                case BOOLEAN:
                    in.nextBoolean();
                    return null;
                default:
                    throw new JsonSyntaxException("Expecting number, got: " + jsonToken);
            }
        }
        @Override
        public void write(JsonWriter out, Number value) throws IOException {
            out.value(value);
        }
    };
    public static final TypeAdapterFactory UNRELIABLE_INTEGER_FACTORY = TypeAdapters.newFactory(int.class, Integer.class, UNRELIABLE_INTEGER);

    /**
     * Some Nightscout emulators (Juggluco) return an empty JSON object {@code {}} instead of an
     * empty array {@code []} for an empty result set. This factory maps that to an empty list for
     * any {@code List<...>} target, so a no-data poll is a normal empty result rather than a
     * {@code BEGIN_ARRAY}/{@code BEGIN_OBJECT} parse failure. Real Nightscout returns {@code []},
     * so it is unaffected.
     */
    public static final TypeAdapterFactory EMPTY_OBJECT_AS_EMPTY_LIST = new TypeAdapterFactory() {
        @Override
        public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
            if (!List.class.isAssignableFrom(type.getRawType())) {
                return null;
            }
            final TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
            return new TypeAdapter<T>() {
                @Override
                @SuppressWarnings("unchecked")
                public T read(final JsonReader in) throws IOException {
                    if (in.peek() == JsonToken.BEGIN_OBJECT) {
                        in.skipValue(); // consume {} and treat as empty
                        return (T) new ArrayList<>();
                    }
                    return delegate.read(in);
                }

                @Override
                public void write(final JsonWriter out, final T value) throws IOException {
                    delegate.write(out, value);
                }
            };
        }
    };
}
