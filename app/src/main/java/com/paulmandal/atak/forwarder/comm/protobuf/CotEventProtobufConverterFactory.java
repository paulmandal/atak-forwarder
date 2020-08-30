package com.paulmandal.atak.forwarder.comm.protobuf;

import java.util.Calendar;

public class CotEventProtobufConverterFactory {
    public static CotEventProtobufConverter createCotEventProtobufConverter() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MONTH, 0);
        cal.set(Calendar.DATE, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startOfYearMs = cal.getTime().getTime();

        return new CotEventProtobufConverter(
                new TakvProtobufConverter(),
                new TrackProtobufConverter(),
                new ServerDestinationProtobufConverter(),
                new RemarksProtobufConverter(),
                new ContactProtobufConverter(),
                new UnderscoreGroupProtobufConverter(),
                new CustomBytesConverter(),
                new CustomBytesExtConverter(),
                new ChatProtobufConverter(
                        new ChatGroupProtobufConverter(),
                        new HierarchyProtobufConverter(new GroupProtobufConverter(new GroupContactProtobufConverter()))),
                new LinkProtobufConverter(),
                new LabelsOnProtobufConverter(),
                new PrecisionLocationProtobufConverter(),
                new DroppedFieldConverter(),
                new StatusProtobufConverter(),
                new HeightAndHeightUnitProtobufConverter(),
                new ModelProtobufConverter(),
                new DetailStyleProtobufConverter(),
                new CeHumanInputProtobufConverter(),
                new FreehandLinkProtobufConverter(),
                startOfYearMs);
    }
}
