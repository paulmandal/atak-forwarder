package com.paulmandal.atak.forwarder.tracker;

import android.content.SharedPreferences;

import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.cot.event.CotPoint;
import com.atakmap.coremap.maps.time.CoordinatedTime;
import com.paulmandal.atak.forwarder.ForwarderConstants;
import com.paulmandal.atak.forwarder.channel.TrackerUserInfo;
import com.paulmandal.atak.forwarder.channel.UserTracker;
import com.paulmandal.atak.forwarder.handlers.InboundMessageHandler;
import com.paulmandal.atak.forwarder.helpers.Logger;
import com.paulmandal.atak.forwarder.plugin.Destroyable;
import com.paulmandal.atak.forwarder.plugin.DestroyableSharedPrefsListener;
import com.paulmandal.atak.forwarder.preferences.PreferencesDefaults;
import com.paulmandal.atak.forwarder.preferences.PreferencesKeys;
import com.paulmandal.atak.libcotshrink.protobuf.cotevent.detail.contact.ContactProtobufConverter;
import com.paulmandal.atak.libcotshrink.protobuf.cotevent.detail.takv.TakvProtobufConverter;
import com.paulmandal.atak.libcotshrink.protobufs.ProtobufContact;
import com.paulmandal.atak.libcotshrink.protobufs.ProtobufTakv;

import java.util.List;

public class TrackerCotGenerator  extends DestroyableSharedPrefsListener implements UserTracker.TrackerUpdateListener {
    private static final String TAG = ForwarderConstants.DEBUG_TAG_PREFIX + TrackerCotGenerator.class.getSimpleName();

    private static final String TYPE_PLI = "a-f-G-U-C";

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

    private static final String VALUE_UID_PREFIX = "MESHTASTIC";
    private static final String VALUE_MESHTASTIC_DEVICE = "Meshtastic Device";
    private static final String VALUE_ATAK_FORWARDER = "ATAK Forwarder";
    private static final String VALUE_HOW_GPS = "m-g";
    private static final String VALUE_UNKNOWN = "???";
    private static final String VALUE_GPS = "GPS";

    private static final int CLEAR_INDEX = 0;
    private static final int TEAM_MEMBER_INDEX = 0;

    private static final String UNKNOWN_CALLSIGN_PREFIX = "Tracker-";

    private int mTrackerStaleTimeMs;

    public static final String[] TEAMS = {
            "Clear",
            "Cyan",
            "White",
            "Yellow",
            "Orange",
            "Magenta",
            "Red",
            "Maroon",
            "Purple",
            "Dark Blue",
            "Blue",
            "Teal",
            "Green",
            "Dark Green",
            "Brown"
    };

    public static final String[] ROLES = {
            "Team Member",
            "Team Lead",
            "HQ",
            "Sniper",
            "Medic",
            "Forward Observer",
            "RTO",
            "K9"
    };

    private final InboundMessageHandler mInboundMessageHandler;
    private final Logger mLogger;

    private final String mPluginVersion;

    public TrackerCotGenerator(List<Destroyable> destroyables,
                               SharedPreferences sharedPreferences,
                               UserTracker userTracker,
                               InboundMessageHandler inboundMessageHandler,
                               Logger logger,
                               String pluginVersion) {
        super(destroyables,
                sharedPreferences,
                new String[] {},
                new String[] {
                    PreferencesKeys.KEY_TRACKER_STALE_AFTER_SECS,
                });

        mInboundMessageHandler = inboundMessageHandler;
        mLogger = logger;
        mPluginVersion = pluginVersion;

        userTracker.addTrackerUpdateListener(this);

        refreshTrackerStaleTime(sharedPreferences);
    }

    @Override
    public void trackerUpdated(TrackerUserInfo trackerUserInfo) {
        drawTracker(trackerUserInfo);
    }

    @Override
    protected void updateSettings(SharedPreferences sharedPreferences) {
        // Do nothing
    }

    @Override
    protected void complexUpdate(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case PreferencesKeys.KEY_TRACKER_STALE_AFTER_SECS:
                refreshTrackerStaleTime(sharedPreferences);
                break;
        }
    }

    private void refreshTrackerStaleTime(SharedPreferences sharedPreferences) {
        int drawStaleAfterSecs = Integer.parseInt(sharedPreferences.getString(PreferencesKeys.KEY_TRACKER_STALE_AFTER_SECS, PreferencesDefaults.DEFAULT_TRACKER_STALE_AFTER_SECS));
        mTrackerStaleTimeMs = drawStaleAfterSecs * 1000;
    }

    private void drawTracker(TrackerUserInfo tracker) {
        mLogger.v(TAG, "drawTracker() callsign: " + tracker.callsign + ", meshId: " + tracker.meshId + ", atakUid: " + tracker.atakUid);
        if (!tracker.gpsValid) {
            mLogger.v(TAG, "drawTracker(), gpsValid: " + tracker.gpsValid);
            // Ignore updates that don't contain a valid GPS point
            return;
        }

        CotEvent spoofedPli = new CotEvent();

        String meshIdWithoutExclamation = tracker.meshId.replaceAll("!", "");
        String uid = String.format("%s-%s", VALUE_UID_PREFIX, meshIdWithoutExclamation);

        CoordinatedTime lastMsgCoordinatedTime = new CoordinatedTime(tracker.lastSeenTime);
        CoordinatedTime staleCoordinatedTime = new CoordinatedTime(tracker.lastSeenTime + mTrackerStaleTimeMs);

        spoofedPli.setUID(uid);
        spoofedPli.setType(TYPE_PLI);
        spoofedPli.setTime(lastMsgCoordinatedTime);
        spoofedPli.setStart(lastMsgCoordinatedTime);
        spoofedPli.setStale(staleCoordinatedTime);
        spoofedPli.setHow(VALUE_HOW_GPS);
        spoofedPli.setPoint(new CotPoint(tracker.lat, tracker.lon, tracker.altitude, UNKNOWN_LE_CE, UNKNOWN_LE_CE));

        CotDetail cotDetail = new CotDetail(TAG_DETAIL);

        TakvProtobufConverter mTakvProtobufConverter = new TakvProtobufConverter();

        ProtobufTakv.Takv.Builder takv = ProtobufTakv.Takv.newBuilder();
        takv.setOs(1);
        takv.setVersion(mPluginVersion);
        takv.setDevice(VALUE_MESHTASTIC_DEVICE);
        takv.setPlatform(VALUE_ATAK_FORWARDER);
        mTakvProtobufConverter.maybeAddTakv(cotDetail, takv.build());

        ProtobufContact.Contact.Builder contact = ProtobufContact.Contact.newBuilder();
        String callsign = tracker.callsign.equals(TrackerUserInfo.CALLSIGN_UNKNOWN) ? UNKNOWN_CALLSIGN_PREFIX + meshIdWithoutExclamation : tracker.callsign;
        contact.setCallsign(callsign);
        ContactProtobufConverter mContactProtobufConverter = new ContactProtobufConverter();
        mContactProtobufConverter.maybeAddContact(cotDetail, contact.build(), false);

        CotDetail uidDetail = new CotDetail(TAG_UID);
        uidDetail.setAttribute(TAG_DROID, tracker.callsign);
        cotDetail.addChild(uidDetail);

        CotDetail precisionLocationDetail = new CotDetail(TAG_PRECISION_LOCATION);
        precisionLocationDetail.setAttribute(TAG_ALTSRC, VALUE_UNKNOWN);
        precisionLocationDetail.setAttribute(TAG_GEOPOINTSRC, VALUE_GPS);
        cotDetail.addChild(precisionLocationDetail);

        String team = TEAMS[CLEAR_INDEX];
        String role = ROLES[TEAM_MEMBER_INDEX];
        if (tracker.shortName != null && tracker.shortName.length() > 1) {
            try {
                // Try to split the shortname and get team/role values
                int roleIndex = Integer.parseInt(tracker.shortName.substring(0, 1));
                int teamIndex = Integer.parseInt(tracker.shortName.substring(1));

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

        if (tracker.batteryPercentage != null) {
            CotDetail statusDetail = new CotDetail(TAG_STATUS);
            statusDetail.setAttribute(TAG_BATTERY, Integer.toString(tracker.batteryPercentage));
            cotDetail.addChild(statusDetail);
        }

        spoofedPli.setDetail(cotDetail);

        mLogger.v(TAG, "  Drew callsign: " + callsign + ", shortName: " + tracker.shortName + ", uid: " + uid + ", lastMsgTime: " + lastMsgCoordinatedTime + ", staleTime: " + staleCoordinatedTime + ", lat: " + tracker.lat + ", lon: " + tracker.lon + ", alt: " + tracker.altitude);

        mInboundMessageHandler.retransmitCotToLocalhost(spoofedPli);
    }
}
