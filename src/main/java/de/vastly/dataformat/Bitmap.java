package de.vastly.dataformat;

import static de.vastly.dataformat.Bitmappable.FACTORY_METHOD_NAME;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class Bitmap implements BinaryType
{
	protected byte[] bitmap;

	protected Class<? extends PDUSerializable> elementType;

	/**
	 * 
	 * @param bitmappables
	 * @param length
	 * @param args
	 *            [0] = string of element type
	 */
	@SuppressWarnings("unchecked")
	public Bitmap(Collection<? extends Bitmappable> bitmappables, int length, String[] args)
	{
		if (args.length < 1)
		{
			throw new IllegalArgumentException("elementType is not specified in args[0]");
		}
		this.elementType = (Class<? extends PDUSerializable>) PDU.resolveElementType(args[0]);
		long bitmapValue = 0L;

		for (Bitmappable bitmappable : bitmappables)
		{
			bitmapValue |= bitmappable.getValue();
		}

		this.bitmap = new UnsignedInteger(bitmapValue, length, null).getValue();
	}

	@SuppressWarnings("unchecked")
	public Bitmap(byte[] bitmap, int length, String[] args) throws PDUException
	{
		if (args.length < 1)
		{
			throw new IllegalArgumentException("elementType is not specified in args[0]");
		}

		this.bitmap = bitmap;
		this.elementType = (Class<? extends PDUSerializable>) PDU.resolveElementType(args[0]);
	}

	@Override
	public byte[] getValue()
	{
		return bitmap;
	}

	@Override
	public int getLength()
	{
		return bitmap.length;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T to(Class<T> klass) throws PDUException
	{
		Set<T> result = new HashSet<>();
		long bitmapValue = new UnsignedInteger(bitmap, bitmap.length).toLong();

		if (Long.class.isAssignableFrom(klass) || long.class.isAssignableFrom(klass))
		{
			return (T) (Long) bitmapValue;
		}
		else if (!Set.class.isAssignableFrom(klass))
		{
			throw new IllegalArgumentException(klass.getCanonicalName() + " is not assignable to Set");
		}

		Method factoryMethod = resolveValueToFlagBitmapFactoryMethod((Class<? extends Bitmappable>) elementType);

		try
		{
			if (bitmapValue == 0)
			{
				T noFlag = (T) factoryMethod.invoke(null, 0L);
				if (noFlag != null)
				{
					result.add(noFlag);
				}
			}
			else
			{
				long counter = 0x01L;
				long maxBit = 0x01L << (bitmap.length * 8);

				while (counter <= maxBit)
				{
					if ((bitmapValue & counter) > 0)
					{
						result.add((T) factoryMethod.invoke(null, counter));
					}

					counter <<= 1;
				}
			}
		}
		catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
		{
			throw new PDUException("Could not convert to " + klass.getCanonicalName(), e);
		}

		return (T) result;
	}

	protected static Method resolveValueToFlagBitmapFactoryMethod(Class<? extends Bitmappable> klass)
	{
		for (Method m : klass.getMethods())
		{
			Class<?>[] parameterTypes = m.getParameterTypes();
			if (m.getName().equals(FACTORY_METHOD_NAME) && parameterTypes.length == 1)
			{
				return m;
			}
		}
		throw new IllegalStateException(klass.getCanonicalName() + " does not implement " + FACTORY_METHOD_NAME);
	}

}
