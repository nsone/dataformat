package de.vastly.dataformat;

import static de.vastly.dataformat.AsNumber.FACTORY_METHOD_NAME;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


public class ValueToFlagBitmap implements BinaryType
{
	// length of bitmap-array is multiple of byteLengthPerBitmap
	protected byte[] bitmaps;
	protected int byteLengthPerBitmap;

	protected Class<? extends PDUSerializable> elementType;
	protected int delta;

	/**
	 * 
	 * @param bitmappables
	 * @param length
	 * @param args
	 *            [0] = string of element type, [1] = byteLength of bitmap, [2]
	 *            delta
	 */
	@SuppressWarnings("unchecked")
	public ValueToFlagBitmap(Collection<? extends ValueToFlagBitmappable> bitmappables, int length, String[] args)
	{
		this.elementType = (Class<? extends PDUSerializable>) PDU.resolveElementType(args[0]);
		this.byteLengthPerBitmap = args.length > 1 ? Integer.parseInt(args[1]) : 1;
		this.delta = args.length > 2 ? Integer.parseInt(args[2]) : 0;

		int bytesCount = bytesCount(max(bitmappables) + delta);
		if (length > -1)
		{
			if (bytesCount >= length)
			{
				throw new IllegalArgumentException("length must be larger than 2**(max value bit)");
			}
			else
			{
				bytesCount = length;
			}
		}

		int bitmapCount = bitmapCount(bytesCount, this.byteLengthPerBitmap);
		this.bitmaps = new byte[bitmapCount * this.byteLengthPerBitmap];

		for (ValueToFlagBitmappable bitmappable : bitmappables)
		{
			int value = bitmappable.getValue() - delta;
			int globalByteIndex = value / 8;
			int bitmapIndex = globalByteIndex / this.byteLengthPerBitmap;
			int byteIndex = this.byteLengthPerBitmap - (globalByteIndex % this.byteLengthPerBitmap) - 1;
			int bitValue = 1 << value % 8;

			this.bitmaps[bitmapIndex * this.byteLengthPerBitmap + byteIndex] |= bitValue;
		}
	}

	protected static int bytesCount(int maxValue)
	{
		return (int) Math.ceil((double) maxValue / 8);
	}

	protected static int bitmapCount(int bytesCount, int bitmapLength)
	{
		return (int) Math.ceil((double) bytesCount / (double) bitmapLength);
	}

	protected static int max(Collection<? extends ValueToFlagBitmappable> bitmappables)
	{
		int max = 0;

		for (ValueToFlagBitmappable bitmappable : bitmappables)
		{
			max = Math.max(max, bitmappable.getValue());
		}

		return max;
	}

	@SuppressWarnings("unchecked")
	public ValueToFlagBitmap(byte[] bitmaps, int length, String[] args) throws PDUException
	{
		this.bitmaps = bitmaps;
		this.elementType = (Class<? extends PDUSerializable>) PDU.resolveElementType(args[0]);
		this.byteLengthPerBitmap = args.length > 1 ? Integer.parseInt(args[1]) : 1;
		this.delta = args.length > 2 ? Integer.parseInt(args[2]) : 0;

		if ((bitmaps.length % byteLengthPerBitmap) != 0)
		{
			throw new PDUException("bitmap.length must be a multiple of byteLengthPerBitmap");
		}

	}

	@Override
	public byte[] getValue()
	{
		return bitmaps;
	}

	@Override
	public int getLength()
	{
		return bitmaps.length;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T to(Class<T> klass) throws PDUException
	{
		Set<T> result = new HashSet<>();

		if (!Set.class.isAssignableFrom(klass))
		{
			throw new IllegalArgumentException(klass.getCanonicalName() + " is not assignable to Set");
		}

		Method factoryMethod = resolveValueToFlagBitmapFactoryMethod((Class<? extends ValueToFlagBitmappable>) elementType);

		try
		{
			int counter = 0;

			for (int bitmapIndex = 0; bitmapIndex < bitmaps.length; bitmapIndex += this.byteLengthPerBitmap)
			{
				for (int byteIndex = byteLengthPerBitmap - 1; byteIndex >= 0; byteIndex--)
				{
					byte slice = bitmaps[bitmapIndex + byteIndex];

					for (int bitIndex = 0; bitIndex < 8; bitIndex++)
					{
						if ((slice & 0x01) == 0x01)
						{
							T flag = (T) factoryMethod.invoke(null, counter + delta);

							// TODO throw exception, if flag is not resolvable?
							// if (flag == null)
							// {
							// throw new
							// IllegalArgumentException("Could not convert VALUE_TO_FLAG_BITMAP to "
							// + elementType.getSimpleName()
							// + ": Value " + (counter + delta) +
							// " is not resolvable");
							// }

							result.add(flag);
						}
						slice >>>= 1;
						counter += 1;
					}
				}
			}

		}
		catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
		{
			throw new PDUException("Could not convert to " + klass.getCanonicalName(), e);
		}

		return (T) result;
	}

	protected static Method resolveValueToFlagBitmapFactoryMethod(Class<? extends ValueToFlagBitmappable> klass)
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
