package jnr.ffi.provider.jffi;

import com.kenai.jffi.CallingConvention;
import com.kenai.jffi.Function;
import com.kenai.jffi.Platform;
import jnr.ffi.Pointer;
import jnr.ffi.struct.Struct;

import java.lang.annotation.Annotation;

import static jnr.ffi.provider.jffi.CodegenUtils.ci;

/**
 *
 */
final class FastIntMethodGenerator extends AbstractFastNumericMethodGenerator {
    private static final int MAX_FASTINT_PARAMETERS = getMaximumFastIntParameters();

    private static final String[] signatures;

    private static final String[] methodNames = {
        "invokeVrI", "invokeIrI", "invokeIIrI", "invokeIIIrI", "invokeIIIIrI", "invokeIIIIIrI", "invokeIIIIIIrI"
    };

    private static final String[] noErrnoMethodNames = {
        "invokeNoErrnoVrI", "invokeNoErrnoIrI", "invokeNoErrnoIIrI", "invokeNoErrnoIIIrI"
    };

    static {
        signatures = new String[MAX_FASTINT_PARAMETERS + 1];
        for (int i = 0; i <= MAX_FASTINT_PARAMETERS; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append('(').append(ci(Function.class));
            for (int n = 0; n < i; n++) {
                sb.append('I');
            }
            signatures[i] = sb.append(")I").toString();
        }
    }

    FastIntMethodGenerator(BufferMethodGenerator bufgen) {
        super(bufgen);
    }

    @Override
    String getInvokerMethodName(Class returnType, Annotation[] resultAnnotations,
            Class[] parameterTypes, Annotation[][] parameterAnnotations, boolean ignoreErrno) {

        final int parameterCount = parameterTypes.length;

        if (ignoreErrno && parameterCount <= MAX_FASTINT_PARAMETERS && parameterCount <= noErrnoMethodNames.length) {
            return noErrnoMethodNames[parameterTypes.length];

        } else if (parameterCount <= MAX_FASTINT_PARAMETERS && parameterCount <= methodNames.length) {
            return methodNames[parameterCount];

        } else {
            throw new IllegalArgumentException("invalid fast-int parameter count: " + parameterCount);
        }
    }

    @Override
    String getInvokerSignature(int parameterCount, Class nativeIntType) {
        if (parameterCount <= MAX_FASTINT_PARAMETERS && parameterCount <= signatures.length) {
            return signatures[parameterCount];
        }
        throw new IllegalArgumentException("invalid fast-int parameter count: " + parameterCount);
    }

    final Class getInvokerType() {
        return int.class;
    }


    public boolean isSupported(Class returnType, Annotation[] resultAnnotations,
            Class[] parameterTypes, Annotation[][] parameterAnnotations, CallingConvention convention) {

        final int parameterCount = parameterTypes.length;

        System.out.println("fast-int checking for supported. paramcount=" + parameterCount);
        System.out.println("longSize=" + Platform.getPlatform().longSize());
        System.out.println("data.model=" + Integer.getInteger("sun.arch.data.model", 0));

        if (parameterCount > MAX_FASTINT_PARAMETERS) {
            return false;
        }
        final Platform platform = Platform.getPlatform();

        for (int i = 0; i < parameterCount; i++) {
            if (!isFastIntParameter(platform, parameterTypes[i], parameterAnnotations[i])) {
                return false;
            }
        }

        boolean supported = isFastIntResult(platform, returnType, resultAnnotations);
        System.out.println("fast-int method supported=" + supported);
        return supported;
    }


    final static int getMaximumFastIntParameters() {
        try {
            com.kenai.jffi.Invoker.class.getDeclaredMethod("invokeIIIIIIrI", Function.class,
                    int.class, int.class, int.class, int.class, int.class, int.class);
            return 6;
        } catch (NoSuchMethodException nex) {
            try {
                com.kenai.jffi.Invoker.class.getDeclaredMethod("invokeIIIrI", Function.class,
                        int.class, int.class, int.class);
                return 3;
            } catch (NoSuchMethodException nex2) {
                return 0;
            }
        } catch (Throwable t) {
            return 0;
        }
    }


    static boolean isInt32(Platform platform, Class type, Annotation[] annotations) {
        return Boolean.class.isAssignableFrom(type) || boolean.class == type
                || Byte.class.isAssignableFrom(type) || byte.class == type
                || Short.class.isAssignableFrom(type) || short.class == type
                || Integer.class.isAssignableFrom(type) || int.class == type
                || NumberUtil.isLong32(platform, type, annotations)
                ;
    }


    static boolean isFastIntResult(Platform platform, Class type, Annotation[] annotations) {
        return isInt32(platform, type, annotations)
            || Void.class.isAssignableFrom(type) || void.class == type
            || (platform.addressSize() == 32
                && (String.class.isAssignableFrom(type) || Pointer.class.isAssignableFrom(type) || Struct.class.isAssignableFrom(type))
               );

    }

    static boolean isFastIntParameter(Platform platform, Class type, Annotation[] annotations) {
        return isInt32(platform, type, annotations)
                || (platform.addressSize() == 32
                    && (Pointer.class.isAssignableFrom(type) || Struct.class.isAssignableFrom(type)))
                ;
    }
}