package com.paulmandal.atak.forwarder.comm.protobuf;

import com.atakmap.coremap.cot.event.CotEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MinimalCotProtobufConverter {
    private static final List<String> SUPPORTED_COT_TYPES = new ArrayList<>(Arrays.asList(""));

    public boolean isSupportedType(String type) {
        return SUPPORTED_COT_TYPES.contains(type);
    }

    public byte[] toByteArray(CotEvent cotEvent) {
        return null;
    }

    public CotEvent toCotEvent(byte[] cotProtobuf) {
        return null;
    }


}
