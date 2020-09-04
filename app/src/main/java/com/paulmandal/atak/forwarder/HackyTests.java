package com.paulmandal.atak.forwarder;

import android.util.Log;

import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.cot.event.CotPoint;
import com.atakmap.coremap.maps.time.CoordinatedTime;
import com.paulmandal.atak.forwarder.comm.protobuf.CotEventProtobufConverter;
import com.paulmandal.atak.forwarder.comm.protobuf.CotEventProtobufConverterFactory;
import com.paulmandal.atak.forwarder.comm.protobuf.MappingNotFoundException;
import com.paulmandal.atak.forwarder.comm.protobuf.UnknownDetailFieldException;
import com.paulmandal.atak.forwarder.xmlutils.XmlComparer;

import java.util.ArrayList;
import java.util.List;

// Tests to validate our protobufs
public class HackyTests {
    private static final String TAG = "ATAKDBG." + HackyTests.class.getSimpleName();

    public void runAllTests() {
        testPli();
        testPliWithZeroEndpointAddr();
        testComplexShape();
        testDrawnShape();
        testFreehand();
        testGroupChat();
        testOp();
        testPeerToPeerChat();
        testRoute();
        testSensor();
        testSimpleShape();
        testUserIcon();
        testWaypoint();
        testCasevacAllFields();
        testCasevacNoFields();
        testCircle();
    }

    public void testPli() {
        validate("PLI", 193, "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><event version='2.0' uid='ANDROID-53af0912586418dc' type='a-f-G-U-C' time='2020-08-29T21:14:00.406Z' start='2020-08-29T21:14:00.406Z' stale='2020-08-29T21:15:15.406Z' how='h-e'><point lat='39.71401955573084' lon='-104.99452709918448' hae='1586.245787738948' ce='9999999.0' le='9999999.0'/><detail><takv os='29' version='4.0.0.7 (a457ad0d).1597850931-CIV' device='GOOGLE PIXEL 4 XL' platform='ATAK-CIV'/><contact endpoint='192.168.1.159:4242:tcp' callsign='dasuberdog'/><uid Droid='dasuberdog'/><precisionlocation altsrc='DTED2' geopointsrc='USER'/><__group role='Team Lead' name='Orange'/><status battery='58'/><track course='327.66875837972367' speed='0.0'/></detail></event>");
    }

    public void testPliWithZeroEndpointAddr() {
        validate("PLI w 0.0.0.0 endpoint", 188, "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><event version='2.0' uid='ANDROID-53af0912586418dc' type='a-f-G-U-C' time='2020-08-29T21:14:00.406Z' start='2020-08-29T21:14:00.406Z' stale='2020-08-29T21:15:15.406Z' how='h-e'><point lat='39.71401955573084' lon='-104.99452709918448' hae='1586.245787738948' ce='9999999.0' le='9999999.0'/><detail><takv os='29' version='4.0.0.7 (a457ad0d).1597850931-CIV' device='GOOGLE PIXEL 4 XL' platform='ATAK-CIV'/><contact endpoint='0.0.0.0:4242:tcp' callsign='dasuberdog'/><uid Droid='dasuberdog'/><precisionlocation altsrc='DTED2' geopointsrc='USER'/><__group role='Team Lead' name='Orange'/><status battery='58'/><track course='327.66875837972367' speed='0.0'/></detail></event>");
    }


    public void testComplexShape() {
        validate("Complex Shape", 226, "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><event version='2.0' uid='1da89ca4-9a57-4bba-b082-3793ebbd3ddb' type='overhead_marker' time='2020-08-29T21:25:54.877Z' start='2020-08-29T21:25:54.877Z' stale='2020-08-30T21:25:54.877Z' how='h-e'><point lat='39.72750837570107' lon='-104.98061468169054' hae='1597.252688001634' ce='9999999.0' le='9999999.0'/><detail><model name='Squad Car'/><track course='0.0'/><contact callsign='Squad Car'/><remarks>Do p</remarks><archive/><link uid='ANDROID-53af0912586418dc' production_time='2020-08-29T21:25:36.669Z' type='a-f-G-U-C' parent_callsign='dasuberdog' relation='p-p'/><labels_on value='false'/><height_unit>1</height_unit><height unit='meters' value='0.0'>0.0</height><precisionlocation altsrc='DTED2'/><color value='-16744704'/></detail></event>");
    }


    public void testDrawnShape() {
        validate("Drawn Shape", 273, "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><event version='2.0' uid='52ce8003-8a24-41fd-8119-7e603ac1e95e' type='u-d-r' time='2020-08-29T21:31:25.109Z' start='2020-08-29T21:31:25.109Z' stale='2020-08-30T21:31:25.109Z' how='h-e'><point lat='39.730614458351475' lon='-104.99506083807847' hae='1588.394668173431' ce='9999999.0' le='9999999.0'/><detail><link point='39.734374690892466,-105.000951756657,1583.000100853604'/><link point='39.72561220672946,-104.9910075127075,1591.5484945254186'/><link point='39.72685459182151,-104.98916936139345'/><link point='39.735616232147535,-104.99911502806049'/><contact callsign='Rectangle 1'/><remarks>Nice</remarks><archive/><strokeColor value='-16744704'/><strokeWeight value='4.0'/><fillColor value='-1778352384'/><labels_on value='true'/><height_unit>4</height_unit><height unit='feet' value='0.9144000000000001'>0.9144000000000001</height><precisionlocation altsrc='DTED2'/><tog enabled='1'/></detail></event>");
    }


    public void testFreehand() {
        validate("Freehand", 606, "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><event version='2.0' uid='30408da0-5e20-4eb5-a818-14dfe25d0be2' type='u-d-f-m' time='2020-08-29T21:51:23.406Z' start='2020-08-29T21:51:23.406Z' stale='2020-08-30T21:51:23.406Z' how='h-e'><point lat='0.0' lon='0.0' hae='9999999.0' ce='9999999.0' le='9999999.0' /><detail><link line='&lt;?xml version=&apos;1.0&apos; encoding=&apos;UTF-8&apos; standalone=&apos;yes&apos;?&gt;&lt;event version=&apos;2.0&apos; uid=&apos;88cde5f4-f5c6-4941-bf1e-0eb43d63ad6b&apos; type=&apos;u-d-f&apos; time=&apos;2020-08-29T21:51:23.406Z&apos; start=&apos;2020-08-29T21:51:23.406Z&apos; stale=&apos;2020-08-30T21:51:23.406Z&apos; how=&apos;h-e&apos;&gt;&lt;point lat=&apos;39.52891154613381&apos; lon=&apos;-105.75749869258964&apos; hae=&apos;3101.27767599931&apos; ce=&apos;9999999.0&apos; le=&apos;9999999.0&apos; /&gt;&lt;detail&gt;&lt;link point=&apos;39.52906154607542,-105.75732260632482&apos;/&gt;&lt;link point=&apos;39.52896154611435,-105.75762059846528&apos;/&gt;&lt;link point=&apos;39.52892972794492,-105.75766123375718&apos;/&gt;&lt;link point=&apos;39.52887063705884,-105.75763414356258&apos;/&gt;&lt;link point=&apos;39.52885245524774,-105.7575799631734&apos;/&gt;&lt;link point=&apos;39.52884336434219,-105.75736775664913&apos;/&gt;&lt;link point=&apos;39.52886154615329,-105.75730454619509&apos;/&gt;&lt;labels_on value=&apos;true&apos;/&gt;&lt;archive/&gt;&lt;strokeColor value=&apos;-16776961&apos;/&gt;&lt;strokeWeight value=&apos;4.0&apos;/&gt;&lt;contact callsign=&apos;Freehand 2&apos;/&gt;&lt;/detail&gt;&lt;/event&gt;'/><labels_on value='true'/><archive/><color value='-16744704'/><remarks>But</remarks><strokeColor value='-1'/><strokeWeight value='4.0'/><height_unit>4</height_unit><height unit='feet' value='0.9144000000000001'>0.9144000000000001</height><contact callsign='Freehand 2'/></detail></event>");
    }


    public void testGroupChat() {
        validate("Group Chat", 355, "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><event version='2.0' uid='GeoChat.ANDROID-53af0912586418dc.okm.35f1f729-4107-4c84-8eb5-c04f3eac7701' type='b-t-f' time='2020-08-29T21:29:33.912Z' start='2020-08-29T21:29:33.912Z' stale='2020-08-30T21:29:33.912Z' how='h-g-i-g-o'><point lat='39.71401955573084' lon='-104.99452709918448' hae='1586.245787738948' ce='9999999.0' le='9999999.0'/><detail><__chat parent='UserGroups' groupOwner='true' chatroom='okm' id='b1367178-c68f-4f86-9753-4fd9a283f1c9' senderCallsign='dasuberdog'><chatgrp uid0='ANDROID-53af0912586418dc' uid1='ANDROID-355499060918435' id='b1367178-c68f-4f86-9753-4fd9a283f1c9'/><hierarchy><group uid='UserGroups' name='Groups'><group uid='b1367178-c68f-4f86-9753-4fd9a283f1c9' name='okm'><contact uid='ANDROID-53af0912586418dc' name='dasuberdog'/><contact uid='ANDROID-355499060918435' name='maya'/></group></group></hierarchy></__chat><link uid='ANDROID-53af0912586418dc' type='a-f-G-U-C' relation='p-p'/><remarks source='BAO.F.ATAK.ANDROID-53af0912586418dc' time='2020-08-29T21:29:33.912Z'>at VDO</remarks><__serverdestination destinations='192.168.1.159:4242:tcp:ANDROID-53af0912586418dc'/></detail></event>");
    }


    public void testOp() {
        validate("OP", 200, "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><event version='2.0' uid='701e1268-3cee-44f9-b45b-8c7e000ef85d' type='b-m-p-s-p-op' time='2020-08-29T21:24:46.588Z' start='2020-08-29T21:24:46.588Z' stale='2020-08-29T21:29:46.588Z' how='h-g-i-g-o'><point lat='39.7265559318419' lon='-104.98673259921553' hae='1588.5983739602902' ce='198.5' le='9999999.0'/><detail><status readiness='true'/><archive/><contact callsign='dasuberdog.29.152430'/><remarks/><archive/><link uid='ANDROID-53af0912586418dc' production_time='2020-08-29T21:24:30.471Z' type='a-f-G-U-C' parent_callsign='dasuberdog' relation='p-p'/><precisionlocation altsrc='DTED2' geopointsrc='USER'/><ce_human_input>true</ce_human_input><color argb='-1'/></detail></event>");
    }


    public void testPeerToPeerChat() {
        validate("P2P Chat", 343, "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><event version='2.0' uid='GeoChat.ANDROID-53af0912586418dc.maya.af7f7c7b-0577-403e-83c3-2eaf6e700d2e' type='b-t-f' time='2020-08-29T21:27:12.460Z' start='2020-08-29T21:27:12.460Z' stale='2020-08-30T21:27:12.460Z' how='h-g-i-g-o'><point lat='39.71401955573084' lon='-104.99452709918448' hae='1586.245787738948' ce='9999999.0' le='9999999.0'/><detail><__chat parent='RootContactGroup' groupOwner='false' chatroom='maya' id='ANDROID-355499060918435' senderCallsign='dasuberdog'><chatgrp uid0='ANDROID-53af0912586418dc' uid1='ANDROID-355499060918435' id='ANDROID-355499060918435'/></__chat><link uid='ANDROID-53af0912586418dc' type='a-f-G-U-C' relation='p-p'/><remarks source='BAO.F.ATAK.ANDROID-53af0912586418dc' to='ANDROID-355499060918435' time='2020-08-29T21:27:12.460Z'>you are a dog</remarks><__serverdestination destinations='192.168.1.159:4242:tcp:ANDROID-53af0912586418dc'/></detail></event>");
    }


    public void testRoute() {
        validate("Route", 887, "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><event version='2.0' uid='3eb5f892-e44f-4605-9693-288815722690' type='b-m-r' time='2020-08-29T21:38:00.716Z' start='2020-08-29T21:38:00.716Z' stale='2020-08-30T21:38:00.716Z' how='h-e'><point lat='0.0' lon='0.0' hae='9999999.0' ce='9999999.0' le='9999999.0'/><detail><link uid='93798514-6c72-4e14-88ce-2288ad85370e' callsign='Route 7 SP' type='b-m-p-w' point='39.530711638779984,-105.76095833293527,3229.2090310525723' remarks='' relation='c'/><link uid='e0b082c8-a522-4661-9960-211b2d0f61cb' callsign='' type='b-m-p-c' point='39.53015542202335,-105.76000133390068,3199.4863308287886' remarks='' relation='c'/><link uid='ae39bb53-4bae-449a-b6c3-80dde5bce84d' callsign='' type='b-m-p-c' point='39.53091028762164,-105.75927125216293,3221.245650987822' remarks='' relation='c'/><link uid='23a5e2db-a00a-47b3-addd-450ff2982360' callsign='CP1' type='b-m-p-w' point='39.529834273062676,-105.75886674741635,3175.9541488988066' remarks='' relation='c'/><link uid='1d9bd771-708e-4793-a6d9-7e7d5dac9d18' callsign='' type='b-m-p-c' point='39.53053285482249,-105.75811364508328,3197.533593376215' remarks='' relation='c'/><link uid='7ac29e4e-a9df-463d-8aa3-3ae18aac0953' callsign='' type='b-m-p-c' point='39.53113873378954,-105.7575973260165,3206.9957389306155' remarks='' relation='c'/><link uid='bda0fba4-8e9d-46ca-8f95-a7bcdad2a7b6' callsign='CP2' type='b-m-p-w' point='39.53023488156001,-105.75715664604867,3178.162436818524' remarks='' relation='c'/><link uid='0ff20d3a-dcef-43a3-a4c5-3ef11ca0440d' callsign='' type='b-m-p-c' point='39.530456706099855,-105.75633777058607,3171.74040652843' remarks='' relation='c'/><link uid='8d633813-f589-4a5a-8cd1-0d61806b0c71' callsign='' type='b-m-p-c' point='39.531188395999955,-105.75618978104464,3183.9218981426966' remarks='' relation='c'/><link uid='2e81f497-651f-4fba-8b45-85d102c9d642' callsign='' type='b-m-p-c' point='39.53076461180442,-105.75506834918622,3140.0895940184528' remarks='' relation='c'/><link uid='37b4e06d-d8ab-4e7c-b6e1-5a689a8b04ff' callsign='TGT' type='b-m-p-w' point='39.52982102980656,-105.75470659697383,3097.669497337116' remarks='' relation='c'/><link_attr planningmethod='Infil' color='-16776961' method='Walking' prefix='CP' type='On Foot' stroke='3' direction='Infil' routetype='Primary' order='Ascending Check Points'/><labels_on value='false'/><__routeinfo><__navcues><__navcue voice='Speed Up' id='bda0fba4-8e9d-46ca-8f95-a7bcdad2a7b6' text='Speed Up'><trigger mode='d' value='70'/></__navcue><__navcue voice='Stop' id='23a5e2db-a00a-47b3-addd-450ff2982360' text='Stop'><trigger mode='d' value='70'/></__navcue></__navcues></__routeinfo><archive/><color value='-16776961'/><remarks>Butts</remarks><strokeColor value='-16776961'/><strokeWeight value='3.0'/><contact callsign='Route 7'/></detail></event>");
    }

    public void testSensor() {
        validate("Sensor", 305, "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><event version='2.0' uid='53dc0c04-d84a-473b-9128-f241350693ef' type='b-m-p-s-p-loc' time='2020-08-29T21:22:06.524Z' start='2020-08-29T21:22:06.524Z' stale='2020-09-15T22:02:35.452Z' how='h-g-i-g-o'><point lat='39.72268266' lon='-105.002122' hae='1580.88835768' ce='9999999.0' le='9999999.0'/><detail><status readiness='true'/><archive/><contact callsign='Sensor Bale'/><remarks>Hi</remarks><archive/><link uid='ANDROID-355499060918435' production_time='2020-08-29T21:18:50.381Z' type='a-f-G-U-C' parent_callsign='maya' relation='p-p'/><precisionlocation altsrc='DTED2'/><sensor fovGreen='0.0' fovBlue='0.0' fovRed='1.0' range='699' azimuth='78' displayMagneticReference='0' fov='130' hideFov='true' fovAlpha='0.44'/><color argb='-1'/><__video uid='efd8f175-9732-4a2a-a7dc-fc7f2c803ed2' url=':6636'><ConnectionEntry networkTimeout='13000' uid='efd8f175-9732-4a2a-a7dc-fc7f2c803ed2' path='' protocol='udp' bufferTime='3000' address='' port='6636' roverPort='-1' rtspReliable='0' ignoreEmbeddedKLV='false' alias='about name'/></__video></detail></event>");
    }


    public void testSimpleShape() {
        validate("Simple Shape", 252, "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><event version='2.0' uid='10a20222-0678-49b8-b0cf-793d3340528b' type='a-h-G' time='2020-08-29T21:17:31.377Z' start='2020-08-29T21:17:31.377Z' stale='2020-08-29T21:22:31.377Z' how='h-g-i-g-o'><point lat='39.72871028603934' lon='-104.99686216622815' hae='1588.0681247063874' ce='30.0' le='9999999.0'/><detail><status readiness='true'/><archive/><contact callsign='Red Market'/><remarks>Remarks about these bitches</remarks><archive/><link uid='ANDROID-53af0912586418dc' production_time='2020-08-29T21:16:36.870Z' type='a-f-G-U-C' parent_callsign='dasuberdog' relation='p-p'/><height_unit>1</height_unit><height unit='meters' value='6.0'>6.0</height><precisionlocation altsrc='DTED2' geopointsrc='USER'/><ce_human_input>true</ce_human_input><color argb='-1'/><usericon iconsetpath='COT_MAPPING_2525B/a-h/a-h-G'/></detail></event>");
    }


    public void testUserIcon() {
        validate("User Icon", 258, "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><event version='2.0' uid='eaa09ae4-50e0-4052-909a-9a5e3cbe905c' type='a-u-G-E-W' time='2020-08-29T21:26:41.476Z' start='2020-08-29T21:26:41.476Z' stale='2020-08-29T21:31:41.476Z' how='h-g-i-g-o'><point lat='39.73047153452518' lon='-104.97315124278549' hae='1607.5676218634023' ce='9999999.0' le='9999999.0'/><detail><status readiness='true'/><archive/><contact callsign='bombing 1'/><remarks>Lol lol</remarks><archive/><link uid='ANDROID-53af0912586418dc' production_time='2020-08-29T21:26:23.123Z' type='a-f-G-U-C' parent_callsign='dasuberdog' relation='p-p'/><precisionlocation altsrc='DTED2'/><color argb='-1'/><usericon iconsetpath='34ae1613-9645-4222-a9d2-e5f243dea2865/Military/bombing.png'/></detail></event>");
    }


    public void testWaypoint() {
        validate("Waypoint", 204, "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><event version='2.0' uid='735fc2c5-2e8a-4e21-b793-a1bfef66fc28' type='b-m-p-w-GOTO' time='2020-08-29T21:22:45.171Z' start='2020-08-29T21:22:45.171Z' stale='2020-08-29T21:27:45.171Z' how='h-g-i-g-o'><point lat='39.72285198329387' lon='-104.99177830439078' hae='1594.3528328257194' ce='9999999.0' le='9999999.0'/><detail><status readiness='true'/><archive/><contact callsign='dasuberdog.29.152230'/><remarks/><archive/><link uid='ANDROID-53af0912586418dc' production_time='2020-08-29T21:22:30.997Z' type='a-f-G-U-C' parent_callsign='dasuberdog' relation='p-p'/><height_unit>4</height_unit><height unit='feet' value='0.0'>0.0</height><precisionlocation altsrc='DTED2'/><color argb='-1'/></detail></event>");
    }

    public void testCasevacAllFields() {
        validate("Casevac w/ All Fields", 696, "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><event version='2.0' uid='5100a84f-0c90-4f53-8651-3af1637b8130' type='b-r-f-h-c' time='2020-09-03T02:55:39.593Z' start='2020-09-03T02:55:39.593Z' stale='2020-09-20T03:36:08.521Z' how='h-g-i-g-o'><point lat='39.65004002280003' lon='-105.09973608933406' hae='1670.374673893096' ce='9999999.0' le='9999999.0' /><detail><status readiness='false'/><archive/><contact callsign='calllsign'/><remarks></remarks><archive/><link uid='ANDROID-53af0912586418dc' production_time='2020-09-03T02:49:05.801Z' type='a-f-G-U-C' parent_callsign='dasuberdog' relation='p-p'/><precisionlocation altsrc='DTED2'/><color argb='-1'/><_flow-tags_ AndroidMedicalLine='2020-09-03T02:55:39.593Z'/><_medevac_ us_civilian='7' freq='67MHz' winds_are_from='nw' ambulatory='5' enemy='Enemy Troops, armed escort required' zone_prot_selection='0' title='title' hoist='true' us_military='6' nonus_military='8' security='3' routine='3' ventilator='true' equipment_other='true' marked_by='E - panel of lasers' urgent='1' terrain_other_detail='other terrain features' terrain_slope_dir='S' friendlies='some friends' terrain_rough='true' terrain_other='true' litter='4' nonus_civilian='9' priority='2' epw='10' terrain_loose='true' extraction_equipment='true' medline_remarks='remark' hlz_other='other marking' hlz_remarks='ready for evac' hlz_marking='4' terrain_slope='true' casevac='false' equipment_detail='other equipment' child='11'><zMistsMap><zMist s='Pulse Radial: Has Radial&#10;Airway: No Airway' t='aleve' i='Burn Deformity' z='zap 2' title='ZMIST2' m='Radiation'/><zMist s='Skin: Cold/Clammy&#10;Pulse Radial: Has Radial&#10;Bleeding: Massive' t='TQ, cric' i='Bleeding Puncture' z='zap number 1' title='ZMIST1' m='Frag Single Burn &gt; 20%'/></zMistsMap></_medevac_></detail></event>");
    }

    public void testCasevacNoFields() {
        validate("Casevac w/ Empty Fields", 242, "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><event version='2.0' uid='daf83b93-a7b2-4f75-8dc3-73a1da6ed5a4' type='b-r-f-h-c' time='2020-09-03T02:57:42.346Z' start='2020-09-03T02:57:42.346Z' stale='2020-09-20T03:38:11.274Z' how='h-g-i-g-o'><point lat='39.65004002280003' lon='-105.09973608933406' hae='1670.374673893096' ce='9999999.0' le='9999999.0' /><detail><status readiness='false'/><archive/><contact callsign='calllsign.1'/><remarks></remarks><archive/><link uid='ANDROID-53af0912586418dc' production_time='2020-09-03T02:57:22.725Z' type='a-f-G-U-C' parent_callsign='dasuberdog' relation='p-p'/><precisionlocation altsrc='DTED2'/><color argb='-1'/><_flow-tags_ AndroidMedicalLine='2020-09-03T02:57:42.349Z'/><_medevac_ medline_remarks='' equipment_none='true' terrain_none='true' casevac='false' zone_prot_selection='0' title='MED.2.205722'/></detail></event>");
    }

    public void testCircle() {
        validate("Circle", -1, "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><event version='2.0' uid='df11ad17-79d8-436b-ba7f-6c12a7d147d3' type='u-d-c-c' time='2020-08-30T18:50:27.452Z' start='2020-08-30T18:50:27.452Z' stale='2020-08-31T18:50:27.452Z' how='h-e'><point lat='39.52907656661326' lon='-105.75665450279342' hae='3100.9553102169134' ce='9999999.0' le='9999999.0' /><detail><shape><ellipse major='10.557862958543073' minor='10.557862958543073' angle='360'/><link uid='df11ad17-79d8-436b-ba7f-6c12a7d147d3.Style' type='b-x-KmlStyle' relation='p-c'><Style><LineStyle><color>ff0000ff</color><width>4.0</width></LineStyle><PolyStyle><color>960000ff</color></PolyStyle></Style></link></shape><contact callsign='Drawing Circle 1'/><remarks></remarks><archive/><strokeColor value='-16776961'/><strokeWeight value='4.0'/><fillColor value='-1778384641'/><labels_on value='true'/><precisionlocation altsrc='DTED2'/></detail></event>");
    }

    public void validate(String messageType, int maxSize, String testXml) {
        CotEvent cotEvent = CotEvent.parse(testXml);

        CotEventProtobufConverter cotEventProtobufConverter = CotEventProtobufConverterFactory.createCotEventProtobufConverter();
        try {
            byte[] cotEventAsBytes = cotEventProtobufConverter.toByteArray(cotEvent);
            CotEvent convertedCotEvent = cotEventProtobufConverter.toCotEvent(cotEventAsBytes);

            // Wipe out known fudged fields
            CoordinatedTime placeholderTime = new CoordinatedTime();
            CotPoint cotPoint = new CotPoint(0.0, 0.0, 0.0, 0.0, 0.0);

            cotEvent.setTime(placeholderTime);
            cotEvent.setStart(placeholderTime);
            cotEvent.setStale(placeholderTime);
            cotEvent.setPoint(cotPoint);

            convertedCotEvent.setTime(placeholderTime);
            convertedCotEvent.setStart(placeholderTime);
            convertedCotEvent.setStale(placeholderTime);
            convertedCotEvent.setPoint(cotPoint);

            CotDetail remarksDetail = cotEvent.getDetail().getFirstChildByName(0, "remarks");
            CotDetail convertedRemarksDetail = convertedCotEvent.getDetail().getFirstChildByName(0, "remarks");
            if (remarksDetail != null) {
                remarksDetail.setAttribute("time", placeholderTime.toString());
                convertedRemarksDetail.setAttribute("time", placeholderTime.toString());
            }

            CotDetail linkDetail = cotEvent.getDetail().getFirstChildByName(0, "link");
            CotDetail convertedLinkDetail = convertedCotEvent.getDetail().getFirstChildByName(0, "link");
            if (linkDetail != null && linkDetail.getAttribute("production_time") != null) {
                linkDetail.setAttribute("production_time", placeholderTime.toString());
                convertedLinkDetail.setAttribute("production_time", placeholderTime.toString());
            }

            List<CotDetail> removeable = new ArrayList<>();
            for (CotDetail child : cotEvent.getDetail().getChildren()) {
                if (child.getElementName().equals("archive")) {
                    removeable.add(child);
                }
            }

            for (CotDetail remove : removeable) {
                cotEvent.getDetail().removeChild(remove);
            }

            for (CotDetail child : cotEvent.getDetail().getChildren()) {
                if (child.getElementName().equals("link") && child.getAttribute("remarks") != null) {
                    String[] uidSplit = child.getAttribute("uid").split("-");
                    child.setAttribute("uid", uidSplit[uidSplit.length - 1]);
                }
            }

            XmlComparer xmlComparer = new XmlComparer();
            boolean matched = xmlComparer.compareXmls(messageType, cotEvent.toString(), convertedCotEvent.toString());
            if (matched) {
                int size = cotEventAsBytes.length;
                if (size > maxSize) {
                    Log.e(TAG, messageType + " size increased past previous value: " + maxSize + ", now: " + size);
                } else if (size < maxSize) {
                    Log.d(TAG, "!!!!!!!!!!!!");
                    Log.d(TAG, messageType + " size decreased from " + maxSize + " to " + size + "! NICEWORK, Update to the new max size: " + size);
                    Log.d(TAG, "!!!!!!!!!!!!");
                } else {
                    Log.d(TAG, messageType + " size: " + cotEventAsBytes.length + " bytes!");
                }
            }
        } catch (UnknownDetailFieldException | MappingNotFoundException e) {
            Log.e(TAG, messageType + " validation failed while marshalling to XML: " + e.getMessage());
            Log.e(TAG, "original: " + cotEvent.toString());
            e.printStackTrace();
        }
    }

}
