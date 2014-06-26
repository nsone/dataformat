package de.vastly.dataformat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StructureCollection implements BinaryType
{
	protected byte[] structureCollection;
	protected Class<? extends PDUSerializable> elementType;
	protected int elementPad;

	@SuppressWarnings("unchecked")
	public StructureCollection(Collection<? extends PDUSerializable> serializables, int length, String[] args) throws PDUException
	{
		if (length != -1)
		{
			throw new IllegalArgumentException("length given: structure collection length is dynamically determined by subtype");
		}

		this.elementType = (Class<? extends PDUSerializable>) PDU.resolveElementType(args[0]);
		this.elementPad = args.length > 1 ? Integer.parseInt(args[1]) : -1;

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		for (PDUSerializable serializable : serializables)
		{
			try
			{
				byte[] writeBytes = new Structure(serializable, -1, new String[0]).getValue();
				baos.write(writeBytes);

				if (elementPad > -1)
				{
					baos.write(new Padding(PDU.resolvePaddingLength(elementPad, writeBytes.length)).getValue());
				}
			}
			catch (IOException e)
			{
				throw new PDUException("Cannot encode STRUCTURE_COLLECTION from " + serializables, e);
			}
		}
		this.structureCollection = baos.toByteArray();

	}

	@SuppressWarnings("unchecked")
	public StructureCollection(byte[] data, int length, String[] args)
	{
		this.elementType = (Class<? extends PDUSerializable>) PDU.resolveElementType(args[0]);
		this.elementPad = args.length > 1 ? Integer.parseInt(args[1]) : -1;

		this.structureCollection = data;
	}

	@Override
	public byte[] getValue()
	{
		return structureCollection;
	}

	@Override
	public int getLength()
	{
		return structureCollection.length;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T to(Class<T> klass) throws PDUException
	{
		Collection<PDUSerializable> result;

		if (Set.class.isAssignableFrom(klass))
		{
			result = new HashSet<>();
		}
		else if (List.class.isAssignableFrom(klass))
		{
			result = new ArrayList<>();
		}
		else
		{
			throw new PDUException("Cannot convert from STRUCTURE_COLLECTION to " + klass);
		}

		int restCount = structureCollection.length;
		while (restCount > 0)
		{
			Structure structure = new Structure(structureCollection, structureCollection.length,
			                                    new String[] { Integer.toString(structureCollection.length - restCount) });
			PDUSerializable packetSerializable = structure.to(elementType);

			if (packetSerializable != null)
			{
				int consumedLength = PDU.resolveLength(packetSerializable);

				if (this.elementPad > -1)
				{
					consumedLength += PDU.resolvePaddingLength(this.elementPad, consumedLength);
				}

				restCount -= consumedLength;
				// System.err.println("Structure collection: convert to " +
				// elementType + " consumed length: " + consumedLength +
				// " bytes, rest count: "
				// + restCount);
				result.add(packetSerializable);
			}
			else
			{
				break;
			}
		}

		return (T) result;
	}

}
