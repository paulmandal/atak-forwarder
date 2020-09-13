package com.paulmandal.atak.forwarder.comm.protobuf;

import com.paulmandal.atak.forwarder.comm.protobuf.medevac.FlowTagsProtobufConverter;
import com.paulmandal.atak.forwarder.comm.protobuf.medevac.MedevacProtobufConverter;
import com.paulmandal.atak.forwarder.comm.protobuf.medevac.MistProtobufConverter;
import com.paulmandal.atak.forwarder.comm.protobuf.medevac.MistsMapProtobufConverter;
import com.paulmandal.atak.forwarder.comm.protobuf.shape.EllipseLinkProtobufConverter;
import com.paulmandal.atak.forwarder.comm.protobuf.shape.EllipseProtobufConverter;
import com.paulmandal.atak.forwarder.comm.protobuf.shape.GeoFenceProtobufConverter;
import com.paulmandal.atak.forwarder.comm.protobuf.shape.LineStyleProtobufConverter;
import com.paulmandal.atak.forwarder.comm.protobuf.shape.PolyStyleProtobufConverter;
import com.paulmandal.atak.forwarder.comm.protobuf.shape.ShapeProtobufConverter;
import com.paulmandal.atak.forwarder.comm.protobuf.shape.StyleProtobufConverter;

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
                new LabelsOnProtobufConverter(),
                new PrecisionLocationProtobufConverter(),
                new DroppedFieldConverter(),
                new StatusProtobufConverter(),
                new HeightAndHeightUnitProtobufConverter(),
                new ModelProtobufConverter(),
                new DetailStyleProtobufConverter(),
                new CeHumanInputProtobufConverter(),
                new FreehandLinkProtobufConverter(),
                new ChatLinkProtobufConverter(),
                new ComplexLinkProtobufConverter(),
                new ShapeLinkProtobufConverter(),
                new RouteProtobufConverter(
                        new NavCuesProtobufConverter(
                                new NavCueProtobufConverter(
                                        new TriggerProtobufConverter()
                                )
                        )
                ),
                new LinkAttrProtobufConverter(),
                new TogProtobufConverter(),
                new SensorProtobufConverter(),
                new VideoProtobufConverter(
                        new ConnectionEntryProtobufConverter()
                ),
                new FlowTagsProtobufConverter(),
                new MedevacProtobufConverter(
                        new MistsMapProtobufConverter(
                                new MistProtobufConverter()
                        )
                ),
                new GeoFenceProtobufConverter(),
                new ShapeProtobufConverter(
                        new EllipseProtobufConverter(),
                        new EllipseLinkProtobufConverter(
                                new StyleProtobufConverter(
                                        new LineStyleProtobufConverter(),
                                        new PolyStyleProtobufConverter()
                                )
                        )
                ),
                startOfYearMs);
    }
}
