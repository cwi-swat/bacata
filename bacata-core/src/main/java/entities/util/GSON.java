package entities.util;

import java.io.Reader;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GSON {
    private static final Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
    .create();

	public static String toJson(Object object) {
		return gson.toJson(object);
	}

	public static <T> T fromJson(Reader reader, Class<T> clz) {
		return gson.fromJson(reader, clz);
	}

    public static <T> T fromJson(String input, Class<T> clz) {
		return gson.fromJson(input, clz);
	}
}
