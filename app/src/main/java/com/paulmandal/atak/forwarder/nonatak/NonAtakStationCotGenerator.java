package com.paulmandal.atak.forwarder.nonatak;

import android.util.Log;

import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.cot.event.CotPoint;
import com.atakmap.coremap.maps.time.CoordinatedTime;
import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.channel.ChannelTracker;
import com.paulmandal.atak.forwarder.channel.NonAtakUserInfo;
import com.paulmandal.atak.forwarder.channel.UserInfo;
import com.paulmandal.atak.forwarder.comm.protobuf.ContactProtobufConverter;
import com.paulmandal.atak.forwarder.comm.protobuf.TakvProtobufConverter;
import com.paulmandal.atak.forwarder.handlers.InboundMessageHandler;
import com.paulmandal.atak.forwarder.protobufs.ProtobufContact;
import com.paulmandal.atak.forwarder.protobufs.ProtobufTakv;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.paulmandal.atak.forwarder.cotutils.CotMessageTypes.TYPE_PLI;

public class NonAtakStationCotGenerator implements ChannelTracker.ChannelMembersUpdateListener {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + NonAtakStationCotGenerator.class.getSimpleName();

    private static final long DELAY_BETWEEN_GENERATING_COTS_MS = 60000;

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

    private static final int WHITE_INDEX = 0;
    private static final int RTO_INDEX = 6;

    public static String[] TEAMS = {
            "White",
            "Yellow",
            "Orange",
            "Magenta",
            "Red",
            "Maroon",
            "Purple",
            "Dark Blue",
            "Blue",
            "Cyan",
            "Teal",
            "Green",
            "Dark Green",
            "Brown"
    };

    public static String[] ROLES = {
            "Team Member",
            "Team Lead",
            "HQ",
            "Sniper",
            "Medic",
            "Forward Observer",
            "RTO",
            "K9"
    };

    private InboundMessageHandler mInboundMessageHandler;

    private final List<NonAtakUserInfo> mNonAtakStations = new CopyOnWriteArrayList<>();
    private final String mPluginVersion;
    private final String mLocalCallsign;
    private CountDownLatch mCountDownLatch;

    public NonAtakStationCotGenerator(ChannelTracker channelTracker, InboundMessageHandler inboundMessageHandler, String pluginVersion, String localCallsign) {
        mInboundMessageHandler = inboundMessageHandler;
        mPluginVersion = pluginVersion;
        mLocalCallsign = localCallsign;

        Thread thread = new Thread(() -> {
            while (true) {
                generateNonAtakStationCots();

                mCountDownLatch = new CountDownLatch(1);
                try {
                    mCountDownLatch.await(DELAY_BETWEEN_GENERATING_COTS_MS, TimeUnit.MILLISECONDS);
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
        for (NonAtakUserInfo userInfo : mNonAtakStations) {
            if (userInfo.callsign.equals(mLocalCallsign)) {
                continue;
            }

            CotEvent spoofedPli = new CotEvent();

            CoordinatedTime nowCoordinatedTime = new CoordinatedTime(System.currentTimeMillis());
            CoordinatedTime staleCoordinatedTime = new CoordinatedTime(nowCoordinatedTime.getMilliseconds() + STALE_TIME_OFFSET_MS);

            spoofedPli.setUID(userInfo.callsign);
            spoofedPli.setType(TYPE_PLI);
            spoofedPli.setTime(nowCoordinatedTime);
            spoofedPli.setStart(nowCoordinatedTime);
            spoofedPli.setStale(staleCoordinatedTime);
            spoofedPli.setHow("h-e");
            Log.e(TAG, "cs: " + userInfo.callsign + ", lat: " + userInfo.lat + ", lon: " + userInfo.lon);
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

            String team = TEAMS[WHITE_INDEX];
            String role = ROLES[RTO_INDEX];
            if (userInfo.shortName != null && userInfo.shortName.length() == 2) {
                try {
                    // Try to split the shortname and get team/role values
                    int teamIndex = Integer.parseInt(userInfo.shortName.substring(0, 1));
                    int roleIndex = Integer.parseInt(userInfo.shortName.substring(1, 2));

                    if (teamIndex > -1 && teamIndex < TEAMS.length) {
                        team = TEAMS[teamIndex];
                    }

                    if (roleIndex > -1 && roleIndex < ROLES.length) {
                        role = ROLES[roleIndex];
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }

            CotDetail groupDetail = new CotDetail(TAG_GROUP);
            groupDetail.setAttribute(TAG_ROLE, role);
            groupDetail.setAttribute(TAG_NAME, team);
            cotDetail.addChild(groupDetail);

            if (userInfo.batteryPercentage != null) {
                CotDetail statusDetail = new CotDetail(TAG_STATUS);
                statusDetail.setAttribute(TAG_BATTERY, Integer.toString(userInfo.batteryPercentage));
                cotDetail.addChild(statusDetail);
            }

            spoofedPli.setDetail(cotDetail);

            mInboundMessageHandler.retransmitCotToLocalhost(spoofedPli);
        }
    }

    @Override
    public void onChannelMembersUpdated(List<UserInfo> atakUsers, List<NonAtakUserInfo> nonAtakStations) {
        mNonAtakStations.clear();
        mNonAtakStations.addAll(nonAtakStations);

        if (mCountDownLatch != null) {
            mCountDownLatch.countDown();
        }
    }
}
