package com.github.tycrelic.cryout;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GenericTypeAdapter<T> extends TypeAdapter<T> {

  @Override
  public void write(JsonWriter out, T value) throws IOException {
    if (value == null) {
      out.nullValue();
      return;
    }

    Class valueClass = value.getClass();

    boolean valueIsNotWritten = true;
    ArrayList<String> notWrittenPropertyNames = null;

    if (writeSimpleValue(valueClass, out, value)) {
      valueIsNotWritten = false;
    } else if (valueClass.isArray()) {
      GenericTypeAdapter<Object> gta = new GenericTypeAdapter();
      out.beginArray();
      for (int i = 0, len = Array.getLength(value); i < len; ++i) {
        gta.write(out, Array.get(value, i));
      }
      out.endArray();
      valueIsNotWritten = false;
    } else if (Object.class.isAssignableFrom(valueClass)) {
      out.beginObject();

      out.name("class").value(valueClass.getName());

      BeanInfo beanInfo;
      try {
        beanInfo = Introspector.getBeanInfo(valueClass);
      } catch (IntrospectionException ex) {
        throw new IOException(ex);
      }

      PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
      if (propertyDescriptors != null && propertyDescriptors.length > 0) {
        for (PropertyDescriptor property : propertyDescriptors) {
          String propertyName = property.getName();
          if (!"class".equals(propertyName) && propertyName != null) {
            Class propertyType = property.getPropertyType();

            Method getter = property.getReadMethod();

            boolean notDeprecated = true;
            if (getter != null && (notDeprecated = !getter.isAnnotationPresent(Deprecated.class))) {
              Object propertyValue;
              try {
                propertyValue = getter.invoke(value);
              } catch (IllegalAccessException ex) {
                throw new IOException(ex);
              } catch (IllegalArgumentException ex) {
                throw new IOException(ex);
              } catch (InvocationTargetException ex) {
                throw new IOException(ex);
              }
              out.name(propertyName);
              if (!writeSimpleValue(propertyType, out, propertyValue)) {
                GenericTypeAdapter<Object> gta = new GenericTypeAdapter();
                gta.write(out, propertyValue);
              }
            } else if (notDeprecated) {
              if (notWrittenPropertyNames == null) {
                notWrittenPropertyNames = new ArrayList<String>();
              }
              notWrittenPropertyNames.add(propertyName);
            }
          }
        }

        valueIsNotWritten = notWrittenPropertyNames != null && !notWrittenPropertyNames.isEmpty();
      }

      if (valueIsNotWritten) {
        if (value instanceof Iterable) {
          out.name(".");
          Iterable itr = (Iterable) value;
          out.beginArray();
          for (Object propertyValue : itr) {
            GenericTypeAdapter<Object> gta = new GenericTypeAdapter();
            gta.write(out, propertyValue);
          }
          out.endArray();
          valueIsNotWritten = false;
        } else if (value instanceof Map) {
          out.name(".");
          Map map = (Map) value;
          Iterable<Map.Entry> itr = (Iterable<Map.Entry>) map.entrySet();
          out.beginArray();
          for (Map.Entry entry : itr) {
            out.beginArray();
            GenericTypeAdapter<Object> gta = new GenericTypeAdapter();
            gta.write(out, entry.getKey());
            gta.write(out, entry.getValue());
            out.endArray();
          }
          out.endArray();
          valueIsNotWritten = false;
        }
      }

      DuckTyping toFromStringDuckTyping = new DuckTyping("ToFromString",
        new DuckTyping.MethodDefinition().setName("toString"),
        new DuckTyping.MethodDefinition().setName("from_string").setParameterTypes(String.class)
      );

      if (toFromStringDuckTyping.isInstance(value)) {
        HashMap<String, Method> methods = toFromStringDuckTyping.getMappedMethods(valueClass);
        try {
          out.name(".").value((String) methods.get("toString").invoke(value));
        } catch (IOException ex) {
          throw new IOException(ex);
        } catch (IllegalAccessException ex) {
          throw new IOException(ex);
        } catch (IllegalArgumentException ex) {
          throw new IOException(ex);
        } catch (InvocationTargetException ex) {
          throw new IOException(ex);
        }
        valueIsNotWritten = false;
      }

      out.endObject();

    }

    if (valueIsNotWritten) {
      int len;
      if (notWrittenPropertyNames == null || (len = notWrittenPropertyNames.size()) == 0) {
        System.out.println("Value of class " + valueClass.getName() + " is not written.");
      } else if (len == 1) {
        System.out.println("Property of class " + valueClass.getName() + " is not written: " + notWrittenPropertyNames.get(0));
      } else {
        boolean first = true;
        for (String propertyName : notWrittenPropertyNames) {
          if (!first) {
            System.out.print(", " + propertyName);
          } else {
            System.out.print("Properties of class " + valueClass.getName() + " are not written: " + propertyName);
            first = false;
          }
        }
      }
    }

  }

  private boolean writeSimpleValue(Class propertyType, JsonWriter out, Object propertyValue) throws IOException {
    if (propertyType == String.class) {
      out.value((String) propertyValue);
    } else if (propertyType == char.class || propertyType == Character.class) {
      out.value(((Character) propertyValue).toString());
    } else if (propertyType == boolean.class) {
      out.value(((Boolean) propertyValue).booleanValue());
    } else if (propertyType == Boolean.class) {
      out.value(((Boolean) propertyValue));
    } else if (propertyType == double.class) {
      out.value(((Double) propertyValue).doubleValue());
    } else if (propertyType == Double.class) {
      out.value(((Double) propertyValue));
    } else if (propertyType == float.class) {
      out.value(((Float) propertyValue).doubleValue());
    } else if (propertyType == int.class || propertyType == Integer.class) {
      out.value(((Integer) propertyValue).longValue());
    } else if (propertyType == long.class || propertyType == Long.class) {
      out.value(((Long) propertyValue).longValue());
    } else if (Number.class.isAssignableFrom(propertyType)) {
      out.value(((Number) propertyValue));
    } else {
      return false;
    }
    return true;
  }

  @Override
  public T read(JsonReader in) throws IOException {
    OUTERMOST_LOOP:
    while (in.hasNext()) {
      int depth = 0;
      do {
        JsonToken jt = in.peek();
        System.out.println(jt);
        switch (jt) {
          case BEGIN_ARRAY:
            in.beginArray();
            ++depth;
            break;
          case END_ARRAY:
            in.endArray();
            --depth;
            break;
          case BEGIN_OBJECT:
            in.beginObject();
            ++depth;
            break;
          case END_OBJECT:
            in.endObject();
            --depth;
            break;
          case NAME:
            in.nextName();
            break;
          case STRING:
            in.nextString();
            break;
          case NUMBER:
            Class cls = double.class;
            //System.out.println(cls);
            if (cls == int.class || cls == Integer.class) {
              in.nextInt();
            } else if (cls == long.class || cls == Long.class) {
              in.nextLong();
            } else if (cls == double.class || cls == Double.class) {
              in.nextDouble();
            }
            break;
          case BOOLEAN:
            in.nextBoolean();
            break;
          case NULL:
            in.nextNull();
            break;
          case END_DOCUMENT:
            break OUTERMOST_LOOP;
          default:
            throw new IOException("Unexpected JsonToken is found: " + jt);
        }
      } while (depth != 0);
    }

    return null;
  }
}
