package com.paulmandal.atak.forwarder.comm.protobuf;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang.ArrayUtils;

public class BitUtils {
    public static long createBitmask(int bits) {
        long bitmask = bits > 0 ? 1 : 0;
        for (int i = 1 ; i < bits ; i++) {
            bitmask |= 1 << i;
        }
        return bitmask;
    }

    public static long packBits(long customBytes, int containerLength, long value, int fieldLength, ShiftTracker shiftTracker) {
        customBytes |= (value & createBitmask(fieldLength)) << containerLength - shiftTracker.accumulatedShift - fieldLength;
        shiftTracker.accumulatedShift += fieldLength;
        return customBytes;
    }

    public static long unpackBits(long customBytes, int containerLength, int fieldLength, ShiftTracker shiftTracker) {
        long value = customBytes >>> containerLength - shiftTracker.accumulatedShift - fieldLength & createBitmask(fieldLength);
        shiftTracker.accumulatedShift += fieldLength;
        return value;
    }

    public static int findMappingForArray(String fieldName, Object[] array, Object objectToFind) throws MappingNotFoundException {
        int index = ArrayUtils.indexOf(array, objectToFind);

        if (index == -1) {
            throw new MappingNotFoundException("Could not find mapping for field: " + fieldName + ", value: " + objectToFind);
        }
        return index;
    }

    public static long setFieldNull(long customBytes, int containerLength, int fieldLength, ShiftTracker shiftTracker) {
        customBytes |= 1L << containerLength - shiftTracker.accumulatedShift - 1;
        shiftTracker.accumulatedShift += fieldLength;
        return customBytes;
    }

    public static long packNonNullMappedString(long customBytes, int containerLength, String value, String fieldName, int fieldLength, String[] mapping, ShiftTracker shiftTracker) throws MappingNotFoundException {
        int index = findMappingForArray(fieldName, mapping, value);
        customBytes |= (index & createBitmask(fieldLength)) << containerLength - shiftTracker.accumulatedShift - fieldLength;
        shiftTracker.accumulatedShift += fieldLength;
        return customBytes;
    }

    public static long packNullableMappedString(long customBytes, int containerLength, String value, String fieldName, int fieldLength, String[] mapping, ShiftTracker shiftTracker) throws MappingNotFoundException {
        if (value != null) {
            return packNonNullMappedString(customBytes, containerLength, value, fieldName, fieldLength, mapping, shiftTracker);
        }
        return setFieldNull(customBytes, containerLength, fieldLength, shiftTracker);
    }

    public static long packNullableInt(long customBytes, int containerLength, Integer value, int fieldLength, ShiftTracker shiftTracker) {
        if (value != null) {
            customBytes |= (value & createBitmask(fieldLength)) << containerLength - shiftTracker.accumulatedShift - fieldLength;
            shiftTracker.accumulatedShift += fieldLength;
            return customBytes;
        }
        return setFieldNull(customBytes, containerLength, fieldLength, shiftTracker);
    }

    public static long packNullableBoolean(long customBytes, int containerLength, Boolean value, int fieldLength, ShiftTracker shiftTracker) {
        if (value != null) {
            customBytes |= ((value ? 1 : 0) & createBitmask(1)) << containerLength - shiftTracker.accumulatedShift - fieldLength;
            shiftTracker.accumulatedShift += fieldLength;
            return customBytes;
        }
        return setFieldNull(customBytes, containerLength, fieldLength, shiftTracker);
    }

    public static boolean hasNullableField(long customBytes, int containerLength, ShiftTracker shiftTracker) {
        return (customBytes >>> containerLength - shiftTracker.accumulatedShift - 1 & createBitmask(1)) == 0;
    }

    @Nullable
    public static String unpackNullableMappedString(long customBytes, int containerLength, int fieldLength, String[] mapping, ShiftTracker shiftTracker) {
        if (!hasNullableField(customBytes, containerLength, shiftTracker)) {
            shiftTracker.accumulatedShift += fieldLength;
            return null;
        }
        return unpackNonNullMappedString(customBytes, containerLength, fieldLength, mapping, shiftTracker);
    }

    @NonNull
    public static String unpackNonNullMappedString(long customBytes, int containerLength, int fieldLength, String[] mapping, ShiftTracker shiftTracker) {
        int index = (int)(customBytes >>> containerLength - shiftTracker.accumulatedShift - fieldLength & createBitmask(fieldLength));
        shiftTracker.accumulatedShift += fieldLength;
        return mapping[index];
    }

    @Nullable
    public static Integer unpackNullableInt(long customBytes, int containerLength, int fieldLength, ShiftTracker shiftTracker) {
        Integer value = null;
        if (hasNullableField(customBytes, containerLength, shiftTracker)) {
            value = (int)(customBytes >>> containerLength - shiftTracker.accumulatedShift - fieldLength & createBitmask(fieldLength));
        }
        shiftTracker.accumulatedShift += fieldLength;
        return value;
    }

    @Nullable
    public static Boolean unpackNullableBoolean(long customBytes, int containerLength, int fieldLength, ShiftTracker shiftTracker) {
        Boolean value = null;
        if (hasNullableField(customBytes, containerLength, shiftTracker)) {
            value = (customBytes >>> containerLength - shiftTracker.accumulatedShift - fieldLength & createBitmask(fieldLength)) == 1;
        }
        shiftTracker.accumulatedShift += fieldLength;
        return value;
    }
}
