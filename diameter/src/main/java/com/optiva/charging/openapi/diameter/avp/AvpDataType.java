package com.optiva.charging.openapi.diameter.avp;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.function.Supplier;

public enum AvpDataType {
    GROUPED(GroupedAvpValue::new, GroupedAvpValue.class),
    ENUMERATED(IntegerAvpValue::new, IntegerAvpValue.class),
    INTEGER_32(IntegerAvpValue::new, IntegerAvpValue.class),
    INTEGER_64(LongAvpValue::new, LongAvpValue.class),
    OCTET_STRING(ByteArrayAvpValue::new, ByteArrayAvpValue.class),
    ADDRESS(AddressAvpValue::new, AddressAvpValue.class),
    UNSIGNED_32(IntegerAvpValue::new, IntegerAvpValue.class),
    UNSIGNED_64(LongAvpValue::new, LongAvpValue.class),
    UTF8_STRING(StringAvpValue::new, StringAvpValue.class),
    IDENTITY(StringAvpValue::new, StringAvpValue.class),
    FLOAT_32(FloatAvpValue::new, FloatAvpValue.class),
    FLOAT_64(DoubleAvpValue::new, DoubleAvpValue.class),
    TIME(TimeAvpValue::new, TimeAvpValue.class);

    private final Supplier<AvpValue<?>> supplier;
    private final Class<?> valueClass;

    AvpDataType(Supplier<AvpValue<?>> supplier, Class<? extends AvpValue<?>> clazz) {
        this.supplier = supplier;
        Type type = ((ParameterizedType) clazz.getGenericSuperclass()).getActualTypeArguments()[0];
        if (type instanceof Class<?> c) {
            valueClass = c;
        } else if (type instanceof ParameterizedType p) {
            valueClass = (Class<?>) p.getRawType();
        } else {
            throw new RuntimeException("Unrecoverable error");
        }
    }

    public <T> AvpValue<T> getValueInstance() {
        return (AvpValue<T>) supplier.get();
    }

    public boolean isNotCompatibleValueClass(Object v) {
        return !valueClass.isInstance(v);
    }

    public String getValueClassName() {
        return valueClass.getSimpleName();
    }
}
