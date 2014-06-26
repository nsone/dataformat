package de.sloc.dataformat;

import java.util.Set;

public interface Bitmappable
{

	public long getValue();

	public static final String FACTORY_METHOD_NAME = "getByValue";

	public static long createBitmap(Set<? extends Bitmappable> bitmappables)
	{
		long bitmap = 0;
		for (Bitmappable bitmappable : bitmappables)
		{
			bitmap |= bitmappable.getValue();
		}
		return bitmap;
	}

}
