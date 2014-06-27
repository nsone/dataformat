# Dataformat

Dataformat is a binary format to POJO converter

## Examples

### Read from java.io.InputStream

```java
PDUInputStream<FrameHeader> is = new PDUInputStream<>(FrameHeader.class, parentIs);
FrameHeader frame = is.readPDU();
if(frame.getEthertype == Ethertype.ARP)
{
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
public class OFLinkDiscovery extends FrameHeader
{
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
@PDUElement(order = 8, type = BITMAP, length = 4, args = "my.package.Feature")
protected Set<Feature> currentFeatures;
```

**my.package.Feature**
```java
public static enum Feature implements Bitmappable
{
	FEATURE1(0x01),
	FEATURE2(0x02),
	FEATURE3(0x04);


	protected static Map<Long, Feature> valueToFeature = new HashMap<>();

	static
	{
		for (Feature feature : EnumSet.allOf(Feature.class))
		{
			valueToFeature.put(feature.getValue(), portFeature);
		}
	}

	public static Feature getByValue(long value)
	{
		return valueToFeature.get(value);
	}

	protected long value;

	Feature(long value)
	{
		this.value = value;
	}

	@Override
	public long getValue()
	{
		return value;
	}
}
```




