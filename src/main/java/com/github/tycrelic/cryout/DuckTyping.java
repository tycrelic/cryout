package com.github.tycrelic.cryout;

import java.lang.reflect.Method;
import java.util.HashMap;

public class DuckTyping {

  private final String name;
  private final MethodDefinition[] methodDefinitions;

  private transient HashMap<Class<?>, HashMap<String, Method>> cache;

  public DuckTyping(String name, MethodDefinition... methodDefinitions) {
    this.name = name;
    this.methodDefinitions = methodDefinitions;
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  public HashMap<String, Method> getMappedMethods(Class<?> cls) {
    if (cache == null) {
      cache = new HashMap<Class<?>, HashMap<String, Method>>();
    }

    HashMap<String, Method> methods = cache.get(cls);

    if (methods == null && !cache.containsKey(cls)) {
      Method[] methodArray = new Method[methodDefinitions.length];
      int i = 0;
      for (MethodDefinition md : methodDefinitions) {
        Class<?>[] parameterTypes = md.getParameterTypes();
        try {
          methodArray[i++] = cls.getMethod(md.getName(), parameterTypes);
        } catch (NoSuchMethodException ex) {
          cache.put(cls, null);
          return null;
        } catch (SecurityException ex) {
          cache.put(cls, null);
          return null;
        }
      }

      methods = new HashMap<String, Method>(methodArray.length);
      for (Method m : methodArray) {
        methods.put(m.getName(), m);
      }

      cache.put(cls, methods);
    }

    return methods;
  }

  public boolean isAssignableFrom(Class<?> cls) {
    return getMappedMethods(cls) != null;
  }

  public boolean isInstance(Object obj) {
    Class<?> cls = obj.getClass();
    return isAssignableFrom(cls);
  }

  public Object newInstance() throws InstantiationException, IllegalAccessException {
    return null;
  }

  public static class MethodDefinition {

    private String name;
    private Class<?>[] parameterTypes;
    private Class<?> returnType;
    private int modifiers;

    /**
     * @return the name
     */
    public String getName() {
      return name;
    }

    /**
     * @param name the name to set
     * @return the MethodDefinition
     */
    public MethodDefinition setName(String name) {
      this.name = name;
      return this;
    }

    /**
     * @return the parameterTypes
     */
    public Class<?>[] getParameterTypes() {
      return parameterTypes;
    }

    /**
     * @param parameterTypes the parameterTypes to set
     * @return the MethodDefinition
     */
    public MethodDefinition setParameterTypes(Class<?>... parameterTypes) {
      this.parameterTypes = parameterTypes;
      return this;
    }

    /**
     * @return the returnType
     */
    public Class<?> getReturnType() {
      return returnType;
    }

    /**
     * @param returnType the returnType to set
     * @return the MethodDefinition
     */
    public MethodDefinition setReturnType(Class<?> returnType) {
      this.returnType = returnType;
      return this;
    }

    /**
     * @return the modifiers
     */
    public int getModifiers() {
      return modifiers;
    }

    /**
     * @param modifiers the modifiers to set
     * @return the MethodDefinition
     */
    public MethodDefinition setModifiers(int modifiers) {
      this.modifiers = modifiers;
      return this;
    }

  }
}
