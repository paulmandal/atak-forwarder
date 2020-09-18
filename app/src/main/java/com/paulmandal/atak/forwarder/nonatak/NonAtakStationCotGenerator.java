package com.paulmandal.atak.forwarder.nonatak;

import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.cot.event.CotPoint;
import com.atakmap.coremap.maps.time.CoordinatedTime;
import com.paulmandal.atak.forwarder.channel.ChannelTracker;
import com.paulmandal.atak.forwarder.channel.NonAtakUserInfo;
import com.paulmandal.atak.forwarder.channel.UserInfo;
import com.paulmandal.atak.forwarder.comm.protobuf.ContactProtobufConverter;
import com.paulmandal.atak.forwarder.comm.protobuf.TakvProtobufConverter;
import com.paulmandal.atak.forwarder.handlers.InboundMessageHandler;
import com.paulmandal.atak.forwarder.protobufs.ProtobufContact;
import com.paulmandal.atak.forwarder.protobufs.ProtobufTakv;

import java.util.ArrayList;
import java.util.List;

import static com.paulmandal.atak.forwarder.cotutils.CotMessageTypes.TYPE_PLI;

public class NonAtakStationCotGenerator implements ChannelTracker.ChannelMembersUpdateListener {
    private static final int DELAY_BETWEEN_GENERATING_COTS_MS = 60000;

    private static final int STALE_TIME_OFFSET_MS = 75000;
    private static final double UNKNOWN_LE_CE = 9999999.0;

    private static final String TAG_DETAIL = "detail";
    private static final String TAG_UID = "uid";
    private static final String TAG_DROID = "Droid";
    private static final String TAG_PRECISION_LOCATION = "precisionlocation";
    private static final String TAG_ALTSRC = "altsrc";
    private static final String TAG_GEOPOINTSRC = "geopointsrc";
    private static final String TAG_GROUP = "__group";
    private static final String TAG_ROLE = "role";
    private static final String TAG_NAME = "name";
    private static final String TAG_STATUS = "status";
    private static final String TAG_BATTERY = "battery";

    private static final String VALUE_MESHTASTIC_DEVICE = "Meshtastic Device";
    private static final String VALUE_ATAK_FORWARDER = "ATAK Forwarder";
    private static final String VALUE_DTED0 = "DTED0";
    private static final String VALUE_USER = "USER";
    private static final String VALUE_RTO = "RTO";
    private static final String VALUE_WHITE = "White";

    private InboundMessageHandler mInboundMessageHandler;

    private final List<NonAtakUserInfo> mNonAtakStations = new ArrayList<>();
    private final String mPluginVersion;

    public NonAtakStationCotGenerator(ChannelTracker channelTracker, InboundMessageHandler inboundMessageHandler, String pluginVersion) {
        mInboundMessageHandler = inboundMessageHandler;
        mPluginVersion = pluginVersion;

        Thread thread = new Thread(() -> {
            while (true) {
                generateNonAtakStationCots();

                try {
                    Thread.sleep(DELAY_BETWEEN_GENERATING_COTS_MS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.setName("NonAtakStationCotGenerator.generateCoTs");
        thread.start();

        channelTracker.addUpdateListener(this);
    }

    private void generateNonAtakStationCots() {
        synchronized (mNonAtakStations) {
            for (NonAtakUserInfo userInfo : mNonAtakStations) {
                CotEvent spoofedPli = new CotEvent();

                CoordinatedTime nowCoordinatedTime = new CoordinatedTime(System.currentTimeMillis());
                CoordinatedTime staleCoordinatedTime = new CoordinatedTime(nowCoordinatedTime.getMilliseconds() + STALE_TIME_OFFSET_MS);

                spoofedPli.setUID(userInfo.callsign);
                spoofedPli.setType(TYPE_PLI);
                spoofedPli.setTime(nowCoordinatedTime);
                spoofedPli.setStart(nowCoordinatedTime);
                spoofedPli.setStale(staleCoordinatedTime);
                spoofedPli.setHow("h-e");
                spoofedPli.setPoint(new CotPoint(userInfo.lat, userInfo.lon, userInfo.altitude, UNKNOWN_LE_CE, UNKNOWN_LE_CE));

                CotDetail cotDetail = new CotDetail(TAG_DETAIL);

                TakvProtobufConverter mTakvProtobufConverter = new TakvProtobufConverter();

                ProtobufTakv.Takv.Builder takv = ProtobufTakv.Takv.newBuilder();
                takv.setOs(1);
                takv.setVersion(mPluginVersion);
                takv.setDevice(VALUE_MESHTASTIC_DEVICE);
                takv.setPlatform(VALUE_ATAK_FORWARDER);
                mTakvProtobufConverter.maybeAddTakv(cotDetail, takv.build());

                ProtobufContact.Contact.Builder contact = ProtobufContact.Contact.newBuilder();
                contact.setCallsign(userInfo.callsign);
                ContactProtobufConverter mContactProtobufConverter = new ContactProtobufConverter();
                mContactProtobufConverter.maybeAddContact(cotDetail, contact.build(), false);

                CotDetail uidDetail = new CotDetail(TAG_UID);
                uidDetail.setAttribute(TAG_DROID, userInfo.callsign);
                cotDetail.addChild(uidDetail);

                CotDetail precisionLocationDetail = new CotDetail(TAG_PRECISION_LOCATION);
                precisionLocationDetail.setAttribute(TAG_ALTSRC, VALUE_DTED0);
                precisionLocationDetail.setAttribute(TAG_GEOPOINTSRC, VALUE_USER);
                cotDetail.addChild(precisionLocationDetail);

                CotDetail groupDetail = new CotDetail(TAG_GROUP);
                groupDetail.setAttribute(TAG_ROLE, VALUE_RTO);
                groupDetail.setAttribute(TAG_NAME, VALUE_WHITE);
                cotDetail.addChild(groupDetail);

                if (userInfo.batteryPercentage != null) {
                    CotDetail statusDetail = new CotDetail(TAG_STATUS);
                    statusDetail.setAttribute(TAG_BATTERY, Integer.toString(userInfo.batteryPercentage));
                    cotDetail.addChild(statusDetail);
                }

                // TODO: remove this
//                CotDetail trackDetail = new CotDetail(TAG_TRACK);
//                trackDetail.setAttribute(TAG_COURSE, VALUE_ZERO_POINT_ZERO);
//                trackDetail.setAttribute(TAG_SPEED, VALUE_ZERO_POINT_ZERO);
//                cotDetail.addChild(trackDetail);

                spoofedPli.setDetail(cotDetail);

                mInboundMessageHandler.retransmitCotToLocalhost(spoofedPli);
            }
        }
    }

    @Override
    public void onChannelMembersUpdated(List<UserInfo> atakUsers, List<NonAtakUserInfo> nonAtakStations) {
        synchronized (mNonAtakStations) {
            mNonAtakStations.clear();
            mNonAtakStations.addAll(nonAtakStations);

            generateNonAtakStationCots();
        }
    }
}
