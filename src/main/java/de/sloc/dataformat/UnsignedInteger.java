package de.sloc.dataformat;

import static de.sloc.dataformat.AsNumber.FACTORY_METHOD_NAME;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class UnsignedInteger implements BinaryType
{
    protected int length;

    protected byte[] value;

    protected static final short BYTE_MASK = 0xFF;

    protected int delta;

    // repeating myself here, as DRY would imply type casts

    public UnsignedInteger(Number numberValue, int length, String[] args)
    {
        this.length = length;
        this.value = new byte[length];
        this.delta = (args != null && args.length > 0) ? Integer.parseInt(args[0]) : 0;

        if (numberValue instanceof Long)
        {
            long value = (Long) numberValue - delta;
            if (length > 7)
            {
                throw new IllegalArgumentException("long can only carry 7 bytes as unsigned value");
            }

            if (value < 0 || length < 0)
            {
                throw new IllegalArgumentException("value or length < 0");
            }

            for (int i = 0; i < length; i++)
            {
                this.value[length - i - 1] = (byte) (value & BYTE_MASK);
                value >>>= 8;
            }
        }
        else if (numberValue instanceof Integer || numberValue instanceof Short)
        {
            int value = (Integer) numberValue - delta;
            if (length > 3)
            {
                throw new IllegalArgumentException("int can only carry 3 bytes as unsigned value");
            }

            if (length < 0)
            {
                throw new IllegalArgumentException("length < 0: length=" + length);
            }

            for (int i = 0; i < length; i++)
            {
                this.value[length - i - 1] = (byte) (value & BYTE_MASK);
                value >>>= 8;
            }
        }
        else if (numberValue instanceof BigInteger)
        {
            BigInteger value = (BigInteger) numberValue;

            if (value.bitLength() > length * 8)
                throw new NumberFormatException("BigInteger doesn't fit into array");

            byte[] b = value.toByteArray();

            if (b.length == length + 1)
                if (b[0] == 0)
                    for (int i = 0; i < this.value.length; i++)
                        this.value[i] = b[i + 1];
                else
                    throw new NumberFormatException("Internal Error");
            else if (b.length <= length)
                for (int i = 0; i < b.length; i++)
                    this.value[this.value.length - 1 - i] = b[b.length - 1 - i];
            else
                throw new NumberFormatException("Internal Error");
        }
    }

    public UnsignedInteger(byte[] value, int length, String[] args)
    {
        this.value = value;
        this.length = length;
        this.delta = (args != null && args.length > 0) ? Integer.parseInt(args[0]) : 0;
    }

    public UnsignedInteger(Number value, int length)
    {
        this(value, length, null);
    }

    public UnsignedInteger(byte[] value, int length)
    {
        this(value, length, null);
    }

    public UnsignedInteger(BigInteger value)
    {
        this(value, (int) Math.ceil((value).bitLength() / 8.0), null);
    }

    public UnsignedInteger(AsNumber asNumber, int length, String[] args)
    {
        this(asNumber.getNumberValue(), sanitizeAsNumberLength(asNumber, length), args);
    }

    private static int sanitizeAsNumberLength(AsNumber asNumber, int length)
    {
        if (length == -1)
        {
            Number number = asNumber.getNumberValue();
            if (number instanceof BigInteger)
            {
                length = ((BigInteger) number).toByteArray().length;
            }
            else if (number instanceof Integer)
            {
                length = 3;
            }
            else if (number instanceof Long)
            {
                length = 7;
            }

        }
        return length;
    }

    public byte[] getValue()
    {
        return value;
    }

    public long toLong()
    {
        if (length > 7)
        {
            throw new IllegalAccessError("length > 7 bytes, therefore not convertible to long");
        }

        long value = 0;

        for (int i = 0; i < length; i++)
        {
            value |= this.value[i] & BYTE_MASK;

            if (i < (length - 1))
            {
                value <<= 8;
            }
        }

        return value + delta;
    }

    public int toInt()
    {
        if (length > 3)
        {
            throw new IllegalAccessError("length > 3 bytes, therefore not convertible to int");
        }

        int value = 0;

        for (int i = 0; i < length; i++)
        {
            value |= this.value[i] & BYTE_MASK;

            if (i < (length - 1))
            {
                value <<= 8;
            }
        }

        return value + delta;
    }

    public short toShort()
    {
        if (length > 1)
        {
            throw new IllegalAccessError("length > 1 bytes, therefore not convertible to short");
        }

        short value = 0;

        for (int i = 0; i < length; i++)
        {
            value |= this.value[i] & BYTE_MASK;

            if (i < (length - 1))
            {
                value <<= 8;
            }
        }

        return (short) (value + delta);
    }

    public BigInteger toBigInteger()
    {
        return new BigInteger(value);
    }

    @Override
    public int getLength()
    {
        return length;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T to(Class<T> klass) throws PDUException
    {
        if (klass.isAssignableFrom(Long.class) || klass.isAssignableFrom(long.class))
        {
            return (T) (Long) toLong();
        }
        else if (klass.isAssignableFrom(Integer.class) || klass.isAssignableFrom(int.class))
        {
            return (T) (Integer) toInt();
        }
        else if (klass.isAssignableFrom(Short.class) || klass.isAssignableFrom(short.class))
        {
            return (T) (Short) toShort();
        }
        else if (klass.isAssignableFrom(BigInteger.class))
        {
            return (T) toBigInteger();
        }
        else if (AsNumber.class.isAssignableFrom(klass))
        {
            try
            {
                Method factoryMethod = resolveAsNumberFactoryMethod((Class<? extends AsNumber>) klass);
                Object result = factoryMethod.invoke(null, to(factoryMethod.getParameterTypes()[0]));
                return (T) result;
            }
            catch (IllegalArgumentException | SecurityException | IllegalAccessException | InvocationTargetException e)
            {
                throw new PDUException("Cannot convert to an AsNumber class", e);
            }
        }

        throw new IllegalArgumentException("Cannot convert to " + klass.getCanonicalName());
    }

    public Number toNumber()
    {
        if (length < 4)
        {
            return toInt();
        }
        else if (length < 8)
        {
            return toLong();
        }

        return toBigInteger();
    }

    protected static Map<Class<? extends AsNumber>, Method> asNumberFactoryCache = new HashMap<>();

    protected static Method resolveAsNumberFactoryMethod(Class<? extends AsNumber> klass)
    {
        if (asNumberFactoryCache.containsKey(klass))
        {
            return asNumberFactoryCache.get(klass);
        }

        for (Method method : klass.getMethods())
        {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (method.getName().equals(FACTORY_METHOD_NAME) && parameterTypes.length == 1)
            {
                asNumberFactoryCache.put(klass, method);
                return method;
            }
        }

        throw new IllegalStateException(klass.getCanonicalName() + " does not implement " + FACTORY_METHOD_NAME);
    }

    @Override
    public String toString()
    {
        return toNumber() + "";
    }

}
