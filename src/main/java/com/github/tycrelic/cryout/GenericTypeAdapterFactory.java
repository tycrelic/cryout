package com.github.tycrelic.cryout;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import java.util.HashMap;

public class GenericTypeAdapterFactory implements TypeAdapterFactory {

  private transient HashMap<TypeToken<?>, TypeAdapter<?>> cache;

  @Override
  public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
    if (cache == null) {
      cache = new HashMap<TypeToken<?>, TypeAdapter<?>>();
    }

    TypeAdapter<T> typeAdapter = (TypeAdapter<T>) cache.get(type);
    if (typeAdapter == null && !cache.containsKey(type)) {
      typeAdapter = new GenericTypeAdapter<T>();
      cache.put(type, typeAdapter);
    }

    return typeAdapter;
  }

}
