package de.sloc.dataformat;

import static de.sloc.dataformat.PDUElement.Type.LENGTH_REFERENCE_PDU_ALL;
import static java.lang.annotation.ElementType.FIELD;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

@Retention(RetentionPolicy.RUNTIME)
@Target(FIELD)
@Documented
@Inherited
public @interface PDUElement
{
	int order();

	int length() default -1;

	Type type();

	int pad() default -1;

	String[] args() default {};

	String references() default LENGTH_REFERENCE_PDU_ALL;

	public enum Type
	{
		UNSIGNED_INTEGER(UnsignedInteger.class),
		UNSIGNED_INTEGER_COLLECTION(UnsignedIntegerCollection.class),
		BITMAP(Bitmap.class),
		VALUE_TO_FLAG_BITMAP(ValueToFlagBitmap.class),
		STRUCTURE(Structure.class),
		STRUCTURE_COLLECTION(StructureCollection.class),
		FIXED_LENGTH_STRING(FixedLengthString.class),
		PADDING(Padding.class),
		LENGTH(Length.class),
		RAW(Raw.class);

		protected static Map<Class<? extends BinaryType>, Type> valueToType = new HashMap<>();

		public static final String LENGTH_REFERENCE_PDU_ALL = "PDU_ALL";

		static
		{
			for (Type type : EnumSet.allOf(Type.class))
			{
				valueToType.put(type.getDataClass(), type);
			}
		}

		protected Class<? extends BinaryType> dataKlass;

		Type(Class<? extends BinaryType> dataKlass)
		{
			this.dataKlass = dataKlass;
		}

		public Class<? extends BinaryType> getDataClass()
		{
			return dataKlass;
		}

		public static Type getByDataClass(Class<? extends BinaryType> klass)
		{
			return valueToType.get(klass);
		}
	}

}
