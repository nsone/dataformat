# dataformat

dataformat is a Java annotation-based binary format to POJO converter

dataformat converts binary data as read from a Java InputStream to Plain-old-JavaObjects (POJO) 
and converts those POJOs back to binary data that can be fed to a 
Java OutputStream. Whenever dealing with binary protocols in Java, one has to write 
parsers and message encoders. While the Java presentation of the protocol model 
can be kept in one place (i.e. one class per PDU type), the programmer has to write 
specific code for encoding the model to binary data and specific code for decoding 
binary data back to the model. Encoding and decoding usually produces two distinct 
types of code, even so they are coupled to each other. As soon as the model changes, 
both encoding and decoding has to be changed. dataformat solves this by defining a 
pattern of Java Annotations and inheritance to describe binary formats within POJOs. 
Using the Java reflection API, the annotations within POJOs can be used to do the 
described binary encoding and decoding.  

## Examples

### Read from java.io.InputStream

```java
PDUInputStream<FrameHeader> is = new PDUInputStream<>(FrameHeader.class, parentIs);
FrameHeader frame = is.readPDU();
if(frame.getEthertype() == Ethertype.ARP) {
   Arp arp = (Arp) frame;
}
```

### Write to java.io.OutputStream

```java
PDUOutputStream<FrameHeader> os = new PDUOutputStream<>(parentOs);
os.write(arp);
```

### Declare a PDU type

#### Binary format 
```
0                                  31
+--------+--------+--------+--------+
|            ID            |   Pad  |
+--------+--------+--------+--------+
|             Port number           |
+--------+--------+--------+--------+        
```


#### POJO Presentation
```java
public class OFLinkDiscovery extends FrameHeader {
	@PDUElement(order = 1, type = Type.UNSIGNED_INTEGER, length = 3)
	protected long id;

	@PDUElement(order = 2, type = Type.PADDING, length = 1)
	protected byte[] padding;

	@PDUElement(order = 3, type = Type.UNSIGNED_INTEGER, length = 4)
	protected long portNumber;

	protected OFLinkDiscovery(){}
}
```

### Bitmaps

```java
@PDUElement(order = 1, type = BITMAP, length = 4, args = "my.package.Feature")
protected Set<Feature> currentFeatures;
```

**my.package.Feature**
```java
public static enum Feature implements Bitmappable {
	FEATURE1(0x01),
	FEATURE2(0x02),
	FEATURE3(0x04);

	protected static Map<Long, Feature> valueToFeature = new HashMap<>();

	static {
		for (Feature feature : EnumSet.allOf(Feature.class)) {
			valueToFeature.put(feature.getValue(), portFeature);
		}
	}

	public static Feature getByValue(long value) {
		return valueToFeature.get(value);
	}

	protected long value;

	Feature(long value) {
		this.value = value;
	}

	public long getValue() {
		return value;
	}
}
```

### Fixed-length Strings

```java
protected static final int DESCRIPTION_STRING_LENGTH = 256;


@PDUElement(order = 1, type = FIXED_LENGTH_STRING, length = DESCRIPTION_STRING_LENGTH)
protected String manufacturerDescription;
```

### Values as enums

```java
@PDUElement(order = 2, type = Type.UNSIGNED_INTEGER, length = 2)
protected Ethertype protocolType;
```

**Ethertype**
```java
public enum Ethertype implements AsNumber {
	IPV4(0x0800),
	ARP(0x0806),
	UNKNOWN(0xFFFF);

	protected static Map<Integer, Ethertype> reverseMap = new HashMap<>();

	static {
		for (Ethertype type : Ethertype.values()) {
			reverseMap.put(type.getValue(), type);
		}
	}

	protected int value;

	Ethertype(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	public Number getNumberValue() {
		return value;
	}

	public static Ethertype getByValue(int value) {
		Ethertype result = reverseMap.get(value);

		if (result == null) {
			result = UNKNOWN;
			result.value = value;
		}
		return result;
	}
}
```

### Length fields

*If a field defines the length of the whole PDU, Type.LENGTH can be used. As a rule of thumb, provide the length-attribute whenever possible. 
If there are dynamic length fields, dataformat tries to derive their length.*

```java
@PDUElement(order = 2, type = Type.LENGTH, length = 1)
protected short length;
```

*If a field defines the length of one or more other fields, Type.LENGTH with references can be used*

```java
@PDUElement(order = 3, type = Type.LENGTH, references = "sourceMac,destinationMac", length = 1)
protected short addressLength;
...
@PDUElement(order = 6, type = Type.RAW)
protected byte[] sourceMac;
...
@PDUElement(order = 8, type = Type.RAW)
protected byte[] destinationMac;
```


### Structures

*If you want to have PDU-structures as instances, use Type.STRUCTURE. If you omit the length of the structure, there must be a length field within it.*

```java
@PDUElement(order = 11, type = STRUCTURE)
protected SomeOtherPDU subPDU;
```

**SomeOtherPDU**
```java
public class SomeOtherPDU implements PDUSerializable {
	@PDUElement(order = 1, type = UNSIGNED_INTEGER, length = 2)
	protected int age;

	@PDUElement(order = 2, type = LENGTH, length = 2)
	protected int length;
}
```

### Padding

*If you want to pad multiple fixed bytes in a PDU, you can do this*
```java
@PDUElement(order = 1, type = PADDING, length = 2)
protected byte[] twoBytesOfZeros;
```

*If you want to pad a field to a word boundary, do this*
```java
@PDUElement(order = 1, type = STRUCTURE, pad = 8)
protected SomeOtherPDU subPDU;
```


### Collections

**Structures**
```java
@PDUElement(order = 14, type = STRUCTURE_COLLECTION, args = "my.package.Element")
protected Set<Element> elements;
```

**Unsigned Integers**
```java
@PDUElement(order = 3, type = UNSIGNED_INTEGER_COLLECTION, length = 16, args = { "java.lang.Long", "4" })
protected List<Long> fourLongs;
```

### Subtypes

*If you have a group of PDUs that share a common header, you can do this*

```java
public abstract class CommonHeader implements Serializable {
...
3 fields of common header
...
@PDUElement(order = 4, type = UNSIGNED_INTEGER, length = 1)
@PDUSubtype
protected ProtocolType type;
...
}
```


```java
public class Type0 extends CommonHeader {
...
}
```

**ProtocolType**
```java
public static enum ProtocolType implements AsNumber, ImplementorMapped {
	TYPE0(0, Type0.class),
	Type1(1, Type1.class);
	protected static Map<Integer, ProtocolType> valueToActionType = new HashMap<>();

	static {
		for (ProtocolType type : EnumSet.allOf(ProtocolType.class)) {
			valueToType.put(type.getValue(), type);
		}
	}

	public static ProtocolType getByValue(int value) {
		return valueToType.get(value);
	}

	protected int value;

	protected Class<? extends Type> implementor;

	ProtocolType(int value, Class<? extends CommonHeader> implementor) {
		this.value = value;
		this.implementor = implementor;
	}

	public Class<? extends PDUSerializable> getImplementor(String... args) {
		return implementor;
	}

	public Number getNumberValue() {
		return value;
	}

	public int getValue() {
		return value;
	}
	
}
```

**Example**
```java
PDUInputStream<CommonHeader> is = new PDUInputStream<>(CommonHeader.class, parentIs);
CommonHeader pdu = is.readPDU();
if(pdu.getType() == ProtocolType.TYPE0) {
   Type0 pduType0 = (Type0) pdu;
}
```