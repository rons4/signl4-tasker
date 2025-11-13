import java.net.Socket;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;

// ====================== CONFIGURATION ======================

String host      = "192.168.2.100";
int    port      = 1883;
String clientId  = "TaskerClient";      // choose any unique ID
String topic     = "android/sensor";         // change as needed

String strValue = tasker.getVariable("mqtt_value");
String json      = "{"
    + "\"source\":\"" + "android" + "\","
    + "\"value\":\"" + strValue
    + "}";  // your JSON payload

int keepAliveSec = 60;   // MQTT keep-alive
// ===========================================================

// Helper: encode MQTT "Remaining Length" field (variable-length)
void encodeRemainingLength(int length, ByteArrayOutputStream out) throws Exception {
    do {
        int digit = length % 128;
        length = length / 128;
        if (length > 0) {
            digit = digit | 0x80;
        }
        out.write(digit);
    } while (length > 0);
}

// Helper: MQTT UTF-8 string format: 2-byte length + bytes
void writeUTF(ByteArrayOutputStream out, String s) throws Exception {
    byte[] utf8 = s.getBytes("UTF-8");
    int len = utf8.length;
    out.write((len >> 8) & 0xFF);
    out.write(len & 0xFF);
    out.write(utf8);
}

// Build MQTT CONNECT packet (protocol MQTT 3.1.1)
byte[] buildConnectPacket(String clientId, int keepAliveSec) throws Exception {
    ByteArrayOutputStream vh = new ByteArrayOutputStream();  // variable header
    ByteArrayOutputStream pl = new ByteArrayOutputStream();  // payload

    // Protocol Name "MQTT"
    writeUTF(vh, "MQTT");
    // Protocol Level = 4 (MQTT 3.1.1)
    vh.write(4);
    // Connect Flags: clean session = 1, others 0
    vh.write(0x02);
    // Keep Alive (seconds) - 2 bytes
    vh.write((keepAliveSec >> 8) & 0xFF);
    vh.write(keepAliveSec & 0xFF);

    // Payload: Client ID
    writeUTF(pl, clientId);

    byte[] vhBytes = vh.toByteArray();
    byte[] plBytes = pl.toByteArray();
    int remainingLength = vhBytes.length + plBytes.length;

    ByteArrayOutputStream pkt = new ByteArrayOutputStream();
    // Fixed header: CONNECT (0x10)
    pkt.write(0x10);
    encodeRemainingLength(remainingLength, pkt);
    // Variable header + payload
    pkt.write(vhBytes);
    pkt.write(plBytes);

    return pkt.toByteArray();
}

// Build MQTT PUBLISH packet (QoS 0, no retain)
byte[] buildPublishPacket(String topic, String payload) throws Exception {
    ByteArrayOutputStream vh = new ByteArrayOutputStream();  // variable header
    ByteArrayOutputStream pl = new ByteArrayOutputStream();  // payload

    // Topic name (UTF-8 string)
    writeUTF(vh, topic);

    // Payload
    byte[] payloadBytes = payload.getBytes("UTF-8");
    pl.write(payloadBytes);

    byte[] vhBytes = vh.toByteArray();
    byte[] plBytes = pl.toByteArray();

    int remainingLength = vhBytes.length + plBytes.length;

    ByteArrayOutputStream pkt = new ByteArrayOutputStream();
    // Fixed header: PUBLISH, DUP=0, QoS=0, RETAIN=0 -> 0x30
    pkt.write(0x30);
    encodeRemainingLength(remainingLength, pkt);

    pkt.write(vhBytes);
    pkt.write(plBytes);

    return pkt.toByteArray();
}

// Build MQTT DISCONNECT packet
byte[] buildDisconnectPacket() throws Exception {
    // Fixed header: DISCONNECT (0xE0), remaining length = 0
    return new byte[] { (byte)0xE0, 0x00 };
}

// ====================== MAIN LOGIC ======================

Socket socket = null;

try {
    socket = new Socket(host, port);
    OutputStream out = socket.getOutputStream();
    InputStream  in  = socket.getInputStream();

    // 1) CONNECT
    byte[] connectPacket = buildConnectPacket(clientId, keepAliveSec);
    out.write(connectPacket);
    out.flush();

    // Optionally read CONNACK (we just read a few bytes and ignore)
    socket.setSoTimeout(1000); // 1s timeout
    try {
        byte[] buf = new byte[4];
        int read = in.read(buf);
        // You could check buf[0] == 0x20 (CONNACK), buf[3] == 0x00 (success)
    } catch (Exception e) {
        // Ignore timeout or read error here
    }

    // 2) PUBLISH JSON message
    byte[] publishPacket = buildPublishPacket(topic, json);
    out.write(publishPacket);
    out.flush();

    // Small delay to ensure broker processes packet
    Thread.sleep(100);

    // 3) DISCONNECT
    byte[] disconnectPacket = buildDisconnectPacket();
    out.write(disconnectPacket);
    out.flush();

} catch (Exception e) {
    // Print to Tasker log
    System.out.println("MQTT error: " + e.toString());
    e.printStackTrace();
} finally {
    if (socket != null) {
        try { socket.close(); } catch (Exception ignore) {}
    }
}
