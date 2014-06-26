package de.vastly.dataformat;

import static de.vastly.dataformat.PDUElement.Type.LENGTH;
import static de.vastly.dataformat.PDUElement.Type.PADDING;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.vastly.common.Utility;

public class PDU
{
	protected static Map<Class<?>, List<AnnotatedElement>> annotatedElementsCache = new HashMap<>();
	protected static Map<Integer, Constructor<?>> assignableConstructorCache = new HashMap<>();
	protected static Map<Class<?>, List<AnnotatedElement>> infoMethodsCache = new HashMap<>();

	@SuppressWarnings("unchecked")
	public static <T extends PDUSerializable> List<AnnotatedElement> resolveAnnotatedElements(Class<T> klass)
	{
		if (annotatedElementsCache.containsKey(klass))
		{
			return annotatedElementsCache.get(klass);
		}

		List<AnnotatedElement> annotatedElements = new ArrayList<>();

		for (Field f : klass.getDeclaredFields())
			if (f.isAnnotationPresent(PDUElement.class))
				annotatedElements.add(f);

		for (Method m : klass.getDeclaredMethods())
			if (m.isAnnotationPresent(PDUElement.class))
				annotatedElements.add(m);

		// sort annotated elements
		Collections.sort(annotatedElements, new Comparator<AnnotatedElement>()
		{
			@Override
			public int compare(AnnotatedElement o1, AnnotatedElement o2)
			{
				return o1.getAnnotation(PDUElement.class).order() - o2.getAnnotation(PDUElement.class).order();
			}
		});

		// collect from parent classes and prepend
		Class<?> superKlass = klass.getSuperclass();
		if (superKlass != null && PDUSerializable.class.isAssignableFrom(superKlass))
		{
			List<AnnotatedElement> superKlassElements = resolveAnnotatedElements((Class<? extends PDUSerializable>) superKlass);
			List<AnnotatedElement> superKlassElementsCopy = new ArrayList<>();
			superKlassElementsCopy.addAll(superKlassElements);
			superKlassElementsCopy.addAll(annotatedElements);
			annotatedElements = superKlassElementsCopy;
		}

		// System.err.println("caching klass " + klass + " " +
		// annotatedElements);
		annotatedElementsCache.put(klass, annotatedElements);

		return annotatedElements;
	}

	/**
	 * TODO: DRY
	 * 
	 * @param klass
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T extends PDUSerializable> List<AnnotatedElement> resolveInfoMethods(Class<T> klass)
	{
		if (infoMethodsCache.containsKey(klass))
		{
			return infoMethodsCache.get(klass);
		}

		List<AnnotatedElement> derivativeElements = new ArrayList<>();

		for (Method m : klass.getDeclaredMethods())
			if (m.isAnnotationPresent(PDUInfo.class))
				derivativeElements.add(m);

		// collect from parent classes and prepend
		Class<?> superKlass = klass.getSuperclass();
		if (superKlass != null && PDUSerializable.class.isAssignableFrom(superKlass))
		{
			List<AnnotatedElement> superKlassElements = resolveInfoMethods((Class<? extends PDUSerializable>) superKlass);
			List<AnnotatedElement> superKlassElementsCopy = new ArrayList<>();
			superKlassElementsCopy.addAll(superKlassElements);
			superKlassElementsCopy.addAll(derivativeElements);
			derivativeElements = superKlassElementsCopy;
		}

		// System.err.println("caching klass " + klass + " " +
		// annotatedElements);
		infoMethodsCache.put(klass, derivativeElements);

		return derivativeElements;
	}

	@SuppressWarnings("unchecked")
	protected static <T extends BinaryType> Constructor<T> findAssignableConstructor(Class<T> klass, Class<?> valueClass)
	                                                                                                                     throws NoSuchMethodException,
	                                                                                                                     SecurityException
	{
		int hashKey = klass.hashCode() + valueClass.hashCode();
		if (assignableConstructorCache.containsKey(hashKey))
		{
			return (Constructor<T>) assignableConstructorCache.get(hashKey);
		}

		Constructor<T> result = null;

		// perfect match
		try
		{
			result = klass.getConstructor(valueClass, int.class, String[].class);
		}
		catch (NoSuchMethodException e)
		{
			// no problem, try on
		}

		// assignable match
		for (Constructor<?> constructor : klass.getConstructors())
		{
			Class<?>[] parameterTypes = constructor.getParameterTypes();
			if (parameterTypes.length == 3 && parameterTypes[0].isAssignableFrom(valueClass) && parameterTypes[1] == int.class
			        && parameterTypes[2] == String[].class)
			{
				result = (Constructor<T>) constructor;
				break;
			}
		}

		if (result == null)
		{
			throw new NoSuchMethodException("Could not find constructor in class " + klass.getCanonicalName() + " for "
			        + valueClass.getCanonicalName());
		}

		assignableConstructorCache.put(hashKey, result);

		return result;
	}

	/**
	 * Find offset and length of length field
	 * 
	 * @param pduClass
	 *            Search in this class
	 * @return metadata - Index 0 is offset, Index 1 is length of length field
	 */
	public static <T extends PDUSerializable> int[] getLengthMetadata(Class<T> pduClass)
	{
		List<AnnotatedElement> annotatedElements = resolveAnnotatedElements(pduClass);

		int[] metadata = new int[2];
		int offset = 0;
		for (AnnotatedElement element : annotatedElements)
		{
			PDUElement pduElement = element.getAnnotation(PDUElement.class);
			if (pduElement.type() == LENGTH)
			{
				metadata[0] = offset;
				metadata[1] = pduElement.length();
				break;
			}
			else
			{
				offset += pduElement.length();
			}
		}

		return metadata;
	}

	/**
	 * Encode a {@link PDUSerializable}
	 * 
	 * @param packetSerializable
	 *            The PDU to encode
	 * @return encoded PDU
	 * @throws PDUException
	 *             Thrown, when encoding goes wrong
	 */
	public static byte[] encode(PDUSerializable packetSerializable) throws PDUException
	{
		List<AnnotatedElement> annotatedElements = resolveAnnotatedElements(packetSerializable.getClass());

		// write data to this byte array
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		// byte offset
		int offset = 0;

		// save location of length fields
		Map<String, LengthFieldMetadata> lengthFieldNamesToMetadata = new HashMap<>();

		// traverse all annotated elements
		for (AnnotatedElement element : annotatedElements)
		{
			PDUElement pduElement = element.getAnnotation(PDUElement.class);
			String fieldName = null;

			try
			{
				// read value from annotated field
				Object value = null;

				if (!(element instanceof Field))
				{
					throw new InternalError("Unsupported AnnotatedElement type " + element.getClass());
				}

				Field field = (Field) element;
				fieldName = field.getName();

				// make it accessible, if its private, protected or package
				if (!field.isAccessible())
				{
					field.setAccessible(true);
				}

				// save length field position
				if (pduElement.type() == LENGTH)
				{
					// length field references
					value = 0;
					for (String reference : pduElement.references().split(","))
					{
						lengthFieldNamesToMetadata.put(reference, new LengthFieldMetadata(fieldName, offset, pduElement.length(), pduElement.args()));
					}
				}
				// padding value is created internally
				else if (pduElement.type() == PADDING)
				{
					int paddingLength;
					if (pduElement.length() == -1)
					{
						paddingLength = resolvePaddingLength(pduElement.pad(), baos.size());
					}
					else
					{
						paddingLength = pduElement.length();
					}
					value = new Padding(paddingLength).getValue();
				}
				// resolve field value
				else
				{
					value = field.get(packetSerializable);
				}

				// resolve an assignable constructor
				Constructor<? extends BinaryType> constructor = findAssignableConstructor(pduElement.type().getDataClass(), value.getClass());

				// call the constructor
				BinaryType binaryType = constructor.newInstance(value, pduElement.length(), pduElement.args());

				// get byte[] representation of BinaryType
				byte[] valueInBytes = binaryType.getValue();

				// save field length if referenced
				if (lengthFieldNamesToMetadata.containsKey(fieldName))
				{
					lengthFieldNamesToMetadata.get(fieldName).setValue(valueInBytes.length);
				}

				// write it to the OutputStream
				baos.write(valueInBytes);

				// append padding, pad() defines boundaries
				int boundary = pduElement.pad();
				if (boundary > -1 && !(binaryType instanceof Padding))
				{
					int paddingLength = (boundary - (valueInBytes.length % boundary)) % boundary;
					baos.write(new Padding(paddingLength).getValue());
					offset += paddingLength;
				}

				// add valueInBytes.length to offset
				offset += valueInBytes.length;
			}
			catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException
			        | InvocationTargetException | IOException e)
			{
				throw new PDUException("Could not encode PDU at " + packetSerializable.getClass() + "." + fieldName, e);
			}
		}

		// set total size of PDU to LENGTH_REFERENCE_PDU_ALL
		if (lengthFieldNamesToMetadata.containsKey(PDUElement.Type.LENGTH_REFERENCE_PDU_ALL))
		{
			lengthFieldNamesToMetadata.get(PDUElement.Type.LENGTH_REFERENCE_PDU_ALL).setValue(baos.size());
		}

		// save PDU as byte[]
		byte[] result = baos.toByteArray();

		// fill length fields with values
		for (LengthFieldMetadata lengthFieldMetadata : lengthFieldNamesToMetadata.values())
		{
			byte[] lengthValueInBytes = new UnsignedInteger(lengthFieldMetadata.getValue(), lengthFieldMetadata.getLength(),
			                                                lengthFieldMetadata.getArgs()).getValue();

			// set it in the byte stream
			System.arraycopy(lengthValueInBytes, 0, result, lengthFieldMetadata.getOffset(), lengthFieldMetadata.getLength());
		}

		return result;
	}

	protected static <T extends PDUSerializable> Class<T> resolveInstantiableClass(Class<T> klass, byte[] data, int startAtOffset)
	                                                                                                                              throws PDUException
	{
		List<AnnotatedElement> annotatedElements = resolveAnnotatedElements(klass);

		Field subtypeField = null;
		PDUElement subtypeElement = null;
		int subtypeOffset = 0;
		int offset = startAtOffset;

		for (AnnotatedElement annotatedElement : annotatedElements)
		{
			if (!(annotatedElement instanceof Field))
			{
				throw new IllegalStateException("Unsupported AnnotatedElement type of " + annotatedElement);
			}

			Field field = (Field) annotatedElement;

			if (field.getDeclaringClass() == klass && field.isAnnotationPresent(PDUSubtype.class))
			{
				subtypeField = field;
				subtypeElement = field.getAnnotation(PDUElement.class);
				subtypeOffset = offset;
			}

			int length = field.getAnnotation(PDUElement.class).length();
			offset += length;
		}

		if (subtypeField == null)
		{
			if (Modifier.isAbstract(klass.getModifiers()))
			{
				throw new IllegalStateException("no subtype found for abstract class " + klass);
			}
			else
			{
				return klass;
			}
		}

		if (!ImplementorMapped.class.isAssignableFrom(subtypeField.getType()))
		{
			throw new IllegalStateException("subtypeField " + subtypeField.getType().getCanonicalName() + " does not implement "
			        + ImplementorMapped.class.getCanonicalName());
		}

		Class<T> subtypeClass = resolveSubtype(subtypeElement, subtypeField, subtypeOffset, data);
		return resolveInstantiableClass(subtypeClass, data, startAtOffset);
	}

	@SuppressWarnings("unchecked")
	protected static <T extends PDUSerializable> Class<T> resolveSubtype(PDUElement element, Field field, int offset, byte[] data)
	                                                                                                                              throws PDUException
	{
		try
		{
			int length = element.length();

			byte[] typeValueBytes = new byte[length];
			System.arraycopy(data, offset, typeValueBytes, 0, length);

			Constructor<? extends BinaryType> constructor = findAssignableConstructor(element.type().getDataClass(), typeValueBytes.getClass());

			BinaryType binaryType = constructor.newInstance(typeValueBytes, length, element.args());

			return (Class<T>) ((ImplementorMapped) binaryType.to(field.getType())).getImplementor(field.getAnnotation(PDUSubtype.class).args());
		}
		catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException e)
		{
			throw new PDUException("Could not resolve subtype of " + element.type(), e);
		}
	}

	protected static boolean isPadding(byte[] data, int offset)
	{
		for (int i = offset; i < data.length; i++)
		{
			if (data[i] != 0)
			{
				return false;
			}
		}
		return true;
	}

	public static <T extends PDUSerializable> T decode(byte[] data, Class<T> klass, int offset) throws PDUException
	{
		try
		{
			// System.err.println("DECODING klass " + klass.getCanonicalName() +
			// " starting at offset " + offset + " data.length " + data.length);

			Class<T> instantiableClass = resolveInstantiableClass(klass, data, offset);
			// System.out.println("found instantiable class " +
			// instantiableClass.getCanonicalName() + " for class " + klass);

			List<AnnotatedElement> elements = resolveAnnotatedElements(instantiableClass);

			Constructor<T> constructor = instantiableClass.getDeclaredConstructor();
			constructor.setAccessible(true);
			T pdu = constructor.newInstance();

			Map<String, LengthFieldMetadata> fieldNameToLengthFieldMetadata = new HashMap<>();

			int restLength = 0;

			for (AnnotatedElement element : elements)
			{
				if (!(element instanceof Field))
				{
					throw new IllegalStateException("element " + element + " is not instance of Field");
				}

				Field field = (Field) element;

				PDUElement pduElement = field.getAnnotation(PDUElement.class);

				// System.err.println("Decoding klass " +
				// field.getDeclaringClass().getCanonicalName() + " field " +
				// field.getName() + " from "
				// + pduElement);

				// save length annotation
				if (pduElement.type() == LENGTH)
				{
					for (String targetField : pduElement.references().split(","))
					{
						byte[] slice = new byte[pduElement.length()];
						System.arraycopy(data, offset, slice, 0, pduElement.length());
						int lengthValue = new Length(slice, pduElement.length(), pduElement.args()).toInt();

						LengthFieldMetadata lengthFieldMetadata = new LengthFieldMetadata(field.getName(), offset, pduElement.length(),
						                                                                  pduElement.args());
						fieldNameToLengthFieldMetadata.put(targetField, lengthFieldMetadata);
						lengthFieldMetadata.setValue(lengthValue);

						if (targetField.equals(PDUElement.Type.LENGTH_REFERENCE_PDU_ALL))
						{
							restLength += lengthValue;
						}
					}
				}

				// length is defined
				int length = pduElement.length();

				// check if length is dynamically inferred, use length field
				// first, if no length field found, just use all data
				if (length == -1)
				{
					if (fieldNameToLengthFieldMetadata.containsKey(field.getName()))
					{
						length = fieldNameToLengthFieldMetadata.get(field.getName()).getValue();
					}
					else if (restLength > -1)
					{
						length = restLength;
					}
					else
					{
						length = data.length - offset;
					}
				}

				// System.out.println("klass: " + field.getDeclaringClass() +
				// " field " + field.getName() + " offset: " + offset +
				// " length: " + length);

				// copy data into slice

				byte[] slice = new byte[length];
				System.arraycopy(data, offset, slice, 0, length);

				// get binary type
				Class<? extends BinaryType> binaryDataClass = pduElement.type().getDataClass();

				// get binary value
				BinaryType binaryValue = binaryDataClass.getConstructor(byte[].class, int.class, String[].class).newInstance(slice, length,
				                                                                                                             pduElement.args());

				if (!field.isAccessible())
				{
					field.setAccessible(true);
				}

				// convert binary value to field type
				Object fieldValue = binaryValue.to(field.getType());
				field.set(pdu, fieldValue);

				// re-adjust length by consumed bytes of fieldValue
				if (fieldValue instanceof PDUSerializable)
				{
					length = resolveLength((PDUSerializable) fieldValue);
				}

				// System.out.println("field " + field.getName() + " set to " +
				// field.get(pdu) + " at offset " + offset + " read length: " +
				// length);

				// ignore n bytes padding
				int padding = pduElement.pad();
				if (padding > -1)
				{
					length += resolvePaddingLength(padding, length);
				}

				restLength -= length;
				offset += length;
			}

			return pdu;
		}
		catch (IllegalArgumentException | IllegalStateException | IllegalAccessException | SecurityException | InstantiationException
		        | InvocationTargetException | NoSuchMethodException e)
		{
			throw new PDUException("Could not decode PDU for " + klass.getCanonicalName(), e);
		}

	}

	public static int resolvePaddingLength(int pad, int length)
	{
		return (pad - (length % pad)) % pad;
	}

	public static Class<?> resolveElementType(String className)
	{
		try
		{
			return (Class<?>) Class.forName(className);
		}
		catch (ClassNotFoundException e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	protected static int resolveLength(PDUSerializable serializable) throws PDUException
	{
		int countLength = 0;
		boolean canBeFixedSize = true;

		try
		{
			// variable size serializable
			for (AnnotatedElement element : PDU.resolveAnnotatedElements(serializable.getClass()))
			{
				PDUElement pduElement = element.getAnnotation(PDUElement.class);

				if (pduElement.type() == LENGTH)
				{
					Field fieldElement = (Field) element;

					if (!fieldElement.isAccessible())
						fieldElement.setAccessible(true);

					int result = fieldElement.getInt(serializable);
					return result;
				}

				int declaredPduLength = pduElement.length();
				if (canBeFixedSize && declaredPduLength > -1)
				{
					countLength += declaredPduLength;
				}
				else
				{
					canBeFixedSize = false;
				}
			}

			// fixed size serializable
			if (canBeFixedSize)
			{
				return countLength;
			}

		}
		catch (IllegalAccessException e)
		{
			throw new PDUException("Could not resolve length", e);
		}

		throw new IllegalArgumentException("Could not find LENGTH field and PDU cannot be fixed size");
	}

	protected static void indent(StringBuffer buffer, int indent)
	{
		for (int i = 0; i < indent; i++)
		{
			buffer.append(' ');
		}
	}

	public static String dump(PDUSerializable packetSerializable, int... indentArgs)
	{
		int indent = (indentArgs.length > 0 ? indentArgs[0] : 0);

		StringBuffer buffer = new StringBuffer();
		indent(buffer, indent);
		buffer.append(packetSerializable.getClass().getSimpleName());
		buffer.append("\n");

		indent += 2;

		List<AnnotatedElement> annotatedElements = PDU.resolveAnnotatedElements(packetSerializable.getClass());
		List<AnnotatedElement> derivatedElements = PDU.resolveInfoMethods(packetSerializable.getClass());
		List<AnnotatedElement> dumpElements = new ArrayList<>();
		dumpElements.addAll(annotatedElements);
		dumpElements.addAll(derivatedElements);

		for (AnnotatedElement dumpElement : dumpElements)
		{
			if (dumpElement instanceof Field)
			{
				PDUElement pduElement = dumpElement.getAnnotation(PDUElement.class);

				indent(buffer, indent);
				Field field = (Field) dumpElement;
				buffer.append(field.getName());
				buffer.append("=");

				try
				{
					if (!field.isAccessible())
					{
						field.setAccessible(true);
					}

					Object value = field.get(packetSerializable);
					String formattedValue = null;

					if (value instanceof PDUSerializable)
					{
						formattedValue = "\n" + dump((PDUSerializable) value, indent);
					}
					else if (value instanceof Collection<?>)
					{
						Collection<?> collection = (Collection<?>) value;

						StringBuffer sb = new StringBuffer(value.getClass().getSimpleName());

						sb.append("\n");
						indent(sb, indent);
						sb.append("[\n");
						indent += 2;

						for (Object item : collection)
						{
							if (item instanceof PDUSerializable)
							{
								sb.append(dump((PDUSerializable) item, indent));
							}
							else
							{
								indent(sb, indent);
								sb.append(item != null ? item.toString() : "null");
							}
							sb.append("\n");
						}

						indent -= 2;

						indent(sb, indent);
						sb.append("]");
						formattedValue = sb.toString();
					}
					else
					{
						switch (pduElement.type())
						{
							case RAW:
								formattedValue = Utility.byteArrayToHexString(value instanceof byte[] ? ((byte[]) value)
								        : new byte[] { (byte) value });
								break;
							default:
								formattedValue = value != null ? value.toString() : "null";
						}
					}

					buffer.append(formattedValue);
				}
				catch (Exception e)
				{
					e.printStackTrace();
					buffer.append(e.getMessage());
				}

				buffer.append(" \n");
			}
			else if (dumpElement instanceof Method)
			{
				indent(buffer, indent);
				Method method = (Method) dumpElement;
				buffer.append(method.getName());
				buffer.append("()=");

				try
				{
					if (!method.isAccessible())
					{
						method.setAccessible(true);
					}

					buffer.append(method.invoke(packetSerializable));
				}
				catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
				{
					e.printStackTrace();
				}
				buffer.append("\n");
			}

		}
		indent -= 2;
		buffer.delete(buffer.length() - 1, buffer.length());

		return buffer.toString();
	}

	public static class LengthFieldMetadata
	{
		protected String lengthFieldName;
		protected int offset;
		protected int length;
		protected String[] args;

		protected int value;

		public LengthFieldMetadata(String lengthFieldName, int offset, int length, String[] args)
		{
			super();
			this.lengthFieldName = lengthFieldName;
			this.offset = offset;
			this.length = length;
			this.args = args;
		}

		public String getLengthFieldName()
		{
			return lengthFieldName;
		}

		public int getOffset()
		{
			return offset;
		}

		public int getLength()
		{
			return length;
		}

		public String[] getArgs()
		{
			return args;
		}

		public void setValue(int value)
		{
			this.value = value;
		}

		public int getValue()
		{
			return value;
		}

		@Override
		public String toString()
		{
			return "LengthFieldMetadata [lengthFieldName=" + lengthFieldName + ", offset=" + offset + ", length=" + length + ", args="
			        + Arrays.toString(args) + ", value=" + value + "]";
		}

	}

}
