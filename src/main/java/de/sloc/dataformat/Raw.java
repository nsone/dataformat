package de.sloc.dataformat;

import static de.sloc.dataformat.Converter.FACTORY_METHOD_NAME;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class Raw implements BinaryType
{
	protected byte[] data;

	public Raw(Byte data, int length, String[] args)
	{
		this.data = new byte[] { data };
	}

	public Raw(byte[] data, int length, String[] args)
	{
		this.data = data;
	}

	@Override
	public byte[] getValue()
	{
		return data;
	}

	@Override
	public int getLength()
	{
		return data.length;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T to(Class<T> klass) throws PDUException
	{
		// convert to byte[]
		if (klass.isArray() && klass.getComponentType() == byte.class)
		{
			return (T) data;
		}
		else if (klass.isAssignableFrom(byte.class) || klass.isAssignableFrom(Byte.class))
		{
			return (T) (Byte) data[0];
		}
        else if (Converter.class.isAssignableFrom(klass))
        {
            try
            {
                Method factoryMethod = resolveConverterFactory((Class<? extends Converter>) klass);
                Object result = factoryMethod.invoke(null, to(factoryMethod.getParameterTypes()[0]));
                return (T) result;
            }
            catch (IllegalArgumentException | SecurityException | IllegalAccessException | InvocationTargetException e)
            {
                throw new PDUException("Cannot convert to an Converter class", e);
            }
        }

		throw new IllegalArgumentException("Cannot convert RAW to " + klass.getCanonicalName());
	}
	
    protected static Map<Class<? extends Converter>, Method> converterFactoryCache = new HashMap<>();

    protected static Method resolveConverterFactory(Class<? extends Converter> klass)
    {
        if (converterFactoryCache.containsKey(klass))
        {
            return converterFactoryCache.get(klass);
        }

        for (Method method : klass.getMethods())
        {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (method.getName().equals(FACTORY_METHOD_NAME) && parameterTypes.length == 1)
            {
                converterFactoryCache.put(klass, method);
                return method;
            }
        }

        throw new IllegalStateException(klass.getCanonicalName() + " does not implement " + FACTORY_METHOD_NAME);
    }

}
