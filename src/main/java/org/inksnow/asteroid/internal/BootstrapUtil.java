package org.inksnow.asteroid.internal;

import lombok.experimental.UtilityClass;
import org.objectweb.asm.Type;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.lang.reflect.Field;

@SuppressWarnings({"unused", "unchecked"})
@UtilityClass
public class BootstrapUtil {
  private static final Unsafe unsafe = createUnsafe();
  private static final MethodHandles.Lookup lookup = createLookup();
  private static final ClassLoader defaultClassLoader = BootstrapUtil.class.getClassLoader();

  @SuppressWarnings("unchecked")
  private static <T extends Throwable> RuntimeException uncheck(Throwable e) throws T {
    throw (T) e;
  }

  private static Unsafe createUnsafe() {
    try {
      Field theUnsafeField = Unsafe.class.getDeclaredField("theUnsafe");
      theUnsafeField.setAccessible(true);
      return (Unsafe) theUnsafeField.get(null);
    } catch (Throwable e) {
      throw uncheck(e);
    }
  }

  private static MethodHandles.Lookup createLookup() {
    try {
      Field implLookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
      return (MethodHandles.Lookup) unsafe.getObject(
          unsafe.staticFieldBase(implLookupField),
          unsafe.staticFieldOffset(implLookupField)
      );
    } catch (Throwable e) {
      throw uncheck(e);
    }
  }

  public static Unsafe unsafe() {
    return unsafe;
  }

  public static MethodHandles.Lookup lookup() {
    return lookup;
  }

  private static Class<?> typeToClass(ClassLoader classLoader, Type type) throws ClassNotFoundException {
    switch (type.getSort()) {
      case Type.VOID:
        return void.class;
      case Type.BOOLEAN:
        return boolean.class;
      case Type.CHAR:
        return char.class;
      case Type.BYTE:
        return byte.class;
      case Type.SHORT:
        return short.class;
      case Type.INT:
        return int.class;
      case Type.FLOAT:
        return float.class;
      case Type.LONG:
        return long.class;
      case Type.DOUBLE:
        return double.class;
      case Type.ARRAY: {
        Class<?> currentClass = Class.forName(type.getElementType().getClassName(), false, classLoader);
        for (int i = 0; i < type.getDimensions(); i++) {
          currentClass = Array.newInstance(currentClass, 0).getClass();
        }
        return currentClass;
      }
      case Type.OBJECT:
        return Class.forName(type.getClassName(), false, classLoader);
      default:
        throw new IllegalArgumentException("Unsupported class type: " + type);
    }
  }

  private static ParsedMethod parseMethod(ClassLoader classLoader, String ref) throws ClassNotFoundException {
    int firstSplitIndex = ref.indexOf(';');
    int secondSplitIndex = ref.indexOf('(');
    ParsedMethod parsedMethod = new ParsedMethod();
    parsedMethod.ownerClass = typeToClass(classLoader, Type.getType(ref.substring(0, firstSplitIndex + 1)));
    parsedMethod.name = ref.substring(firstSplitIndex + 1, secondSplitIndex);
    Type methodType = Type.getMethodType(ref.substring(secondSplitIndex));
    Type[] argsType = methodType.getArgumentTypes();
    Class<?>[] argsClass = new Class[argsType.length];
    for (int i = 0; i < argsType.length; i++) {
      argsClass[i] = typeToClass(classLoader, argsType[i]);
    }
    parsedMethod.type = MethodType.methodType(typeToClass(classLoader, methodType.getReturnType()), argsClass);
    return parsedMethod;
  }

  private static ParsedField parseField(ClassLoader classLoader, String ref) throws ClassNotFoundException {
    int firstSplitIndex = ref.indexOf(';');
    int secondSplitIndex = ref.indexOf(':');
    ParsedField parsedField = new ParsedField();
    parsedField.ownerClass = typeToClass(classLoader, Type.getType(ref.substring(0, firstSplitIndex + 1)));
    parsedField.name = ref.substring(firstSplitIndex + 1, secondSplitIndex);
    parsedField.type = typeToClass(classLoader, Type.getType(ref.substring(secondSplitIndex + 1)));
    return parsedField;
  }

  public static MethodHandle ofInit(String ref) {
    return ofInit(defaultClassLoader, ref);
  }

  public static MethodHandle ofInit(Class<?> caller, String ref) {
    return ofInit(caller.getClassLoader(), ref);
  }

  public static MethodHandle ofInit(ClassLoader classLoader, String ref) {
    try {
      ParsedMethod parsed = parseMethod(classLoader, ref);
      return lookup().findConstructor(parsed.ownerClass, parsed.type);
    } catch (Exception e) {
      throw uncheck(e);
    }
  }

  public static MethodHandle ofStatic(String ref) {
    return ofStatic(defaultClassLoader, ref);
  }

  public static MethodHandle ofStatic(Class<?> caller, String ref) {
    return ofStatic(caller.getClassLoader(), ref);
  }

  public static MethodHandle ofStatic(ClassLoader classLoader, String ref) {
    try {
      ParsedMethod parsed = parseMethod(classLoader, ref);
      return lookup().findStatic(parsed.ownerClass, parsed.name, parsed.type);
    } catch (Exception e) {
      throw uncheck(e);
    }
  }

  public static MethodHandle ofVirtual(String ref) {
    return ofVirtual(defaultClassLoader, ref);
  }

  public static MethodHandle ofVirtual(Class<?> caller, String ref) {
    return ofVirtual(caller.getClassLoader(), ref);
  }

  public static MethodHandle ofVirtual(ClassLoader classLoader, String ref) {
    try {
      ParsedMethod parsed = parseMethod(classLoader, ref);
      return lookup().findVirtual(parsed.ownerClass, parsed.name, parsed.type);
    } catch (Exception e) {
      throw uncheck(e);
    }
  }

  public static MethodHandle ofStaticGet(String ref) {
    return ofStaticGet(defaultClassLoader, ref);
  }

  public static MethodHandle ofStaticGet(Class<?> caller, String ref) {
    return ofStaticGet(caller.getClassLoader(), ref);
  }

  public static MethodHandle ofStaticGet(ClassLoader classLoader, String ref) {
    try {
      ParsedField parsed = parseField(classLoader, ref);
      return lookup().findStaticGetter(parsed.ownerClass, parsed.name, parsed.type);
    } catch (Exception e) {
      throw uncheck(e);
    }
  }

  public static MethodHandle ofStaticSet(String ref) {
    return ofStaticSet(defaultClassLoader, ref);
  }

  public static MethodHandle ofStaticSet(Class<?> caller, String ref) {
    return ofStaticSet(caller.getClassLoader(), ref);
  }

  public static MethodHandle ofStaticSet(ClassLoader classLoader, String ref) {
    try {
      ParsedField parsed = parseField(classLoader, ref);
      return lookup().findStaticSetter(parsed.ownerClass, parsed.name, parsed.type);
    } catch (Exception e) {
      throw uncheck(e);
    }
  }

  public static MethodHandle ofSet(String ref) {
    return ofSet(defaultClassLoader, ref);
  }

  public static MethodHandle ofSet(Class<?> caller, String ref) {
    return ofSet(caller.getClassLoader(), ref);
  }

  public static MethodHandle ofSet(ClassLoader classLoader, String ref) {
    try {
      ParsedField parsed = parseField(classLoader, ref);
      return lookup().findSetter(parsed.ownerClass, parsed.name, parsed.type);
    } catch (Exception e) {
      throw uncheck(e);
    }
  }

  public static MethodHandle ofGet(String ref) {
    return ofGet(defaultClassLoader, ref);
  }

  public static MethodHandle ofGet(Class<?> caller, String ref) {
    return ofGet(caller.getClassLoader(), ref);
  }

  public static MethodHandle ofGet(ClassLoader classLoader, String ref) {
    try {
      ParsedField parsed = parseField(classLoader, ref);
      return lookup().findGetter(parsed.ownerClass, parsed.name, parsed.type);
    } catch (Exception e) {
      throw uncheck(e);
    }
  }

  private static class ParsedMethod {
    private Class<?> ownerClass;
    private String name;
    private MethodType type;
  }

  private static class ParsedField {
    private Class<?> ownerClass;
    private String name;
    private Class<?> type;
  }
}
